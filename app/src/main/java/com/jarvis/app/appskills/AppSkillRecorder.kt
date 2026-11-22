package com.jarvis.app.appskills

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jarvis.app.data.AppLog
import com.jarvis.app.live.CursorGeometry
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.overlay.OverlayLifecycleOwner
import com.jarvis.app.ui.theme.JarvisTheme

/**
 * Draggable overlay cursors for recording App Skill actions. Uses Accessibility overlays.
 * Records normalized coordinates and element metadata; no screen content is stored from sensitive screens.
 */
object AppSkillRecorder {
    private data class Marker(val id: Int, var x: Float, var y: Float, val view: View, val lp: WindowManager.LayoutParams)
    private val markers = linkedMapOf<Int, Marker>()
    private var nextId = 1
    private var panel: View? = null
    private var wm: WindowManager? = null
    private var ctx: Context? = null
    private var owner: OverlayLifecycleOwner? = null
    private var activeProfile by mutableStateOf<AppSkillProfile?>(null)
    private var status by mutableStateOf("Add 2-3 cursors, drag them, then record Tap/Swipe/Text")

    fun isRecording(): Boolean = panel != null

    fun start(context: Context, profile: AppSkillProfile) {
        if (panel != null) return
        if (!JarvisAccessibilityService.enabled) { status = "Enable Accessibility service first"; return }
        ctx = context.applicationContext
        wm = ctx!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        owner = OverlayLifecycleOwner().also { it.start() }
        activeProfile = profile
        if (markers.isEmpty()) { addMarker(0f, 20f); addMarker(-20f, -10f); addMarker(20f, -10f) }
        showPanel()
    }

    fun stop() {
        markers.values.forEach { m -> try { wm?.removeViewImmediate(m.view) } catch (e: Exception) { } }
        markers.clear()
        try { panel?.let { wm?.removeViewImmediate(it) } } catch (e: Exception) { }
        panel = null
        try { owner?.stop() } catch (e: Exception) { }
        owner = null
        activeProfile = null
        status = "Add 2-3 cursors, drag them, then record Tap/Swipe/Text"
    }

    private fun showPanel() {
        val c = ctx ?: return
        val cv = ComposeView(c)
        attachOwners(cv)
        cv.setContent { JarvisTheme { PanelUi() } }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(60) }
        try { wm?.addView(cv, lp); panel = cv } catch (e: Exception) { AppLog.w(e) }
    }

    @Composable
    private fun PanelUi() {
        val p = activeProfile
        Column(
            Modifier.background(Color(0xEE141414), RoundedCornerShape(16.dp)).border(1.dp, Color(0x66D4AF37), RoundedCornerShape(16.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(p?.name ?: "App Skill", color = Color(0xFFF5F0E6), fontSize = 13.sp)
            Text(status, color = Color(0xFFB8B0A0), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn("+Cursor") { addMarker(0f, 0f) }
                Btn("Tap") { recordTap() }
                Btn("Long") { recordLong() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn("Swipe12") { recordSwipe(0, 1) }
                Btn("Back") { recordSimple("back") }
                Btn("Wait") { recordWait() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn("Save") { activeProfile?.let { AppSkills.save(it); status = "Saved ${it.actions.size} actions" } }
                Btn("Close", danger = true) { stop() }
            }
        }
    }

    @Composable
    private fun Btn(text: String, danger: Boolean = false, onClick: () -> Unit) {
        Box(
            Modifier.background(if (danger) Color(0xFF7A2E2E) else Color(0xFF2A2418), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)
        ) { Text(text, color = Color(0xFFF5F0E6), fontSize = 11.sp) }
    }

    private fun addMarker(nx: Float, ny: Float) {
        val c = ctx ?: return; val w = wm ?: return
        val id = nextId++
        val size = dp(44)
        val v = FrameLayout(c).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0x55D4AF37); setStroke(dp(2), 0xFFD4AF37.toInt())
            }
        }
        val tv = android.widget.TextView(c).apply {
            text = id.toString(); setTextColor(0xFFF5F0E6.toInt()); textSize = 12f; gravity = android.view.Gravity.CENTER
        }
        v.addView(tv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val sz = CursorGeometry.screenSizeOrNull() ?: return
        val px = CursorGeometry.toPx(nx, ny, sz)
        val lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = (px.x - size / 2).toInt(); y = (px.y - size / 2).toInt() }
        var dx = 0f; var dy = 0f; var ox = 0; var oy = 0
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; ox = lp.x; oy = lp.y; true }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (e.rawX - dx).toInt(); lp.y = oy + (e.rawY - dy).toInt()
                    try { w.updateViewLayout(v, lp) } catch (ex: Exception) { }
                    val norm = CursorGeometry.toNorm(lp.x + size / 2f, lp.y + size / 2f, CursorGeometry.screenSize(c))
                    markers[id]?.x = norm.x; markers[id]?.y = norm.y
                    true
                }
                else -> false
            }
        }
        try { w.addView(v, lp); markers[id] = Marker(id, nx, ny, v, lp) } catch (e: Exception) { AppLog.w(e) }
    }

    private fun recordTap() { recordPointAction("tap") }
    private fun recordLong() { recordPointAction("long_press") }

    private fun recordPointAction(type: String) {
        val p = activeProfile ?: return
        val m = markers.values.firstOrNull() ?: return
        val sz = CursorGeometry.screenSizeOrNull()
        val px = CursorGeometry.toPx(m.x, m.y, sz ?: return)
        val node = JarvisAccessibilityService.nodeAt(px.x.toInt(), px.y.toInt())
        val label = node.substringAfter("text='", "").substringBefore("'").ifBlank { type }
        p.actions.add(AppSkillAction(
            type = type, label = label, x = m.x, y = m.y,
            screenW = sz.w, screenH = sz.h, nodeText = label,
            durationMs = if (type == "long_press") 700 else 60
        ))
        status = "Recorded $type at ${CursorGeometry.fmt(m.x)},${CursorGeometry.fmt(m.y)}"
    }

    private fun recordSwipe(from: Int, to: Int) {
        val p = activeProfile ?: return
        val list = markers.values.toList()
        if (list.size <= maxOf(from, to)) { status = "Need at least ${maxOf(from, to) + 1} cursors"; return }
        val a = list[from]; val b = list[to]
        val sz = CursorGeometry.screenSizeOrNull() ?: return
        p.actions.add(AppSkillAction(type = "swipe", label = "Swipe ${a.id}->${b.id}", x = a.x, y = a.y, toX = b.x, toY = b.y, screenW = sz.w, screenH = sz.h, durationMs = 350))
        status = "Recorded swipe ${a.id}->${b.id}"
    }

    private fun recordSimple(type: String) {
        val p = activeProfile ?: return
        p.actions.add(AppSkillAction(type = type, label = type.replaceFirstChar { it.uppercase() }))
        status = "Recorded $type"
    }

    private fun recordWait() {
        val p = activeProfile ?: return
        p.actions.add(AppSkillAction(type = "wait", label = "Wait 800ms", durationMs = 800))
        status = "Recorded wait"
    }

    private fun attachOwners(v: View) {
        val o = owner ?: return
        v.setViewTreeLifecycleOwner(o)
        v.setViewTreeSavedStateRegistryOwner(o)
        v.setViewTreeViewModelStoreOwner(o)
    }

    private fun dp(v: Int): Int = ((ctx?.resources?.displayMetrics?.density ?: 1f) * v).toInt()
}
