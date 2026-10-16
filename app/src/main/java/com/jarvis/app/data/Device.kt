package com.jarvis.app.data

import android.app.ActivityManager
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Stats(
    val battery: Int, val charging: Boolean, val tempC: Float,
    val storageFreeGb: Float, val storageTotalGb: Float, val ramFreeGb: Float, val ramTotalGb: Float,
    val thermal: String
) {
    fun summary() =
        "Battery $battery% (${if (charging) "charging" else "not charging"}), battery temperature ${"%.1f".format(tempC)} C, " +
            "thermal status $thermal, storage free ${"%.1f".format(storageFreeGb)} of ${"%.0f".format(storageTotalGb)} GB, " +
            "memory free ${"%.1f".format(ramFreeGb)} of ${"%.1f".format(ramTotalGb)} GB"
}

object DeviceStats {
    fun read(ctx: Context): Stats {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val fs = StatFs(Environment.getDataDirectory().path)
        val gb = 1024f * 1024f * 1024f
        val mi = ActivityManager.MemoryInfo()
        (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        var thermal = "Normal"
        if (Build.VERSION.SDK_INT >= 29) {
            val ps = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            thermal = when (ps.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                else -> "Critical"
            }
        }
        return Stats(pct, charging, temp, fs.availableBytes / gb, fs.totalBytes / gb, mi.availMem / gb, mi.totalMem / gb, thermal)
    }
}

/** Real phone controls. Android does not let apps flip Wi-Fi/Bluetooth/Airplane directly, so those open the system panel. */
object DeviceControl {
    var torchOn by mutableStateOf(false)
    private var torchInit = false

    fun init(ctx: Context) {
        if (torchInit) return
        torchInit = true
        try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torchOn = enabled }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) { AppLog.w(e); }
    }

    fun setTorch(ctx: Context, on: Boolean): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cm.setTorchMode(id, on); true
        } catch (e: CameraAccessException) { AppLog.w(e); false } catch (e: Exception) { AppLog.w(e); false }
    }

    private fun am(ctx: Context) = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    fun volume(ctx: Context, stream: Int): Float {
        val a = am(ctx); return a.getStreamVolume(stream).toFloat() / a.getStreamMaxVolume(stream).coerceAtLeast(1)
    }
    fun setVolume(ctx: Context, stream: Int, frac: Float) {
        val a = am(ctx)
        try { a.setStreamVolume(stream, (frac * a.getStreamMaxVolume(stream)).toInt(), 0) } catch (e: Exception) { AppLog.w(e); }
    }
    fun setMusicVolumePercent(ctx: Context, pct: Int) = setVolume(ctx, AudioManager.STREAM_MUSIC, pct.coerceIn(0, 100) / 100f)

    fun canWriteSettings(ctx: Context) = Settings.System.canWrite(ctx)
    fun brightness(ctx: Context): Float = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    } catch (e: Exception) { AppLog.w(e); 0.5f }
    fun setBrightness(ctx: Context, frac: Float) {
        try {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (frac * 255).toInt().coerceIn(1, 255))
        } catch (e: Exception) { AppLog.w(e); }
    }
    fun askWriteSettings(ctx: Context) = open(ctx, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    fun wifiOn(ctx: Context): Boolean = try {
        (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    } catch (e: Exception) { AppLog.w(e); false }
    fun bluetoothOn(ctx: Context): Boolean = try {
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true
    } catch (e: Exception) { AppLog.w(e); false }
    fun airplaneOn(ctx: Context): Boolean = try {
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    } catch (e: Exception) { AppLog.w(e); false }

    private fun nm(ctx: Context) = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun dndAllowed(ctx: Context) = nm(ctx).isNotificationPolicyAccessGranted
    fun dndOn(ctx: Context) = nm(ctx).currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
        nm(ctx).currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    fun setDnd(ctx: Context, on: Boolean) {
        if (!dndAllowed(ctx)) { open(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); return }
        nm(ctx).setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun open(ctx: Context, i: Intent) {
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { AppLog.w(e); }
    }
    fun openWifi(ctx: Context) = open(ctx, Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS))
    fun openBluetooth(ctx: Context) = open(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openAirplane(ctx: Context) = open(ctx, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    fun openLocation(ctx: Context) = open(ctx, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    fun openAppSettings(ctx: Context) = open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
}
