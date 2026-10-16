package com.jarvis.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.AppLog
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

private val ErrRed = Color(0xFFE0665C)
private val WarnAmber = Color(0xFFE0A93C)

private fun copy(ctx: Context, text: String, what: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("console", text))
    Toast.makeText(ctx, what, Toast.LENGTH_SHORT).show()
}

/** Every error, crash and event of the app, with time, file, line, function and stack. Copy or delete any of them. */
@Composable
fun ConsoleScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var filter by remember { mutableStateOf(0) }            // 0 all, 1 errors, 2 warnings, 3 info
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    val open = remember { mutableStateListOf<Long>() }

    val all = AppLog.entries
    val shown = all.filter {
        when (filter) { 1 -> it.level == AppLog.Level.ERROR; 2 -> it.level == AppLog.Level.WARN; 3 -> it.level == AppLog.Level.INFO; else -> true }
    }.asReversed()

    ScreenScaffold(null, {}) {
        SubHeader("Console", onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All ${all.size}", "Errors ${all.count { it.level == AppLog.Level.ERROR }}", "Handled ${all.count { it.level == AppLog.Level.WARN }}", "Info ${all.count { it.level == AppLog.Level.INFO }}")
                .forEachIndexed { i, label ->
                    val sel = filter == i
                    Box(
                        Modifier.clip(RoundedCornerShape(18.dp))
                            .then(if (sel) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)))
                            .clickable { filter = i }.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { JText(label, 12, if (sel) Bg else Gold, if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton("Copy all", Icons.Outlined.ContentCopy, { copy(ctx, AppLog.textAll(shown), "Copied ${shown.size} entries") }, Modifier.weight(1f))
            OutlineButton(if (selecting) "Done" else "Select", Icons.Outlined.Checklist, { selecting = !selecting; selected.clear() }, Modifier.weight(1f))
            OutlineButton("Clear all", Icons.Outlined.DeleteSweep, { AppLog.clear(); selected.clear() }, Modifier.weight(1f))
        }
        if (selecting) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton("Select all", null, { selected.clear(); selected.addAll(shown.map { it.id }) }, Modifier.weight(1f))
                GoldButton("Delete ${selected.size}", Icons.Outlined.Delete, { AppLog.remove(selected.toList()); selected.clear() }, Modifier.weight(1f))
            }
        }
        if (shown.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { JText("No entries. Nothing went wrong.", 13, Muted) }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { e ->
                    val color = when (e.level) { AppLog.Level.ERROR -> ErrRed; AppLog.Level.WARN -> WarnAmber; else -> Gold }
                    val shape = RoundedCornerShape(16.dp)
                    val isOpen = e.id in open
                    Column(
                        Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, color.copy(alpha = 0.35f), shape)
                            .clickable { if (selecting) { if (e.id in selected) selected.remove(e.id) else selected.add(e.id) } else { if (isOpen) open.remove(e.id) else open.add(e.id) } }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selecting) Icon(if (e.id in selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null, tint = Gold, modifier = Modifier.size(20.dp))
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                JText(e.level.name + if (e.count > 1) " ×${e.count}" else "", 10, color, FontWeight.Medium)
                            }
                            JText(AppLog.time(e), 11, Muted, modifier = Modifier.weight(1f))
                            if (!selecting) Icon(Icons.Outlined.ContentCopy, "Copy", tint = Muted, modifier = Modifier.size(18.dp).clickable { copy(ctx, AppLog.text(e), "Copied") })
                        }
                        JText(e.where, 11, Gold, FontWeight.Medium)
                        JText(e.message, 12, Ivory, lh = 17)
                        if (isOpen && e.stack.isNotBlank()) JText(e.stack, 10, Muted, lh = 14)
                        if (!isOpen && e.stack.isNotBlank() && !selecting) JText("Tap to show details", 10, Muted)
                    }
                }
            }
        }
    }
}
