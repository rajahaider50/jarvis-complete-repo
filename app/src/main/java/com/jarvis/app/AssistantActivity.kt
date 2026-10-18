package com.jarvis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jarvis.app.audio.AudioRouter
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.CallEngine
import com.jarvis.app.live.LiveClient
import com.jarvis.app.live.WakeService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

/** Floating Siri-style assistant that opens over whatever is on screen (started by the wake phrase). */
class AssistantActivity : ComponentActivity() {
    companion object { @Volatile var alive = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alive = true
        Prefs.init(this)
        WakeService.setPaused(true)
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(89)
        setContent { JarvisTheme { Overlay(onDone = { finish() }, onOpenApp = {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish()
        }) } }
    }

    override fun onDestroy() {
        alive = false
        // Round 3 (§4): WakeService.setPaused(false) itself refuses to resume while a live call (or the app's own
        // recorder) owns the mic — otherwise the wake engine would open a second AudioRecord and silence the call.
        WakeService.setPaused(false)
        super.onDestroy()
    }
}

@Composable
private fun Overlay(onDone: () -> Unit, onOpenApp: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val live = remember { LiveClient(ctx, scope) }
    val micOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        if (!micOk) { onOpenApp(); return@LaunchedEffect }
        AudioRouter.start(ctx)
        if (!ModelCatalog.ready) ModelCatalog.refresh()
        live.start()
    }
    DisposableEffect(Unit) { onDispose { live.stop(); AudioRouter.stop(ctx) } }

    val mode = when { live.state != CallEngine.State.LIVE -> 2; live.speaking -> 3; else -> 1 }
    val level = if (live.speaking) live.outLevel else live.micLevel

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() }) {
        val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(shape).background(Bg).border(1.dp, Gold.copy(alpha = 0.4f), shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Muted.copy(alpha = 0.5f)))
            SiriOrb(190.dp, level, mode)
            VoiceWave(level, live.state == CallEngine.State.LIVE)
            JText(
                when (live.state) { CallEngine.State.CONNECTING -> "Connecting…"; CallEngine.State.ERROR -> "Stopped"; CallEngine.State.IDLE -> "Starting…"
                    else -> if (live.speaking) "Speaking…" else "Listening…" }, 18, Ivory, FontWeight.Medium, serif = true
            )
            Column(Modifier.fillMaxWidth().heightIn(max = 90.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                if (live.userText.isNotBlank()) JText("“${live.userText.trim()}”", 13, GoldLight, align = TextAlign.Center, lh = 19)
                if (live.aiText.isNotBlank()) JText(live.aiText.trim(), 13, Ivory, align = TextAlign.Center, lh = 19)
                live.error?.let { JText(it, 12, Color(0xFFE0665C), align = TextAlign.Center, lh = 17) }
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Circle(if (live.muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, 48) { live.toggleMute() }
                Circle(Icons.Outlined.OpenInNew, 48) { onOpenApp() }
                Circle(Icons.Outlined.Close, 56, Color(0xFFC4483F)) { onDone() }
            }
            JText(Prefs.assistantName, 11, Muted)
        }
    }
}

@Composable
private fun Circle(icon: ImageVector, size: Int, bg: Color = Surface1, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).background(bg, CircleShape).border(1.dp, Gold.copy(alpha = 0.25f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Ivory, modifier = Modifier.size(22.dp))
    }
}
