package com.jarvis.app.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jarvis.app.live.CursorController
import com.jarvis.app.live.CursorGeometry
import com.jarvis.app.live.Diag
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.GoldButton
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.OutlineButton
import com.jarvis.app.ui.theme.Bg
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.hypot

/**
 * Tap Test (bugfix Masla A / §2.6). Five targets (centre + 4 corners). For each one it puts a real cursor on the
 * target and fires a REAL accessibility gesture, then compares the pixel the tap was sent to with the raw pixel that
 * actually received the touch. This is the calibration ground truth: it tells us whether the coordinate mapping and
 * `dispatchGesture` agree, and whether the cursor window swallows the tap.
 */
@Composable
fun TapTestScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // ±65 keeps the 4 corners clear of the top/bottom controls so the measured ring is never under UI.
    val targets = remember { listOf(0f to 0f, -65f to 65f, 65f to 65f, -65f to -65f, 65f to -65f) }
    var rows by remember { mutableStateOf<List<TapRow>>(emptyList()) }
    var running by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("Place the phone flat and start. Each target gets a real tap; we compare sent px vs received px.") }
    val received = remember { mutableStateOf<Pair<Float, Float>?>(null) }

    fun runTest() {
        if (!JarvisAccessibilityService.enabled) {
            note = "Accessibility service is OFF. Enable Jarvis in Android Settings > Accessibility first."
            return
        }
        running = true; rows = emptyList(); step = 0
        note = "Running…"
        scope.launch {
            val sz = CursorGeometry.screenSize(ctx)
            val out = mutableListOf<TapRow>()
            CursorController.clear()
            delay(250)
            Diag.log("TapTest", "start display=${sz.w}x${sz.h} caps=${JarvisAccessibilityService.gestureCapabilities()}")
            targets.forEachIndexed { i, (nx, ny) ->
                step = i + 1
                received.value = null
                val id = CursorController.add(nx, ny, "TT${i + 1}")
                delay(350)
                val exp = CursorGeometry.toPx(nx, ny, sz)
                val r = withContext(Dispatchers.Default) { CursorController.clickResult(id, CursorController.ClickType.TAP, 700) }
                delay(550)                                     // give the touch time to reach the view
                var rec = received.value
                var retried = false
                if (rec == null) {
                    // the cursor window probably swallowed the tap (the NOT_TOUCHABLE race) — re-measure with a clear screen
                    retried = true
                    CursorController.clear(); delay(200); received.value = null
                    withContext(Dispatchers.Default) { JarvisAccessibilityService.tapPxAwait(exp.x, exp.y) }
                    delay(550)
                    rec = received.value
                }
                val diff = rec?.let { hypot((it.first - exp.x).toDouble(), (it.second - exp.y).toDouble()).toFloat() }
                out += TapRow(i + 1, nx, ny, exp.x to exp.y, r.cursorScreen, r.outcome.name.lowercase(), r.touchableAtClick, r.underCursor, r.effect, rec, diff, retried)
                rows = out.toList()
                Diag.log("TapTest", "T${i + 1} norm=${CursorGeometry.fmt(nx)},${CursorGeometry.fmt(ny)} expected=(${exp.x.toInt()},${exp.y.toInt()}) " +
                    "received=${rec?.let { "(${it.first.toInt()},${it.second.toInt()})" } ?: "NO_TOUCH"} diff=${diff?.let { String.format(Locale.US, "%.1f", it) }} " +
                    "outcome=${r.outcome} effect=${r.effect} touchableAtClick=${r.touchableAtClick} cursorSwallowedTap=$retried")
                delay(250)
            }
            CursorController.clear()
            running = false; step = 0
            note = "Done. diff = received − expected (px). ≤5 px is good; a big diff or NO_TOUCH is the bug."
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        AndroidView(
            factory = { c -> TapTestView(c) { rx, ry -> received.value = rx to ry } },
            update = { v -> v.setTargets(targets) },
            modifier = Modifier.fillMaxSize()
        )

        // top centre status (never under a target)
        JText(
            if (running) "Running… target $step/5" else "Tap Test",
            12, Gold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp)
        )
        // a real back button (small, top-left, clear of the corner rings)
        OutlineButton("Back", null, onBack, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 24.dp))

        if (!running) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Surface2.copy(alpha = 0.96f))
                    .padding(14.dp)
            ) {
                JText(note, 11, GoldLight)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton("Run tap test", null, { runTest() }, Modifier.weight(1f))
                    OutlineButton("Copy log", null, {
                        Diag.log("TapTest", "summary: " + rows.joinToString(" | ") { "T${it.n} exp=(${it.expected.first.toInt()},${it.expected.second.toInt()}) rec=${it.received?.let { p -> "(${p.first.toInt()},${p.second.toInt()})" } ?: "NO_TOUCH"} diff=${it.diff?.let { d -> String.format(Locale.US, "%.1f", d) }}" })
                    }, Modifier.weight(1f))
                }
                if (rows.isNotEmpty()) {
                    JText("Results (sent px · received px · diff · outcome · touchable · effect)", 11, Gold, FontWeight.Medium, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(rows) { r ->
                            JText(
                                "T${r.n}  ${CursorGeometry.fmt(r.normX)},${CursorGeometry.fmt(r.normY)}  " +
                                    "sent(${r.expected.first.toInt()},${r.expected.second.toInt()})  " +
                                    "recv${r.received?.let { "(${it.first.toInt()},${it.second.toInt()})" } ?: "(NO TOUCH)"}  " +
                                    "diff=${r.diff?.let { String.format(Locale.US, "%.1f", it) } ?: "-"}px  " +
                                    "${r.outcome}  touchable=${r.touchable}  effect=${r.effect}",
                                10, if (r.received == null || (r.diff ?: 0f) > 5f) Gold else Ivory
                            )
                            JText("under: ${r.under}", 9, Muted)
                            JText("cursor window: ${r.cursorScreen}${if (r.retried) "  ·  cursor swallowed the tap → re-measured with a clear screen" else ""}", 9, if (r.retried) Gold else Muted)
                        }
                    }
                } else {
                    JText("Running the 5 taps takes about 10 seconds.", 10, Muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        // auto-run once on entry so the four corner rings are never covered by a button
        LaunchedEffect(Unit) { delay(800); if (!running && rows.isEmpty()) runTest() }
    }
}

