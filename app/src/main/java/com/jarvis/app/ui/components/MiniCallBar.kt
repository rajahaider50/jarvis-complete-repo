package com.jarvis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.CallEngine
import com.jarvis.app.live.CallHost
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Surface2

/**
 * A small "call is running" bar shown on every screen except the call itself. Tapping it returns to the call;
 * the red button ends it. This is what lets the call keep going in the app while you use other screens.
 */
@Composable
fun MiniCallBar(onOpen: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val live = CallHost.client ?: return
    val mode = when {
        live.state != CallEngine.State.LIVE -> 2
        live.speaking -> 3
        else -> 1
    }
    val label = when (live.state) {
        CallEngine.State.CONNECTING -> "Connecting…"
        CallEngine.State.ERROR -> "Call stopped"
        CallEngine.State.IDLE -> "Starting…"
        CallEngine.State.LIVE -> if (live.muted) "Muted" else if (live.speaking) "Speaking…" else "Listening…"
    }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp)).background(Surface2)
            .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable { onOpen() }.padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SiriOrb(30.dp, if (live.speaking) live.outLevel else live.micLevel, mode)
        Column(Modifier.weight(1f)) {
            JText("${Prefs.assistantName} · live call", 12, Ivory, FontWeight.Medium)
            JText(label, 11, GoldLight)
        }
        Icon(Icons.Outlined.CallEnd, "End call", tint = Color(0xFFE0665C), modifier = Modifier.size(22.dp).clickable { onEnd() })
    }
}
