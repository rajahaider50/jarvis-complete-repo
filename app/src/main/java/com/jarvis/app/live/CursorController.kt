package com.jarvis.app.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog

/**
 * Draws the on-screen AI cursor(s) with TYPE_ACCESSIBILITY_OVERLAY (no extra permission needed). Each cursor is
 * its own small window so the rest of the screen stays touchable. Cursors can be dragged by the user; before an
 * AI gesture they are made NOT_TOUCHABLE so the tap goes to the app underneath, then touchable again.
 */
object CursorController {
    private const val MAX = 10
    private val colors = intArrayOf(
        0xFFFFD479.toInt(), 0xFF66D0A0.toInt(), 0xFF7FB4FF.toInt(), 0xFFFF9E9E.toInt(), 0xFFC9A0FF.toInt(),
        0xFFFFE08A.toInt(), 0xFF8AE0FF.toInt(), 0xFFB0FFB0.toInt(), 0xFFFFB0E0.toInt(), 0xFFFFC080.toInt()
    )

    class Cur(val id: Int, var x: Float, var y: Float, val label: String, val color: Int) {
        var view: View? = null
        var lp: WindowManager.LayoutParams? = null
    }

    private val cursors = LinkedHashMap<Int, Cur>()
    private var nextId = 1
    private var wm: WindowManager? = null
    private var ctx: Context? = null
    private var gridView: View? = null

    var anyShown by mutableStateOf(false)
        private set
    var gridOn by mutableStateOf(false)
        private set
    var sizeDp by mutableStateOf(56)

    /** The cursor the manual panel / joystick is controlling (first one if none chosen). */
    var selectedId by mutableStateOf<Int?>(null)
    fun select(id: Int?) { selectedId = id }

    internal fun wmOrNull(): WindowManager? { ensure(); return wm }
    internal fun ctxOrNull(): Context? { ensure(); return ctx }
    internal fun dpInt(v: Int): Int = dp(v)

    fun ensureAttached(): Boolean = ensure()

