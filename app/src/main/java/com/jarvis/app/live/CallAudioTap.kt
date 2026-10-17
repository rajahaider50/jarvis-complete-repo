package com.jarvis.app.live

/**
 * A tap into the live call's raw audio, used by the in-app screen recorder to mix both sides of the call:
 * the user's microphone ([mic], 16 kHz mono) and the assistant's voice coming from Gemini ([out], 24 kHz mono).
 * Both callbacks are invoked from the call's own audio threads, so listeners must be fast and thread-safe.
 */
object CallAudioTap {
    /** 16 kHz mono 16-bit PCM straight from the microphone, [len] valid bytes in [buf]. */
    @Volatile var mic: ((buf: ByteArray, len: Int) -> Unit)? = null

    /** 24 kHz mono 16-bit PCM of the assistant's voice, [len] valid bytes in [buf]. */
    @Volatile var out: ((buf: ByteArray, len: Int) -> Unit)? = null
}
