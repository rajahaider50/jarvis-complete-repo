package com.jarvis.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val ORB_STYLES = listOf(
    "Classic Blobs", "Plasma", "Ripple Rings", "Aurora Waves", "Orbit Dots", "Spiral Galaxy", "Pulse Core", "Glass Sphere",
    "Neon Ring", "Equalizer Ring", "Liquid Drop", "Starburst", "Particle Halo", "Diamond Rings", "Breathing Sweep", "Wave Sphere"
)

/** mode: 0 idle, 1 listening (reacts to level), 2 thinking, 3 speaking. style: 0..15 (see ORB_STYLES). */
@Composable
fun SiriOrb(diameter: Dp, level: Float, mode: Int, style: Int = Prefs.orbStyle) {
    val inf = rememberInfiniteTransition(label = "orb")
    val t by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "t")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val lv by animateFloatAsState(level, tween(120), label = "lv")
    val amp = when (mode) {
        1 -> 0.05f + lv * 0.28f
        2 -> 0.04f + pulse * 0.08f
        3 -> 0.08f + (0.06f + lv * 0.2f) * abs(sin(t * 4f))
        else -> 0.03f + pulse * 0.02f
    }
    Canvas(Modifier.size(diameter)) {
        val r = size.minDimension / 2f * 0.84f
        val c = center
        drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.30f), Color.Transparent), center = c, radius = r * 1.22f), radius = r * 1.22f, center = c)
        when (style.coerceIn(0, 15)) {
            0 -> blobs(c, r, t, amp, 4, false)
            1 -> blobs(c, r, t * 1.6f, amp * 1.4f, 7, true)
            2 -> ripples(c, r, t, amp)
            3 -> aurora(c, r, t, amp)
            4 -> orbitDots(c, r, t, amp)
            5 -> galaxy(c, r, t, amp)
            6 -> pulseCore(c, r, t, amp)
            7 -> glass(c, r, t, amp)
            8 -> neonRing(c, r, t, amp)
            9 -> equalizer(c, r, t, amp)
            10 -> liquid(c, r, t, amp)
            11 -> starburst(c, r, t, amp)
            12 -> halo(c, r, t, amp)
            13 -> diamonds(c, r, t, amp)
            14 -> sweep(c, r, t, amp)
            else -> waveSphere(c, r, t, amp)
        }
    }
}

private fun glow(col: Color, a: Float = 0.95f, ) = col.copy(alpha = a)

private fun DrawScope.ball(c: Offset, r: Float, bg: Color = Color(0xFF0D0C09), block: DrawScope.() -> Unit) {
    val clip = Path().apply { addOval(Rect(c, r)) }
    clipPath(clip) { drawRect(bg); block() }
    drawCircle(GoldLight.copy(alpha = 0.45f), radius = r, center = c, style = Stroke(width = 1.5f * density))
}

private fun DrawScope.soft(col: Color, pos: Offset, rad: Float, a: Float = 0.95f) =
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a), col.copy(alpha = 0f)), center = pos, radius = rad), radius = rad, center = pos)

private fun warm() = lerp(Gold, Color(0xFFE08A2E), 0.45f)

private fun DrawScope.blobs(c: Offset, r: Float, t: Float, amp: Float, n: Int, plasma: Boolean) = ball(c, r) {
    val cols = listOf(Gold, warm(), GoldLight, GoldDark, Gold, warm(), GoldLight)
    for (i in 0 until n) {
        val ang = t * (1f + i * 0.35f) * (if (i % 2 == 0) 1f else -1f) + i * 1.7f
        val dist = r * (0.20f + amp * 0.9f) * (if (plasma) 1.5f else 1f)
        val pos = Offset(c.x + cos(ang) * dist, c.y + sin(ang * 1.2f) * dist)
        soft(cols[i % cols.size], pos, r * ((if (plasma) 0.55f else 0.62f) * (1f - i * 0.04f) + amp))
    }
    soft(Color.White, c, r * (0.36f + amp * 0.8f))
}

private fun DrawScope.ripples(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val k = t / (2 * PI.toFloat())
    for (i in 0 until 5) {
        val f = (k * 2f + i / 5f) % 1f
        drawCircle(Gold.copy(alpha = (1f - f) * (0.55f + amp)), radius = r * f * (1f + amp), center = c, style = Stroke(width = 2.2f * density))
    }
    soft(GoldLight, c, r * (0.45f + amp), 0.9f); soft(Color.White, c, r * 0.22f)
}