    private fun ensure(): Boolean {
        val svc = JarvisAccessibilityService.inst ?: return false
        if (wm == null) {
            ctx = svc
            wm = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
        return true
    }

    private fun dp(v: Int): Int = ((ctx?.resources?.displayMetrics?.density ?: 1f) * v).toInt()

    private fun center(x: Float, y: Float): PointF {
        val c = ctx ?: return PointF()
        return CursorGeometry.toPx(x, y, CursorGeometry.screenSize(c))
    }

    fun add(x: Float, y: Float, label: String? = null): Int {
        if (!ensure()) return -1
        if (cursors.size >= MAX) return -1
        val c = ctx ?: return -1
        val id = nextId++
        val cur = Cur(id, CursorGeometry.clamp(x), CursorGeometry.clamp(y), label ?: "$id", colors[(id - 1) % colors.size])
        val v = CursorView(c, cur.label, cur.color) { dx, dy -> dragTo(cur, dx, dy) }
        val px = center(cur.x, cur.y)
        val sp = dp(sizeDp)
        val lp = WindowManager.LayoutParams(
            sp, sp,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; this.x = (px.x - sp / 2f).toInt(); this.y = (px.y - sp / 2f).toInt() }
        cur.view = v; cur.lp = lp
        try { wm!!.addView(v, lp) } catch (e: Exception) { AppLog.w(e); return -1 }
        cursors[id] = cur
        if (selectedId == null) selectedId = id
        anyShown = true
        return id
    }

    fun move(id: Int, x: Float, y: Float) {
        val cur = cursors[id] ?: return
        cur.x = CursorGeometry.clamp(x); cur.y = CursorGeometry.clamp(y)
        place(cur)
    }

    /** Smoothly slides a cursor to a new point (used by the AI so the user can follow it). */
    fun moveAnimated(id: Int, x: Float, y: Float, durationMs: Long = 450) {
        val cur = cursors[id] ?: return
        val sx = cur.x; val sy = cur.y
        val tx = CursorGeometry.clamp(x); val ty = CursorGeometry.clamp(y)
        val t0 = android.os.SystemClock.uptimeMillis()
        val cb = object : android.view.Choreographer.FrameCallback {
            override fun doFrame(now: Long) {
                val k = ((now - t0).toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                val e = k * k * (3 - 2 * k)                     // smoothstep
                cur.x = sx + (tx - sx) * e; cur.y = sy + (ty - sy) * e
                place(cur)
                if (k < 1f) android.view.Choreographer.getInstance().postFrameCallback(this)
            }
        }
        android.view.Choreographer.getInstance().postFrameCallback(cb)
    }

    private fun place(cur: Cur) {
        val v = cur.view ?: return; val lp = cur.lp ?: return
        val px = center(cur.x, cur.y)
        val sp = dp(sizeDp)
        lp.x = (px.x - sp / 2f).toInt(); lp.y = (px.y - sp / 2f).toInt()
        try { wm?.updateViewLayout(v, lp) } catch (e: Exception) { AppLog.w(e) }
    }

    private fun dragTo(cur: Cur, dxPx: Float, dyPx: Float) {
        val c = ctx ?: return
        val s = CursorGeometry.screenSize(c)
        val cx = s.w / 2f; val cy = s.h / 2f
        cur.x = CursorGeometry.clamp(cur.x + dxPx / cx * 100f)
        cur.y = CursorGeometry.clamp(cur.y - dyPx / cy * 100f)
        place(cur)
    }

    fun remove(id: Int) {
        val cur = cursors.remove(id) ?: return
        try { wm?.removeView(cur.view) } catch (e: Exception) { AppLog.w(e) }
        if (selectedId == id) selectedId = cursors.keys.firstOrNull()
        if (cursors.isEmpty()) anyShown = false
    }

    fun clear() { cursors.keys.toList().forEach { remove(it) } }

    fun list(): String = if (cursors.isEmpty()) "no cursors" else cursors.values.joinToString("; ") {
        "id=${it.id} ${it.label} x=${CursorGeometry.fmt(it.x)} y=${CursorGeometry.fmt(it.y)}"
    }

    fun positions(): List<Triple<Int, Float, Float>> = cursors.values.map { Triple(it.id, it.x, it.y) }

    private fun setTouchable(touchable: Boolean) {
        cursors.values.forEach { cur ->
            val lp = cur.lp ?: return@forEach
            lp.flags = if (touchable) lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            else lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            try { wm?.updateViewLayout(cur.view, lp) } catch (e: Exception) { AppLog.w(e) }
        }
    }

    /** Runs an AI gesture with all cursors made untouchable, then restores them. */
    fun gesture(block: (done: (Boolean) -> Unit) -> Unit, cb: ((Boolean) -> Unit)?) {
        setTouchable(false)
        block { ok ->
            setTouchable(true)
            cb?.invoke(ok)
        }
    }

    // ---------- gestures through cursors ----------
    enum class ClickType { TAP, DOUBLE, LONG }

    fun click(id: Int, type: ClickType = ClickType.TAP, cb: ((Boolean) -> Unit)? = null) {
        val cur = cursors[id]
        if (cur == null) { cb?.invoke(false); return }
        move(id, cur.x, cur.y)
        gesture({ done ->
            when (type) {
                ClickType.TAP -> JarvisAccessibilityService.tapNorm(cur.x, cur.y) { done(it) }
                ClickType.DOUBLE -> JarvisAccessibilityService.doubleTapNorm(cur.x, cur.y) { done(it) }
                ClickType.LONG -> JarvisAccessibilityService.longPressNorm(cur.x, cur.y) { done(it) }
            }
        }, cb)
    }

    fun swipe(id: Int, toX: Float, toY: Float, durationMs: Long = 300, cb: ((Boolean) -> Unit)? = null) {
        val cur = cursors[id] ?: run { cb?.invoke(false); return }
        gesture({ done -> JarvisAccessibilityService.swipeNorm(cur.x, cur.y, toX, toY, durationMs) { done(it) } }) { ok ->
            if (ok) move(id, CursorGeometry.clamp(toX), CursorGeometry.clamp(toY))
            cb?.invoke(ok)
        }
    }

    /** Taps every cursor at once (one stroke each) — the "click all" command. */
    fun clickAll(cb: ((Boolean) -> Unit)? = null) {
        val pts = cursors.values.map { it.x to it.y }
        if (pts.isEmpty()) { cb?.invoke(false); return }
        gesture({ done -> JarvisAccessibilityService.clickAll(pts) { done(it) } }, cb)
    }

    fun clickAt(x: Float, y: Float, type: ClickType = ClickType.TAP, cb: ((Boolean) -> Unit)? = null): Int {
        val id = add(x, y)
        if (id < 0) { cb?.invoke(false); return -1 }
        click(id, type, cb)
        return id
    }

    // ---------- grid ----------
    fun showGrid(on: Boolean) {
        gridOn = on
        if (on) {
            if (!ensure()) return
            if (gridView != null) return
            val c = ctx ?: return
            val v = GridView(c)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }
            try { wm!!.addView(v, lp); gridView = v } catch (e: Exception) { AppLog.w(e) }
        } else {
            try { gridView?.let { wm?.removeView(it) } } catch (e: Exception) { AppLog.w(e) }
            gridView = null
        }
    }

    fun panic() {
        try { showGrid(false) } catch (e: Exception) { }
        try { clear() } catch (e: Exception) { }
    }
}

private class CursorView(c: Context, private val label: String, color: Int, private val onDrag: (Float, Float) -> Unit) : View(c) {
    private val d = c.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(3); this.color = color }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color; alpha = 40 }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = dp(11); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private var lastX = 0f; private var lastY = 0f
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = (minOf(width, height) / 2f) - dp(3)
        canvas.drawCircle(cx, cy, r, glow)
        canvas.drawCircle(cx, cy, r, ring)
        canvas.drawCircle(cx, cy, dp(3), dot)
        canvas.drawText(label, cx, height - dp(2), text)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; dragging = true; return true }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) { onDrag(event.rawX - lastX, event.rawY - lastY); lastX = event.rawX; lastY = event.rawY }
                return true
            }
            MotionEvent.ACTION_UP -> { dragging = false; performClick(); return true }
        }
        return super.onTouchEvent(event)
    }
    override fun performClick(): Boolean = super.performClick()
}

private class GridView(c: Context) : View(c) {
    private val d = c.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFD479; strokeWidth = 1f }
    private val strong = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFD479.toInt(); strokeWidth = 2f }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFD479.toInt(); textSize = dp(9) }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        for (v in -100..100 step 10) {
            val p = if (v == 0) strong else line
            val x = cx + v / 100f * cx; val y = cy - v / 100f * cy
            canvas.drawLine(x, 0f, x, h, p)
            canvas.drawLine(0f, y, w, y, p)
            if (v % 50 == 0) canvas.drawText(if (v > 0) "+$v" else "$v", x + dp(2), cy + dp(12), label)
        }
    }
}
