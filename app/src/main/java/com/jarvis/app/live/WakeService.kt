package com.jarvis.app.live

import com.jarvis.app.data.AppLog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jarvis.app.AssistantActivity
import com.jarvis.app.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.jarvis.app.data.Prefs
import java.io.File

/**
 * Listens in the background for your wake phrase (any name / command you chose). When heard, the assistant orb opens over the
 * current screen (like Siri). Uses Android's speech recognizer in a loop, so it needs internet and shows the mic indicator.
 */
class WakeService : Service() {
    companion object {
        @Volatile var pausedState = false
        @Volatile var running = false
        @Volatile var micOpen = false                     // true while the wake engine really has the mic open (evidence)
        @Volatile var forcedPause = false                 // paused only because a call / the app's own recorder has the mic
        @Volatile var engineError: String? = null      // shown in Assistant settings
        @Volatile var engineName: String = ""
        private var inst: WakeService? = null

        /**
         * Pause = release the microphone right now (used by the live call and the assistant panel).
         *
         * Round 3 (§4 background mic): the wake engine must NEVER hold the mic while a live call — or the app's own
         * mic hub — owns it. Two AudioRecords in the same app make one of them read exact zeros, which is what killed
         * the call mic a few seconds after `press_button home`. So a "resume" while the mic is busy stays paused
         * (`forcedPause`), and the tick releases it by itself as soon as the mic is free again.
         */
        fun setPaused(p: Boolean) {
            val busy = CallHost.active || MicCaptureHub.isRunning
            val forced = !p && busy
            pausedState = p || busy
            forcedPause = forced
            val why = when {
                !pausedState -> "resumed -> wake word may listen again"
                forced -> "resume ignored -> ${if (CallHost.active) "a live call" else "Jarvis's own recorder"} owns the mic"
                else -> "paused -> releasing the mic for the call"
            }
            Diag.log("wake", why)
            if (pausedState) inst?.let { s -> s.main.post { s.destroyRec(); s.pausePm(); s.schedule(1000) } }
        }

        /** Called once per wake tick: frees a pause that only existed because the mic was busy. */
        fun releaseForcedPause() {
            if (forcedPause && !CallHost.active && !MicCaptureHub.isRunning) {
                forcedPause = false
                pausedState = false
                Diag.log("wake", "the mic is free again -> wake word resumes by itself")
            }
        }

        fun start(ctx: Context) {
            val i = Intent(ctx, WakeService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, WakeService::class.java)) }

        private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

        private fun lev(a: String, b: String): Int {
            if (a == b) return 0
            var prev = IntArray(b.length + 1) { it }
            for (i in 1..a.length) {
                val cur = IntArray(b.length + 1); cur[0] = i
                for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = cur
            }
            return prev[b.length]
        }

        /** Exact match, or a close match (speech recognizers often hear "my up" for "my app"). */
        fun matches(heard: String, phrase: String): Boolean {
            val h = norm(heard); val p = norm(phrase)
            if (p.isEmpty() || h.isEmpty()) return false
            val pc = p.replace(" ", "")
            if (h.contains(p) || h.replace(" ", "").contains(pc)) return true
            if (pc.length < 4) return false
            val hw = h.split(" ")
            val n = p.split(" ").size
            val allowed = maxOf(1, pc.length / 4)
            for (size in maxOf(1, n - 1)..(n + 1)) {
                if (size > hw.size) continue
                for (i in 0..(hw.size - size)) {
                    val w = hw.subList(i, i + size).joinToString("")
                    if (lev(w, pc) <= allowed) return true
                }
            }
            return false
        }

