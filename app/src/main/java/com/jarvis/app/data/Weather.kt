package com.jarvis.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Today's weather from Open-Meteo (free, no key) using the phone's last known location. */
object Weather {
    private fun lastLocation(ctx: Context): Location? {
        val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (best == null || l.time > best.time)) best = l
            } catch (e: SecurityException) { AppLog.w(e); } catch (e: Exception) { AppLog.w(e); }
        }
        return best
    }

    private fun describe(code: Int) = when (code) {
        0 -> "clear sky"; 1, 2 -> "mostly clear"; 3 -> "overcast"; 45, 48 -> "foggy"
        in 51..57 -> "drizzle"; in 61..67 -> "rain"; in 71..77 -> "snow"; in 80..82 -> "rain showers"
        85, 86 -> "snow showers"; in 95..99 -> "thunderstorm"; else -> "mixed weather"
    }

    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
    }

    private fun forecast(lat: Double, lon: Double, name: String): String {
        val cur = get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code&timezone=auto").getJSONObject("current")
        return "$name: ${describe(cur.getInt("weather_code"))}, ${cur.getDouble("temperature_2m").toInt()}°C (feels like ${cur.getDouble("apparent_temperature").toInt()}°C), " +
            "humidity ${cur.getInt("relative_humidity_2m")}%, wind ${cur.getDouble("wind_speed_10m").toInt()} km/h"
    }

    /** Weather for a city name (used when you ask "what is the weather in ..."). */
    suspend fun fetchCity(city: String): String? = withContext(Dispatchers.IO) {
        try {
            val place = get("https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + java.net.URLEncoder.encode(city, "UTF-8")).optJSONArray("results")?.optJSONObject(0)
                ?: return@withContext "I could not find a place called $city."
            forecast(place.getDouble("latitude"), place.getDouble("longitude"), place.optString("name", city))
        } catch (e: Exception) { AppLog.w(e); null }
    }

    /** Weather where you are: GPS/network location when allowed, otherwise the place your internet connection points to. */
    suspend fun fetch(ctx: Context): String? = withContext(Dispatchers.IO) {
        try {
            val loc = lastLocation(ctx)
            if (loc != null) return@withContext forecast(loc.latitude, loc.longitude, "Your location")
        } catch (e: Exception) { AppLog.w(e) }
        try {
            val ip = get("https://ipwho.is/")
            if (ip.optBoolean("success", false)) return@withContext forecast(ip.getDouble("latitude"), ip.getDouble("longitude"), ip.optString("city", "Your area") + " (approximate)")
        } catch (e: Exception) { AppLog.w(e) }
        null
    }
}
