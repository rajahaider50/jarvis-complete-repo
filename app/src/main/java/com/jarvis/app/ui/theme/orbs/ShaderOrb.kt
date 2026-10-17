package com.jarvis.app.ui.theme.orbs

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.jarvis.app.ui.theme.AssistantState
import com.jarvis.app.ui.theme.anim.rememberPhase
import com.jarvis.app.ui.theme.anim.speedFor
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ShaderOrb(agsl: String, state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderOrbApi33(agsl, state, level, modifier)
    } else {
        FallbackOrb(state, level, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderOrbApi33(agsl: String, state: AssistantState, level: Float, modifier: Modifier) {
    // runCatching = a bad shader never crashes the app; we show the fallback instead.
    val shader = remember(agsl) { runCatching { RuntimeShader(agsl) }.getOrNull() }
    if (shader == null) {
        FallbackOrb(state, level, modifier)
        return
    }
    val time by rememberPhase(speedFor(state))
    val smooth by animateFloatAsState(level.coerceIn(0f, 1f), tween(120), label = "orbLevel")
    Canvas(modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("level", smooth)
        drawRect(brush = ShaderBrush(shader))
    }
}

@Composable
fun FallbackOrb(state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    val t by rememberPhase(speedFor(state))
    Canvas(modifier) {
        val r = min(size.width, size.height) / 2f * (0.55f + 0.05f * sin(t * 2f) + 0.2f * level)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF9AD8FF), Color(0xFF3B6BFF), Color(0x00000000)),
                center = center, radius = r * 1.4f
            ),
            radius = r * 1.4f, center = Offset(center.x, center.y)
        )
    }
}
