package com.jarvis.app.ui.theme.fx

import androidx.annotation.RawRes

enum class FxEvent { BOOT, LISTEN_START, LISTEN_END, THINKING, SPEAK_START, ERROR, THEME_SWITCH }

sealed interface FxSpec {
    data class Raw(@RawRes val resId: Int, val rate: Float = 1f) : FxSpec
    data class Synth(val f0: Float, val f1: Float, val ms: Int, val volume: Float = 0.35f) : FxSpec
}

class FxProfile(val map: Map<FxEvent, FxSpec>)

object FxPresets {
    // Synth-only defaults: they work with ZERO asset files, so nothing can be "missing".
    val tech = FxProfile(mapOf(
        FxEvent.BOOT to FxSpec.Synth(200f, 1400f, 600),
        FxEvent.LISTEN_START to FxSpec.Synth(600f, 1000f, 120),
        FxEvent.LISTEN_END to FxSpec.Synth(1000f, 600f, 120),
        FxEvent.THINKING to FxSpec.Synth(500f, 520f, 200, 0.2f),
        FxEvent.SPEAK_START to FxSpec.Synth(700f, 900f, 100),
        FxEvent.ERROR to FxSpec.Synth(300f, 150f, 350),
        FxEvent.THEME_SWITCH to FxSpec.Synth(400f, 1200f, 180),
    ))

    val warm = FxProfile(tech.map.mapValues { (_, s) ->
        if (s is FxSpec.Synth) s.copy(f0 = s.f0 * 0.7f, f1 = s.f1 * 0.7f) else s
    })

    val soft = FxProfile(tech.map.mapValues { (_, s) ->
        if (s is FxSpec.Synth) s.copy(volume = s.volume * 0.6f, ms = (s.ms * 1.3f).toInt()) else s
    })
}
