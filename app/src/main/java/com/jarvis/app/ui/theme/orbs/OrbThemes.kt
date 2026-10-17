package com.jarvis.app.ui.theme.orbs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jarvis.app.ui.theme.AssistantTheme
import com.jarvis.app.ui.theme.AssistantState
import com.jarvis.app.ui.theme.ThemeCategory
import com.jarvis.app.ui.theme.fx.FxProfile

class ShaderOrbTheme(
    override val id: String, override val displayName: String,
    private val agsl: String, override val fx: FxProfile
) : AssistantTheme {
    override val category = ThemeCategory.ORB
    @Composable override fun Render(state: AssistantState, level: Float, modifier: Modifier) =
        ShaderOrb(agsl, state, level, modifier)
}

class ParticleSphereTheme(
    override val id: String, override val displayName: String, override val fx: FxProfile
) : AssistantTheme {
    override val category = ThemeCategory.ORB
    @Composable override fun Render(state: AssistantState, level: Float, modifier: Modifier) =
        ParticleSphere(state, level, modifier)
}
