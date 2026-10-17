package com.jarvis.app.ui.theme.hud

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.ui.theme.AssistantState
import com.jarvis.app.ui.theme.AssistantTheme
import com.jarvis.app.ui.theme.ThemeCategory
import com.jarvis.app.ui.theme.anim.arcRing
import com.jarvis.app.ui.theme.anim.rememberPhase
import com.jarvis.app.ui.theme.anim.speedFor
import com.jarvis.app.ui.theme.anim.tickRing
import com.jarvis.app.ui.theme.fx.FxProfile
import com.jarvis.app.ui.theme.orbs.ParticleSphere
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
private fun label(text: String, color: Color, size: Int = 11, weight: FontWeight = FontWeight.Medium) =
    Text(text, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace, fontWeight = weight)

class HudTheme(
    override val id: String, override val displayName: String, override val fx: FxProfile,
    private val content: @Composable (AssistantState, Float, Modifier) -> Unit
) : AssistantTheme {
    override val category = ThemeCategory.HUD
    @Composable override fun Render(state: AssistantState, level: Float, modifier: Modifier) = content(state, level, modifier)
}

// ------------------------------------------------------------------- Arc Reactor
@Composable
fun HudArcReactor(state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    val phase by rememberPhase(speedFor(state))
    val smooth by animateFloatAsState(level.coerceIn(0f, 1f), tween(100), label = "hudLevel")
    val teal = Color(0xFF19F0D0)
    val tealDim = teal.copy(alpha = 0.35f)
    val amber = Color(0xFFFF8A2B)
    Box(modifier.background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width, size.height) / 2f * 0.92f
            val px = r / 100f
            drawCircle(tealDim, radius = r, center = c, style = Stroke(1.5f * px))
            tickRing(c, r, 72, 4f * px, phase * 6f, tealDim, px)
            for (k in 0 until 3) {
                val base = r * (0.82f - k * 0.12f)
                val dir = if (k % 2 == 0) 1f else -1f
                for (s in 0 until 4) {
                    arcRing(c, base, phase * (30f + 15f * k) * dir + s * 90f, 55f, teal, (3.5f - k * 0.6f) * px)
                }
            }
            arcRing(c, r * 0.95f, -phase * 20f, 18f, amber, 3f * px)
            arcRing(c, r * 0.95f, -phase * 20f + 180f, 18f, amber, 3f * px)
            val coreR = r * (0.22f + 0.10f * smooth)
            drawCircle(
                Brush.radialGradient(listOf(teal, teal.copy(alpha = 0f)), center = c, radius = coreR * 1.8f),
                radius = coreR * 1.8f, center = c
            )
            drawCircle(teal, radius = coreR * 0.55f, center = c)
        }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopStart) { label("ARC REACTOR", teal) }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomEnd) { label(state.name, amber) }
    }
}

// ------------------------------------------------------------------- Radar
@Composable
fun HudRadar(state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    val phase by rememberPhase(speedFor(state))
    val green = Color(0xFF3DFF7A)
    val blips = remember { listOf(0.4f to 0.35f, 2.1f to 0.6f, 4.0f to 0.8f, 5.2f to 0.5f, 1.2f to 0.75f) }
    Box(modifier.background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width, size.height) / 2f * 0.9f
            val px = r / 100f
            for (i in 1..4) drawCircle(green.copy(alpha = 0.25f), radius = r * i / 4f, center = c, style = Stroke(1.5f * px))
            drawLine(green.copy(alpha = 0.2f), Offset(c.x - r, c.y), Offset(c.x + r, c.y))
            drawLine(green.copy(alpha = 0.2f), Offset(c.x, c.y - r), Offset(c.x, c.y + r))
            rotate(degrees = phase * 90f, pivot = c) {
                drawCircle(Brush.sweepGradient(listOf(Color.Transparent, green.copy(alpha = 0.55f)), center = c), radius = r, center = c)
            }
            val sweep = (phase * 90f) % 360f
            blips.forEach { (angDeg, dist) ->
                val aDeg = Math.toDegrees(angDeg.toDouble())
                val diff = ((sweep - aDeg) % 360.0 + 360.0) % 360.0
                val fade = (1f - (diff / 120.0).toFloat()).coerceIn(0f, 1f)
                if (fade > 0.05f) {
                    val a = angDeg
                    drawCircle(green.copy(alpha = 0.15f + 0.85f * fade), radius = 3f * px, center = Offset(c.x + cos(a) * r * dist, c.y + sin(a) * r * dist))
                }
            }
            drawCircle(green.copy(alpha = 0.4f + 0.6f * level), radius = r, center = c, style = Stroke((1.5f + level * 2f) * px))
        }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopStart) { label("RADAR SWEEP", green) }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomEnd) { label("RANGE 100", green) }
    }
}

