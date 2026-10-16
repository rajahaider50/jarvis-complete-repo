package com.jarvis.app.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.ChatStore
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.CallEngine
import com.jarvis.app.live.CallHost
import com.jarvis.app.live.LiveClient
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

private val Danger = Color(0xFFC4483F)

/** The floating call bubble: your own orb, draggable anywhere. Tap it for mute / open / end. */
@Composable
fun BubbleUi(
    expanded: Boolean, onRight: Boolean,
    onToggle: () -> Unit, onMute: () -> Unit, onOpen: () -> Unit, onEnd: () -> Unit
) {
    val live = CallHost.client
    val level = when { live == null -> 0f; live.speaking -> live.outLevel; else -> live.micLevel }
    val mode = when { live == null || live.state != CallEngine.State.LIVE -> 2; live.speaking -> 3; else -> 1 }
    val muted = live?.muted == true
    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (expanded && onRight) Controls(muted, onMute, onOpen, onEnd)
        Box(
            Modifier.size(68.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Bg)
                .border(1.dp, Gold.copy(alpha = 0.55f), CircleShape).clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) { SiriOrb(62.dp, level, mode) }
        if (expanded && !onRight) Controls(muted, onMute, onOpen, onEnd)
    }
}

@Composable
private fun Controls(muted: Boolean, onMute: () -> Unit, onOpen: () -> Unit, onEnd: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.shadow(8.dp, shape).clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.4f), shape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RoundIcon(if (muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, if (muted) Gold.copy(alpha = 0.3f) else Surface2, onMute)
        RoundIcon(Icons.Outlined.OpenInNew, Surface2, onOpen)
        RoundIcon(Icons.Outlined.Close, Danger, onEnd)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, bg: Color, onClick: () -> Unit, size: Int = 42) {
    Box(Modifier.size(size.dp).background(bg, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Ivory, modifier = Modifier.size(20.dp))
    }
}

/** The "drop here to end the call" target that appears while the bubble is being dragged. */
@Composable
fun CloseZoneUi(visible: Boolean, hot: Boolean) {
    val scale by animateFloatAsState(if (hot) 1.3f else 1f, label = "zone")
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        if (visible) {
            Box(
                Modifier.size(62.dp).scale(scale).clip(CircleShape)
                    .background(if (hot) Danger else Surface1.copy(alpha = 0.92f))
                    .border(1.dp, Gold.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Close, null, tint = Ivory, modifier = Modifier.size(28.dp)) }
        }
    }
}

/** Small chat card shown after the call ends. Drag it up to open the full app. */
@Composable
fun PopupUi(onContinue: () -> Unit, onCall: () -> Unit, onClose: () -> Unit) {
    val last = ChatStore.messages.lastOrNull { !it.user && !it.error }?.text
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().shadow(14.dp, shape).clip(shape).background(Bg)
            .border(1.dp, Gold.copy(alpha = 0.45f), shape).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Muted.copy(alpha = 0.5f)))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SiriOrb(44.dp, 0.1f, 0)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                JText(Prefs.assistantName, 15, Ivory, FontWeight.Medium, serif = true)
                JText("Call ended", 11, Muted)
            }
            RoundIcon(Icons.Outlined.Close, Surface2, onClose, 34)
        }
        if (!last.isNullOrBlank()) {
            JText(last.trim(), 13, Ivory, lh = 19, modifier = Modifier.fillMaxWidth().heightIn(max = 84.dp).verticalScroll(rememberScrollState()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GoldButton("Continue chat", Icons.Outlined.ChatBubbleOutline, onContinue, Modifier.weight(1f))
            RoundIcon(Icons.Outlined.Mic, Surface2, onCall, 48)
        }
        JText("Drag up to open the full app", 10, Muted)
    }
}
