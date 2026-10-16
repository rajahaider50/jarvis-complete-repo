package com.jarvis.app.audio

import com.jarvis.app.data.AppLog
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

object PcmPlayer {
    /** Plays 16-bit mono PCM and returns when playback has finished (or the coroutine is cancelled). */
    suspend fun play(pcm: ByteArray, rate: Int = 24000) = withContext(Dispatchers.IO) {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()
            )
            .setBufferSizeInBytes(maxOf(min, rate))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            var off = 0
            while (off < pcm.size && isActive) {
                val n = minOf(4096, pcm.size - off)
                track.write(pcm, off, n)
                off += n
            }
            val frames = pcm.size / 2
            val limit = System.currentTimeMillis() + frames * 1000L / rate + 2500
            while (isActive && track.playbackHeadPosition < frames && System.currentTimeMillis() < limit) delay(40)
        } finally {
            try { track.stop() } catch (e: Exception) { AppLog.w(e); }
            track.release()
        }
    }
}
