package com.jarvis.app.ui.theme.orbs

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jarvis.app.ui.theme.AssistantState
import com.jarvis.app.ui.theme.anim.rememberPhase
import com.jarvis.app.ui.theme.anim.speedFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun fibonacciSphere(n: Int): FloatArray {
    val out = FloatArray(n * 3)
    val golden = PI * (3.0 - sqrt(5.0))
    for (i in 0 until n) {
        val y = 1f - (i / (n - 1f)) * 2f
        val ring = sqrt(1f - y * y)
        val th = (golden * i).toFloat()
        out[i * 3] = cos(th) * ring
        out[i * 3 + 1] = y
        out[i * 3 + 2] = sin(th) * ring
    }
    return out
}

@Composable
fun ParticleSphere(
    state: AssistantState, level: Float, modifier: Modifier = Modifier,
    color: Color = Color(0xFF4DF5E0), count: Int = 1400
) {
    val pts = remember(count) { fibonacciSphere(count) }
    val phase by rememberPhase(speedFor(state))
    Canvas(modifier) {
        val c = center
        val radius = min(size.width, size.height) * 0.42f * (1f + level * 0.08f)
        val rot = phase * 0.5f
        val cosR = cos(rot)
        val sinR = sin(rot)
        val dot = 1.3.dp.toPx()
        for (i in 0 until count) {
            val x = pts[i * 3]
            val y = pts[i * 3 + 1]
            val z = pts[i * 3 + 2]
            val xr = x * cosR + z * sinR
            val zr = -x * sinR + z * cosR
            val depth = (zr + 1f) / 2f              // 0 back .. 1 front
            drawCircle(
                color = color.copy(alpha = 0.12f + 0.88f * depth),
                radius = dot * (0.6f + 0.6f * depth),
                center = Offset(c.x + xr * radius, c.y + y * radius)
            )
        }
    }
}
