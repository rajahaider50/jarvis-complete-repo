package com.jarvis.app.live

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.jarvis.app.data.Prefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** The real result of a dispatched gesture (bugfix Masla A / §2.1). Top-level so CursorController can map it too. */
enum class GestureOutcome { COMPLETED, CANCELLED, REJECTED, TIMEOUT, RATE_LIMITED, NO_SERVICE }

/**
 * Lets the assistant see what is on the screen and tap, type, scroll and press Back / Home on any app,
 * only after you switch it on in Android Settings > Accessibility. Used by the live-call tools.
 */
class JarvisAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var inst: JarvisAccessibilityService? = null
        val enabled: Boolean get() = inst != null

        /** Everything readable on the current screen, one line per element. Interactive ones are marked. */
        fun readScreen(maxChars: Int = 3500): String {
            val root = inst?.rootInActiveWindow ?: return "Accessibility is off or no window is open."
            val sb = StringBuilder()
            fun walk(n: AccessibilityNodeInfo?, depth: Int) {
                if (n == null || sb.length > maxChars) return
                val t = (n.text ?: n.contentDescription)?.toString()?.trim().orEmpty()
                if (t.isNotEmpty() && n.isVisibleToUser) {
                    sb.append(t.take(120))
                    if (n.isClickable) sb.append("  [tap]")
                    if (n.isEditable) sb.append("  [text field]")
                    sb.append('\n')
                }
                for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
            }
            walk(root, 0)
            return sb.toString().ifBlank { "The screen has no readable text." }
        }

        fun clickByText(text: String): Boolean {
            ScreenShareService.burst()
            val root = inst?.rootInActiveWindow ?: return false
            val hits = root.findAccessibilityNodeInfosByText(text)
            for (h in hits) {
                var n: AccessibilityNodeInfo? = h
                while (n != null) {                         // climb to the first tappable parent
                    if (n.isClickable && n.isEnabled) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    n = n.parent
                }
            }
            return false
        }

        fun typeText(text: String): Boolean {
            val root = inst?.rootInActiveWindow ?: return false
            val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findEditable(root) ?: return false
            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        private fun findEditable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.isEditable) return n
            for (i in 0 until n.childCount) findEditable(n.getChild(i))?.let { return it }
            return null
        }

        private fun findScrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.isScrollable) return n
            for (i in 0 until n.childCount) findScrollable(n.getChild(i))?.let { return it }
            return null
        }

        fun scroll(forward: Boolean): Boolean {
            val node = findScrollable(inst?.rootInActiveWindow) ?: return false
            return node.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }

        fun longClickByText(text: String): Boolean {
            val root = inst?.rootInActiveWindow ?: return false
            for (h in root.findAccessibilityNodeInfosByText(text)) {
                var n: AccessibilityNodeInfo? = h
                while (n != null) {
                    if (n.isLongClickable) return n.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    n = n.parent
                }
            }
            return false
        }

        /** Presses the keyboard's Enter / Search / Send key in the focused field. */
        fun submit(): Boolean {
            val root = inst?.rootInActiveWindow ?: return false
            val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (field != null && Build.VERSION.SDK_INT >= 30 && field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) return true
            return clickByText("Search") || clickByText("Send") || clickByText("Go") || clickByText("Post")
        }

        fun paste(): Boolean {
            val root = inst?.rootInActiveWindow ?: return false
            val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findEditable(root) ?: return false
            return field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }

        // ---------- screen capture for the live call (no MediaProjection needed, Android 11+) ----------
        private val shotExecutor: Executor by lazy { Executors.newSingleThreadExecutor() }

        /** True on Android 11+ where the accessibility service may take screenshots for the call. */
        val canScreenshot: Boolean get() = Build.VERSION.SDK_INT >= 30

        /**
         * Grabs the current screen as a software Bitmap via AccessibilityService.takeScreenshot.
         * [cb] gets null if the service is off, Android < 11, or the system rate-limits the call.
         */
        fun screenshot(cb: (Bitmap?) -> Unit) {
            val s = inst
            if (s == null || Build.VERSION.SDK_INT < 30) { cb(null); return }
            try {
                s.takeScreenshot(Display.DEFAULT_DISPLAY, shotExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        val bmp = try {
                            Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                        } catch (e: Exception) { null } finally {
                            try { result.hardwareBuffer.close() } catch (e: Exception) { }
                        }
                        cb(bmp)
                    }
                    override fun onFailure(errorCode: Int) { cb(null) }
                })
            } catch (e: Exception) { cb(null) }
        }

        /** The package of the app currently in front (privacy check for screen sharing). */
        fun foregroundPackage(): String? = try { inst?.rootInActiveWindow?.packageName?.toString() } catch (e: Exception) { null }

        /** A password field is focused on screen — never share/learn this screen. */
        fun isSensitiveScreen(): Boolean {
            val root = inst?.rootInActiveWindow ?: return false
            fun walk(n: AccessibilityNodeInfo?): Boolean {
                if (n == null) return false
                if (n.isPassword) return true
                for (i in 0 until n.childCount) if (walk(n.getChild(i))) return true
                return false
            }
            return walk(root)
        }

        /** Finds an element by its visible text or description and returns its centre in normalized coordinates. */
        fun findElementNorm(query: String): String {
            val hits = findElements(query, 5)
            if (hits.isEmpty()) return "not found: '$query'"
            val sz = CursorGeometry.screenSizeOrNull() ?: return "service OFF"
            return hits.joinToString(" ; ") { e ->
                "'${e.label.take(50)}' at x=${CursorGeometry.fmt(e.norm.x)} y=${CursorGeometry.fmt(e.norm.y)} " +
                    "(id=${e.viewId.substringAfterLast('/')} bounds ${e.bounds.left},${e.bounds.top},${e.bounds.right},${e.bounds.bottom} clickable=${e.clickable})"
            }
        }

        /** One tappable/visible element, captured as evidence (Round 3 / B5). */
        class ElementRef(
            val label: String, val viewId: String, val cls: String, val bounds: Rect,
            val norm: android.graphics.PointF, val clickable: Boolean, val enabled: Boolean,
            val node: AccessibilityNodeInfo
        )

        /** Every visible element that has text, a description, an id or is clickable (capped). */
        fun clickableElements(limit: Int = 40): List<ElementRef> {
            val s = inst ?: return emptyList()
            val root = try { activeApp()?.root ?: s.rootInActiveWindow } catch (e: Exception) { null } ?: return emptyList()
            val sz = CursorGeometry.screenSize(s)
            val out = ArrayList<ElementRef>()
            fun walk(n: AccessibilityNodeInfo?) {
                if (n == null || out.size >= limit) return
                val t = (n.text ?: n.contentDescription)?.toString()?.trim().orEmpty()
                val id = n.viewIdResourceName.orEmpty()
                if (n.isVisibleToUser && (t.isNotEmpty() || id.isNotEmpty() || n.isClickable)) {
                    val r = Rect(); n.getBoundsInScreen(r)
                    if (r.width() > 0 && r.height() > 0) {
                        out.add(ElementRef(t, id, n.className?.toString()?.substringAfterLast('.').orEmpty(), Rect(r),
                            CursorGeometry.toNorm(r.centerX().toFloat(), r.centerY().toFloat(), sz), n.isClickable, n.isEnabled, n))
                    }
                }
                for (i in 0 until n.childCount) walk(n.getChild(i))
            }
            walk(root)
            return out
        }

        /** A readable list of what can be tapped right now. */
        fun getClickableElementsText(limit: Int = 40): String {
            if (inst == null) return "Accessibility is off."
            val els = clickableElements(limit)
            if (els.isEmpty()) return "No tappable elements were found on this screen."
            val sb = StringBuilder("${els.size} element(s) (label | id | class | centre x,y | bounds | clickable/enabled):\n")
            els.forEach { e ->
                val focusable = if (e.clickable) "clickable" else "not-clickable"
                sb.append("${e.label.take(60).ifBlank { "(no label)" }} | ${e.viewId.substringAfterLast('/').ifBlank { "-" }} | ${e.cls.ifBlank { "-" }} | " +
                    "x=${CursorGeometry.fmt(e.norm.x)},y=${CursorGeometry.fmt(e.norm.y)} | [${e.bounds.left},${e.bounds.top},${e.bounds.right},${e.bounds.bottom}] | $focusable/${if (e.enabled) "enabled" else "disabled"}\n")
            }
            return sb.toString()
        }

        /** Best-effort match by exact id/label first, then "contains". */
        fun findElements(query: String, limit: Int = 6): List<ElementRef> {
            val q = query.trim().lowercase()
            if (q.isBlank()) return emptyList()
            val els = clickableElements(200)
            val exact = els.filter { it.label.lowercase() == q || it.viewId.substringAfterLast('/').lowercase() == q }
            if (exact.isNotEmpty()) return exact.take(limit)
            return els.filter { it.label.lowercase().contains(q) || it.viewId.lowercase().contains(q) }.take(limit)
        }

        /**
         * Clicks an element by id or label: ACTION_CLICK on the node (or its first clickable parent) and, if that
         * changes nothing, a real gesture at its centre. Reports the truth either way (Round 3 / B5 + B6).
         */
        fun clickElement(query: String): String {
            val hits = findElements(query, 3)
            if (hits.isEmpty()) return "not found: '$query'. Call get_clickable_elements to see what is on the screen."
            val e = hits.first()
            val target = if (e.node.isClickable && e.node.isEnabled) e.node else clickableAncestor(e.node)
            if (target != null) {
                val res = clickNodeAndVerify(target)
                if (res == "clicked (screen changed)") return "clicked '${e.label.take(50)}' via accessibility: $res"
                // accepted but nothing happened, or not accepted: fall through to a real tap at the centre
            }
            val p = CursorGeometry.toPx(e.norm.x, e.norm.y, CursorGeometry.screenSizeOrNull() ?: return "service OFF")
            val o = tapPxAwait(p.x, p.y)
            val under = nodeAt(p.x.toInt(), p.y.toInt())
            return "tapped '${e.label.take(50)}' at pixel (${p.x.toInt()},${p.y.toInt()}) outcome=${o.name.lowercase()} " +
                "(accessibility click was ${if (target != null) "accepted but did not change the screen" else "not possible"}) under[$under]"
        }

        private fun gesture(build: (GestureDescription.Builder, Float, Float) -> Unit): Boolean {
            ScreenShareService.burst()
            val s = inst ?: return false
            // Round 3 (T2): use the SAME real display size the cursor engine uses — not resources.displayMetrics,
            // which can differ (navigation bar / cutout) and would move percent taps off target.
            val sz = CursorGeometry.screenSize(s)
            val b = GestureDescription.Builder()
            build(b, sz.w.toFloat(), sz.h.toFloat())
            return s.dispatchGesture(b.build(), null, null)
        }

        // ---------- normalized cursor gestures (center 0, right/up positive) with callbacks + rate limit ----------
        private val clickTimes = ArrayDeque<Long>()

        /** Enforces the max clicks-per-second the user chose. Returns false when over the limit. */
        fun allowClick(): Boolean {
            val rate = Prefs.cursorClickRate.coerceIn(1, 20)
            val now = System.currentTimeMillis()
            while (clickTimes.isNotEmpty() && now - clickTimes.first() > 1000) clickTimes.removeFirst()
            if (clickTimes.size >= rate) return false
            clickTimes.addLast(now)
            return true
        }

        private fun dispatchNorm(cb: ((Boolean) -> Unit)?, build: (GestureDescription.Builder, CursorGeometry.ScreenSize) -> Unit): Boolean {
            val s = inst
            if (s == null) { cb?.invoke(false); return false }
            ScreenShareService.burst()
            val sz = CursorGeometry.screenSize(s)
            val b = GestureDescription.Builder()
            build(b, sz)
            return s.dispatchGesture(b.build(), object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) { cb?.invoke(true) }
                override fun onCancelled(gestureDescription: GestureDescription?) { cb?.invoke(false) }
            }, null)
        }

        /** Tap at a normalized point (-100..+100). */
        fun tapNorm(x: Float, y: Float, cb: ((Boolean) -> Unit)? = null): Boolean {
            if (!allowClick()) { cb?.invoke(false); return false }
            return dispatchNorm(cb) { b, sz ->
                val p = CursorGeometry.toPx(CursorGeometry.clamp(x), CursorGeometry.clamp(y), sz)
                val path = Path(); path.moveTo(p.x, p.y)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            }
        }

        fun doubleTapNorm(x: Float, y: Float, cb: ((Boolean) -> Unit)? = null): Boolean {
            if (!allowClick()) { cb?.invoke(false); return false }
            return dispatchNorm(cb) { b, sz ->
                val p = CursorGeometry.toPx(CursorGeometry.clamp(x), CursorGeometry.clamp(y), sz)
                val path = Path(); path.moveTo(p.x, p.y)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, 40))
                b.addStroke(GestureDescription.StrokeDescription(path, 140, 40))
            }
        }

        fun longPressNorm(x: Float, y: Float, ms: Long = 700, cb: ((Boolean) -> Unit)? = null): Boolean {
            if (!allowClick()) { cb?.invoke(false); return false }
            return dispatchNorm(cb) { b, sz ->
                val p = CursorGeometry.toPx(CursorGeometry.clamp(x), CursorGeometry.clamp(y), sz)
                val path = Path(); path.moveTo(p.x, p.y)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, ms.coerceIn(300, 3000)))
            }
        }

        fun swipeNorm(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300, cb: ((Boolean) -> Unit)? = null): Boolean {
            if (!allowClick()) { cb?.invoke(false); return false }
            return dispatchNorm(cb) { b, sz ->
                val a = CursorGeometry.toPx(CursorGeometry.clamp(x1), CursorGeometry.clamp(y1), sz)
                val c = CursorGeometry.toPx(CursorGeometry.clamp(x2), CursorGeometry.clamp(y2), sz)
                val path = Path(); path.moveTo(a.x, a.y); path.lineTo(c.x, c.y)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(50, 3000)))
            }
        }

        /** Taps every cursor at once; one stroke per point (capped to the system max). */
        fun clickAll(points: List<Pair<Float, Float>>, cb: ((Boolean) -> Unit)? = null): Boolean {
            if (points.isEmpty()) { cb?.invoke(false); return false }
            if (!allowClick()) { cb?.invoke(false); return false }
            val max = GestureDescription.getMaxStrokeCount()
            return dispatchNorm(cb) { b, sz ->
                points.take(max).forEach { (x, y) ->
                    val p = CursorGeometry.toPx(CursorGeometry.clamp(x), CursorGeometry.clamp(y), sz)
                    val path = Path(); path.moveTo(p.x, p.y)
                    b.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                }
            }
        }

        // ---------- truthful gestures: the real outcome, not a guess (bugfix Masla A / §2.1) ----------
        // GestureOutcome is declared at the top of this file so other classes (CursorController) can map it too.

        /** Every accessibility event seen — used as a "did the screen react" signal after a tap. */
        @Volatile private var eventCount = 0L
        fun eventsNow(): Long = eventCount

        /** Pure evidence: what the service is allowed to do, the real display size and the stroke limit. */
        fun gestureCapabilities(): String {
            val s = inst ?: return "service OFF"
            val caps = try { s.serviceInfo.capabilities } catch (e: Exception) { 0 }
            val gestures = (caps and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0
            val sz = CursorGeometry.screenSize(s)
            return "canPerformGesturesCapability=$gestures caps=0x${Integer.toHexString(caps)} " +
                "display=${sz.w}x${sz.h} maxStrokes=${GestureDescription.getMaxStrokeCount()}"
        }

        /** Dispatches a gesture built in SCREEN pixels and waits for the real result. Blocking — call off the main thread. */
        private fun dispatchAwait(timeoutMs: Long, build: (GestureDescription.Builder, CursorGeometry.ScreenSize) -> Unit): GestureOutcome {
            val s = inst ?: return GestureOutcome.NO_SERVICE
            if (!allowClick()) return GestureOutcome.RATE_LIMITED
            ScreenShareService.burst()
            val sz = CursorGeometry.screenSize(s)
            val b = GestureDescription.Builder()
            build(b, sz)
            val latch = CountDownLatch(1)
            var outcome = GestureOutcome.TIMEOUT
            val accepted = try {
                s.dispatchGesture(b.build(), object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) { outcome = GestureOutcome.COMPLETED; latch.countDown() }
                    override fun onCancelled(gestureDescription: GestureDescription?) { outcome = GestureOutcome.CANCELLED; latch.countDown() }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) { Diag.fail("gesture.dispatchAwait", e); false }
            if (!accepted) return GestureOutcome.REJECTED
            try { latch.await(timeoutMs, TimeUnit.MILLISECONDS) } catch (e: InterruptedException) { }
            return outcome
        }

        /** Tap a raw SCREEN pixel and report what really happened. */
        fun tapPxAwait(px: Float, py: Float, durationMs: Long = 60, timeoutMs: Long = 2000): GestureOutcome =
            dispatchAwait(timeoutMs) { b, _ ->
                val path = Path(); path.moveTo(px, py)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(20, 3000)))
            }

        // ---------- truthful percent gestures (Round 3 / T2, B3): real size + real outcome ----------

        /** Swipe by direction, reporting the real gesture outcome (not just "did it get accepted"). */
        fun swipeAwait(direction: String, timeoutMs: Long = 2500): GestureOutcome = dispatchAwait(timeoutMs) { b, sz ->
            val w = sz.w.toFloat(); val h = sz.h.toFloat()
            val p = Path()
            when (direction.lowercase()) {
                "up" -> { p.moveTo(w / 2, h * 0.75f); p.lineTo(w / 2, h * 0.25f) }
                "down" -> { p.moveTo(w / 2, h * 0.30f); p.lineTo(w / 2, h * 0.75f) }
                "left" -> { p.moveTo(w * 0.80f, h / 2); p.lineTo(w * 0.20f, h / 2) }
                else -> { p.moveTo(w * 0.20f, h / 2); p.lineTo(w * 0.80f, h / 2) }
            }
            b.addStroke(GestureDescription.StrokeDescription(p, 0, 300))
        }

        /** Tap anywhere by percentage of the REAL screen (0-100), reporting the real outcome. */
        fun tapAtAwait(xPercent: Float, yPercent: Float, timeoutMs: Long = 2000): GestureOutcome = dispatchAwait(timeoutMs) { b, sz ->
            val path = Path(); path.moveTo(sz.w * xPercent / 100f, sz.h * yPercent / 100f)
            b.addStroke(GestureDescription.StrokeDescription(path, 0, 60))
        }

        fun doubleTapAtAwait(xPercent: Float, yPercent: Float, timeoutMs: Long = 2500): GestureOutcome = dispatchAwait(timeoutMs) { b, sz ->
            val path = Path(); path.moveTo(sz.w * xPercent / 100f, sz.h * yPercent / 100f)
            b.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            b.addStroke(GestureDescription.StrokeDescription(path, 130, 50))
        }

        /**
         * ACTION_CLICK on a node, then check the screen actually reacted (~800 ms of accessibility events).
         * Round 3 / B6: a click that the framework accepted but that changed nothing is reported as such.
         */
        fun clickNodeAndVerify(node: AccessibilityNodeInfo): String {
            val before = eventsNow()
            val ok = try { node.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (e: Exception) { Diag.fail("a11y.clickNode", e); false }
            if (!ok) return "action_not_accepted"
            ScreenShareService.burst()
            val t0 = System.currentTimeMillis()
            while (System.currentTimeMillis() - t0 < 800) {
                if (eventsNow() > before) return "clicked (screen changed)"
                try { Thread.sleep(50) } catch (e: InterruptedException) { break }
            }
            return "click_accepted_but_screen_did_not_change"
        }

        fun doubleTapPxAwait(px: Float, py: Float, timeoutMs: Long = 2500): GestureOutcome =
            dispatchAwait(timeoutMs) { b, _ ->
                val path = Path(); path.moveTo(px, py)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, 40))
                b.addStroke(GestureDescription.StrokeDescription(path, 140, 40))
            }

        fun longPressPxAwait(px: Float, py: Float, ms: Long = 700, timeoutMs: Long = 2500): GestureOutcome =
            dispatchAwait(timeoutMs) { b, _ ->
                val path = Path(); path.moveTo(px, py)
                b.addStroke(GestureDescription.StrokeDescription(path, 0, ms.coerceIn(300, 3000)))
            }

        /** The real app/launcher window in front — OUR overlay windows are ignored on purpose. */
        private fun activeApp(): AccessibilityWindowInfo? {
            return try {
                val ws = inst?.windows ?: return null
                ws.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.firstOrNull { it.isActive }
                    ?: ws.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            } catch (e: Exception) { null }
        }

        /** Hit-test: which element sits under this SCREEN pixel (overlays ignored)? Pure evidence for a click. */
        fun nodeAt(px: Int, py: Int): String {
            val s = inst ?: return "service OFF"
            val root = try { activeApp()?.root ?: s.rootInActiveWindow } catch (e: Exception) { null } ?: return "no window"
            val tmp = Rect()
            fun walk(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                if (n == null) return null
                n.getBoundsInScreen(tmp)
                if (!tmp.contains(px, py) || !n.isVisibleToUser) return null
                for (i in n.childCount - 1 downTo 0) walk(n.getChild(i))?.let { return it }
                return n
            }
            val n = walk(root) ?: return "nothing (empty area)"
            val hit = Rect(); n.getBoundsInScreen(hit)
            val txt = (n.text ?: n.contentDescription)?.toString()?.trim().orEmpty()
            var up: AccessibilityNodeInfo? = n
            var target: AccessibilityNodeInfo? = null
            while (up != null) { if (up.isClickable && up.isEnabled) { target = up; break }; up = up.parent }
            val cr = Rect(); (target ?: n).getBoundsInScreen(cr)
            return "pkg=${n.packageName} class=${n.className?.toString()?.substringAfterLast('.')} text='${txt.take(40)}' " +
                "id=${n.viewIdResourceName?.substringAfterLast('/')} clickable=${n.isClickable} hit=[${hit.left},${hit.top},${hit.right},${hit.bottom}] " +
                "clickTarget=${if (target != null) "[${cr.left},${cr.top},${cr.right},${cr.bottom}]" else "none"}"
        }

        /** Swipe the finger: "up" moves to the next video / scrolls the feed down, "down" goes back, left / right switch pages. */
        fun swipe(direction: String): Boolean = gesture { b, w, h ->
            val p = Path()
            when (direction.lowercase()) {
                "up" -> { p.moveTo(w / 2, h * 0.75f); p.lineTo(w / 2, h * 0.25f) }
                "down" -> { p.moveTo(w / 2, h * 0.30f); p.lineTo(w / 2, h * 0.75f) }
                "left" -> { p.moveTo(w * 0.80f, h / 2); p.lineTo(w * 0.20f, h / 2) }
                else -> { p.moveTo(w * 0.20f, h / 2); p.lineTo(w * 0.80f, h / 2) }
            }
            b.addStroke(GestureDescription.StrokeDescription(p, 0, 300))
        }

        /** Tap anywhere by percentage of the screen (0-100). */
        fun tapAt(xPercent: Float, yPercent: Float): Boolean = gesture { b, w, h ->
            val p = Path(); p.moveTo(w * xPercent / 100f, h * yPercent / 100f)
            b.addStroke(GestureDescription.StrokeDescription(p, 0, 60))
        }

        /** Double tap (for example the "like" gesture on video apps). */
        fun doubleTapAt(xPercent: Float, yPercent: Float): Boolean = gesture { b, w, h ->
            val p = Path(); p.moveTo(w * xPercent / 100f, h * yPercent / 100f)
            b.addStroke(GestureDescription.StrokeDescription(p, 0, 50))
            b.addStroke(GestureDescription.StrokeDescription(p, 130, 50))
        }

        private val endWords = Regex("(?i)^(end( the)? call|end|hang ?up|decline|reject|disconnect|leave( call)?|cancel call|end all)$")
        private val endIds = Regex("(?i)(endcall|end_call|endbutton|end_button|hangup|hang_up|disconnect|decline|reject)")

        private fun clickableAncestor(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            var x = n
            while (x != null) { if (x.isClickable && x.isEnabled) return x; x = x.parent }
            return null
        }

        /** Public wrapper (Round 3 / B5) so the tool layer can reuse the same "climb to the tappable parent" rule. */
        fun clickableAncestorOf(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? = clickableAncestor(n)

        /** Finds the red "End call / Hang up / Decline" button of whatever call screen is open and taps it. */
        fun endCall(): Boolean {
            val root = inst?.rootInActiveWindow ?: return false
            var target: AccessibilityNodeInfo? = null
            fun walk(n: AccessibilityNodeInfo?) {
                if (n == null || target != null) return
                val label = (n.contentDescription ?: n.text)?.toString()?.trim().orEmpty()
                val id = n.viewIdResourceName.orEmpty()
                if ((label.length in 1..22 && endWords.containsMatchIn(label)) || (id.isNotEmpty() && endIds.containsMatchIn(id.substringAfter('/')))) {
                    target = clickableAncestor(n)
                    if (target != null) return
                }
                for (i in 0 until n.childCount) walk(n.getChild(i))
            }
            walk(root)
            return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        }

        fun press(button: String): Boolean {
            val s = inst ?: return false
            return when (button.lowercase()) {
                "back" -> s.performGlobalAction(GLOBAL_ACTION_BACK)
                "home" -> s.performGlobalAction(GLOBAL_ACTION_HOME)
                "recents" -> s.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "notifications" -> s.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "quick_settings" -> s.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "power_dialog", "power_menu" -> s.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)       // shows the power menu; you tap the option yourself
                "lock_screen" -> Build.VERSION.SDK_INT >= 28 && s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                "screenshot" -> Build.VERSION.SDK_INT >= 28 && s.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                else -> false
            }
        }
    }

    override fun onServiceConnected() { inst = this; Diag.log("a11y", "onServiceConnected (service is READY)"); CursorController.restoreIfAny() }
    override fun onUnbind(intent: android.content.Intent?): Boolean { Diag.log("a11y", "onUnbind -> clearing cursors"); try { CursorController.snapshotForRestore(); CursorController.panic() } catch (e: Exception) { }; inst = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { eventCount++ }
    override fun onInterrupt() { }
}
