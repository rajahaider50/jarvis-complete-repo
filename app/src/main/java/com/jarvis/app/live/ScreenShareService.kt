package com.jarvis.app.live

import com.jarvis.app.data.AppLog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.ImageUtil
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Shares the screen with the live call WITHOUT opening a second MediaProjection session (that would kill the
 * in-app recorder on Android 14). It uses AccessibilityService.takeScreenshot instead, ~1 frame/second, so share
 * and recording can both run at once. Frame policy: skip unchanged screens, slow down when idle, send a burst
 * right after an action, and pause on password/sensitive screens.
 */
class ScreenShareService : Service() {
    companion object {
        @Volatile var frameSink: ((ByteArray) -> Unit)? = null
        @Volatile var running = false
        /** True while sharing is on but frames are withheld because a password / sensitive screen is showing. */
        var paused by mutableStateOf(false)

        fun start(ctx: Context) {
            val i = Intent(ctx, ScreenShareService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, ScreenShareService::class.java)) }

        /** Called after a click/swipe so the assistant gets a fresh frame within ~0.3s (verify loop). */
        @Volatile private var burstUntil = 0L
        fun burst(ms: Long = 1200) { burstUntil = System.currentTimeMillis() + ms }

        private val blocklist = listOf(
            "bank", "com.android.settings", "paytm", "phonepe", "gpay", "com.google.android.apps.nbu.paisa",
            "com.google.android.gms.auth", "keepass", "1password", "bitwarden", "lastpass"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_NOT_STICKY
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("share", "Screen sharing", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "share").setContentTitle("Jarvis is viewing your screen")
            .setContentText("Screen sharing with the live call").setSmallIcon(android.R.drawable.ic_menu_view).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(77, n)
        if (!JarvisAccessibilityService.canScreenshot) {
            AppLog.i("Share", "Screen sharing needs Android 11+ and the accessibility service")
            stopSelf(); return START_NOT_STICKY
        }
        running = true
        Thread({ loop() }, "jarvis-share").start()
        return START_NOT_STICKY
    }

    private fun loop() {
        val gray = IntArray(64 * 64)
        val prev = IntArray(64 * 64)
        var havePrev = false
        var idle = 0
        try {
            while (running) {
                val now = System.currentTimeMillis()
                val burst = now < burstUntil

                val pkg = JarvisAccessibilityService.foregroundPackage()
                val sensitive = pkg != null && blocklist.any { pkg.contains(it, true) } || JarvisAccessibilityService.isSensitiveScreen()
                paused = sensitive
                if (sensitive) { Thread.sleep(600); continue }

                val bmp = grab()
                if (bmp == null) { Thread.sleep(if (burst) 250 else 1000); continue }

                downscaleGray(bmp, gray)
                val changed = !havePrev || meanDiff(prev, gray) > 6
                var send = changed
                if (changed) { System.arraycopy(gray, 0, prev, 0, gray.size); havePrev = true; idle = 0 } else idle++
                if (burst) send = true

                if (send) {
                    try {
                        val jpeg = ImageUtil.toJpeg(bmp, 720, 60)
                        (frameSink ?: { bytes: ByteArray -> CallHost.client?.sendJpeg(bytes) })(jpeg)
                        Diag.framesSent++
                        if (Diag.framesSent % 10 == 1) Diag.log("share", "frame #${Diag.framesSent} ${jpeg.size}B burst=$burst")
                    } catch (e: Exception) { Diag.fail("share.frame", e); AppLog.w(e) }
                } else Diag.framesDropped++
                bmp.recycle()

                val wait = when {
                    burst -> 300L
                    changed -> 1000L
                    idle >= 5 -> 3000L
                    else -> 1000L
                }
                Thread.sleep(wait)
            }
        } catch (e: Exception) { AppLog.w(e) } finally {
            running = false; paused = false
        }
    }

    private fun grab(): Bitmap? {
        val latch = CountDownLatch(1)
        var bmp: Bitmap? = null
        JarvisAccessibilityService.screenshot { b -> bmp = b; latch.countDown() }
        return if (latch.await(1500, TimeUnit.MILLISECONDS)) bmp else null
    }

    /** Nearest-neighbour downscale to 64x64 grayscale for cheap change detection. */
    private fun downscaleGray(src: Bitmap, out: IntArray) {
        val w = src.width; val h = src.height
        for (y in 0 until 64) {
            val sy = (y * h / 64).coerceIn(0, h - 1)
            for (x in 0 until 64) {
                val sx = (x * w / 64).coerceIn(0, w - 1)
                val c = src.getPixel(sx, sy)
                out[y * 64 + x] = ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
            }
        }
    }

    private fun meanDiff(a: IntArray, b: IntArray): Int {
        var s = 0L
        for (i in a.indices) s += kotlin.math.abs(a[i] - b[i])
        return (s / a.size).toInt()
    }

    override fun onDestroy() { running = false; paused = false; super.onDestroy() }
}
