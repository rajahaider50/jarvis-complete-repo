package com.jarvis.app.live

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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

        private fun gesture(build: (GestureDescription.Builder, Float, Float) -> Unit): Boolean {
            val s = inst ?: return false
            val dm = s.resources.displayMetrics
            val b = GestureDescription.Builder()
            build(b, dm.widthPixels.toFloat(), dm.heightPixels.toFloat())
            return s.dispatchGesture(b.build(), null, null)
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

    override fun onServiceConnected() { inst = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { inst = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() { }
}
