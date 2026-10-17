package com.jarvis.app.ui.theme

import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.theme.fx.FxEvent
import com.jarvis.app.ui.theme.fx.FxPlayer

/** Persists the selected assistant theme id using the app's existing settings storage (Prefs). */
object ThemeRepository {
    fun selectedId(): String = Prefs.assistantTheme

    fun select(id: String) {
        Prefs.assistantTheme = ThemeRegistry.byId(id).id
        Prefs.save()
        Fx.play(FxEvent.THEME_SWITCH)
    }
}

/** One place that plays sound effects for the whole app, using the active theme's FxProfile. */
object Fx {
    private var player: FxPlayer? = null

    fun init(context: android.content.Context) {
        if (player == null) player = FxPlayer(context.applicationContext)
    }

    fun play(event: FxEvent) {
        if (!Prefs.fxEnabled) return
        val p = player ?: return
        p.volume = Prefs.fxVolume
        p.play(ThemeRegistry.byId(Prefs.assistantTheme).fx, event)
    }

    fun release() { player?.release(); player = null }
}