// ------------------------------------------------------------------- Holo Globe
@Composable
fun HudGlobe(state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    val phase by rememberPhase(speedFor(state))
    val cyan = Color(0xFF46E0FF)
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        ParticleSphere(state, level, Modifier.fillMaxSize(), color = cyan, count = 900)
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width, size.height) / 2f * 0.9f
            val px = r / 100f
            tickRing(c, r, 60, 3f * px, phase * 4f, cyan.copy(alpha = 0.3f), px)
            rotate(degrees = 20f, pivot = c) {
                drawOval(cyan.copy(alpha = 0.5f), topLeft = Offset(c.x - r * 0.95f, c.y - r * 0.32f), size = Size(r * 1.9f, r * 0.64f), style = Stroke(2f * px))
            }
            val od = phase % (2 * PI.toFloat()); val ox = c.x + cos(od) * r * 0.95f; val oy = c.y + sin(od) * r * 0.32f
            drawCircle(Color.White, radius = 3f * px, center = Offset(ox, oy))
            for (k in 0 until 3) {
                val h = (0.3f + 0.7f * abs(sin(phase * (k + 1)))).toFloat()
                val bx = size.width - 26 * px
                val bw = 8 * px
                val bh = r * 1.2f * h
                drawRoundRect(cyan.copy(alpha = 0.7f), topLeft = Offset(bx, c.y + r * 0.6f - bh), size = Size(bw, bh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * px))
            }
        }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.TopStart) { label("LINK: ACTIVE", cyan) }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) { label("STATE: ${state.name}", cyan) }
    }
}

// ------------------------------------------------------------------- Stark Amber
@Composable
fun HudAmber(state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    val phase by rememberPhase(speedFor(state))
    val smooth by animateFloatAsState(level.coerceIn(0f, 1f), tween(120), label = "amberLevel")
    val orange = Color(0xFFFF7A00)
    val red = Color(0xFFFF2D2D)
    val hexOffsets = remember {
        FloatArray(24) { j ->
            val i = j / 2
            val a = PI / 3 * i
            if (j % 2 == 0) cos(a).toFloat() else sin(a).toFloat()
        }
    }
    Box(modifier.background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width, size.height) / 2f * 0.9f
            val px = r / 100f
            val hexR = r * 0.16f
            for (row in -2..2) for (col in -2..2) {
                val hx = c.x + col * hexR * 1.5f
                val hy = c.y + row * hexR * 1.732f + (if (col % 2 != 0) hexR * 0.866f else 0f)
                val p = Path()
                for (i in 0 until 6) {
                    val x = hx + hexOffsets[i * 2] * hexR
                    val y = hy + hexOffsets[i * 2 + 1] * hexR
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, orange.copy(alpha = 0.12f), style = Stroke(1f))
            }
            val gauge = 90f + 60f * smooth
            arcRing(c, r * 0.8f, 150f, gauge, orange, 6f * px)
            arcRing(c, r * 0.8f, 150f, 210f, orange.copy(alpha = 0.15f), 6f * px)
            if (state == AssistantState.THINKING) {
                val fa = 0.4f + 0.6f * abs(sin(phase * 3f))
                arcRing(c, r * 0.95f, -phase * 10f, 40f, red.copy(alpha = fa), 4f * px)
            }
            val n = 24
            val barW = size.width * 0.7f / (n * 1.8f)
            for (i in 0 until n) {
                val h = (0.15f + 0.85f * abs(sin(phase * 2 + i * 0.5f)) * (0.4f + 0.6f * smooth)).toFloat()
                val x = size.width * 0.15f + i * barW * 1.8f
                val bh = min(size.height * 0.18f, size.height * 0.18f * h)
                drawRoundRect(if (i % 3 == 0) red else orange, topLeft = Offset(x, size.height - 16 * px - bh), size = Size(barW, bh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f))
            }
        }
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopStart) { label("POWER ${(smooth * 100).toInt()}%", orange, 16, FontWeight.Bold) }
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) { label("SYS OK", red) }
    }
}

// ------------------------------------------------------------------- Minimal Frost
@Composable
fun HudFrost(state: AssistantState, level: Float, modifier: Modifier = Modifier) {
    val phase by rememberPhase(speedFor(state))
    val smooth by animateFloatAsState(level.coerceIn(0f, 1f), tween(160), label = "frostLevel")
    val ice = Color(0xFFE8F4FF)
    Box(modifier.background(Color(0xFF05070A))) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width, size.height) / 2f * 0.85f
            val px = r / 100f
            for (k in 0 until 3) {
                arcRing(c, r * (0.6f + k * 0.15f), phase * (12f + 6f * k) * if (k % 2 == 0) 1f else -1f, 70f, ice.copy(alpha = 0.18f + 0.12f * k), 1f * px)
            }
            val coreR = r * (0.16f + 0.12f * smooth)
            drawCircle(Brush.radialGradient(listOf(ice.copy(alpha = 0.6f), ice.copy(alpha = 0f)), center = c, radius = coreR * 2f), radius = coreR * 2f, center = c)
            drawCircle(ice.copy(alpha = 0.9f), radius = coreR * 0.4f, center = c)
        }
        Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.BottomCenter) { label(state.name, ice.copy(alpha = 0.8f), 12) }
    }
}
