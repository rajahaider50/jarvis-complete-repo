package com.jarvis.app.overlay

import com.jarvis.app.data.AppLog
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jarvis.app.MainActivity
import com.jarvis.app.live.CallHost
import com.jarvis.app.ui.overlay.BubbleUi
import com.jarvis.app.ui.overlay.CloseZoneUi
import com.jarvis.app.ui.overlay.PopupUi
import com.jarvis.app.ui.theme.JarvisTheme
import kotlin.math.hypot

/**
 * Floating windows drawn over every app (needs "Display over other apps"):
 *  - the call bubble (your orb), draggable, with a drop-to-end target
 *  - the mini chat popup shown after a call ends (drag up = open the full app)
 * All calls must happen on the main thread.
 */
object Overlay {
    private lateinit var app: Context
    private lateinit var wm: WindowManager
    private var owner: OverlayLifecycleOwner? = null
    private val main = Handler(Looper.getMainLooper())

    var expanded by mutableStateOf(false); private set
    var onRight by mutableStateOf(true); private set
    var closeVisible by mutableStateOf(false); private set
    var closeHot by mutableStateOf(false); private set

    private var bubble: DragLayout? = null
    private var bubbleLp: WindowManager.LayoutParams? = null
    private var zone: View? = null
    private var popup: DragLayout? = null
    private var lastX = -1
    private var lastY = -1
    private val collapseTask = Runnable { updateExpanded(false) }

    // ---------- helpers ----------
    private fun ensure(ctx: Context) {
        if (owner != null) return
        app = ctx.applicationContext
        wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        owner = OverlayLifecycleOwner().also { it.start() }
    }

    fun canDraw(ctx: Context): Boolean = Settings.canDrawOverlays(ctx.applicationContext)
    private fun dp(v: Int) = (v * app.resources.displayMetrics.density).toInt()
    private fun screenW() = app.resources.displayMetrics.widthPixels
    private fun screenH() = app.resources.displayMetrics.heightPixels

