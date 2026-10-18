package com.jarvis.app.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog

/**
 * The user's floating control panel for the AI cursor: cursor list, X/Y readout (2 decimals), nudge, click /
 * double / long / click-all, grid, lock, joystick (speed 1-15) and a PANIC button. Built with plain Views so it
 * works as an accessibility overlay without Compose lifecycle owners.
 */
object CursorPanel {
    private var panel: View? = null
    private var joy: View? = null
    private var readout: TextView? = null
    private var note: TextView? = null
    private var gridBtn: Button? = null
    private var lockBtn: Button? = null
    private var speedLabel: TextView? = null
    private var appCtx: Context? = null

    var speed by mutableStateOf(5)          // 1..15
    var lockDrag by mutableStateOf(false)
    private val steps = floatArrayOf(0.1f, 0.5f, 1f)
    private var stepIdx = 1

    fun isShown() = panel != null

    fun toggle() { if (panel == null) show() else hide() }

    fun show() {
        if (panel != null) return
        if (!CursorController.ensureAttached()) return
        val c = CursorController.ctxOrNull() ?: return
        val wm = CursorController.wmOrNull() ?: return
        appCtx = c
        if (CursorController.positions().isEmpty()) CursorController.add(0f, 0f)

        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(0xF2101216.toInt()); setStroke(dp(1), 0x99FFD479.toInt()) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val header = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("AI Cursor", 13, 0xFFFFD479.toInt(), bold = true), lpWrap())
        header.addView(View(c), lpWeight())
        header.addView(btn("PANIC", 0xFFC4483F.toInt()) { panic() }, lpWrap())
        header.addView(btn("✕", null) { hide() }, lpWrap())
        root.addView(header, lpMatch())

        readout = label("", 12, 0xFFFFE9B8.toInt()).also { root.addView(it, lpMatch()) }
        note = label("", 10, 0xFF9AA0A6.toInt()).also { root.addView(it, lpMatch()) }

        val cursorsRow = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        cursorsRow.addView(btn("◀", null) { cycle(-1) }, lpWrap())
        cursorsRow.addView(btn("+ cursor", null) { addCursor() }, lpWeight())
        cursorsRow.addView(btn("del", null) { delCursor() }, lpWrap())
        cursorsRow.addView(btn("▶", null) { cycle(1) }, lpWrap())
        root.addView(cursorsRow, lpMatch())

