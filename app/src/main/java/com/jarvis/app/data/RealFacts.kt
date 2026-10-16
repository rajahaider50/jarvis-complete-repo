package com.jarvis.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The real state of the phone, read from Android at the moment it is needed. It is given to the AI so it never has to guess,
 * and the AI is told to say "I cannot read that" instead of inventing a value.
 */
object RealFacts {
    private var app: Context? = null
    @Volatile private var weather = ""
    @Volatile private var weatherAt = 0L

    fun init(ctx: Context) { app = ctx.applicationContext; refreshWeather() }

    /** Reloads the weather in the background (at most every 20 minutes unless forced). */
    fun refreshWeather(force: Boolean = false) {
        val c = app ?: return
        if (!force && System.currentTimeMillis() - weatherAt < 20 * 60_000L) return
        CoroutineScope(Dispatchers.IO).launch {
            val w = Weather.fetch(c)
            if (w != null) { weather = w; weatherAt = System.currentTimeMillis() }
        }
    }

    fun timeLine(): String {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        return now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", Locale.ENGLISH)) + " (" + ZoneId.systemDefault().id + ")"
    }

    /** One line per fact, read from the phone right now. */
    fun phoneFacts(): String {
        val c = app ?: return "Phone facts are not available."
        val sb = StringBuilder()
        sb.append("Time: ").append(timeLine()).append('\n')
        try { sb.append(DeviceStats.read(c).summary()).append('\n') } catch (e: Exception) { AppLog.w(e); sb.append("Battery/storage/memory: could not be read.\n") }
        try {
            DeviceControl.init(c)
            sb.append("Wi-Fi ${if (DeviceControl.wifiOn(c)) "on" else "off"}, Bluetooth ${if (DeviceControl.bluetoothOn(c)) "on" else "off"}, ")
            sb.append("airplane mode ${if (DeviceControl.airplaneOn(c)) "on" else "off"}, do-not-disturb ${if (DeviceControl.dndOn(c)) "on" else "off"}, ")
            sb.append("media volume ${(DeviceControl.volume(c, android.media.AudioManager.STREAM_MUSIC) * 100).toInt()}%, brightness ${(DeviceControl.brightness(c) * 100).toInt()}%\n")
        } catch (e: Exception) { AppLog.w(e) }
        sb.append("Weather: ").append(if (weather.isNotBlank()) weather else "not available (no location and no internet answer yet)").append('\n')
        return sb.toString().trim()
    }

    /** Added to the AI's instructions so every answer about the phone, the weather or the time is real. */
    fun block(): String = "REAL FACTS READ FROM THIS PHONE NOW (the only source for phone, time and weather answers):\n" + phoneFacts() +
        "\nNever invent or estimate phone status, weather, date or time. If a fact is not listed here, say you cannot read it. "
}
