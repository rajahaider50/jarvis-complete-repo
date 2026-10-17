package com.jarvis.app.live

import com.jarvis.app.data.AppLog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import com.jarvis.app.MainActivity
import com.jarvis.app.data.Prefs

/**
 * Keeps the live call (and its microphone) alive while the app is in the background, as Android requires:
 * a foreground service of type "microphone" with a permanent notification. Camera use of the call adds the
 * "camera" type. This lets the whole call keep running while you use other screens of the app or leave it.
 */
class CallService : Service() {
    companion object {
        const val ACTION_END = "com.jarvis.app.END_CALL"
        private const val ID = 87

        @Volatile private var camera = false
        @Volatile var running = false
            private set

        fun start(ctx: Context) {
            val i = Intent(ctx, CallService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { running = false; ctx.stopService(Intent(ctx, CallService::class.java)) }

        /** Tell the service the call now also uses the camera (or stopped): updates the foreground service type. */
        fun setCamera(ctx: Context, on: Boolean) {
            if (camera == on) return
            camera = on
            if (running) start(ctx)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END) {
            CallHost.end(applicationContext, showPopup = true)
            stopSelf()
            return START_NOT_STICKY
        }
        Prefs.init(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("call", "Live call", NotificationManager.IMPORTANCE_LOW))

        val open = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java).putExtra("route", "live").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val end = PendingIntent.getService(
            this, 3, Intent(this, CallService::class.java).setAction(ACTION_END), PendingIntent.FLAG_IMMUTABLE
        )
        val endAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "End call", end
        ).build()
        val n = Notification.Builder(this, "call")
            .setContentTitle("${Prefs.assistantName} · live call")
            .setContentText("Tap to return to the call")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open).addAction(endAction).setOngoing(true).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (camera) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                startForeground(ID, n, type)
            } else startForeground(ID, n)
            running = true
        } catch (e: Exception) { AppLog.w(e); stopSelf(); return START_NOT_STICKY }
        return START_NOT_STICKY
    }

    override fun onDestroy() { running = false; super.onDestroy() }
}
