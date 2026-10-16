package com.jarvis.app.live

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.audio.AudioRouter
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.ChatStore
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.overlay.Overlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the one live call of the whole process.
 *
 * The call is NOT tied to a screen: it keeps running when you press Home (kept alive by [CallService]) and is shown
 * as a floating bubble (see [Overlay]). Screens, the bubble and the notification all talk to the same [client].
 */
object CallHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The running call, or null. Compose reads this, so the UI follows it automatically. */
    var client by mutableStateOf<CallEngine?>(null)
        private set

    val active: Boolean get() = client != null

    /** Starts the call (once). Call only after the microphone permission is granted. */
    fun begin(ctx: Context): CallEngine {
        client?.let { return it }
        val app = ctx.applicationContext
        val provider = CustomProviders.callProvider()
        val c: CallEngine = if (provider != null) PipelineClient(app, scope, provider)
        else LiveClient(app, scope).also { l -> l.onUnavailable = { backup(app, l) } }
        c.onTurn = { user, ai -> ChatStore.addVoiceTurn(user, ai) }     // every spoken turn is saved, so you can continue in chat
        client = c
        AppLog.i("Call", "Call started with " + (provider?.name ?: "Gemini Live"))
        Overlay.hideAll()
        WakeService.setPaused(true)                                      // free the microphone for the call
        AudioRouter.start(app)
        CallService.start(app)
        scope.launch {
            if (!ModelCatalog.ready && Prefs.apiKey.isNotBlank()) ModelCatalog.refresh()
            if (client === c) c.start()
        }
        return c
    }

    /** Gemini Live is unusable (for example no quota): continue the same call with a provider marked "backup". */
    private fun backup(app: Context, failed: LiveClient) {
        if (client !== failed) return
        val p = CustomProviders.list.firstOrNull { it.fallback && CustomProviders.canCall(it) } ?: return
        AppLog.i("Call", "Gemini Live is not available, continuing the call with ${p.name}")
        val n = PipelineClient(app, scope, p)
        n.onTurn = failed.onTurn
        client = n
        failed.stop()
        n.start()
    }

    /**
     * Hangs up and releases everything.
     * @param showPopup show the small "continue chat" card over the current screen (used when the call is ended from the bubble)
     */
    fun end(ctx: Context, showPopup: Boolean) {
        val app = ctx.applicationContext
        val c = client ?: return
        client = null
        AppLog.i("Call", "Live call ended")
        c.flushTurn()
        c.stop()
        ScreenShareService.frameSink = null
        ScreenShareService.stop(app)
        AudioRouter.stop(app)
        CallService.stop(app)
        WakeService.setPaused(false)
        Overlay.hideBubble()
        if (showPopup && Overlay.canDraw(app)) Overlay.showPopup(app)
    }
}
