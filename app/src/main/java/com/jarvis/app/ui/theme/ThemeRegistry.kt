package com.jarvis.app.ui.theme

import com.jarvis.app.ui.theme.fx.FxPresets
import com.jarvis.app.ui.theme.hud.HudAmber
import com.jarvis.app.ui.theme.hud.HudArcReactor
import com.jarvis.app.ui.theme.hud.HudFrost
import com.jarvis.app.ui.theme.hud.HudGlobe
import com.jarvis.app.ui.theme.hud.HudRadar
import com.jarvis.app.ui.theme.hud.HudTheme
import com.jarvis.app.ui.theme.orbs.OrbShaders
import com.jarvis.app.ui.theme.orbs.ParticleSphereTheme
import com.jarvis.app.ui.theme.orbs.ShaderOrbTheme

/** The only place themes are listed. */
object ThemeRegistry {
    val all: List<AssistantTheme> = listOf(
        ClassicTheme(),
        ShaderOrbTheme("orb_glass", "Glass Orb", OrbShaders.GLASS, FxPresets.soft),
        ShaderOrbTheme("orb_fire", "Fire Ring", OrbShaders.FIRE, FxPresets.warm),
        ShaderOrbTheme("orb_sun", "Solar Orb", OrbShaders.SUN, FxPresets.warm),
        ShaderOrbTheme("orb_gold", "Gold Pulse", OrbShaders.GOLD, FxPresets.soft),
        ParticleSphereTheme("orb_particles", "Particle Sphere", FxPresets.tech),
        HudTheme("hud_reactor", "Arc Reactor", FxPresets.tech) { s, l, m -> HudArcReactor(s, l, m) },
        HudTheme("hud_radar", "Radar Sweep", FxPresets.tech) { s, l, m -> HudRadar(s, l, m) },
        HudTheme("hud_globe", "Holo Globe", FxPresets.tech) { s, l, m -> HudGlobe(s, l, m) },
        HudTheme("hud_amber", "Stark Amber", FxPresets.warm) { s, l, m -> HudAmber(s, l, m) },
        HudTheme("hud_frost", "Minimal Frost", FxPresets.soft) { s, l, m -> HudFrost(s, l, m) },
    )

    fun byId(id: String?): AssistantTheme = all.firstOrNull { it.id == id } ?: all.first()
}