private fun DrawScope.aurora(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val cols = listOf(Gold, GoldLight, warm(), GoldDark)
    for (b in 0 until 4) {
        val p = Path()
        val base = c.y - r + r * 2f * (0.2f + b * 0.2f)
        p.moveTo(c.x - r, c.y + r)
        var x = c.x - r
        while (x <= c.x + r) {
            val y = base + sin((x - c.x) / r * 3f + t * (1f + b * 0.4f) + b) * r * (0.10f + amp * 0.8f)
            p.lineTo(x, y); x += r / 24f
        }
        p.lineTo(c.x + r, c.y + r); p.close()
        drawPath(p, Brush.verticalGradient(listOf(cols[b].copy(alpha = 0.65f), Color.Transparent), startY = base - r * 0.3f, endY = c.y + r))
    }
}

private fun DrawScope.orbitDots(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    soft(GoldLight, c, r * (0.40f + amp), 0.95f); soft(Color.White, c, r * 0.2f)
    for (o in 0 until 3) {
        val rad = r * (0.45f + o * 0.22f)
        drawCircle(Gold.copy(alpha = 0.18f), radius = rad, center = c, style = Stroke(width = 1f * density))
        for (d in 0..(o + 1)) {
            val a = t * (o % 2 * 2 - 1).let { if (it == 0) 1 else it } * (1.2f - o * 0.25f) + d * (2 * PI.toFloat() / (o + 2))
            drawCircle(listOf(Gold, GoldLight, warm())[o], radius = r * (0.07f + amp * 0.2f), center = Offset(c.x + cos(a) * rad, c.y + sin(a) * rad))
        }
    }
}

private fun DrawScope.galaxy(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    soft(GoldLight, c, r * (0.35f + amp), 0.9f)
    for (arm in 0 until 3) for (i in 0 until 46) {
        val f = i / 46f
        val a = t * 0.8f + arm * 2.094f + f * 5.2f
        val d = r * (0.08f + f * (0.85f + amp))
        drawCircle(listOf(Gold, GoldLight, warm())[arm].copy(alpha = 1f - f * 0.7f), radius = r * (0.045f - f * 0.025f) + 0.5f, center = Offset(c.x + cos(a) * d, c.y + sin(a) * d))
    }
}

private fun DrawScope.pulseCore(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    soft(Gold, c, r * (0.85f + amp), 0.85f); soft(GoldLight, c, r * (0.6f + amp), 0.95f); soft(Color.White, c, r * (0.32f + amp * 0.7f))
    drawCircle(Gold.copy(alpha = 0.5f), radius = r * (0.7f + amp), center = c, style = Stroke(width = 2f * density))
}

private fun DrawScope.glass(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r, Color(0xFF111015)) {
    soft(Gold, Offset(c.x + cos(t) * r * 0.3f, c.y + sin(t * 1.3f) * r * 0.25f), r * (0.7f + amp), 0.8f)
    soft(GoldDark, Offset(c.x - cos(t * 0.8f) * r * 0.3f, c.y + sin(t) * r * 0.3f), r * (0.6f + amp), 0.8f)
    soft(Color.White, Offset(c.x - r * 0.35f, c.y - r * 0.45f), r * 0.45f, 0.55f)
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), center = c, radius = r), radius = r, center = c)
}

private fun DrawScope.neonRing(c: Offset, r: Float, t: Float, amp: Float) {
    soft(Gold, c, r * 1.05f, 0.18f)
    val w = r * (0.10f + amp * 0.6f)
    rotate(t * 57.3f, c) {
        drawCircle(Brush.sweepGradient(listOf(Gold.copy(alpha = 0.1f), GoldLight, Gold, warm(), Gold.copy(alpha = 0.1f)), center = c), radius = r * 0.82f, center = c, style = Stroke(width = w))
    }
    drawCircle(Gold.copy(alpha = 0.25f), radius = r * 0.55f, center = c, style = Stroke(width = 1.5f * density))
    soft(Color.White, c, r * (0.2f + amp), 0.8f)
}