private data class TapRow(
    val n: Int, val normX: Float, val normY: Float,
    val expected: Pair<Float, Float>, val cursorScreen: String,
    val outcome: String, val touchable: Boolean, val under: String, val effect: String,
    val received: Pair<Float, Float>?, val diff: Float?, val retried: Boolean
)

/** Draws the 5 target rings exactly where the coordinate engine says the pixels are, and reports raw touches. */
private class TapTestView(c: Context, private val onTouch: (Float, Float) -> Unit) : View(c) {
    private var targets: List<Pair<Float, Float>> = emptyList()
    private val d = c.resources.displayMetrics.density
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f * d; color = 0xFFFFD479.toInt() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0x22FFD479 }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFFFFD479.toInt() }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD479.toInt(); textSize = 11f * d; textAlign = Paint.Align.CENTER }
    private val cross = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFD479; strokeWidth = 1f * d }
    private val loc = IntArray(2)

    fun setTargets(t: List<Pair<Float, Float>>) { targets = t; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val sz = CursorGeometry.screenSize(context)
        getLocationOnScreen(loc)
        targets.forEachIndexed { i, t ->
            val sc: PointF = CursorGeometry.toPx(t.first, t.second, sz)
            val lx = sc.x - loc[0]; val ly = sc.y - loc[1]
            val r = 30f * d
            canvas.drawCircle(lx, ly, r, fill)
            canvas.drawCircle(lx, ly, r, ring)
            canvas.drawCircle(lx, ly, 3f * d, dot)
            canvas.drawLine(lx - r * 1.5f, ly, lx + r * 1.5f, ly, cross)
            canvas.drawLine(lx, ly - r * 1.5f, lx, ly + r * 1.5f, cross)
            canvas.drawText("T${i + 1}", lx, ly + r + 13f * d, txt)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) onTouch(event.rawX, event.rawY)
        return true
    }
}
