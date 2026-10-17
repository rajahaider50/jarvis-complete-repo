package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.ModelRouter
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RouterWarn = Color(0xFFE0665C)

/** Shows the smart router's real local health: what it used today, what is cooling down and how fast models answered. */
@Composable
fun ColumnScope.RouterLogScreen() {
    var tick by remember { mutableStateOf(0) }
    val rows = remember(tick) { ModelRouter.snapshot() }
    val now = remember(tick) { System.currentTimeMillis() }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JText("This is real local data the smart router keeps: tokens used today per model, cooldowns after errors or limits, and average speed. Nothing here is invented.", 12, Muted, lh = 17)
        if (rows.isEmpty()) JText("No provider model has been used yet. Pick a custom provider in Chat and send a message.", 13, Muted, lh = 18)
        rows.forEach { r ->
            val card = RoundedCornerShape(16.dp)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JText(r.modelId, 13, Ivory, FontWeight.Medium)
                maybeProvider(r.providerId)
                val cooling = r.cooldownUntil > now
                val line = buildString {
                    append("used ").append(r.usedTokens).append(" tokens today · ok ").append(r.successes).append(" · failed ").append(r.failures)
                    if (r.latencyMs > 0) append(" · ~").append(r.latencyMs).append(" ms")
                    if (cooling) append(" · cooling until ").append(fmt.format(Date(r.cooldownUntil)))
                }
                JText(line, 11, if (cooling) RouterWarn else Muted, lh = 15)
            }
        }
        OutlineButton("Refresh", Icons.Outlined.Refresh, { tick++ })
        OutlineButton("Clear router log", Icons.Outlined.Delete, { ModelRouter.clearAll(); tick++ })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun maybeProvider(id: String) { if (id.isNotBlank()) JText(id, 10, Muted) }