    private fun layout(w: Int, h: Int, touchable: Boolean = true): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT)
    }

    private fun attachOwners(v: View) {
        val o = owner ?: return
        v.setViewTreeLifecycleOwner(o)
        v.setViewTreeSavedStateRegistryOwner(o)
        v.setViewTreeViewModelStoreOwner(o)
    }

    private fun compose(content: @androidx.compose.runtime.Composable () -> Unit): ComposeView {
        val cv = ComposeView(app)
        attachOwners(cv)
        cv.setContent { JarvisTheme { content() } }
        return cv
    }

    private fun dragHost(axis: DragLayout.Axis, content: @androidx.compose.runtime.Composable () -> Unit): DragLayout {
        val root = DragLayout(app, axis)
        attachOwners(root)
        root.addView(compose(content), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return root
    }

    private fun update(v: View, lp: WindowManager.LayoutParams) {
        try { wm.updateViewLayout(v, lp) } catch (e: IllegalArgumentException) { AppLog.w(e); /* view already removed */ }
    }

    private fun remove(v: View?) {
        if (v == null) return
        try { wm.removeViewImmediate(v) } catch (e: Exception) { AppLog.w(e); }
    }

    fun openApp(route: String) {
        val i = Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("route", route)
        try { app.startActivity(i) } catch (e: Exception) { AppLog.w(e); }
    }

    // ---------- call bubble ----------
    fun showBubble(ctx: Context) {
        ensure(ctx)
        if (bubble != null || !CallHost.active || !canDraw(ctx)) return
        hidePopup()
        val root = dragHost(DragLayout.Axis.ANY) {
            BubbleUi(
                expanded = expanded, onRight = onRight,
                onToggle = { toggle() },
                onMute = { CallHost.client?.toggleMute() },
                onOpen = { openApp("live") },
                onEnd = { CallHost.end(app, showPopup = true) }
            )
        }
        val lp = layout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (lastX >= 0) lastX else screenW() - dp(84)
            y = if (lastY >= 0) lastY else (screenH() * 0.35f).toInt()
        }
        onRight = lp.x + dp(34) >= screenW() / 2
        var startX = 0
        var startY = 0
        root.onDragStart = {
            startX = lp.x; startY = lp.y
            updateExpanded(false)
            closeVisible = true
        }
        root.onDrag = { dx, dy ->
            lp.x = (startX + dx).toInt().coerceIn(0, maxOf(0, screenW() - root.width))
            lp.y = (startY + dy).toInt().coerceIn(0, maxOf(0, screenH() - root.height))
            closeHot = isOverZone(lp, root)
            update(root, lp)
        }
        root.onDragEnd = { _, _ ->
            closeVisible = false
            if (closeHot) {
                closeHot = false
                CallHost.end(app, showPopup = true)          // dropped on the X: hang up and show the chat popup
            } else snapToEdge(lp, root)
        }
        try { wm.addView(root, lp) } catch (e: Exception) { AppLog.w(e); return }
        bubble = root
        bubbleLp = lp
        addZone()
    }

    fun hideBubble() {
        main.removeCallbacks(collapseTask)
        remove(bubble); bubble = null; bubbleLp = null
        remove(zone); zone = null
        expanded = false; closeVisible = false; closeHot = false
    }

    private fun toggle() {
        updateExpanded(!expanded)
        if (expanded) main.postDelayed(collapseTask, 5000)
    }

    private fun updateExpanded(v: Boolean) {
        main.removeCallbacks(collapseTask)
        if (expanded == v) return
        expanded = v
        val b = bubble ?: return
        val lp = bubbleLp ?: return
        // wait until the window has been re-measured, then keep it glued to the screen edge
        b.postDelayed({ snapToEdge(lp, b) }, 60)
    }

    private fun snapToEdge(lp: WindowManager.LayoutParams, v: DragLayout) {
        val w = v.width
        val right = lp.x + w / 2 >= screenW() / 2
        onRight = right
        val target = if (right) screenW() - w - dp(4) else dp(4)
        lastX = target
        lastY = lp.y
        ValueAnimator.ofInt(lp.x, target).apply {
            duration = 180
            addUpdateListener { lp.x = it.animatedValue as Int; update(v, lp) }
            start()
        }
    }

    private fun isOverZone(lp: WindowManager.LayoutParams, v: View): Boolean {
        val cx = lp.x + v.width / 2
        val cy = lp.y + v.height / 2
        return hypot((cx - screenW() / 2).toDouble(), (cy - (screenH() - dp(120))).toDouble()) < dp(90)
    }

    private fun addZone() {
        if (zone != null) return
        val root = FrameLayout(app)
        attachOwners(root)
        root.addView(compose { CloseZoneUi(closeVisible, closeHot) })
        val lp = layout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, touchable = false).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(60)
        }
        try { wm.addView(root, lp); zone = root } catch (e: Exception) { AppLog.w(e); }
    }

    // ---------- mini chat popup ----------
    fun showPopup(ctx: Context) {
        ensure(ctx)
        if (popup != null || !canDraw(ctx)) return
        hideBubble()
        val root = dragHost(DragLayout.Axis.VERTICAL) {
            PopupUi(
                onContinue = { openApp("chat"); hidePopup() },
                onCall = { openApp("live"); hidePopup() },
                onClose = { hidePopup() }
            )
        }
        val lp = layout(screenW() - dp(24), WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(28)
        }
        val baseY = lp.y
        root.onDrag = { _, dy ->
            lp.y = (baseY - dy).toInt().coerceAtLeast(dp(4))     // follows the finger: up = bigger y
            update(root, lp)
        }
        root.onDragEnd = { _, dy ->
            when {
                dy < -dp(110) -> { openApp("chat"); hidePopup() }                // long drag up: full app
                dy > dp(90) -> hidePopup()                                       // drag down: dismiss
                else -> ValueAnimator.ofInt(lp.y, baseY).apply {                 // short drag: spring back
                    duration = 160
                    addUpdateListener { lp.y = it.animatedValue as Int; update(root, lp) }
                    start()
                }
            }
        }
        try { wm.addView(root, lp); popup = root } catch (e: Exception) { AppLog.w(e); }
    }

    fun hidePopup() {
        remove(popup); popup = null
    }

    /** Call when the app comes to the foreground: floating windows are not needed then. */
    fun hideAll() { hideBubble(); hidePopup() }
}
