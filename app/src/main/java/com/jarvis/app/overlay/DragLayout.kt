package com.jarvis.app.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Root view of a floating window. Taps go to the Compose content (buttons); as soon as a finger moves further than the
 * touch slop this layout takes over and reports the drag, so the whole window can be dragged from anywhere on it.
 */
internal class DragLayout(context: Context, private val axis: Axis) : FrameLayout(context) {
    enum class Axis { ANY, VERTICAL }

    var onDragStart: (() -> Unit)? = null
    var onDrag: ((dx: Float, dy: Float) -> Unit)? = null
    var onDragEnd: ((dx: Float, dy: Float) -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    private fun exceedsSlop(dx: Float, dy: Float) =
        if (axis == Axis.ANY) hypot(dx, dy) > slop else abs(dy) > slop && abs(dy) > abs(dx)

    private fun begin() {
        if (dragging) return
        dragging = true
        onDragStart?.invoke()
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; dragging = false }
            MotionEvent.ACTION_MOVE -> if (exceedsSlop(e.rawX - downX, e.rawY - downY)) { begin(); return true }
        }
        return false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; dragging = false }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                if (!dragging && exceedsSlop(dx, dy)) begin()
                if (dragging) onDrag?.invoke(dx, dy)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    onDragEnd?.invoke(e.rawX - downX, e.rawY - downY)
                }
            }
        }
        return true
    }
}
