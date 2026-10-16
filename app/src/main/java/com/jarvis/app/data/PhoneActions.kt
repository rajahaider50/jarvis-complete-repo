package com.jarvis.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarvis.app.live.JarvisAccessibilityService

/** Things the assistant can do on the phone for you (browser, apps, screen). Used by the live-call tools. */
object PhoneActions {
    fun openApp(ctx: Context, name: String): String {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
        val q = name.trim().lowercase()
        val hit = apps.firstOrNull { it.loadLabel(pm).toString().lowercase() == q }
            ?: apps.firstOrNull { it.loadLabel(pm).toString().lowercase().contains(q) }
            ?: return "No app called \"$name\" is installed."
        val i = pm.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return "Cannot open that app."
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return "Opened ${hit.loadLabel(pm)}."
    }

    fun openUrl(ctx: Context, url: String): String {
        val u = if (url.startsWith("http")) url else "https://$url"
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(Intent(view).setPackage("com.android.chrome")) }       // Chrome first
        catch (e: Exception) { ctx.startActivity(view) }                                  // otherwise the default browser
        return "Opened $u in the browser."
    }

    /** Play / pause, next, previous, volume and mute for whatever video or music is playing. */
    fun media(ctx: Context, action: String): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        fun key(code: Int) {
            am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, code))
            am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, code))
        }
        when (action.lowercase()) {
            "play_pause", "play", "pause" -> key(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "next" -> key(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            "previous" -> key(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "mute" -> am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, 0)
            "unmute" -> am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_UNMUTE, 0)
            "volume_up" -> am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, 0)
            "volume_down" -> am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, 0)
            else -> return "Unknown media action."
        }
        return "Done."
    }

    private fun canManageCalls(ctx: Context) =
        android.os.Build.VERSION.SDK_INT >= 26 && androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Hangs up the phone call: Android's own call API first, then the red button on the call screen. */
    fun endPhoneCall(ctx: Context): String {
        if (android.os.Build.VERSION.SDK_INT >= 28 && canManageCalls(ctx)) {
            val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            try { if (tm.endCall()) return "The phone call was ended." } catch (e: SecurityException) { AppLog.w(e) }
        }
        if (!JarvisAccessibilityService.enabled) return "Cannot end the call: allow 'Answer and end phone calls' or switch on screen control in Access & permissions."
        return if (JarvisAccessibilityService.endCall()) "Pressed the end-call button." else "I could not find an end-call button on the screen."
    }

    fun answerPhoneCall(ctx: Context): String {
        if (!canManageCalls(ctx)) return "Allow 'Answer and end phone calls' in Access & permissions first."
        val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        return try { tm.acceptRingingCall(); "Answering the call." } catch (e: SecurityException) { AppLog.w(e); "Android did not allow answering." }
    }

    /** Locks the screen: with the device-admin permission, otherwise with screen control. */
    fun lockScreen(ctx: Context): String {
        if (com.jarvis.app.assistant.DeviceAdmin.isActive(ctx) && com.jarvis.app.assistant.DeviceAdmin.lockNow(ctx)) return "The screen is locked."
        if (JarvisAccessibilityService.enabled && JarvisAccessibilityService.press("lock_screen")) return "The screen is locked."
        return "Cannot lock: switch on 'Lock the screen' (device admin) in Access & permissions."
    }

    fun openSettings(ctx: Context, page: String): String {
        val a = when (page.lowercase().trim().replace(' ', '_')) {
            "wifi", "wi-fi" -> android.provider.Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> android.provider.Settings.ACTION_SOUND_SETTINGS
            "battery" -> android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> android.provider.Settings.ACTION_APPLICATION_SETTINGS
            "location" -> android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "accessibility" -> android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
            "date", "time" -> android.provider.Settings.ACTION_DATE_SETTINGS
            "language" -> android.provider.Settings.ACTION_LOCALE_SETTINGS
            "storage" -> android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "security" -> android.provider.Settings.ACTION_SECURITY_SETTINGS
            "privacy" -> android.provider.Settings.ACTION_PRIVACY_SETTINGS
            "airplane" -> android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS
            "nfc" -> android.provider.Settings.ACTION_NFC_SETTINGS
            "network", "hotspot", "mobile_data" -> android.provider.Settings.ACTION_WIRELESS_SETTINGS
            "about" -> android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
            else -> android.provider.Settings.ACTION_SETTINGS
        }
        ctx.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opened the $page settings."
    }

    fun setTimer(ctx: Context, seconds: Int, label: String): String {
        val i = Intent(android.provider.AlarmClock.ACTION_SET_TIMER)
            .putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds.coerceIn(1, 86400))
            .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return "Timer set for $seconds seconds."
    }

    fun setAlarm(ctx: Context, hour: Int, minute: Int, label: String): String {
        val i = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
            .putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return "Alarm set for %02d:%02d.".format(hour, minute)
    }

    /** Opens the dialer with the number filled in; the user presses the green button. */
    fun dial(ctx: Context, number: String): String {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "The dialer is open with $number. Press call."
    }

    fun needsAccessibility(): String? =
        if (JarvisAccessibilityService.enabled) null
        else "Accessibility is not switched on. Ask the user to enable Jarvis in Android Settings > Accessibility."
}
