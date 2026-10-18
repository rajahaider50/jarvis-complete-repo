package com.jarvis.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.live.CursorGeometry
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.GoldButton
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.JToggle
import com.jarvis.app.ui.components.ScreenScaffold
import com.jarvis.app.ui.components.SubHeader
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface2

private val targets = listOf(
    -100f to 100f, 100f to 100f, -100f to -100f, 100f to -100f, 0f to 0f
)
private val targetNames = listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center")

/**
 * Cursor Lab: proves the normalized coordinate system (center 0,0; right/up positive; 2 decimals) and lets you
 * send a real tap to each of the 5 targets (4 corners + centre). Overlay cursors arrive in the next phase.
 */
@Composable
fun CursorLabScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val size = remember { CursorGeometry.screenSize(ctx) }
    var grid by remember { mutableStateOf(true) }
    var tapX by remember { mutableStateOf(0f) }
    var tapY by remember { mutableStateOf(0f) }
    var hasTap by remember { mutableStateOf(false) }
    var nextTarget by remember { mutableStateOf(0) }
    var lastResult by remember { mutableStateOf("Tap a target to test a real tap on the screen.") }
    val measurer = rememberTextMeasurer()

    ScreenScaffold(null, {}) {
        SubHeader("Cursor Lab", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            JText("Screen: ${size.w} x ${size.h} px · centre is (0,0)", 12, Muted)
            JText("Right & up are positive (+). Left & down are negative (-).", 11, Muted)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                JText("Grid", 14, Ivory, modifier = Modifier.weight(1f))
                JToggle(grid) { grid = it }
            }
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Surface2)
                    .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { off ->
                            val nx = ((off.x - this.size.width / 2f) / (this.size.width / 2f)) * 100f
                            val ny = -((off.y - this.size.height / 2f) / (this.size.height / 2f)) * 100f
                            tapX = CursorGeometry.clamp(nx); tapY = CursorGeometry.clamp(ny); hasTap = true
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = this.size.width; val h = this.size.height
                    fun px(nx: Float, ny: Float) = Offset(w / 2f + nx / 100f * (w / 2f), h / 2f - ny / 100f * (h / 2f))
                    if (grid) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
                        for (v in -100..100 step 10) {
                            val strong = v == 0
                            val col = Gold.copy(alpha = if (strong) 0.55f else 0.14f)
                            drawLine(col, px(v.toFloat(), 100f), px(v.toFloat(), -100f), strokeWidth = if (strong) 2f else 1f, pathEffect = if (strong) null else dash)
                            drawLine(col, px(-100f, v.toFloat()), px(100f, v.toFloat()), strokeWidth = if (strong) 2f else 1f, pathEffect = if (strong) null else dash)
                        }
                        for (v in -100..100 step 50) {
                            val t1 = measurer.measure(CursorGeometry.fmt(v.toFloat()), TextStyle(color = GoldLight, fontSize = 9.sp))
                            drawText(t1, topLeft = Offset(w / 2f + v / 100f * (w / 2f) + 3f, h / 2f + 3f))
                            drawText(t1, topLeft = Offset(w / 2f + 3f, h / 2f - v / 100f * (h / 2f) - 12f))
                        }
                    }
                    targets.forEachIndexed { i, (nx, ny) ->
                        val p = px(nx, ny)
                        val sel = i == nextTarget
                        drawCircle(Gold.copy(alpha = if (sel) 0.9f else 0.5f), 20f, p, style = Stroke(width = if (sel) 4f else 2f))
                        drawCircle(Gold.copy(alpha = 0.85f), 4f, p)
                        val label = measurer.measure("${i + 1} ${CursorGeometry.fmt(nx)},${CursorGeometry.fmt(ny)}", TextStyle(color = if (sel) Gold else GoldLight, fontSize = 10.sp))
                        drawText(label, topLeft = Offset(p.x + 24f, p.y - 14f))
                    }
                    if (hasTap) {
                        val p = px(tapX, tapY)
                        drawCircle(Color(0xFF66D0A0), 12f, p, style = Stroke(width = 3f))
                    }
                }
            }
            JText(if (hasTap) "Last tap:  X ${CursorGeometry.fmt(tapX)}   Y ${CursorGeometry.fmt(tapY)}" else "Tap inside the grid to read its position.", 13, Ivory, FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
            JText(lastResult, 11, Muted, modifier = Modifier.padding(top = 2.dp))
            val t = targets[nextTarget]
            GoldButton("Real tap at ${targetNames[nextTarget]} (${CursorGeometry.fmt(t.first)}, ${CursorGeometry.fmt(t.second)})", null, {
                if (!JarvisAccessibilityService.enabled) { lastResult = "Accessibility service is off. Turn it on in Android Settings > Accessibility." }
                else if (!JarvisAccessibilityService.canScreenshot && android.os.Build.VERSION.SDK_INT < 30) { lastResult = "Tap needs Android gestures (all versions support this)." }
                else {
                    JarvisAccessibilityService.tapNorm(t.first, t.second) { ok ->
                        lastResult = if (ok) "Tap sent to ${targetNames[nextTarget]} — did the tap land on the target ring?" else "Tap was blocked (rate limit) or the service is busy."
                    }
                }
                nextTarget = (nextTarget + 1) % targets.size
            }, modifier = Modifier.padding(vertical = 10.dp))
        }
    }
}
