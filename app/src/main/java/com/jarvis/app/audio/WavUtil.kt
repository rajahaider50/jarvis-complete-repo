package com.jarvis.app.audio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Small helpers to move audio between phone PCM and the WAV files that speech servers use. */
object WavUtil {
    /** Wraps 16-bit mono PCM in a WAV header (for speech-to-text uploads). */
    fun fromPcm16(pcm: ByteArray, rate: Int): ByteArray {
        val b = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(pcm.size).put(pcm)
        return b.array()
    }

    /** Reads a WAV file from a text-to-speech server and returns 16-bit mono PCM at 24 kHz (what the player expects). */
    fun toPcm24k(wav: ByteArray): ByteArray {
        if (wav.size < 44 || String(wav, 0, 4) != "RIFF" || String(wav, 8, 4) != "WAVE") throw IOException("The voice server did not return WAV audio")
        val b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var channels = 1; var rate = 24000; var bits = 16
        var dataStart = -1; var dataLen = 0
        while (pos + 8 <= wav.size) {
            val id = String(wav, pos, 4)
            val size = b.getInt(pos + 4)
            if (id == "fmt ") {
                channels = b.getShort(pos + 10).toInt(); rate = b.getInt(pos + 12); bits = b.getShort(pos + 22).toInt()
            } else if (id == "data") {
                dataStart = pos + 8
                dataLen = if (size <= 0 || dataStart + size > wav.size) wav.size - dataStart else size   // streamed files can have a bad size
                break
            }
            pos += 8 + size + (size and 1)
        }
        if (dataStart < 0) throw IOException("The voice file has no audio")
        if (bits != 16) throw IOException("Unsupported voice format ($bits-bit)")
        val frames = dataLen / (2 * channels)
        val mono = ShortArray(frames)
        for (i in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) sum += b.getShort(dataStart + (i * channels + c) * 2).toInt()
            mono[i] = (sum / channels).toShort()
        }
        val out = if (rate == 24000) mono else {
            val n = (frames.toLong() * 24000 / rate).toInt()
            ShortArray(n) { i ->
                val src = i.toDouble() * rate / 24000
                val i0 = src.toInt().coerceAtMost(frames - 1)
                val i1 = (i0 + 1).coerceAtMost(frames - 1)
                (mono[i0] + (mono[i1] - mono[i0]) * (src - i0)).toInt().toShort()
            }
        }
        val res = ByteBuffer.allocate(out.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        out.forEach { res.putShort(it) }
        return res.array()
    }
}
