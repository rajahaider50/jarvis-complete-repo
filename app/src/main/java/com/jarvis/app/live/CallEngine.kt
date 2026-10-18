package com.jarvis.app.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.Prefs

/**
 * What every kind of voice call looks like to the screens, the floating bubble and the notification.
 * Two engines exist: [LiveClient] (Gemini Live, real-time) and [PipelineClient] (any custom provider:
 * speech-to-text, then its chat model, then its own voices).
 */
abstract class CallEngine {
    enum class State { IDLE, CONNECTING, LIVE, ERROR }

    var state by mutableStateOf(State.IDLE)
    var micLevel by mutableStateOf(0f)
    var outLevel by mutableStateOf(0f)
    var userText by mutableStateOf("")
    var aiText by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var muted by mutableStateOf(false)
    var speaking by mutableStateOf(false)
    var modelName by mutableStateOf("")
    var info by mutableStateOf("")
    /** True when the microphone keeps returning silence (for example another app/screen-recorder is holding it). */
    var micSilent by mutableStateOf(false)
    /** True when the audio source had to change and the echo canceller is gone: speaker calls now need half-duplex. */
    var aecLost by mutableStateOf(false)
    /** True when the mic is being held back while the assistant speaks (half-duplex echo guard). */
    var echoGuard by mutableStateOf(false)

    /** Called with (what you said, what the assistant said) when a turn is finished, so the call can be continued in chat. */
    var onTurn: ((user: String, ai: String) -> Unit)? = null
    private var savedSig = ""

    fun flushTurn() {
        val u = userText.trim()
        val a = aiText.trim()
        val sig = u + "\u0000" + a
        if ((u.isNotEmpty() || a.isNotEmpty()) && sig != savedSig) { savedSig = sig; onTurn?.invoke(u, a) }
    }

    abstract fun start()
    abstract fun stop()

    open fun toggleMute() { muted = !muted; if (muted) micLevel = 0f }
    open fun sendJpeg(bytes: ByteArray) { }
    open fun sendText(text: String) { }
    open fun switchLanguage(code: String) { Prefs.lang = code; Prefs.save() }
}
