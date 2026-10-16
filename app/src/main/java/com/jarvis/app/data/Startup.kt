package com.jarvis.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.audio.PcmPlayer
import com.jarvis.app.audio.Speaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The prepared "system active" briefing: text + voice audio, cached so Home can play it instantly. */
object Greeting {
    var text = ""
    var pcm: ByteArray? = null
    var key = ""
    var error by mutableStateOf<String?>(null)
    fun currentKey() = "${Prefs.lang}|${Prefs.voice}|${Prefs.voiceStyle}|${Prefs.userName}|${Prefs.assistantName}"
}

object GreetingPlayer {
    private var job: Job? = null
    fun play(scope: CoroutineScope, onDone: () -> Unit = {}) {
        job?.cancel()
        job = scope.launch {
            try {
                var p = Greeting.pcm
                if (p == null && Greeting.text.isNotBlank() && Prefs.apiKey.isNotBlank()) {
                    try { p = Speaker.synth(Greeting.text); Greeting.pcm = p; Greeting.error = null }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { AppLog.w(e); Greeting.error = e.message }
                }
                if (p != null) PcmPlayer.play(p)
            } finally { onDone() }
        }
    }
    fun stop() { job?.cancel() }
}

object Startup {
    var ready by mutableStateOf(false)

    /** Runs the real start-up work and reports progress 0..100 with a label. */
    suspend fun prepare(ctx: Context, report: (Int, String) -> Unit) {
        suspend fun <T> stage(from: Int, to: Int, label: String, block: suspend () -> T): T = coroutineScope {
            report(from, label)
            val tick = launch {
                var p = from.toFloat()
                while (isActive) {
                    p += (to - 2 - p) * 0.05f + 0.04f
                    if (p > to - 2) p = (to - 2).toFloat()
                    report(p.toInt(), label); delay(70)
                }
            }
            try { block() } finally { tick.cancel(); report(to, label) }
        }

        val name = Prefs.assistantName
        stage(0, 20, "Connecting to Gemini") {
            if (Prefs.apiKey.isNotBlank() && !ModelCatalog.ready) withTimeoutOrNull(12000) { ModelCatalog.refresh() }
        }
        val stats = stage(20, 40, "Reading device status") { DeviceStats.read(ctx) }
        val weather = stage(40, 55, "Checking today's weather") { withTimeoutOrNull(6000) { Weather.fetch(ctx) } }
        val text = stage(55, 72, "Composing your briefing") {
            withTimeoutOrNull(12000) { compose(stats, weather) } ?: fallback(stats, weather)
        }
        val pcm = stage(72, 99, "Preparing $name's voice") {
            Greeting.error = null
            if (Prefs.apiKey.isBlank()) null else {
                val r = try { withTimeoutOrNull(45000) { Speaker.synth(text) } }
                    catch (e: CancellationException) { throw e } catch (e: Exception) { AppLog.w(e); Greeting.error = e.message; null }
                if (r == null && Greeting.error == null) Greeting.error = "Voice request timed out"
                r
            }
        }
        Greeting.text = text; Greeting.pcm = pcm; Greeting.key = Greeting.currentKey()
        ready = true
        report(100, "Systems active")
    }

    private fun fallback(s: Stats, weather: String?): String {
        var t = Langs.bootLine(Prefs.lang, Prefs.userName.ifBlank { "Sir" }, s.battery, s.tempC.toInt(), s.storageFreeGb.toInt())
        if (weather != null && Prefs.lang == "en-US") t += " Today's weather: $weather."
        return t
    }

    private suspend fun compose(s: Stats, weather: String?): String {
        val model = ModelCatalog.pick(ModelCatalog.Task.FAST)
        if (model == null || Prefs.apiKey.isBlank()) return fallback(s, weather)
        return try {
            val prompt = "Write a spoken system-activation briefing in ${Langs.of(Prefs.lang).english}, at most 4 short sentences, no markdown, no emojis. " +
                "Say that the system is now active. Mention: battery ${s.battery}% (${if (s.charging) "charging" else "not charging"}), " +
                "phone temperature ${"%.0f".format(s.tempC)} degrees Celsius, ${"%.0f".format(s.storageFreeGb)} GB storage free, " +
                "today's weather: ${weather ?: "not available"}. Finish by saying the user can proceed. Address the user as ${Prefs.userName.ifBlank { "Sir" }}."
            GeminiClient.chat(Prefs.apiKey, model.id, "You are ${Prefs.assistantName}, an elegant, calm AI concierge. ${Voices.genderRule()}", listOf(GeminiClient.Turn("user", prompt)))
        } catch (e: CancellationException) { throw e } catch (e: Exception) { AppLog.w(e); fallback(s, weather) }
    }
}