private fun DrawScope.equalizer(c: Offset, r: Float, t: Float, amp: Float) {
    val n = 56
    for (i in 0 until n) {
        val a = i * 2 * PI.toFloat() / n
        val h = r * (0.08f + (0.10f + amp * 1.2f) * abs(sin(a * 3f + t * 3f) * cos(a * 2f - t * 2f)))
        val s = Offset(c.x + cos(a) * r * 0.62f, c.y + sin(a) * r * 0.62f)
        val e = Offset(c.x + cos(a) * (r * 0.62f + h), c.y + sin(a) * (r * 0.62f + h))
        drawLine(if (i % 2 == 0) Gold else GoldLight, s, e, strokeWidth = 3f * density, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    soft(Gold, c, r * (0.5f + amp), 0.5f); soft(Color.White, c, r * 0.18f, 0.9f)
}

private fun DrawScope.liquid(c: Offset, r: Float, t: Float, amp: Float) {
    val p = Path(); val n = 90
    for (i in 0..n) {
        val a = i * 2 * PI.toFloat() / n
        val d = r * (0.62f + (0.05f + amp * 0.5f) * (sin(a * 3f + t * 2f) + 0.6f * sin(a * 5f - t * 3f)))
        val x = c.x + cos(a) * d; val y = c.y + sin(a) * d
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, Brush.linearGradient(listOf(GoldLight, Gold, GoldDark), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)))
    soft(Color.White, Offset(c.x - r * 0.2f, c.y - r * 0.25f), r * 0.3f, 0.6f)
}

private fun DrawScope.starburst(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val n = 24
    for (i in 0 until n) {
        val a = i * 2 * PI.toFloat() / n + t * 0.5f
        val len = r * (0.55f + (0.2f + amp * 1.5f) * abs(sin(i * 1.3f + t * 2f)))
        val e = Offset(c.x + cos(a) * len, c.y + sin(a) * len)
        drawLine(Brush.linearGradient(listOf(GoldLight, Color.Transparent), start = c, end = e), c, e, strokeWidth = 3f * density)
    }
    soft(Color.White, c, r * (0.3f + amp), 0.95f)
}

private fun DrawScope.halo(c: Offset, r: Float, t: Float, amp: Float) {
    for (i in 0 until 90) {
        val a = i * 2 * PI.toFloat() / 90 + t * 0.4f
        val d = r * (0.72f + 0.1f * sin(i * 1.9f + t * 3f) + amp * 0.5f * sin(i * 0.7f + t * 4f))
        drawCircle(if (i % 3 == 0) GoldLight else Gold, radius = r * (0.025f + 0.02f * abs(sin(i + t * 2f))) + amp * 4f, center = Offset(c.x + cos(a) * d, c.y + sin(a) * d))
    }
    soft(Gold, c, r * (0.45f + amp), 0.7f); soft(Color.White, c, r * 0.15f)
}

private fun DrawScope.diamonds(c: Offset, r: Float, t: Float, amp: Float) {
    for (i in 0 until 4) {
        val s = r * (0.35f + i * 0.17f + amp * 0.4f)
        rotate((t * 57.3f * (if (i % 2 == 0) 1f else -1f)) / (1 + i * 0.5f) + i * 12f, c) {
            drawRect(listOf(Gold, GoldLight, warm(), GoldDark)[i].copy(alpha = 0.85f - i * 0.12f), topLeft = Offset(c.x - s / 1.4f, c.y - s / 1.4f), size = Size(s * 1.43f, s * 1.43f), style = Stroke(width = 2f * density))
        }
    }
    soft(Color.White, c, r * (0.2f + amp), 0.9f)
}

private fun DrawScope.sweep(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    rotate(t * 57.3f, c) {
        drawCircle(Brush.sweepGradient(listOf(Gold, GoldLight, warm(), GoldDark, Gold), center = c), radius = r, center = c)
    }
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Transparent), center = c, radius = r * (0.55f + amp)), radius = r, center = c)
}

private fun DrawScope.waveSphere(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (b in 0 until 3) {
        val p = Path(); val level = c.y + r * (0.15f - b * 0.12f) - amp * r * 0.8f
        p.moveTo(c.x - r, c.y + r); var x = c.x - r
        while (x <= c.x + r) { p.lineTo(x, level + sin((x - c.x) / r * 4f + t * (2f + b) + b * 2f) * r * (0.06f + amp * 0.5f)); x += r / 20f }
        p.lineTo(c.x + r, c.y + r); p.close()
        drawPath(p, listOf(GoldDark, Gold, GoldLight)[b].copy(alpha = 0.75f))
    }
}
