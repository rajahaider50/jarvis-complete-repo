package com.jarvis.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Decides whether to draw the lighter version (fewer particles/details) on weak or saving devices. */
object PerformanceTier {
    fun isLow(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null && am.isLowRamDevice) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && pm.isPowerSaveMode) return true
        }
        return false
    }
}
