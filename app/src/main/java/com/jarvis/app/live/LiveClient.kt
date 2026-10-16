package com.jarvis.app.live

import com.jarvis.app.data.AppLog
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.DeviceStats
import com.jarvis.app.data.Langs
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.Voices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Real-time voice call with Gemini (Live API over WebSocket).
 * Mic PCM 16 kHz streams up continuously, Gemini's voice PCM 24 kHz streams back. Camera / screen frames are sent as JPEG.
 */
class LiveClient(private val ctx: Context, private val scope: CoroutineScope) : CallEngine() {
    /** Called when Gemini Live cannot be used at all (quota, no model ...), so a backup provider can take over the call. */
    var onUnavailable: (() -> Unit)? = null
    private var liveModels = listOf<String>()
    private var modelIdx = 0

    private val okc get() = Http.client   // one shared client: creating a new one on every call made the Live screen slow to open
    private var ws: WebSocket? = null
    private var attempt = 0
    private var gotSetup = false
    @Volatile private var connId = 0
    private var newTurn = true

    @Volatile private var recording = false
    private var recorder: AudioRecord? = null
    private var recThread: Thread? = null

    @Volatile private var playing = false
    private var track: AudioTrack? = null
    private val playQ = LinkedBlockingQueue<ByteArray>()
    private var playThread: Thread? = null
    @Volatile private var lastAudioAt = 0L

    private fun ui(block: () -> Unit) { scope.launch(Dispatchers.Main) { block() } }

    // ---------- connection ----------
    override fun start() {
        error = null; attempt = 0; modelIdx = 0; userText = ""; aiText = ""
        liveModels = ModelCatalog.liveCandidates()
        connect()
    }

