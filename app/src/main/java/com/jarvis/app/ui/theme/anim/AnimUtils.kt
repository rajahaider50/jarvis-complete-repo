package com.jarvis.app.ui.theme.anim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jarvis.app.ui.theme.AssistantState
import kotlin.math.cos
import kotlin.math.sin

/** Time that advances at a changeable speed WITHOUT jumping when the speed changes. */
@Composable
fun rememberPhase(speed: Float): State<Float> {
    val speedState = rememberUpdatedState(speed)
    return produceState(0f) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) value += (now - last) / 1_000_000_000f * speedState.value
                last = now
            }
        }
    }
}

fun speedFor(state: AssistantState): Float = when (state) {
    AssistantState.IDLE -> 0.6f
    AssistantState.LISTENING -> 1.0f
    AssistantState.THINKING -> 2.4f
    AssistantState.SPEAKING -> 1.4f
}

fun DrawScope.arcRing(
    center: Offset, radius: Float, startDeg: Float, sweepDeg: Float,
    color: Color, strokePx: Float
) {
    drawArc(
        color = color, startAngle = startDeg, sweepAngle = sweepDeg, useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )
}

fun DrawScope.tickRing(
    center: Offset, radius: Float, count: Int, lengthPx: Float,
    rotationDeg: Float, color: Color, strokePx: Float
) {
    for (i in 0 until count) {
        val a = Math.toRadians((rotationDeg + i * 360f / count).toDouble())
        val cs = cos(a).toFloat()
        val sn = sin(a).toFloat()
        drawLine(
            color,
            Offset(center.x + cs * (radius - lengthPx), center.y + sn * (radius - lengthPx)),
            Offset(center.x + cs * radius, center.y + sn * radius),
            strokePx
        )
    }
}
