package com.jarvis.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    val light = Palettes.current.isLight
    val scheme = if (light) lightColorScheme(
        primary = Gold, onPrimary = Bg, background = Bg, onBackground = Ivory, surface = Surface1, onSurface = Ivory
    ) else darkColorScheme(
        primary = Gold, onPrimary = Bg, background = Bg, onBackground = Ivory, surface = Surface1, onSurface = Ivory
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val w = (view.context as? Activity)?.window
            if (w != null) {
                val c = WindowCompat.getInsetsController(w, view)
                c.isAppearanceLightStatusBars = light      // dark icons on the light interface
                c.isAppearanceLightNavigationBars = light
            }
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