        val pad = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        pad.addView(btn("▲", null) { nudge(0f, step()) }, lpWrap())
        val mid = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL }
        mid.addView(btn("◀", null) { nudge(-step(), 0f) }, lpWrap())
        val stepB = btn("±${"%.1f".format(step())}", null) { stepIdx = (stepIdx + 1) % steps.size; }
        stepB.setOnClickListener { stepIdx = (stepIdx + 1) % steps.size; stepB.text = "±${"%.1f".format(step())}" }
        mid.addView(stepB, lpWrap())
        mid.addView(btn("▶", null) { nudge(step(), 0f) }, lpWrap())
        pad.addView(mid, lpWrap())
        pad.addView(btn("▼", null) { nudge(0f, -step()) }, lpWrap())
        root.addView(pad, lpMatch())

        val actions = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(btn("Tap", null) { clickAction(CursorController.ClickType.TAP) }, lpWeight())
        actions.addView(btn("Dbl", null) { clickAction(CursorController.ClickType.DOUBLE) }, lpWeight())
        actions.addView(btn("Long", null) { clickAction(CursorController.ClickType.LONG) }, lpWeight())
        actions.addView(btn("All", null) { all() }, lpWeight())
        root.addView(actions, lpMatch())

        val toggles = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        gridBtn = btn("Grid", null) { CursorController.showGrid(!CursorController.gridOn); gridBtn?.alpha = if (CursorController.gridOn) 1f else 0.6f }
        lockBtn = btn("Lock", null) { lockDrag = !lockDrag; lockBtn?.alpha = if (lockDrag) 1f else 0.6f }
        toggles.addView(gridBtn, lpWeight())
        toggles.addView(lockBtn, lpWeight())
        toggles.addView(btn("Joystick", null) { toggleJoystick() }, lpWeight())
        toggles.addView(btn("Save spot", null) { saveSpot() }, lpWeight())
        root.addView(toggles, lpMatch())

        val speedRow = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        speedLabel = label("Speed $speed", 11, 0xFFFFE9B8.toInt())
        speedRow.addView(speedLabel, lpWrap())
        val sb = SeekBar(c).apply { max = 14; progress = speed - 1 }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { speed = p + 1; speedLabel?.text = "Speed $speed" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        speedRow.addView(sb, lpWeight())
        root.addView(speedRow, lpMatch())

        val lp = WindowManager.LayoutParams(
            dp(260), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(160) }
        header.setOnTouchListener(object : View.OnTouchListener {
            private var dx = 0f; private var dy = 0f; private var ox = 0; private var oy = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; ox = lp.x; oy = lp.y; return true }
                    MotionEvent.ACTION_MOVE -> { lp.x = ox + (e.rawX - dx).toInt(); lp.y = oy + (e.rawY - dy).toInt(); try { wm.updateViewLayout(root, lp) } catch (ex: Exception) { }; return true }
                }
                return false
            }
        })
        try { wm.addView(root, lp); panel = root } catch (e: Exception) { AppLog.w(e) }
        refresh()
    }

    fun hide() {
        val wm = CursorController.wmOrNull()
        try { joy?.let { wm?.removeView(it) } } catch (e: Exception) { }
        joy = null
        try { panel?.let { wm?.removeView(it) } } catch (e: Exception) { }
        panel = null; readout = null; note = null
    }

    fun panic() { CursorController.panic(); hide() }

    fun refresh() {
        if (!CursorController.isMainThread()) { CursorController.postMain { refresh() }; return }
        val r = readout ?: return
        val p = CursorController.positions()
        if (p.isEmpty()) { r.text = "no cursor — tap + cursor"; return }
        val id = CursorController.selectedId
        val cur = p.firstOrNull { it.first == id } ?: p.first()
        r.text = "cursor #${cur.first}   X ${CursorGeometry.fmt(cur.second)}   Y ${CursorGeometry.fmt(cur.third)}"
    }

    private fun selectedPoint(): Triple<Int, Float, Float>? {
        val p = CursorController.positions()
        if (p.isEmpty()) return null
        val id = CursorController.selectedId
        return p.firstOrNull { it.first == id } ?: p.first()
    }

    private fun step() = steps[stepIdx]

    private fun addCursor() { CursorController.add(0f, 0f); refresh(); flash("cursor added") }
    private fun delCursor() { val p = selectedPoint() ?: return; CursorController.remove(p.first); refresh(); flash("cursor removed") }
    private fun cycle(d: Int) {
        val p = CursorController.positions(); if (p.isEmpty()) return
        val i = p.indexOfFirst { it.first == CursorController.selectedId }
        val n = ((if (i < 0) 0 else i) + d + p.size) % p.size
        CursorController.select(p[n].first); refresh()
    }
    private fun nudge(dx: Float, dy: Float) { val p = selectedPoint() ?: return; CursorController.move(p.first, p.second + dx, p.third + dy); refresh() }
    private fun clickAction(type: CursorController.ClickType) { val p = selectedPoint() ?: return; CursorController.click(p.first, type) { ok -> flash(if (ok) "click sent" else "click blocked (rate limit)") } }
    private fun all() { CursorController.clickAll { ok -> flash(if (ok) "clicked all" else "blocked") } }
    private fun flash(s: String) { note?.text = s }

    /** Saves the selected cursor's spot. If the AI had placed a target there, this becomes a correction. */
    private fun saveSpot() {
        val p = selectedPoint() ?: return
        val pkg = JarvisAccessibilityService.foregroundPackage()
        val old = CursorController.lastAiPoint
        val label = CursorController.lastAiTarget
        val res = if (!label.isNullOrBlank() && old != null) ClickMemory.correct(pkg, label, old.first, old.second, p.second, p.third)
        else ClickMemory.remember(pkg, "spot ${p.first}", p.second, p.third, source = "user_marked")
        flash(res)
    }

    private fun toggleJoystick() {
        if (joy != null) { val wm = CursorController.wmOrNull(); try { joy?.let { wm?.removeView(it) } } catch (e: Exception) { }; joy = null; return }
        val c = appCtx ?: return
        val wm = CursorController.wmOrNull() ?: return
        val v = JoystickView(c, { speed }, { dxx, dyy, dt ->
            val p = selectedPoint()
            if (p != null && dt > 0f && dt < 0.2f) {
                val maxU = 8f * speed
                val vx = Math.signum(dxx) * dxx * dxx * maxU
                val vy = Math.signum(dyy) * dyy * dyy * maxU
                CursorController.move(p.first, p.second + vx * dt, p.third - vy * dt)
                refresh()
            }
        })
        val sz = dp(150)
        val lp = WindowManager.LayoutParams(
            sz, sz,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.START; x = dp(20); y = dp(20) }
        try { wm.addView(v, lp); joy = v } catch (e: Exception) { AppLog.w(e) }
    }

    private fun dp(v: Int): Int = ((appCtx?.resources?.displayMetrics?.density ?: 1f) * v).toInt()

    private fun label(t: String, sp: Int, color: Int, bold: Boolean = false): TextView = TextView(appCtx).apply {
        text = t; setTextColor(color); setTextSize(TypedValue.COMPLEX_UNIT_SP, sp.toFloat())
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun btn(t: String, bg: Int?, onClick: () -> Unit): Button = Button(appCtx).apply {
        text = t; isAllCaps = false; setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTextColor(0xFFFFE9B8.toInt())
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            if (bg != null) setColor(bg) else { setColor(0x22FFFFFF); setStroke(dp(1), 0x55FFD479.toInt()) }
        }
        minWidth = 0; minimumWidth = 0; minimumHeight = 0
        setPadding(dp(8), dp(4), dp(8), dp(4))
        setOnClickListener { onClick() }
    }

    private fun lpWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    private fun lpWeight() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    private fun lpMatch() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(2), 0, dp(2)) }
}

