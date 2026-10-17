package com.jarvis.app.ui.theme.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Single entry point for sound effects. It never throws and always falls back to the synth. */
class FxPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val soundIds = mutableMapOf<Int, Int>()     // resId -> soundId
    private val ready = mutableSetOf<Int>()             // loaded soundIds

    @Volatile var enabled = true
    @Volatile var volume = 0.8f                         // 0..1, from Settings

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready += id } }
    }

    fun preload(profile: FxProfile) {
        profile.map.values.filterIsInstance<FxSpec.Raw>().forEach { raw ->
            if (raw.resId !in soundIds) soundIds[raw.resId] = pool.load(appContext, raw.resId, 1)
        }
    }

    fun play(profile: FxProfile, event: FxEvent) {
        if (!enabled) return
        when (val spec = profile.map[event] ?: return) {
            is FxSpec.Synth -> SynthFx.play(spec, volume)
            is FxSpec.Raw -> {
                val id = soundIds[spec.resId] ?: return
                val ok = synchronized(ready) { id in ready }
                if (ok) pool.play(id, volume, volume, 1, 0, spec.rate)
            }
        }
    }

    fun release() = pool.release()
}
