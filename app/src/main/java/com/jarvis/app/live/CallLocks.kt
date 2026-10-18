package com.jarvis.app.live

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import com.jarvis.app.data.AppLog

/**
 * Holds a partial wake lock (CPU) and a Wi-Fi high-performance lock for the whole live call, so Android's Doze /
 * Wi-Fi sleep cannot suspend the socket in the middle of a long call or while the screen is off. Released on hang-up.
 */
object CallLocks {
    private var wake: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null

    @Synchronized
    fun acquire(ctx: Context) {
        if (wake == null) {
            try {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:call").apply {
                    setReferenceCounted(false); acquire()
                }
                Diag.log("locks", "partial wake lock acquired")
            } catch (e: Exception) { Diag.fail("locks.wake", e); AppLog.w(e) }
        }
        if (wifi == null) {
            try {
                val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val lock = if (Build.VERSION.SDK_INT >= 29) wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "jarvis:call")
                           else wm.createWifiLock(WifiManager.WIFI_MODE_FULL)
                wifi = lock.apply { setReferenceCounted(false); acquire() }
                Diag.log("locks", "wifi lock acquired")
            } catch (e: Exception) { Diag.fail("locks.wifi", e); AppLog.w(e) }
        }
    }

    @Synchronized
    fun release() {
        try { wake?.let { if (it.isHeld) it.release() }; Diag.log("locks", "wake lock released") } catch (e: Exception) { AppLog.w(e) }
        wake = null
        try { wifi?.let { if (it.isHeld) it.release() }; Diag.log("locks", "wifi lock released") } catch (e: Exception) { AppLog.w(e) }
        wifi = null
    }
}
