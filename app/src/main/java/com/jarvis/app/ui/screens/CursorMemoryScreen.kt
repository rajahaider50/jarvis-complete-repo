package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.live.ClickMemory
import com.jarvis.app.live.CursorGeometry
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.OutlineButton
import com.jarvis.app.ui.components.ScreenScaffold
import com.jarvis.app.ui.components.SubHeader
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cursor memory: the click lessons JARVIS learned (ui_clicks.json / ui_clicks.md). The user can review, delete
 * one, or clear all. Nothing sensitive (banking / payment / password screens) is ever stored.
 */
@Composable
fun CursorMemoryScreen(onBack: () -> Unit) {
    var tick by remember { mutableStateOf(0) }
    val df = remember { SimpleDateFormat("MMM d HH:mm", Locale.US) }
    val lessons = remember(tick) { ClickMemory.all() }
    val session = remember(tick) { ClickMemory.sessionText() }

    ScreenScaffold(null, {}) {
        SubHeader("Cursor memory", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            JText("Spots JARVIS remembers for tapping. You can delete any line.", 12, Muted)
            JText("Files: memory/ui_clicks.json · memory/ui_clicks.md", 11, Muted)
            if (session.isNotBlank()) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp))
                        .background(Surface2).border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(10.dp)
                ) {
                    JText("This call", 12, GoldLight, FontWeight.Medium)
                    JText(session, 11, Ivory)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton("Refresh", null, { tick++ }, Modifier.weight(1f))
                OutlineButton("Clear all", null, { ClickMemory.clearAll(); tick++ }, Modifier.weight(1f))
            }
            if (lessons.isEmpty()) {
                JText("No lessons yet. They appear here after JARVIS clicks for you.", 12, Muted)
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(lessons) { i, l ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface2)
                                .border(1.dp, Gold.copy(alpha = 0.22f), RoundedCornerShape(14.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                JText(l.target.ifBlank { "(tap)" }, 14, Ivory, FontWeight.Medium)
                                JText("${l.pkg}${if (l.screen.isNotBlank()) " / ${l.screen}" else ""}", 10, Muted)
                                JText("x=${CursorGeometry.fmt(l.x)}  y=${CursorGeometry.fmt(l.y)}   ok=${l.ok} fail=${l.fail}  ${l.source}", 10, GoldLight)
                                JText(df.format(Date(l.last)), 10, Muted)
                            }
                            OutlineButton("Delete", null, { ClickMemory.delete(i); tick++ })
                        }
                    }
                }
            }
        }
    }
}
