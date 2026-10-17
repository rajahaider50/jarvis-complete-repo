package com.jarvis.app.live

/**
 * Joins the two sides of a live call into one 44.1 kHz mono stream for the in-app screen recorder:
 * the user's microphone (16 kHz) and the assistant's voice (24 kHz). Each side is resampled and appended to a
 * ring buffer; a feeder thread drains it in fixed frames for the AAC encoder. In a normal back-and-forth call
 * the two sides rarely overlap, so appending keeps both voices in the recording.
 */
class AudioMixer(private val outRate: Int = 44100) {
    private val lock = Object()
    private val ring = ShortArray(outRate * 5)      // ~5 s of slack
    private var r = 0
    private var size = 0

    @Volatile var active = true
        private set

    fun add(pcm: ByteArray, len: Int, srcRate: Int) {
        if (!active || len <= 0) return
        val rs = resample(pcm, len, srcRate, outRate)
        synchronized(lock) {
            var i = 0
            while (i < rs.size && size < ring.size) {
                val idx = (r + size) % ring.size
                ring[idx] = rs[i]
                size++; i++
            }
            lock.notifyAll()
        }
    }

    /** Blocks until at least one sample is available (or stopped); returns how many shorts were written to [dst]. */
    fun drain(dst: ShortArray): Int {
        synchronized(lock) {
            while (size == 0 && active) { try { lock.wait(200) } catch (e: InterruptedException) { } }
            val n = minOf(dst.size, size)
            for (i in 0 until n) { dst[i] = ring[r]; r = (r + 1) % ring.size; size-- }
            return n
        }
    }

    fun stop() { active = false; synchronized(lock) { lock.notifyAll() } }

    private fun resample(src: ByteArray, len: Int, srcRate: Int, dstRate: Int): ShortArray {
        val inN = len / 2
        if (inN <= 0) return ShortArray(0)
        fun sample(i: Int): Int = ((src[2 * i + 1].toInt() shl 8) or (src[2 * i].toInt() and 0xFF)).toShort().toInt()
        if (srcRate == dstRate) {
            val o = ShortArray(inN)
            for (i in 0 until inN) o[i] = sample(i).toShort()
            return o
        }
        val outN = (inN.toLong() * dstRate / srcRate).toInt().coerceAtLeast(1)
        val o = ShortArray(outN)
        for (i in 0 until outN) {
            val pos = i.toDouble() * srcRate / dstRate
            val i0 = pos.toInt().coerceIn(0, inN - 1)
            val i1 = (i0 + 1).coerceAtMost(inN - 1)
            val f = pos - i0
            val s0 = sample(i0); val s1 = sample(i1)
            o[i] = (s0 + (s1 - s0) * f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return o
    }
}
