package com.jarvis.app.live

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import com.jarvis.app.data.AppLog
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

/**
 * The ONE owner of the microphone. Everything that needs mic PCM (the Gemini sender, the in-app recorder, the level
 * meter) attaches as a listener; nothing else in the app opens an AudioRecord for a live call.
 *
 * It keeps the audio source constant (VOICE_COMMUNICATION while the call is up) and watches for the exact-zero
 * buffer case (another app / the system recorder holding the mic, or the source going silent). Recovery re-creates
 * the AudioRecord on the SAME source first (so the echo canceller stays), and only moves to another source after
 * two failed tries — and then it says so (onAecLost), because a source without AEC is what turns the assistant's own
 * voice on the speaker into "the user is speaking again".
 */
object MicCaptureHub {
    const val RATE = 16000
    private const val READ_BYTES = 3200          // ~100 ms per buffer
    private const val SILENT_BUFFERS = 15        // ~1.5 s of exact-zero audio before we call it silent
    private const val SAME_SOURCE_TRIES = 2      // attempts 1..2 stay on VOICE_COMMUNICATION (with its echo canceller)
    private const val MAX_ATTEMPTS = 6           // after this we stop hammering the microphone

    /** Audio sources tried in order; the first that yields real audio is kept. */
    private val SOURCES = intArrayOf(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.DEFAULT
    )
    private fun srcName(src: Int) = when (src) {
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
        else -> "src$src"
    }

    private val listeners = CopyOnWriteArrayList<(ByteArray, Int) -> Unit>()

    private var appCtx: Context? = null
    private var rec: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private var sourceIdx = 0
    // The effect objects MUST be held: an unreferenced AudioEffect can be released by the garbage collector and the
    // echo canceller then stops working silently — which is exactly what makes the assistant interrupt itself.
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    @Volatile var sourceName = "-"; private set
    @Volatile var level = 0f; private set
    @Volatile var silent = false; private set
    @Volatile var foreignRecorder = "-"; private set
    @Volatile var aecAvailable = false; private set
    @Volatile var aecEnabled = false; private set          // echo canceller really attached + enabled on the current record
    @Volatile var aecLost = false; private set             // we had to move off VOICE_COMMUNICATION: echo cancellation is gone

    /** Called when the silent state flips (so the UI can show/hide the warning). */
    @Volatile var onSilent: ((Boolean) -> Unit)? = null

    /** Called when echo cancellation is lost (the source had to change): the UI shows a warning and turns on half-duplex. */
    @Volatile var onAecLost: ((Boolean) -> Unit)? = null

    val isRunning: Boolean get() = running

    fun addListener(l: (ByteArray, Int) -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: (ByteArray, Int) -> Unit) { listeners.remove(l) }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start(): Boolean {
        if (running) return true
        sourceIdx = 0
        aecLost = false
        val r = open(SOURCES[sourceIdx])
        if (r == null) { Diag.log("mic", "MicCaptureHub: AudioRecord could not be created"); return false }
        rec = r; running = true; silent = false
        try { r.startRecording() } catch (e: Exception) { Diag.fail("mic.start", e); closeRec(); running = false; return false }
        Diag.micOpens++; Diag.micSource = sourceName; Diag.micState = "recording"
        Diag.log("mic", "MicCaptureHub START source=$sourceName rate=$RATE aec=$aecEnabled listeners=${listeners.size}")
        thread = Thread({ loop() }, "jarvis-mic").also { it.start() }
        return true
    }

    @Synchronized
    fun stop() {
        if (!running && rec == null) return
        running = false
        try { thread?.join(400) } catch (e: Exception) { AppLog.w(e) }
        thread = null
        closeRec()
        silent = false; level = 0f
        Diag.micReleases++; Diag.micState = "idle"; Diag.micLevel = 0f
        Diag.log("mic", "MicCaptureHub STOP")
    }

    @SuppressLint("MissingPermission")
    private fun open(src: Int): AudioRecord? {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val r = try {
            AudioRecord(src, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, RATE))
        } catch (e: Exception) { Diag.fail("mic.open(${srcName(src)})", e); return null }
        if (r.state != AudioRecord.STATE_INITIALIZED) { Diag.log("mic", "open(${srcName(src)}) not initialized"); try { r.release() } catch (e: Exception) { }; return null }
        sourceName = srcName(src)

