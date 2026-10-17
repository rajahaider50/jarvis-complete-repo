package com.jarvis.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jarvis.app.ui.theme.fx.FxProfile

enum class ThemeCategory { ORB, HUD }

/** One selectable assistant visual. [Render] draws it for the current state and voice [level] (0..1). */
interface AssistantTheme {
    val id: String
    val displayName: String
    val category: ThemeCategory
    val fx: FxProfile

    @Composable
    fun Render(state: AssistantState, level: Float, modifier: Modifier)
}
