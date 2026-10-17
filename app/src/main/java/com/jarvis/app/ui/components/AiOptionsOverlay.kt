package com.jarvis.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.live.AiPanel
import com.jarvis.app.ui.theme.Bg
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface2

/**
 * The option popups the assistant shows during a call. Five animated cards; the user answers by voice
 * ("aur dikhao", "teesra") or by tapping one.
 */
@Composable
fun AiOptionsOverlay(modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = AiPanel.visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 6 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 6 },
        modifier = modifier
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { AiPanel.close() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Bg)
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    JText(AiPanel.title, 12, Gold, FontWeight.Medium, ls = 1.6f, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Outlined.Close, "Close",
                        tint = Muted, modifier = Modifier.size(20.dp).clickable { AiPanel.close() }
                    )
                }
                AiPanel.options.forEachIndexed { i, opt ->
                    var sel = AiPanel.selectedId == opt.id
                    val t = (i * 45).coerceAtMost(260)
                    val alpha by animateFloatAsState(1f, tween(220, delayMillis = t), label = "aiopt")
                    Row(
                        Modifier.fillMaxWidth().alpha(alpha)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) Gold.copy(alpha = 0.18f) else Surface2)
                            .border(1.dp, Gold.copy(alpha = if (sel) 0.8f else 0.16f), RoundedCornerShape(16.dp))
                            .clickable { AiPanel.select(AiPanel.category, opt.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(Gold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {                         JText("${i + 1}", 12, GoldLight, FontWeight.Medium) }
                        Text(opt.label, color = Ivory, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
