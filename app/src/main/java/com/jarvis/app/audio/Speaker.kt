package com.jarvis.app.audio

import com.jarvis.app.data.AppLog
import android.content.Context
import com.jarvis.app.data.GeminiClient
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Speaks ONLY with Gemini's own voices. There is deliberately no phone / Google default-voice fallback.
 * The voice model is picked automatically from the models your API key can use; if one is out of quota
 * or rejects the request, the next one is tried by itself.
 */
object Speaker {
    var lastError: String? = null
    @Volatile var lastModel: String = ""
    private val cache = object : LinkedHashMap<String, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean = size > 24
    }

    @Suppress("UNUSED_PARAMETER")
    fun init(ctx: Context) { }

    suspend fun synth(
        text: String, voice: String = Prefs.voice, style: String = Prefs.voiceStyle,
        onModel: (String) -> Unit = {}
    ): ByteArray {
        val key = Prefs.apiKey.trim()
        if (key.isEmpty()) throw IllegalStateException("Add your Gemini API key first")
        val prompt = if (style.isBlank()) text else "$style: $text"
        val ck = "$voice|$prompt"
        synchronized(cache) { cache[ck] }?.let { return it }
        if (ModelCatalog.tts.isEmpty() && ModelCatalog.chat.isEmpty()) ModelCatalog.refresh()
        val models = ModelCatalog.ttsCandidates().take(6)
        if (models.isEmpty()) throw IllegalStateException("No voice model is available for this API key")
        var lastErr = ""
        for (pass in 0..1) {
            var transient = false
            for (m in models) {
                onModel(m)
                var tries = 0
                while (true) {
                    tries++
                    try {
                        val pcm = GeminiClient.tts(key, m, voice, prompt)
                        lastModel = m; lastError = null
                        AppLog.i("Voice", "Spoke with model $m, voice $voice")
                        synchronized(cache) { cache[ck] = pcm }
                        return pcm
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val msg = e.message ?: "error"
                        lastErr = "$m: ${msg.take(150)}"
                        if (msg.contains("API key", true)) throw e                      // wrong key: other models will not help
                        if (msg.contains("503") && tries < 2) { delay(1200); continue } // busy for a moment: ask the same model once more
                        AppLog.i("Voice", "$m: ${GeminiClient.shortReason(e)}, trying the next voice model")
                        if (msg.contains("429") || msg.contains("503") || msg.contains("quota", true)) transient = true
                        break
                    }
                }
            }
            if (pass == 0 && transient) delay(2500) else break
        }
        lastError = lastErr
        AppLog.e("Voice", "All voice models failed. $lastErr")
        throw IOException(lastErr)
    }

    suspend fun speak(text: String, voice: String = Prefs.voice, style: String = Prefs.voiceStyle) {
        try { PcmPlayer.play(synth(text, voice, style)) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { AppLog.w(e); lastError = e.message }
    }
}
