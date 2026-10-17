package com.jarvis.app.audio

/** Turns 16-bit PCM into a 0..1 loudness value so themes can react to the voice. */
object AudioLevel {
    /** RMS loudness 0..1 from 16-bit PCM samples (mic input OR the assistant's playback chunks). */
    fun rms(pcm: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) { val v = pcm[i] / 32768.0; sum += v * v }
        return kotlin.math.sqrt(sum / count).toFloat().coerceIn(0f, 1f)
    }

    /** Boosted + clamped, ready to feed a theme's level. */
    fun boosted(pcm: ShortArray, count: Int): Float = (rms(pcm, count) * 4f).coerceIn(0f, 1f)

    /** A gentle simulated level for when there is no PCM access (e.g. a ready audio file / TTS). */
    fun simulated(seconds: Float): Float = 0.3f + 0.4f * kotlin.math.abs(kotlin.math.sin(seconds * 6f))
}
