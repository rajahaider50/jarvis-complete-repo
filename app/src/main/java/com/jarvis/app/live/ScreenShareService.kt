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
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.jarvis.app.data.ImageUtil

/** Foreground service that captures the screen (MediaProjection) and hands ~1 JPEG frame/second to the live call. */
class ScreenShareService : Service() {
    companion object {
        @Volatile var frameSink: ((ByteArray) -> Unit)? = null
        @Volatile var running = false
        fun start(ctx: Context, code: Int, data: Intent) {
            val i = Intent(ctx, ScreenShareService::class.java).putExtra("code", code).putExtra("data", data)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, ScreenShareService::class.java)) }
    }

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var last = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.let {
            @Suppress("DEPRECATION") it.getParcelableExtra<Intent>("data")
        }
        if (intent == null || data == null) { stopSelf(); return START_NOT_STICKY }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("share", "Screen sharing", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "share").setContentTitle("Jarvis is viewing your screen")
            .setContentText("Screen sharing with the live call").setSmallIcon(android.R.drawable.ic_menu_view).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(77, n)
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val p = mpm.getMediaProjection(intent.getIntExtra("code", 0), data)
            projection = p
            p.registerCallback(object : MediaProjection.Callback() { override fun onStop() { cleanup(); stopSelf() } }, Handler(Looper.getMainLooper()))
            val dm = resources.displayMetrics
            val w = 540; val h = (dm.heightPixels * 540f / dm.widthPixels).toInt()
            val r = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            reader = r
            display = p.createVirtualDisplay("jarvis-share", w, h, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, null)
            r.setOnImageAvailableListener({ rd ->
                val img = rd.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val now = System.currentTimeMillis()
                    if (now - last >= 1000) {
                        last = now
                        val pl = img.planes[0]
                        val rowPx = pl.rowStride / pl.pixelStride
                        val bmp = Bitmap.createBitmap(rowPx, h, Bitmap.Config.ARGB_8888)
                        bmp.copyPixelsFromBuffer(pl.buffer)
                        val cropped = Bitmap.createBitmap(bmp, 0, 0, w, h)
                        val jpeg = ImageUtil.toJpeg(cropped, 720, 60)
                        val sink = frameSink
                        if (sink != null) sink(jpeg) else CallHost.client?.sendJpeg(jpeg)
                    }
                } catch (e: Exception) { AppLog.w(e); } finally { img.close() }
            }, Handler(Looper.getMainLooper()))
            running = true
        } catch (e: Exception) { AppLog.w(e); cleanup(); stopSelf() }
        return START_NOT_STICKY
    }

    private fun cleanup() {
        running = false
        try { display?.release() } catch (e: Exception) { AppLog.w(e); }
        try { reader?.close() } catch (e: Exception) { AppLog.w(e); }
        try { projection?.stop() } catch (e: Exception) { AppLog.w(e); }
        display = null; reader = null; projection = null
    }

    override fun onDestroy() { cleanup(); super.onDestroy() }
}
