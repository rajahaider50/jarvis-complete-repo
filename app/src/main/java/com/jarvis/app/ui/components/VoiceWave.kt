package com.jarvis.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jarvis.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

/** Siri-style layered voice waves that follow the voice level. */
@Composable
fun VoiceWave(level: Float, active: Boolean, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "wave")
    val t by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "t")
    val lv by animateFloatAsState(if (active) level else 0.04f, tween(100), label = "lv")
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        val mid = size.height / 2f
        val layers = listOf(
            Triple(Gold, 1.0f, 1.0f), Triple(GoldLight, 0.75f, -1.3f), Triple(GoldDark, 0.55f, 1.7f), Triple(Color.White, 0.35f, -2.1f)
        )
        layers.forEachIndexed { i, (col, k, sp) ->
            val p = Path()
            val n = 90
            for (s in 0..n) {
                val x = size.width * s / n
                val env = sin(PI.toFloat() * s / n).let { it * it }
                val y = mid + sin(s / n.toFloat() * (4f + i * 1.5f) * PI.toFloat() + t * sp + i) * env * (size.height * 0.46f) * (0.08f + lv * 1.1f) * k
                if (s == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            drawPath(p, col.copy(alpha = 0.85f - i * 0.15f), style = Stroke(width = (2.6f - i * 0.4f) * density))
        }
    }
}
