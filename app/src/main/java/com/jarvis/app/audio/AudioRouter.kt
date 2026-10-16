package com.jarvis.app.audio

import com.jarvis.app.data.AppLog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Route { AUTO, SPEAKER, EARPIECE, BLUETOOTH, WIRED }

/**
 * Call audio routing. AUTO = wired headset > Bluetooth > earpiece when the phone is at your ear (proximity sensor) > loudspeaker.
 */
object AudioRouter {
    var selected by mutableStateOf(Route.AUTO)
    var active by mutableStateOf(Route.SPEAKER)
    var hasBt by mutableStateOf(false)
    var hasWired by mutableStateOf(false)
    private var near = false
    private var started = false
    private var am: AudioManager? = null
    private var sm: SensorManager? = null
    private var wake: PowerManager.WakeLock? = null
    private var savedVoiceVol = -1

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val s = e.sensor
            near = e.values[0] < minOf(5f, s.maximumRange)
            if (selected == Route.AUTO) apply()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    private val devCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) { refresh(); apply() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) { refresh(); apply() }
    }

    fun start(ctx: Context) {
        if (started) return
        started = true
        val a = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am = a
        savedVoiceVol = a.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        a.mode = AudioManager.MODE_IN_COMMUNICATION
        a.registerAudioDeviceCallback(devCallback, Handler(Looper.getMainLooper()))
        val s = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm = s
        s.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { s.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wake = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "jarvis:proximity")
        }
        refresh(); apply()
    }

    fun stop(ctx: Context) {
        if (!started) return
        started = false
        try { sm?.unregisterListener(sensorListener) } catch (e: Exception) { AppLog.w(e); }
        try { am?.unregisterAudioDeviceCallback(devCallback) } catch (e: Exception) { AppLog.w(e); }
        releaseWake()
        val a = am ?: return
        try {
            if (Build.VERSION.SDK_INT >= 31) a.clearCommunicationDevice()
            else { @Suppress("DEPRECATION") a.isSpeakerphoneOn = false; @Suppress("DEPRECATION") a.stopBluetoothSco() }
            if (savedVoiceVol >= 0) a.setStreamVolume(AudioManager.STREAM_VOICE_CALL, savedVoiceVol, 0)
        } catch (e: Exception) { AppLog.w(e); }
        a.mode = AudioManager.MODE_NORMAL
    }

    fun select(r: Route) { selected = r; apply() }

    private fun refresh() {
        val a = am ?: return
        val outs = a.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        hasBt = outs.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) }
        hasWired = outs.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
    }

    private fun resolve(): Route = when (selected) {
        Route.AUTO -> when { hasWired -> Route.WIRED; hasBt -> Route.BLUETOOTH; near -> Route.EARPIECE; else -> Route.SPEAKER }
        Route.BLUETOOTH -> if (hasBt) Route.BLUETOOTH else Route.SPEAKER
        Route.WIRED -> if (hasWired) Route.WIRED else Route.SPEAKER
        else -> selected
    }

    private fun apply() {
        val a = am ?: return
        val target = resolve()
        active = target
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val want = when (target) {
                    Route.SPEAKER -> listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                    Route.EARPIECE -> listOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
                    Route.BLUETOOTH -> listOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET)
                    else -> listOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET)
                }
                val dev = a.availableCommunicationDevices.firstOrNull { it.type in want }
                if (dev != null) a.setCommunicationDevice(dev)
            } else {
                @Suppress("DEPRECATION")
                when (target) {
                    Route.SPEAKER -> { a.stopBluetoothSco(); a.isBluetoothScoOn = false; a.isSpeakerphoneOn = true }
                    Route.BLUETOOTH -> { a.isSpeakerphoneOn = false; a.startBluetoothSco(); a.isBluetoothScoOn = true }
                    else -> { a.stopBluetoothSco(); a.isBluetoothScoOn = false; a.isSpeakerphoneOn = false }
                }
            }
            if (target == Route.SPEAKER) a.setStreamVolume(AudioManager.STREAM_VOICE_CALL, a.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
        } catch (e: Exception) { AppLog.w(e); }
        if (target == Route.EARPIECE) acquireWake() else releaseWake()
    }

    private fun acquireWake() { try { if (wake?.isHeld == false) wake?.acquire(60 * 60 * 1000L) } catch (e: Exception) { AppLog.w(e); } }
    private fun releaseWake() { try { if (wake?.isHeld == true) wake?.release() } catch (e: Exception) { AppLog.w(e); } }
}
