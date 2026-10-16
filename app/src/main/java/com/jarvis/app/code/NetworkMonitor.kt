package com.jarvis.app.code

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog

/** Process-level network state used by coding mode; callbacks are delivered on the main thread. */
object NetworkMonitor {
    var online by mutableStateOf(true)
        private set
    private val main = Handler(Looper.getMainLooper())
    private val listeners = LinkedHashSet<(Boolean) -> Unit>()
    private var manager: ConnectivityManager? = null
    private var registered = false

    fun init(ctx: Context) {
        if (registered) return
        manager = ctx.applicationContext.getSystemService(ConnectivityManager::class.java)
        online = isConnected()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish(true)
            override fun onLost(network: Network) { if (!isConnected()) publish(false) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { publish(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) }
        }
        try { manager?.registerDefaultNetworkCallback(callback); registered = true }
        catch (e: Exception) { AppLog.w(e) }
    }

    fun addListener(listener: (Boolean) -> Unit) {
        listeners += listener
        listener(online)
    }

    private fun isConnected(): Boolean {
        val c = manager ?: return true
        val n = c.activeNetwork ?: return false
        val caps = c.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun publish(value: Boolean) {
        main.post {
            if (online == value) return@post
            online = value
            AppLog.i("Network", if (value) "Network available" else "Network lost")
            listeners.toList().forEach { it(value) }
        }
    }
}
