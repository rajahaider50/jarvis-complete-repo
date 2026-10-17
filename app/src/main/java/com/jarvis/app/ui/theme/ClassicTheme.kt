package com.jarvis.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.SiriOrb
import com.jarvis.app.ui.theme.fx.FxPresets

/** Keeps the app's original animated orb available as a theme (the default). */
class ClassicTheme : AssistantTheme {
    override val id = "classic"
    override val displayName = "Classic Gold"
    override val category = ThemeCategory.ORB
    override val fx = FxPresets.soft

    @Composable
    override fun Render(state: AssistantState, level: Float, modifier: Modifier) {
        val mode = when (state) {
            AssistantState.IDLE -> 0
            AssistantState.LISTENING -> 2
            AssistantState.THINKING -> 2
            AssistantState.SPEAKING -> 3
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            SiriOrb(150.dp, level.coerceIn(0f, 1f), mode, Prefs.orbStyle)
        }
    }
}
