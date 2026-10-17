package com.jarvis.app.ui.theme.fx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper

object SynthFx {
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun play(spec: FxSpec.Synth, masterVolume: Float) {
        runCatching {
            val sr = 22050
            val n = sr * spec.ms / 1000
            val buf = ShortArray(n)
            var phase = 0.0
            for (i in 0 until n) {
                val p = i / n.toFloat()
                val f = spec.f0 + (spec.f1 - spec.f0) * p
                phase += 2.0 * Math.PI * f / sr
                val env = Math.sin(Math.PI * p).toFloat()          // fade in + out, no clicks
                buf[i] = (Math.sin(phase).toFloat() * env * spec.volume * masterVolume * 32767f).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(n * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buf, 0, n)
            track.play()
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { track.release() } }, spec.ms + 300L)
        }
    }
}
