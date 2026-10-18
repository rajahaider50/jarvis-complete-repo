package com.jarvis.app.live

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.view.WindowManager
import java.util.Locale

/**
 * The JARVIS cursor coordinate system. The screen CENTER is (0,0). X: left -100, right +100.
 * Y: bottom -100, top +100 (up is positive). Values are clamped to -100..+100 with 2-decimal display.
 * Pixel conversion always uses the REAL current display size, so rotation/density are handled automatically.
 */
object CursorGeometry {
    data class ScreenSize(val w: Int, val h: Int)

    fun screenSize(ctx: Context): ScreenSize {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealSize(p)
        return ScreenSize(p.x.coerceAtLeast(1), p.y.coerceAtLeast(1))
    }

    /** normalized (-100..+100, up +) -> screen pixels. */
    fun toPx(x: Float, y: Float, s: ScreenSize): PointF {
        val cx = s.w / 2f; val cy = s.h / 2f
        return PointF(cx + (x / 100f) * cx, cy - (y / 100f) * cy)
    }

    /** screen pixels -> normalized (-100..+100, up +). */
    fun toNorm(px: Float, py: Float, s: ScreenSize): PointF {
        val cx = s.w / 2f; val cy = s.h / 2f
        return PointF(((px - cx) / cx) * 100f, -((py - cy) / cy) * 100f)
    }

    fun clamp(v: Float): Float = v.coerceIn(-100f, 100f)

    /** "+12.50" / "-87.25" */
    fun fmt(v: Float): String = (if (v >= 0f) "+" else "") + String.format(Locale.US, "%.2f", v)
}
