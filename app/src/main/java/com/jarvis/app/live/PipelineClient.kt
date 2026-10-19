package com.jarvis.app.live

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import com.jarvis.app.audio.PcmPlayer
import com.jarvis.app.audio.Speaker
import com.jarvis.app.audio.WavUtil
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.ChatStore
import com.jarvis.app.data.CustomProvider
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.GeminiClient
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.OpenAiCompat
import com.jarvis.app.data.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.sqrt

/**
 * A voice call with ANY provider: your speech is cut out of the microphone, turned into text (the provider's speech model),
 * answered by the provider's chat model (which can use the phone tools), and spoken with the provider's own voice.
 * It is turn-based: it listens while you speak and talks while it answers, so wait for it to finish before speaking.
 */
class PipelineClient(private val ctx: Context, private val scope: CoroutineScope, private val provider: CustomProvider) : CallEngine() {
    private var job: Job? = null
    @Volatile private var recorder: AudioRecord? = null
    private val history = ArrayList<GeminiClient.Turn>()

    override fun start() {
        job?.cancel()
        error = null; userText = ""; aiText = ""; state = State.CONNECTING; info = "Connecting…"
        modelName = provider.name + " · " + provider.modelId
        job = scope.launch {
            try { loop() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { AppLog.e("Call", e); error = e.message ?: "The call failed"; state = State.ERROR }
            finally { releaseMic() }
        }
    }

    override fun stop() {
        job?.cancel(); job = null
        state = State.IDLE
        releaseMic()
        speaking = false; outLevel = 0f
    }

    private suspend fun loop() {
        val sp = CustomProviders.speechOf(provider)
        state = State.LIVE; info = "Listening"
        AppLog.i("Call", "Custom call started with ${provider.name} (${provider.modelId})")
        while (currentCoroutineContext().isActive) {
            val wav = listen() ?: continue
            info = "Thinking…"
            val text = transcribe(sp, wav)
            if (text.isBlank()) { info = "Listening"; continue }
            userText = text; aiText = ""
            history.add(GeminiClient.Turn("user", text, null))
            val system = ChatStore.systemPrompt(true) + " This is a spoken call: answer in one to three short sentences."
            val reply = OpenAiCompat.chatWithTools(provider, system, history.takeLast(20)) { name, args -> PhoneTools.execute(ctx, name, args) }
            aiText = reply
            history.add(GeminiClient.Turn("model", reply, null))
            flushTurn()
            speak(sp, reply)
            info = "Listening"
        }
    }

    private suspend fun transcribe(sp: CustomProvider, wav: ByteArray): String {
        val lang = Prefs.lang.substringBefore('-')
        if (sp.sttModel.isNotBlank()) return OpenAiCompat.transcribe(sp, wav, lang)
        val key = Prefs.apiKey.trim()
        if (key.isEmpty()) throw IOException("No speech-to-text. Set an STT model (for example whisper-large-v3-turbo) in this provider, or add a Gemini key.")
        var last: Exception? = null
        for (m in ModelCatalog.chatCandidates(ModelCatalog.Task.FAST).take(3)) {
            try { return GeminiClient.transcribe(key, m.id, wav) } catch (e: CancellationException) { throw e } catch (e: Exception) { last = e }
        }
        throw IOException("Could not hear you: ${last?.message ?: "no speech model available"}")
    }

    private suspend fun speak(sp: CustomProvider, text: String) {
        speaking = true; outLevel = 0.5f; info = "Speaking…"
        try {
            // speak sentence by sentence: the first one starts while the next is already being made
            val parts = text.split(Regex("(?<=[.!?۔؟])\\s+")).filter { it.isNotBlank() }
            val chunks = ArrayList<String>()
            parts.forEach { s -> if (chunks.isNotEmpty() && chunks.last().length < 40) chunks[chunks.lastIndex] = chunks.last() + " " + s else chunks.add(s) }
            suspend fun make(t: String) = if (sp.ttsModel.isNotBlank()) OpenAiCompat.speech(sp, t) else Speaker.synth(t)   // no voice model: Gemini's voice
            var next = scope.async { make(chunks.first()) }
            for (i in chunks.indices) {
                val pcm = next.await()
                if (i + 1 < chunks.size) next = scope.async { make(chunks[i + 1]) }
                PcmPlayer.play(pcm)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { AppLog.e("Call", e); info = "Voice failed: ${(e.message ?: "").take(90)}" }
        finally { speaking = false; outLevel = 0f }
    }

    /** Waits until you speak and returns what you said as a WAV, or null if nothing was said. */
    @SuppressLint("MissingPermission")
    private suspend fun listen(): ByteArray? = withContext(Dispatchers.IO) {
        val rate = 16000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, rate))
        if (rec.state != AudioRecord.STATE_INITIALIZED) throw IOException("Microphone could not start (permission?)")
        try { if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(rec.audioSessionId)?.enabled = true } catch (e: Exception) { AppLog.w(e) }
        try { if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(rec.audioSessionId)?.enabled = true } catch (e: Exception) { AppLog.w(e) }
        recorder = rec
        rec.startRecording()
        val frame = ShortArray(rate / 50)                          // 20 ms
        val speech = ByteArrayOutputStream()
        val pre = ArrayDeque<ShortArray>()                         // 300 ms before the first loud frame, so no word is cut
        var floor = 0.01
        var started = false
        var voiced = 0
        var silentMs = 0
        var totalMs = 0
        var lastUi = 0L
        fun put(a: ShortArray, n: Int) { for (i in 0 until n) { speech.write(a[i].toInt() and 0xFF); speech.write((a[i].toInt() shr 8) and 0xFF) } }
        try {
            while (isActive) {
                val n = rec.read(frame, 0, frame.size)
                if (n <= 0) continue
                var sum = 0.0
                for (i in 0 until n) sum += frame[i].toDouble() * frame[i]
                val rms = sqrt(sum / n) / 32768.0
                if (muted) { started = false; voiced = 0; speech.reset(); continue }
                CallAudioTap.putMicShorts(frame, n, rate)                  // your voice also goes to the in-app call recorder
                val now = System.currentTimeMillis()
                if (now - lastUi > 100) { lastUi = now; val l = (rms * 7).toFloat().coerceIn(0f, 1f); withContext(Dispatchers.Main) { micLevel = l } }
                if (!started) {
                    floor = floor * 0.95 + rms * 0.05
                    pre.addLast(frame.copyOf(n)); if (pre.size > 15) pre.removeFirst()
                    if (rms > maxOf(0.02, floor * 3.0)) voiced++ else voiced = 0
                    if (voiced >= 3) { started = true; pre.forEach { put(it, it.size) }; pre.clear(); silentMs = 0; totalMs = 0 }
                } else {
                    put(frame, n); totalMs += 20
                    if (rms < maxOf(0.015, floor * 2.5)) silentMs += 20 else silentMs = 0
                    if ((silentMs >= 650 && totalMs > 450) || totalMs > 25000) break
                }
            }
        } finally { releaseMic() }
        withContext(Dispatchers.Main) { micLevel = 0f }
        if (started) WavUtil.fromPcm16(speech.toByteArray(), rate) else null
    }

    private fun releaseMic() {
        val r = recorder ?: return
        recorder = null
        try { r.stop() } catch (e: Exception) { AppLog.w(e) }
        try { r.release() } catch (e: Exception) { AppLog.w(e) }
    }
}