    private fun connect() {
        val key = Prefs.apiKey
        if (key.isBlank()) { fail("Add your Gemini API key in Profile > Gemini API"); return }
        val model = liveModels.getOrNull(modelIdx)
        if (model == null) { fail("No Live-capable model was found for your key. Open Profile > Gemini API and tap Test & Load Models."); return }
        modelName = model; state = State.CONNECTING; gotSetup = false; info = "Connecting…"
        val req = Request.Builder()
            .url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$key").build()
        val id = ++connId
        ws = okc.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (id != connId) return
                webSocket.send(buildSetup(model).toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) { if (id == connId) handle(text) }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { if (id == connId) handle(bytes.utf8()) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (id != connId) return
                webSocket.close(1000, null); ended(code, reason)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (id != connId) return
                ended(-1, t.message ?: "connection failed")
            }
        })
    }

    private fun ended(code: Int, reason: String) {
        ui {
            if (state == State.IDLE) return@ui
            stopAudio()
            val why = reason.ifBlank { "code $code" }
            AppLog.e("LiveClient", "Live connection closed · model=$modelName · setup=$gotSetup · attempt=$attempt · code=$code · $why")
            val keyProblem = why.contains("API key", true) || why.contains("permission", true) || why.contains("API_KEY", true)
            val quota = why.contains("quota", true) || why.contains("exhausted", true) || why.contains("exceeded", true) || why.contains("billing", true)
            if (!gotSetup && !keyProblem) {
                if (!quota && attempt < 2) {
                    attempt++; info = "Retrying ($attempt)…"; ws = null; connect()
                } else if (modelIdx + 1 < liveModels.size) {
                    modelIdx++; attempt = 0; info = "Trying ${liveModels[modelIdx]}…"; ws = null; connect()      // next Live model of your key
                } else {
                    fail(if (quota) "Gemini Live has no quota left on this key ($why). Use a custom provider for calls, or try again later." else "Live connection closed: $why")
                    onUnavailable?.invoke()
                }
            } else if (!gotSetup) {
                fail("Live connection closed: $why")
            } else {
                state = State.ERROR; error = "Call ended: $why"
            }
        }
    }

    private fun fail(m: String) { ui { error = m; state = State.ERROR; stopAudio() } }

    fun systemText(): String {
        val sb = StringBuilder()
        sb.append("You are ${Prefs.assistantName}, a private AI concierge: elegant, calm, precise. ")
        sb.append("Speak ${Langs.of(Prefs.lang).english} fluently and naturally by default; switch only if the user clearly speaks another language. ")
        sb.append("This is a live voice call, so keep answers short and conversational. ")
        sb.append("If you receive camera or screen images, look at them and help with what you see. ")
        sb.append("You can control the phone with your tools (flashlight, volume, device status, weather, reading and tapping the screen, opening apps and websites) and search the web when asked. To operate other apps: open_app, wait_seconds, read_screen to see what is there, then click_on_screen / type_text / submit_text / swipe_screen / double_tap, and read_screen again to check the result. For videos use media_control (mute, play_pause) and swipe_screen up for the next video. Never say you did something before the tool confirms it. ")
        sb.append("Only end a phone call, lock the phone, take a screenshot, share the screen or open the power menu when the user clearly asks. For the power menu tell the user to tap the option themselves. To end this conversation use end_assistant_call. ")
        if (Prefs.userName.isNotBlank()) sb.append("The user's name is ${Prefs.userName}. ")
        if (Prefs.memories.isNotEmpty()) sb.append("Known facts: ${Prefs.memories.joinToString("; ")}. ")
        if (Prefs.instructions.isNotBlank()) sb.append("Custom instructions: ${Prefs.instructions} ")
        sb.append(Voices.genderRule())
        sb.append(com.jarvis.app.data.RealFacts.block())
        sb.append("For anything about the phone, weather, date or time call device_status, get_weather or get_time and report exactly what they return. ")
        if (Prefs.voiceStyle.isNotBlank()) sb.append("Speaking style: ${Prefs.voiceStyle}. ")
        return sb.toString()
    }

    private fun tools(withSearch: Boolean): JSONArray {
        val arr = JSONArray().put(PhoneTools.gemini())
        if (withSearch) arr.put(JSONObject().put("googleSearch", JSONObject()))        // live web search (Google Search grounding)
        return arr
    }

    private fun buildSetup(model: String): JSONObject {
        val speech = JSONObject().put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", Prefs.voice)))
        val setup = JSONObject().put("model", "models/$model")
        if (attempt < 3) {
            setup.put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("AUDIO")).put("speechConfig", speech))
        } else {
            setup.put("responseModalities", JSONArray().put("AUDIO")).put("speechConfig", speech)
        }
        setup.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemText()))))
        if (attempt <= 1) setup.put("inputAudioTranscription", JSONObject()).put("outputAudioTranscription", JSONObject())
        if (attempt <= 1) setup.put("tools", tools(withSearch = attempt == 0 && Prefs.webSearch))
        return JSONObject().put("setup", setup)
    }

    // ---------- incoming ----------
    private fun handle(raw: String) {
        val j = try { JSONObject(raw) } catch (e: Exception) { AppLog.w(e); return }
        if (j.has("setupComplete")) {
            gotSetup = true
            ui { state = State.LIVE; info = "Live"; startPlayback(); startAudio() }
            return
        }
        j.optJSONObject("toolCall")?.let { onTool(it) }
        val sc = j.optJSONObject("serverContent") ?: return
        if (sc.optBoolean("interrupted", false)) {
            playQ.clear()
            try { track?.pause(); track?.flush(); track?.play() } catch (e: Exception) { AppLog.w(e); }
            ui { speaking = false }
        }
        sc.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
            for (i in 0 until parts.length()) {
                val d = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
                val bytes = Base64.decode(d.optString("data"), Base64.DEFAULT)
                if (bytes.isNotEmpty()) { lastAudioAt = System.currentTimeMillis(); playQ.add(bytes) }
            }
        }
        sc.optJSONObject("inputTranscription")?.optString("text")?.let { t ->
            ui { if (newTurn) { userText = ""; aiText = ""; newTurn = false }; userText += t }
        }
        sc.optJSONObject("outputTranscription")?.optString("text")?.let { t -> ui { newTurn = false; aiText += t } }
        if (sc.optBoolean("turnComplete", false)) ui { flushTurn(); newTurn = true }
    }

    private fun onTool(tc: JSONObject) {
        val calls = tc.optJSONArray("functionCalls") ?: return
        val resp = JSONArray()
        for (i in 0 until calls.length()) {
            val c = calls.getJSONObject(i)
            val name = c.optString("name"); val args = c.optJSONObject("args") ?: JSONObject()
            val result = PhoneTools.execute(ctx, name, args)
            resp.put(JSONObject().put("id", c.optString("id")).put("name", name).put("response", JSONObject().put("result", result)))
        }
        ws?.send(JSONObject().put("toolResponse", JSONObject().put("functionResponses", resp)).toString())
    }

    // ---------- outgoing media ----------
    override fun sendJpeg(bytes: ByteArray) {
        if (state != State.LIVE) return
        ws?.send(JSONObject().put("realtimeInput", JSONObject().put("video",
            JSONObject().put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)).put("mimeType", "image/jpeg"))).toString())
    }

    override fun sendText(text: String) {
        if (state != State.LIVE) return
        ws?.send(JSONObject().put("realtimeInput", JSONObject().put("text", text)).toString())
    }

    @SuppressLint("MissingPermission")
    private fun startAudio() {
        if (recording) return
        val rate = 16000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, rate))
        } catch (e: Exception) { AppLog.w(e); fail("Microphone could not start: ${e.message}"); return }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { fail("Microphone could not start (permission?)"); return }
        try { if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(rec.audioSessionId)?.enabled = true } catch (e: Exception) { AppLog.w(e); }
        try { if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(rec.audioSessionId)?.enabled = true } catch (e: Exception) { AppLog.w(e); }
        recorder = rec; recording = true; rec.startRecording()
        recThread = thread(name = "live-mic") {
            val buf = ByteArray(3200)
            while (recording) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                var k = 0
                while (k + 1 < n) { val s = ((buf[k + 1].toInt() shl 8) or (buf[k].toInt() and 0xFF)).toShort().toInt(); sum += s.toDouble() * s; k += 2 }
                val rms = sqrt(sum / (n / 2)) / 32768.0
                val lvl = (rms * 7).toFloat().coerceIn(0f, 1f)
                ui { micLevel = if (muted) 0f else lvl }
                if (!muted) {
                    ws?.send(JSONObject().put("realtimeInput", JSONObject().put("audio",
                        JSONObject().put("data", Base64.encodeToString(buf, 0, n, Base64.NO_WRAP)).put("mimeType", "audio/pcm;rate=16000"))).toString())
                }
            }
        }
    }

    private fun startPlayback() {
        if (playing) return
        val rate = 24000
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min, rate)).setTransferMode(AudioTrack.MODE_STREAM).build()
        track = t; t.play(); playing = true
        playThread = thread(name = "live-play") {
            while (playing) {
                val b = playQ.poll(100, TimeUnit.MILLISECONDS)
                if (b == null) {
                    if (System.currentTimeMillis() - lastAudioAt > 700) ui { speaking = false; outLevel *= 0.5f }
                    continue
                }
                var sum = 0.0; var k = 0
                while (k + 1 < b.size) { val s = ((b[k + 1].toInt() shl 8) or (b[k].toInt() and 0xFF)).toShort().toInt(); sum += s.toDouble() * s; k += 2 }
                val lvl = (sqrt(sum / (b.size / 2).coerceAtLeast(1)) / 32768.0 * 6).toFloat().coerceIn(0f, 1f)
                ui { speaking = true; outLevel = lvl }
                try { t.write(b, 0, b.size) } catch (e: Exception) { AppLog.w(e); }
            }
        }
    }

    private fun stopAudio() {
        recording = false; playing = false
        try { recorder?.stop() } catch (e: Exception) { AppLog.w(e); }
        try { recorder?.release() } catch (e: Exception) { AppLog.w(e); }
        recorder = null
        try { track?.stop() } catch (e: Exception) { AppLog.w(e); }
        try { track?.release() } catch (e: Exception) { AppLog.w(e); }
        track = null; playQ.clear()
        micLevel = 0f; outLevel = 0f; speaking = false
    }

    /** Change the talking language at any time: the call reconnects with a new instruction and keeps going. */
    override fun switchLanguage(code: String) {
        Prefs.lang = code; Prefs.save()
        if (state == State.IDLE) return
        connId++
        val old = ws; ws = null
        try { old?.close(1000, "lang") } catch (e: Exception) { AppLog.w(e); }
        stopAudio(); userText = ""; aiText = ""; newTurn = true
        start()
    }

    override fun stop() {
        state = State.IDLE
        connId++
        val old = ws; ws = null
        try { old?.close(1000, "bye") } catch (e: Exception) { AppLog.w(e); }
        stopAudio()
    }
}

private object Http {
    val client: OkHttpClient by lazy { OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build() }
}
