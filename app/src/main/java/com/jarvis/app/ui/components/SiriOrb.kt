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
    "Neon Ring", "Equalizer Ring", "Liquid Drop", "Starburst", "Particle Halo", "Diamond Rings", "Breathing Sweep", "Wave Sphere",
    "Solar Crown", "Lotus Bloom", "Hex Weave", "Golden Torus", "Prism Light", "Plasma Veil", "Comet Trail", "Binary Rain",
    "Rose Window", "Spirograph", "Nebula", "Pendulum", "Barcode Ring", "Wavelength", "Kaleidoscope", "Sonic Rings",
    "Crystal Core", "Aurora Ribbon", "Quantum Dots", "Infinity Loop", "Gear Ring", "Facet Diamond", "Particle Storm", "Liquid Metal",
    "Pulse Grid", "Star Lattice", "Halo Orbits", "Vortex", "Ripple Mesh", "Aurora Curtains", "Obsidian", "Golden Spiral",
    "Prism Sweep", "Supernova"
)

/** mode: 0 idle, 1 listening (reacts to level), 2 thinking, 3 speaking. style: 0..49 (see ORB_STYLES). */
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
        when (style.coerceIn(0, 49)) {
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
            15 -> waveSphere(c, r, t, amp)
            16 -> solarCrown(c, r, t, amp)
            17 -> lotus(c, r, t, amp)
            18 -> hexWeave(c, r, t, amp)
            19 -> torus(c, r, t, amp)
            20 -> prism(c, r, t, amp)
            21 -> plasmaVeil(c, r, t, amp)
            22 -> comet(c, r, t, amp)
            23 -> binaryRain(c, r, t, amp)
            24 -> rose(c, r, t, amp)
            25 -> spiro(c, r, t, amp)
            26 -> nebula(c, r, t, amp)
            27 -> pendulum(c, r, t, amp)
            28 -> barcode(c, r, t, amp)
            29 -> wavelength(c, r, t, amp)
            30 -> kaleido(c, r, t, amp)
            31 -> sonic(c, r, t, amp)
            32 -> crystal(c, r, t, amp)
            33 -> ribbon(c, r, t, amp)
            34 -> quantum(c, r, t, amp)
            35 -> infinity(c, r, t, amp)
            36 -> gear(c, r, t, amp)
            37 -> facet(c, r, t, amp)
            38 -> storm(c, r, t, amp)
            39 -> metal(c, r, t, amp)
            40 -> pulseGrid(c, r, t, amp)
            41 -> lattice(c, r, t, amp)
            42 -> haloOrbits(c, r, t, amp)
            43 -> vortex(c, r, t, amp)
            44 -> mesh(c, r, t, amp)
            45 -> curtains(c, r, t, amp)
            46 -> obsidian(c, r, t, amp)
            47 -> goldenSpiral(c, r, t, amp)
            48 -> prismSweep(c, r, t, amp)
            else -> supernova(c, r, t, amp)
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

private fun polygon(c: Offset, r: Float, n: Int, rot: Float = 0f): Path {
    val p = Path()
    for (i in 0 until n) {
        val a = rot + i * 2 * PI.toFloat() / n
        val x = c.x + cos(a) * r; val y = c.y + sin(a) * r
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

private fun DrawScope.solarCrown(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val n = 32
    for (i in 0 until n) {
        val a = i * 2 * PI.toFloat() / n + t * 0.5f
        val len = r * (0.6f + (0.25f + amp * 1.4f) * abs(sin(i * 0.7f + t * 2f)))
        val e = Offset(c.x + cos(a) * len, c.y + sin(a) * len)
        drawLine(Brush.linearGradient(listOf(GoldLight, Color.Transparent), start = c, end = e), c, e, strokeWidth = 2f * density)
    }
    soft(Gold, c, r * (0.5f + amp), 0.7f); soft(Color.White, c, r * (0.28f + amp * 0.6f))
}

private fun DrawScope.lotus(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val petals = 8
    for (i in 0 until petals) {
        val a = i * 2 * PI.toFloat() / petals + t * 0.3f
        val tip = Offset(c.x + cos(a) * r * (0.82f + amp * 0.6f), c.y + sin(a) * r * (0.82f + amp * 0.6f))
        val l = Offset(c.x + cos(a - 0.35f) * r * 0.3f, c.y + sin(a - 0.35f) * r * 0.3f)
        val rr = Offset(c.x + cos(a + 0.35f) * r * 0.3f, c.y + sin(a + 0.35f) * r * 0.3f)
        val p = Path(); p.moveTo(c.x, c.y); p.quadraticBezierTo(l.x, l.y, tip.x, tip.y); p.quadraticBezierTo(rr.x, rr.y, c.x, c.y); p.close()
        drawPath(p, listOf(Gold, GoldLight, warm())[i % 3].copy(alpha = 0.5f))
    }
    soft(Color.White, c, r * (0.25f + amp), 0.9f)
}

private fun DrawScope.hexWeave(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (i in 0 until 5) {
        val s = r * (0.25f + i * 0.16f + amp * 0.3f)
        drawPath(polygon(c, s, 6, t * (if (i % 2 == 0) 0.4f else -0.3f) + i * 0.2f), listOf(Gold, GoldLight, warm())[i % 3].copy(alpha = 0.7f - i * 0.08f), style = Stroke(width = 1.8f * density))
    }
    soft(GoldLight, c, r * (0.3f + amp), 0.8f)
}

private fun DrawScope.torus(c: Offset, r: Float, t: Float, amp: Float) {
    soft(Gold, c, r * 1.0f, 0.2f)
    rotate(t * 57.3f, c) {
        drawCircle(Brush.sweepGradient(listOf(Gold.copy(alpha = 0.05f), GoldLight, Gold, warm(), GoldDark, Gold.copy(alpha = 0.05f)), center = c), radius = r * 0.7f, center = c, style = Stroke(width = r * (0.22f + amp * 0.5f)))
    }
    drawCircle(Gold.copy(alpha = 0.3f), radius = r * 0.55f, center = c, style = Stroke(width = 1f * density))
    soft(Color.White, c, r * (0.16f + amp), 0.8f)
}

private fun DrawScope.prism(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (i in 0 until 6) {
        val a = t * 0.6f + i * 2 * PI.toFloat() / 6
        val tip = Offset(c.x + cos(a) * r * (0.8f + amp * 0.4f), c.y + sin(a) * r * (0.8f + amp * 0.4f))
        val p = Path(); p.moveTo(c.x, c.y); p.lineTo(c.x + cos(a - 0.5f) * r * 0.5f, c.y + sin(a - 0.5f) * r * 0.5f); p.lineTo(tip.x, tip.y); p.close()
        drawPath(p, listOf(Gold, GoldLight, warm(), GoldDark)[i % 4].copy(alpha = 0.45f))
    }
    soft(Color.White, c, r * (0.22f + amp), 0.85f)
}

private fun DrawScope.plasmaVeil(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val cols = listOf(GoldDark, Gold, GoldLight, warm())
    for (b in 0 until 5) {
        val base = c.y + r * (0.5f - b * 0.25f)
        val p = Path(); p.moveTo(c.x - r, c.y + r); var x = c.x - r
        while (x <= c.x + r) {
            val y = base + sin((x - c.x) / r * 2.5f + t * (1f + b * 0.3f) + b) * r * (0.08f + amp * 0.6f)
            p.lineTo(x, y); x += r / 20f
        }
        p.lineTo(c.x + r, c.y + r); p.close()
        drawPath(p, Brush.verticalGradient(listOf(cols[b % 4].copy(alpha = 0.5f), Color.Transparent), startY = base - r * 0.4f, endY = c.y + r))
    }
}

private fun DrawScope.comet(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    soft(GoldLight, c, r * (0.3f + amp), 0.7f)
    val a = t * 1.3f
    for (i in 0 until 18) {
        val f = i / 18f
        val aa = a - f * 1.1f
        val d = r * (0.72f - f * 0.1f)
        drawCircle(GoldLight.copy(alpha = 0.7f * (1f - f)), radius = r * (0.07f * (1f - f * 0.7f) + amp * 0.15f), center = Offset(c.x + cos(aa) * d, c.y + sin(aa) * d))
    }
    soft(Color.White, Offset(c.x + cos(a) * r * 0.72f, c.y + sin(a) * r * 0.72f), r * (0.12f + amp), 0.95f)
}

private fun DrawScope.binaryRain(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val cols = 7
    for (i in 0 until cols) {
        val x = c.x - r + (i + 0.5f) * (2f * r / cols)
        val off = (sin(i * 1.7f) + 1f) / 2f
        val f = ((t / (2 * PI.toFloat())) + off) % 1f
        for (j in 0 until 9) {
            val g = (f + j / 9f) % 1f
            drawCircle(GoldLight.copy(alpha = (1f - g) * (0.6f + amp)), radius = r * (0.02f + amp * 0.1f), center = Offset(x, c.y - r + g * 2f * r))
        }
    }
}

private fun DrawScope.rose(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val p = Path(); val n = 200; val k = 5f
    for (i in 0..n) {
        val a = i * 2 * PI.toFloat() / n
        val d = r * (0.85f + amp * 0.3f) * abs(cos(k * a + t))
        val x = c.x + cos(a) * d; val y = c.y + sin(a) * d
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, Brush.linearGradient(listOf(GoldLight, Gold, warm()), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)), style = Stroke(width = 2.2f * density))
    soft(Color.White, c, r * (0.2f + amp), 0.8f)
}

private fun DrawScope.spiro(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val bigR = r * 0.55f; val small = r * 0.22f; val d = r * 0.5f
    val p = Path(); val n = 320
    for (i in 0..n) {
        val a = i * 2 * PI.toFloat() / n
        val x = c.x + (bigR - small) * cos(a) + d * cos((bigR - small) / small * a + t)
        val y = c.y + (bigR - small) * sin(a) - d * sin((bigR - small) / small * a + t)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, Gold.copy(alpha = 0.25f), style = Stroke(width = (4f + amp * 10f) * density))
    drawPath(p, GoldLight.copy(alpha = 0.9f), style = Stroke(width = 1.6f * density))
    soft(Color.White, c, r * (0.16f + amp))
}

private fun DrawScope.nebula(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val cols = listOf(Gold, GoldLight, warm(), GoldDark)
    for (i in 0 until 9) {
        val a = i * 2.399f + t * 0.4f
        val d = r * (0.15f + (i % 4) * 0.18f)
        soft(cols[i % 4], Offset(c.x + cos(a) * d, c.y + sin(a) * d), r * (0.4f + amp * 0.5f), 0.5f)
    }
    soft(Color.White, c, r * (0.3f + amp), 0.85f)
}

private fun DrawScope.pendulum(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (i in 0 until 5) {
        val a = sin(t * (1f + i * 0.2f) + i) * 0.9f
        val e = Offset(c.x + sin(a) * r * 0.8f, c.y - r * 0.1f + cos(a) * r * 0.8f)
        drawLine(listOf(Gold, GoldLight, warm(), GoldDark)[i % 4].copy(alpha = 0.8f), Offset(c.x, c.y - r * 0.6f), e, strokeWidth = 2f * density)
        soft(Color.White, e, r * (0.1f + amp * 0.5f), 0.9f)
    }
}

private fun DrawScope.barcode(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val n = 40
    for (i in 0 until n) {
        val a = i * 2 * PI.toFloat() / n
        val h = r * (0.1f + (0.12f + amp) * abs(sin(i * 1.7f + t * 2f)))
        val s = Offset(c.x + cos(a) * r * 0.6f, c.y + sin(a) * r * 0.6f)
        val e = Offset(c.x + cos(a) * (r * 0.6f + h), c.y + sin(a) * (r * 0.6f + h))
        drawLine(if (i % 3 == 0) GoldLight else Gold.copy(alpha = 0.7f), s, e, strokeWidth = 2.5f * density, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    soft(Gold, c, r * (0.45f + amp), 0.5f); soft(Color.White, c, r * 0.16f)
}

private fun DrawScope.wavelength(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (b in 0 until 6) {
        val p = Path(); var x = c.x - r
        val y0 = c.y + r * (0.5f - b * 0.2f)
        p.moveTo(x, y0)
        while (x <= c.x + r) { p.lineTo(x, y0 + sin((x - c.x) / r * 3f + t * (2f + b * 0.5f) + b) * r * (0.05f + amp * 0.4f)); x += r / 24f }
        drawPath(p, listOf(Gold, GoldLight, warm())[b % 3].copy(alpha = 0.75f - b * 0.08f), style = Stroke(width = 2f * density))
    }
}

private fun DrawScope.kaleido(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val seg = 12
    for (i in 0 until seg) {
        rotate(i * 360f / seg + t * 10f, c) {
            val p = Path(); p.moveTo(c.x, c.y)
            p.lineTo(c.x + r * (0.7f + amp * 0.5f), c.y - r * 0.12f)
            p.lineTo(c.x + r * (0.7f + amp * 0.5f), c.y + r * 0.12f); p.close()
            drawPath(p, if (i % 2 == 0) Gold.copy(alpha = 0.5f) else GoldLight.copy(alpha = 0.4f))
        }
    }
    soft(Color.White, c, r * (0.2f + amp), 0.85f)
}

private fun DrawScope.sonic(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val k = t / (2 * PI.toFloat())
    for (i in 0 until 6) {
        val f = (k * 1.5f + i / 6f) % 1f
        drawCircle(GoldLight.copy(alpha = (1f - f) * (0.5f + amp)), radius = r * f * (0.9f + amp * 0.4f), center = c, style = Stroke(width = (1.5f + f * 2f) * density))
    }
    soft(Gold, c, r * (0.4f + amp), 0.6f); soft(Color.White, c, r * 0.16f)
}

private fun DrawScope.crystal(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    rotate(t * 20f, c) {
        for (i in 0 until 3) {
            drawPath(polygon(c, r * (0.75f - i * 0.2f + amp * 0.3f), 6, i * 0.6f), listOf(Gold, GoldLight, warm())[i].copy(alpha = 0.65f - i * 0.12f), style = Stroke(width = 1.8f * density))
        }
    }
    soft(GoldLight, c, r * (0.35f + amp), 0.8f); soft(Color.White, c, r * (0.2f + amp))
}

private fun DrawScope.ribbon(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val p = Path(); val n = 80
    for (i in 0..n) {
        val f = i / n.toFloat()
        val x = c.x - r + f * 2f * r
        val y = c.y + sin(f * 6f + t * 2f) * r * (0.3f + amp * 0.6f)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, Brush.linearGradient(listOf(GoldLight, Gold, warm(), GoldDark), start = Offset(c.x - r, c.y), end = Offset(c.x + r, c.y)), style = Stroke(width = r * (0.18f + amp * 0.4f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    soft(Color.White, c, r * 0.18f, 0.6f)
}

private fun DrawScope.quantum(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val n = 6
    for (ix in 0 until n) for (iy in 0 until n) {
        val x = c.x - r * 0.7f + ix * (1.4f * r / (n - 1))
        val y = c.y - r * 0.7f + iy * (1.4f * r / (n - 1))
        val ph = sin(ix * 0.9f + iy * 1.3f + t * 2f)
        drawCircle(GoldLight.copy(alpha = 0.35f + 0.4f * abs(ph)), radius = r * (0.05f + amp * 0.15f) * (0.6f + abs(ph)), center = Offset(x, y))
    }
}

private fun DrawScope.infinity(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    rotate(t * 15f, c) {
        val p = Path(); val n = 240
        for (i in 0..n) {
            val a = i * 2 * PI.toFloat() / n
            val d = r * (0.6f + amp * 0.3f)
            val den = 1f + sin(a) * sin(a)
            val x = c.x + d * cos(a) / den
            val y = c.y + d * sin(a) * cos(a) / den
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, Gold.copy(alpha = 0.3f), style = Stroke(width = (5f + amp * 12f) * density))
        drawPath(p, GoldLight.copy(alpha = 0.9f), style = Stroke(width = 2f * density))
    }
    soft(Color.White, c, r * 0.16f)
}

private fun DrawScope.gear(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val teeth = 16
    for (i in 0 until teeth) {
        val a = i * 2 * PI.toFloat() / teeth + t * 0.3f
        val inner = r * (0.55f + amp * 0.2f)
        val outer = r * (0.78f + amp * 0.3f)
        drawLine(if (i % 2 == 0) Gold else GoldLight, Offset(c.x + cos(a) * inner, c.y + sin(a) * inner), Offset(c.x + cos(a) * outer, c.y + sin(a) * outer), strokeWidth = r * 0.08f)
    }
    drawCircle(Gold.copy(alpha = 0.6f), radius = r * (0.55f + amp * 0.2f), center = c, style = Stroke(width = 2f * density))
    soft(Color.White, c, r * (0.22f + amp), 0.85f)
}

private fun DrawScope.facet(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    rotate(t * 18f, c) {
        val reach = r * (0.85f + amp * 0.3f)
        drawPath(polygon(c, reach, 4, 0.7854f), Brush.linearGradient(listOf(Gold, GoldLight, warm(), GoldDark), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)), style = Stroke(width = 2.5f * density))
        for (i in 0 until 4) {
            val a = 0.7854f + i * 2 * PI.toFloat() / 4
            drawLine(GoldLight.copy(alpha = 0.5f), c, Offset(c.x + cos(a) * reach, c.y + sin(a) * reach), strokeWidth = 1.2f * density)
        }
    }
    soft(Color.White, c, r * (0.18f + amp), 0.8f)
}

private fun DrawScope.storm(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (i in 0 until 70) {
        val a = i * 2.399f + t * (2f + (i % 5) * 0.4f)
        val d = r * ((i % 10) / 10f) * (0.9f + amp * 0.4f)
        drawCircle(listOf(Gold, GoldLight, warm())[i % 3].copy(alpha = 0.85f - (i % 10) * 0.06f), radius = r * (0.03f + amp * 0.12f), center = Offset(c.x + cos(a) * d, c.y + sin(a) * d))
    }
    soft(Color.White, c, r * (0.2f + amp), 0.7f)
}

private fun DrawScope.metal(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    rotate(t * 40f, c) {
        drawCircle(Brush.sweepGradient(listOf(GoldDark, GoldLight, Color.White, Gold, warm(), GoldDark), center = c), radius = r * (0.72f + amp * 0.3f), center = c)
    }
    soft(GoldDark, Offset(c.x + r * 0.3f, c.y + r * 0.3f), r * 0.5f, 0.5f)
    soft(Color.White, Offset(c.x - r * 0.3f, c.y - r * 0.35f), r * 0.3f, 0.7f)
}

private fun DrawScope.pulseGrid(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (i in 0 until 5) {
        val s = r * (0.2f + i * 0.18f + amp * 0.35f)
        val al = (0.8f - i * 0.13f) * (0.6f + 0.4f * abs(sin(t * 2f + i)))
        drawRect(GoldLight.copy(alpha = al), topLeft = Offset(c.x - s, c.y - s), size = Size(s * 2f, s * 2f), style = Stroke(width = 2f * density))
    }
    soft(Color.White, c, r * (0.16f + amp))
}

private fun DrawScope.lattice(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val n = 4
    val pts = ArrayList<Offset>()
    for (ix in 0 until n) for (iy in 0 until n) {
        pts.add(Offset(c.x - r * 0.6f + ix * (1.2f * r / (n - 1)), c.y - r * 0.6f + iy * (1.2f * r / (n - 1))))
    }
    for (i in pts.indices) for (j in i + 1 until pts.size) {
        val dx = pts[i].x - pts[j].x; val dy = pts[i].y - pts[j].y
        if (dx * dx + dy * dy <= r * r * 0.56f) drawLine(Gold.copy(alpha = 0.35f), pts[i], pts[j], strokeWidth = 1.2f * density)
    }
    pts.forEachIndexed { i, p -> drawCircle(if (i % 3 == 0) GoldLight else Gold, radius = r * (0.03f + amp * 0.1f), center = p) }
}

private fun DrawScope.haloOrbits(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    soft(Gold, c, r * (0.5f + amp), 0.5f)
    for (i in 0 until 4) {
        rotate(t * (8f + i * 6f) * (if (i % 2 == 0) 1f else -1f), c) {
            drawOval(listOf(Gold, GoldLight, warm(), GoldDark)[i].copy(alpha = 0.7f), topLeft = Offset(c.x - r * (0.7f + i * 0.05f), c.y - r * (0.3f + amp * 0.2f)), size = Size(r * (1.4f + i * 0.1f), r * (0.6f + amp * 0.4f)), style = Stroke(width = 1.8f * density))
        }
    }
    soft(Color.White, c, r * (0.2f + amp), 0.9f)
}

private fun DrawScope.vortex(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    for (s in 0 until 6) {
        val p = Path(); val n = 60
        for (i in 0..n) {
            val f = i / n.toFloat()
            val a = f * 6f * PI.toFloat() + t * 1.5f + s * 1.05f
            val d = r * f * (0.9f + amp * 0.3f)
            val x = c.x + cos(a) * d; val y = c.y + sin(a) * d
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, listOf(Gold, GoldLight, warm())[s % 3].copy(alpha = 0.6f), style = Stroke(width = 1.6f * density))
    }
    soft(Color.White, c, r * (0.18f + amp), 0.9f)
}

private fun DrawScope.mesh(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val rings = 5
    for (i in 1..rings) drawCircle(Gold.copy(alpha = 0.35f), radius = r * i / rings * (1f + amp * 0.2f), center = c, style = Stroke(width = 1.2f * density))
    val spokes = 16
    for (i in 0 until spokes) {
        val a = i * 2 * PI.toFloat() / spokes + t * 0.4f
        drawLine(GoldLight.copy(alpha = 0.3f), c, Offset(c.x + cos(a) * r * (1f + amp * 0.2f), c.y + sin(a) * r * (1f + amp * 0.2f)), strokeWidth = 1f * density)
    }
    soft(Color.White, c, r * (0.18f + amp), 0.85f)
}

private fun DrawScope.curtains(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val cols = listOf(GoldLight, Gold, warm(), GoldDark)
    for (b in 0 until 5) {
        val x0 = c.x - r + b * (2f * r / 4f)
        for (j in 0 until 14) {
            val g = j / 14f
            val x = x0 + sin(t * 1.5f + g * 4f + b) * r * (0.15f + amp * 0.5f)
            drawCircle(cols[b % 4].copy(alpha = (1f - g) * 0.5f), radius = r * (0.08f + amp * 0.12f), center = Offset(x, c.y - r + g * 2f * r))
        }
    }
    soft(Color.White, c, r * (0.22f + amp), 0.7f)
}

private fun DrawScope.obsidian(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r, Color(0xFF111015)) {
    rotate(t * 12f, c) {
        for (i in 0 until 6) {
            val a0 = i * 2 * PI.toFloat() / 6
            val a1 = (i + 1) * 2 * PI.toFloat() / 6
            val reach = r * (0.8f + amp * 0.2f)
            val p = Path(); p.moveTo(c.x, c.y)
            p.lineTo(c.x + cos(a0) * reach, c.y + sin(a0) * reach)
            p.lineTo(c.x + cos(a1) * reach, c.y + sin(a1) * reach); p.close()
            drawPath(p, Gold.copy(alpha = 0.12f * (i + 1)))
        }
    }
    drawCircle(GoldLight.copy(alpha = 0.7f), radius = r * (0.8f + amp * 0.2f), center = c, style = Stroke(width = 1.5f * density))
    soft(Color.White, c, r * (0.16f + amp), 0.6f)
}

private fun DrawScope.goldenSpiral(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val p = Path(); val n = 220
    for (i in 0..n) {
        val f = i / n.toFloat()
        val a = f * 6.5f * PI.toFloat() + t * 0.6f
        val d = r * (0.08f + f * 0.85f) * (1f + amp * 0.2f)
        val x = c.x + cos(a) * d; val y = c.y + sin(a) * d
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, Brush.linearGradient(listOf(GoldLight, Gold, warm()), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)), style = Stroke(width = 2.2f * density))
    soft(Color.White, c, r * (0.16f + amp), 0.85f)
}

private fun DrawScope.prismSweep(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val seg = 8
    rotate(t * 30f, c) {
        val s = r * (1.6f + amp * 0.4f)
        for (i in 0 until seg) {
            drawArc(listOf(Gold, GoldLight, warm(), GoldDark)[i % 4].copy(alpha = 0.7f), startAngle = i * 360f / seg, sweepAngle = 360f / seg - 4f, useCenter = true, topLeft = Offset(c.x - s / 2f, c.y - s / 2f), size = Size(s, s))
        }
    }
    soft(Color.White, c, r * (0.18f + amp), 0.8f)
}

private fun DrawScope.supernova(c: Offset, r: Float, t: Float, amp: Float) = ball(c, r) {
    val burst = (t / (2 * PI.toFloat()) * 2f) % 1f
    for (i in 0 until 40) {
        val a = i * 2.399f
        val d = r * (0.15f + burst * 0.8f + amp * 0.3f)
        drawCircle(listOf(GoldLight, Gold, warm())[i % 3].copy(alpha = (1f - burst) * 0.8f), radius = r * (0.05f * (1f - burst) + amp * 0.08f), center = Offset(c.x + cos(a) * d, c.y + sin(a) * d))
    }
    soft(Color.White, c, r * (0.3f * (1f - burst) + amp), 0.95f)
}
