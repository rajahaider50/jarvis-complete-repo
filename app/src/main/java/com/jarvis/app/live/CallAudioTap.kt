package com.jarvis.app.live

/**
 * A tap into the live call's raw audio, used by the in-app screen recorder to mix both sides of the call:
 * the user's microphone ([mic]) and the assistant's voice ([out]).
 *
 * The sample rate travels WITH every buffer (16 kHz for the microphone, 24 kHz for Gemini Live, 24 kHz for Gemini
 * TTS), so the recorder can resample each side correctly instead of assuming a rate.
 *
 * Round 3 (recording): the counters [micBytes]/[outBytes] are the evidence that both sides really reached the
 * recorder — the Diagnostics screen and the `JARVIS_DIAG` dump show them, so a silent recording can be diagnosed
 * instead of guessed at.
 *
 * Both callbacks are invoked from the call's own audio threads, so listeners must be fast and thread-safe.
 */
object CallAudioTap {
    /** 16-bit mono PCM straight from the microphone, [len] valid bytes in [buf] at [rate] Hz. */
    @Volatile var mic: ((buf: ByteArray, len: Int, rate: Int) -> Unit)? = null

    /** 16-bit mono PCM of the assistant's voice, [len] valid bytes in [buf] at [rate] Hz. */
    @Volatile var out: ((buf: ByteArray, len: Int, rate: Int) -> Unit)? = null

    /** Bytes each side has contributed since a recorder attached (0 when no recorder is listening). */
    @Volatile var micBytes = 0L
    @Volatile var outBytes = 0L

    /** True while a recorder is attached to the microphone side. */
    val micAttached: Boolean get() = mic != null
    val outAttached: Boolean get() = out != null

    /** Hand microphone PCM to the recorder (bytes). */
    fun putMic(buf: ByteArray, len: Int, rate: Int) {
        val f = mic ?: return
        micBytes += len
        try { f(buf, len, rate) } catch (e: Exception) { Diag.fail("rec.micTap", e) }
    }

    /** Hand assistant PCM to the recorder (bytes). */
    fun putOut(buf: ByteArray, len: Int, rate: Int) {
        val f = out ?: return
        outBytes += len
        try { f(buf, len, rate) } catch (e: Exception) { Diag.fail("rec.outTap", e) }
    }

    /** Same, for engines that read a ShortArray (the provider pipeline, the TTS player). */
    fun putMicShorts(s: ShortArray, n: Int, rate: Int) {
        val f = mic ?: return
        val b = toBytes(s, n)
        micBytes += b.size
        try { f(b, b.size, rate) } catch (e: Exception) { Diag.fail("rec.micTap", e) }
    }

    fun putOutShorts(s: ShortArray, n: Int, rate: Int) {
        val f = out ?: return
        val b = toBytes(s, n)
        outBytes += b.size
        try { f(b, b.size, rate) } catch (e: Exception) { Diag.fail("rec.outTap", e) }
    }

    fun resetCounters() { micBytes = 0L; outBytes = 0L }

    private fun toBytes(s: ShortArray, n: Int): ByteArray {
        val b = ByteArray(n * 2)
        for (i in 0 until n) { val v = s[i].toInt(); b[2 * i] = v.toByte(); b[2 * i + 1] = (v shr 8).toByte() }
        return b
    }
}