        // Echo canceller + noise suppressor. We KEEP both objects in fields: if the only reference is local the
        // garbage collector may release the effect and the echo canceller silently disappears mid-call.
        aecAvailable = try { AcousticEchoCanceler.isAvailable() } catch (e: Exception) { false }
        aecEnabled = false
        if (aecAvailable) {
            try {
                val e = AcousticEchoCanceler.create(r.audioSessionId)
                if (e != null) { e.enabled = true; aec = e; aecEnabled = e.enabled }
            } catch (e: Exception) { AppLog.w(e); aec = null }
        }
        var nsOn = false
        try {
            if (NoiseSuppressor.isAvailable()) {
                val e = NoiseSuppressor.create(r.audioSessionId)
                if (e != null) { e.enabled = true; ns = e; nsOn = e.enabled }
            }
        } catch (e: Exception) { AppLog.w(e); ns = null }
        Diag.log("mic", "open(${srcName(src)}) session=${r.audioSessionId} aecAvailable=$aecAvailable aecCreated=${aec != null} aecEnabled=$aecEnabled nsEnabled=$nsOn")
        return r
    }

    private fun closeRec() {
        try { aec?.let { it.enabled = false; it.release() } } catch (e: Exception) { AppLog.w(e) }
        aec = null; aecEnabled = false
        try { ns?.let { it.enabled = false; it.release() } } catch (e: Exception) { AppLog.w(e) }
        ns = null
        val r = rec ?: return
        rec = null
        try { r.stop() } catch (e: Exception) { AppLog.w(e) }
        try { r.release() } catch (e: Exception) { AppLog.w(e) }
    }

    private fun loop() {
        try { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (e: Exception) { AppLog.w(e) }
        val buf = ByteArray(READ_BYTES)
        var zeroRun = 0
        Diag.log("mic", "capture thread started")
        while (running) {
            val r = rec ?: break
            val n = try { r.read(buf, 0, buf.size) } catch (e: Exception) { Diag.fail("mic.read", e); -1 }
            if (n <= 0) { if (n < 0) { Diag.log("mic", "read error $n"); try { Thread.sleep(20) } catch (e: Exception) { } }; continue }

            // peak level + exact-zero detection
            var allZero = true
            var sum = 0.0
            var k = 0
            while (k + 1 < n) {
                val s = ((buf[k + 1].toInt() shl 8) or (buf[k].toInt() and 0xFF)).toShort().toInt()
                if (s != 0) allZero = false
                sum += s.toDouble() * s; k += 2
            }
            val rms = sqrt(sum / (n / 2).coerceAtLeast(1)) / 32768.0
            val lvl = (rms * 7).toFloat().coerceIn(0f, 1f)
            level = lvl; Diag.micLevel = lvl

            if (allZero) zeroRun++ else zeroRun = 0
            val nowSilent = zeroRun >= SILENT_BUFFERS
            if (nowSilent != silent) {
                silent = nowSilent
                if (nowSilent) { Diag.micZero(); Diag.foreignRecorder = foreignRecorder() } else Diag.log("mic", "audio returned (level=${"%.3f".format(lvl)})")
                onSilent?.invoke(nowSilent)
            }

            // fan-out to every consumer (Gemini sender, in-app recorder, ...)
            for (l in listeners) { try { l(buf, n) } catch (e: Exception) { Diag.fail("mic.listener", e) } }

            if (nowSilent) recover(zeroRun).also { if (it) zeroRun = 0 }
        }
        Diag.log("mic", "capture thread ended")
    }

    /**
     * One recovery step per silent cycle. Attempts 1-2 re-create the AudioRecord on the SAME source, so the echo
     * canceller is kept. From attempt 3 we move to the next source — and only then do we admit echo cancellation is
     * gone (onAecLost), which the UI turns into a warning + half-duplex.
     */
    private fun recover(zeroRun: Int): Boolean {
        if (zeroRun % SILENT_BUFFERS != 0) return false          // don't retry on every silent buffer
        val attempt = zeroRun / SILENT_BUFFERS
        if (attempt > MAX_ATTEMPTS) return false
        val keepSource = attempt <= SAME_SOURCE_TRIES
        if (!keepSource) {
            sourceIdx = (sourceIdx + 1) % SOURCES.size
            if (!aecLost) {
                aecLost = true
                Diag.micAecLost++
                Diag.log("mic", "source had to change -> echo cancellation LOST (warning + half-duplex)")
                onAecLost?.invoke(true)
            }
        }
        Diag.log("mic", "silent: recovery attempt $attempt/$MAX_ATTEMPTS source=${srcName(SOURCES[sourceIdx])} sameSource=$keepSource")
        closeRec()
        if (attempt > 1) { try { Thread.sleep(250L * attempt) } catch (e: Exception) { } }
        val r = open(SOURCES[sourceIdx]) ?: return false
        rec = r
        try { r.startRecording() } catch (e: Exception) { Diag.fail("mic.recover.start", e); closeRec(); return false }
        Diag.micOpens++; Diag.micSource = sourceName
        // probe ~600 ms: any non-zero sample means the mic is alive again
        val probe = ByteArray(READ_BYTES)
        val t0 = System.currentTimeMillis()
        var voiced = false
        while (System.currentTimeMillis() - t0 < 600) {
            val m = try { r.read(probe, 0, probe.size) } catch (e: Exception) { -1 }
            if (m > 0 && probe.any { it.toInt() != 0 }) { voiced = true; break }
        }
        if (voiced) {
            Diag.micRecoveries++; Diag.log("mic", "RECOVERED with source=$sourceName aec=$aecEnabled")
            silent = false; level = 0f
            onSilent?.invoke(false)
            return true
        }
        Diag.log("mic", "source=$sourceName still silent (aec=$aecEnabled) — keeping it open for the next try")
        return false
    }

    /** Best-effort: is another app also recording (i.e. holding/silencing the mic)? Uses public recording APIs only. */
    fun foreignRecorder(): String {
        val ctx = appCtx ?: return "-"
        return try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val configs = am.activeRecordingConfigurations.orEmpty()
            val captures = configs.count {
                it.clientAudioSource == MediaRecorder.AudioSource.MIC ||
                    it.clientAudioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION ||
                    it.clientAudioSource == MediaRecorder.AudioSource.DEFAULT
            }
            val silenced = Build.VERSION.SDK_INT >= 29 && configs.any { it.isClientSilenced }
            when {
                captures > 1 -> "another app is recording ($captures active captures)"
                silenced -> "another app is silencing Jarvis's mic"
                else -> "-"
            }
        } catch (e: Exception) { Diag.fail("mic.foreignRecorder", e); "-" }
    }

    fun setContext(ctx: Context) { if (appCtx == null) appCtx = ctx.applicationContext }
}