private class JoystickView(
    c: Context,
    private val speedProvider: () -> Int,
    private val onMove: (Float, Float, Float) -> Unit
) : View(c) {
    private val d = c.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d)
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF; style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FFD479.toInt(); style = Paint.Style.STROKE; strokeWidth = dp(2) }
    private val stick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFD479.toInt(); style = Paint.Style.FILL }
    private var dx = 0f; private var dy = 0f; private var active = false
    private var lastT = 0L
    private val cb = Choreographer.FrameCallback { t -> onFrame(t) }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = minOf(width, height) / 2f - dp(6)
        canvas.drawCircle(cx, cy, r, base)
        canvas.drawCircle(cx, cy, r, ring)
        canvas.drawCircle(cx + dx * r, cy + dy * r, dp(16), stick)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val cx = width / 2f; val cy = height / 2f; val r = minOf(width, height) / 2f - dp(6)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var ox = (e.x - cx) / r; var oy = (e.y - cy) / r
                val m = kotlin.math.hypot(ox, oy)
                if (m > 1f) { ox /= m; oy /= m }
                dx = ox; dy = oy
                if (!active) { active = true; lastT = 0L; Choreographer.getInstance().postFrameCallback(cb) }
                invalidate(); return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { dx = 0f; dy = 0f; active = false; invalidate(); return true }
        }
        return super.onTouchEvent(e)
    }

    private fun onFrame(t: Long) {
        if (!active) return
        val dt = if (lastT == 0L) 0f else ((t - lastT) / 1_000_000_000.0).toFloat()
        lastT = t
        onMove(dx, dy, dt)
        if (speedProvider() > 0) Choreographer.getInstance().postFrameCallback(cb)
    }
}