        /** Language used to hear the wake phrase: the one you picked, otherwise guessed from the phrase's script. */
        fun wakeLocale(): String {
            if (Prefs.wakeLang != "auto") return Prefs.wakeLang
            val p = Prefs.wakePhrase
            return when {
                p.any { it in '\u0600'..'\u06FF' } -> if (Prefs.lang == "ar-SA") "ar-SA" else "ur-PK"
                p.any { it in '\u0900'..'\u097F' } -> "hi-IN"
                p.any { it in '\u0400'..'\u04FF' } -> "ru-RU"
                else -> if (Prefs.lang in listOf("en-US", "es-ES", "fr-FR", "tr-TR")) Prefs.lang else "en-US"
            }
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private var rec: SpeechRecognizer? = null
    private val am by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val mutedStreams = mutableListOf<Int>()
    private val unmuteTask = Runnable { unmuteBeeps() }
    private val loop = Runnable { listen() }
    @Volatile private var foreignMic = false
    private var pm: PorcupineManager? = null
    private var pmActive = false
    private var usingPorcupine = false
    private var usingVosk = false
    private var vosk: SpeechService? = null
    private var voskModel: Model? = null
    private var voskActive = false

    /** Offline phrase engine (Vosk): listens for YOUR phrase on the phone itself. No key, no internet, no start/stop beeps. */
    private fun startVosk(): Boolean {
        engineError = null
        if (Prefs.wakePhrase.isBlank()) { engineError = "Type your wake phrase first"; return false }
        if (!VoskModels.ready(this, Prefs.voskLang)) { engineError = "Download the offline voice model first (Assistant settings)"; return false }
        return try {
            val m = Model(VoskModels.dir(this, Prefs.voskLang).absolutePath)
            val grammar = JSONArray().put(Prefs.wakePhrase.lowercase().trim()).put("[unk]").toString()   // only your phrase is listened for
            vosk = SpeechService(Recognizer(m, 16000f, grammar), 16000f)
            voskModel = m; voskActive = false
            engineName = "offline phrase"
            true
        } catch (e: Exception) { AppLog.e("WakeService", e); engineError = e.message ?: "Offline engine could not start"; false }
    }

    private val voskListener = object : org.vosk.android.RecognitionListener {
        override fun onPartialResult(hypothesis: String?) = heard(hypothesis)
        override fun onResult(hypothesis: String?) = heard(hypothesis)
        override fun onFinalResult(hypothesis: String?) = heard(hypothesis)
        override fun onError(exception: Exception?) { if (exception != null) AppLog.w(exception) }
        override fun onTimeout() { }
        private fun heard(json: String?) {
            val t = try { JSONObject(json ?: return).let { it.optString("text").ifEmpty { it.optString("partial") } } } catch (e: Exception) { return }
            if (t.isNotBlank() && matches(t, Prefs.wakePhrase)) main.post { trigger() }
        }
    }

    private fun voskTick() {
        releaseForcedPause()
        // Round 3 (§4): !CallHost.active so the wake engine can never steal the mic from a running call.
        val shouldRun = Prefs.wakeEnabled && !pausedState && !foreignMic && !inPhoneCall() && !CallHost.active
        try {
            if (shouldRun && !voskActive) { vosk?.startListening(voskListener); voskActive = true; micOpen = true; Diag.log("wake", "vosk startListening (mic OPEN)") }
            if (!shouldRun && voskActive) { vosk?.stop(); voskActive = false; micOpen = false; Diag.log("wake", "vosk stop (mic CLOSED)") }
        } catch (e: Exception) { AppLog.w(e); engineError = e.message }
        schedule(1000)
    }

    private fun stopVosk() {
        try { vosk?.stop() } catch (e: Exception) { AppLog.w(e) }
        try { vosk?.shutdown() } catch (e: Exception) { AppLog.w(e) }
        try { voskModel?.close() } catch (e: Exception) { AppLog.w(e) }
        vosk = null; voskModel = null; voskActive = false
    }

    /** Offline wake word (Picovoice Porcupine): listens on-device, never restarts, so there are no mic on/off sounds. */
    private fun startPorcupine(): Boolean {
        engineError = null
        if (Prefs.pvKey.isBlank()) { engineError = "Add your free Picovoice AccessKey to use the offline wake word"; return false }
        return try {
            val b = PorcupineManager.Builder().setAccessKey(Prefs.pvKey.trim()).setSensitivity(0.7f)
            if (Prefs.pvKeyword == "custom") {
                val f = File(filesDir, "wake.ppn")
                if (!f.exists()) { engineError = "Import your custom .ppn wake word file first"; return false }
                b.setKeywordPath(f.absolutePath)
            } else b.setKeyword(Porcupine.BuiltInKeyword.valueOf(Prefs.pvKeyword))
            pm = b.build(applicationContext, PorcupineManagerCallback { _ -> main.post { trigger() } })
            pmActive = false
            Diag.log("wake", "porcupine built (mic OPEN)")
            engineName = if (Prefs.pvKeyword == "custom") "custom word" else Prefs.pvKeyword.replace('_', ' ').lowercase()
            true
        } catch (e: PorcupineException) {
            AppLog.e("WakeService", e); engineError = e.message ?: "Wake engine could not start"; false
        } catch (e: Exception) {
            AppLog.w(e); engineError = e.message ?: "Wake engine could not start"; false
        }
    }

    private fun porcupineTick() {
        releaseForcedPause()
        // Round 3 (§4): !CallHost.active so the wake engine can never steal the mic from a running call.
        val shouldRun = Prefs.wakeEnabled && !pausedState && !foreignMic && !inPhoneCall() && !CallHost.active
        try {
            if (shouldRun && !pmActive) { pm?.start(); pmActive = true; micOpen = true; Diag.log("wake", "porcupine start (mic OPEN)") }
            if (!shouldRun && pmActive) { pm?.stop(); pmActive = false; micOpen = false; Diag.log("wake", "porcupine stop (mic CLOSED)") }
        } catch (e: Exception) { AppLog.w(e); engineError = e.message }
        schedule(1000)
    }
    private var recCbRegistered = false

    /** Another app is in a call (WhatsApp, Meet, phone): release the microphone and come back when it ends. */
    private val recCb = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            val busy = !CallHost.active && configs.orEmpty().any {
                it.clientAudioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION || it.clientAudioSource == MediaRecorder.AudioSource.VOICE_CALL
            }
            if (busy != foreignMic) { foreignMic = busy; if (busy) destroyRec() else schedule(800) }   // porcupineTick pauses/resumes itself
        }
    }

    private fun inPhoneCall(): Boolean {
        if (CallHost.active) return false
        val m = am.mode
        return m == AudioManager.MODE_IN_CALL || m == AudioManager.MODE_IN_COMMUNICATION ||
            (Build.VERSION.SDK_INT >= 29 && m == AudioManager.MODE_CALL_SCREENING)
    }
    private fun schedule(delay: Long) { main.removeCallbacks(loop); main.postDelayed(loop, delay) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Prefs.init(this)
        inst = this
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("wake", "Wake word", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "wake").setContentTitle("${Prefs.assistantName} is listening")
            .setContentText(if (Prefs.wakeEngine == "vosk" || (Prefs.wakeEngine == "porcupine" && Prefs.pvKey.isNotBlank())) "Say your wake word to start" else "Say “${Prefs.wakePhrase}” to start").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open).setOngoing(true).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(88, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) else startForeground(88, n)
        } catch (e: Exception) { AppLog.w(e); stopSelf(); return START_NOT_STICKY }
        running = true
        if (!recCbRegistered) { try { am.registerAudioRecordingCallback(recCb, main); recCbRegistered = true } catch (e: Exception) { AppLog.w(e); } }
        main.removeCallbacksAndMessages(null)
        stopPorcupine(); stopVosk()
        usingVosk = Prefs.wakeEngine == "vosk" && startVosk()
        usingPorcupine = Prefs.wakeEngine == "porcupine" && startPorcupine()
        if (!usingVosk && !usingPorcupine && Prefs.wakeEngine != "basic") AppLog.e("WakeService", "Offline wake word not running: $engineError. Falling back to the basic speech loop.")
        schedule(0)
        return START_STICKY
    }

    // ---- silence the recognizer's start/stop "tonk" sounds (only for a moment, so music and ringtones are not affected) ----
    private fun muteBeeps(forMs: Long) {
        main.removeCallbacks(unmuteTask)
        if (mutedStreams.isEmpty()) {
            val streams = if (am.isMusicActive) listOf(AudioManager.STREAM_SYSTEM, AudioManager.STREAM_NOTIFICATION)
            else listOf(AudioManager.STREAM_SYSTEM, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_MUSIC)
            for (s in streams) {
                try {
                    if (!am.isStreamMute(s)) { am.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0); mutedStreams.add(s) }
                } catch (e: Exception) { AppLog.w(e); }
            }
        }
        main.postDelayed(unmuteTask, forMs)
    }

    private fun unmuteBeeps() {
        for (s in mutedStreams) { try { am.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0) } catch (e: Exception) { AppLog.w(e); } }
        mutedStreams.clear()
    }

    private fun listen() {
        if (!running) return
        releaseForcedPause()
        if (usingVosk) { voskTick(); return }
        if (usingPorcupine) { porcupineTick(); return }
        if (pausedState || foreignMic || inPhoneCall() || CallHost.active || !Prefs.wakeEnabled || Prefs.wakePhrase.isBlank()) { destroyRec(); schedule(1500); return }
        destroyRec()
        muteBeeps(1100)
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        rec = r
        micOpen = true
        Diag.log("wake", "basic SpeechRecognizer created (mic OPEN)")
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { muteBeeps(900) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { muteBeeps(1000) }
            override fun onError(error: Int) {
                muteBeeps(1000)
                restart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1200 else 200)
            }
            override fun onResults(results: Bundle?) {
                muteBeeps(1000)
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (list.any { matches(it, Prefs.wakePhrase) }) trigger() else restart(150)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (list.any { matches(it, Prefs.wakePhrase) }) trigger()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, wakeLocale())
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, wakeLocale())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        try { r.startListening(i) } catch (e: Exception) { AppLog.w(e); restart(1000) }
    }

    private fun restart(delay: Long) { destroyRec(); schedule(delay) }

    private fun trigger() {
        if (pausedState || CallHost.active) return
        pausedState = true
        destroyRec()
        pausePm()
        AppLog.i("WakeService", "Wake word heard")
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) { AppLog.w(e); }
        val intent = Intent(this, AssistantActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // 1) direct start (works when "Display over other apps" is allowed)  2) full-screen notification as a backup
        try { startActivity(intent) } catch (e: Exception) { AppLog.w(e); }
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("assistant", "Assistant", NotificationManager.IMPORTANCE_HIGH))
            val pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            nm.notify(89, Notification.Builder(this, "assistant").setContentTitle(Prefs.assistantName).setContentText("Tap to talk")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).setCategory(Notification.CATEGORY_CALL)
                .setFullScreenIntent(pi, true).setContentIntent(pi).setAutoCancel(true).build())
        } catch (e: Exception) { AppLog.w(e); }
        // if Android blocked the background start, do not stay deaf: resume listening — but never while a call owns
        // the mic (Round 3 / §4: that is exactly how the call mic went silent).
        main.postDelayed({
            if (!AssistantActivity.alive) {
                if (CallHost.active) Diag.log("wake", "assistant did not open but a live call owns the mic -> staying paused")
                else { pausedState = false; restart(300) }
            }
        }, 3500)
    }

    private fun pausePm() {
        try { pm?.stop() } catch (e: Exception) { AppLog.w(e) }; pmActive = false
        try { vosk?.stop() } catch (e: Exception) { AppLog.w(e) }; voskActive = false
        micOpen = false
    }

    private fun stopPorcupine() {
        try { pm?.stop() } catch (e: Exception) { AppLog.w(e) }
        try { pm?.delete() } catch (e: Exception) { AppLog.w(e) }
        pm = null; pmActive = false
    }

    private fun destroyRec() {
        rec?.let { Diag.log("wake", "SpeechRecognizer destroyed (mic CLOSED)"); try { it.cancel(); it.destroy() } catch (e: Exception) { AppLog.w(e); } }
        rec = null
        micOpen = false
    }

    override fun onDestroy() {
        running = false; inst = null; micOpen = false
        main.removeCallbacksAndMessages(null)
        if (recCbRegistered) { try { am.unregisterAudioRecordingCallback(recCb) } catch (e: Exception) { AppLog.w(e); } }
        stopPorcupine(); stopVosk()
        unmuteBeeps(); destroyRec()
        super.onDestroy()
    }
}
