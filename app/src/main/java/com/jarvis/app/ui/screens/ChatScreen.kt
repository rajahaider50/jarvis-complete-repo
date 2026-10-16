package com.jarvis.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.data.ChatStore
import com.jarvis.app.data.ImageUtil
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Msg
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import com.jarvis.app.audio.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(onNav: (String) -> Unit) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val msgs = ChatStore.messages
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pending = ImageUtil.fromUri(ctx, uri)
    }

    fun send(text: String) {
        val t = text.trim(); val img = pending
        if ((t.isEmpty() && img == null) || ChatStore.busy) return
        pending = null
        scope.launch { ChatStore.ask(t, img, false) }
    }
    LaunchedEffect(msgs.size, ChatStore.busy, ChatStore.streaming.length / 40, ChatStore.thinking.length / 80) { scroll.animateScrollTo(scroll.maxValue) }

    ScreenScaffold("chat", onNav) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).background(goldBrush(), CircleShape), contentAlignment = Alignment.Center) { JText(Prefs.assistantName.take(1).uppercase(), 19, Bg, FontWeight.Medium, serif = true) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                JText(Prefs.assistantName, 18, Ivory, FontWeight.Medium, serif = true)
                Box {
                    Row(Modifier.clickable { menu = true }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Gold, CircleShape))
                        JText(
                            if (Prefs.manualModel.startsWith("custom:")) {
                                val p = CustomProviders.selected()
                                "${p?.name ?: "Custom"} · ${p?.modelId?.ifBlank { "Auto" } ?: "?"}"
                            }
                            else if (Prefs.manualModel.isNotBlank()) Prefs.manualModel
                            else "Auto · ${ChatStore.lastModel.ifBlank { "best model for each task" }}", 11, GoldLight
                        )
                        Icon(Icons.Outlined.ExpandMore, null, tint = GoldLight, modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { JText("Auto (best model for each task)", 13, if (Prefs.manualModel.isBlank()) Gold else Ivory) }, onClick = { Prefs.manualModel = ""; Prefs.save(); menu = false })
                        ModelCatalog.chat.forEach { m ->
                            DropdownMenuItem(text = { JText(m.id, 13, if (Prefs.manualModel == m.id) Gold else Ivory) }, onClick = { Prefs.manualModel = m.id; Prefs.save(); menu = false })
                        }
                        CustomProviders.list.forEach { p ->
                            DropdownMenuItem(text = { JText("★ ${p.name} · Auto (${p.modelIds.size} models)", 13, if (Prefs.manualModel == "custom:${p.id}" && p.modelId.isBlank()) Gold else Ivory) }, onClick = {
                                CustomProviders.upsert(p.copy(modelId = "")); Prefs.manualModel = "custom:${p.id}"; Prefs.save(); menu = false
                            })
                            p.modelIds.forEach { id ->
                                DropdownMenuItem(text = { JText("   ${p.name} · $id", 13, if (Prefs.manualModel == "custom:${p.id}" && p.modelId == id) Gold else Ivory) }, onClick = {
                                    CustomProviders.upsert(p.copy(modelId = id)); Prefs.manualModel = "custom:${p.id}"; Prefs.save(); menu = false
                                })
                            }
                            if (p.modelIds.isEmpty() && p.modelId.isNotBlank()) DropdownMenuItem(text = { JText("★ ${p.name} · ${p.modelId}", 13, if (Prefs.manualModel == "custom:${p.id}") Gold else Ivory) }, onClick = { Prefs.manualModel = "custom:${p.id}"; Prefs.save(); menu = false })
                        }
                    }
                }
            }
            Icon(Icons.Outlined.History, "History", tint = if (showHistory) Gold else Ivory, modifier = Modifier.size(24.dp).clickable { showHistory = !showHistory })
            Icon(Icons.Outlined.Add, "New chat", tint = Ivory, modifier = Modifier.size(26.dp).clickable { ChatStore.newSession(); showHistory = false })
        }
        Hairline()
        if (showHistory) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                JText("CHAT HISTORY", 11, Gold, FontWeight.Medium, ls = 2.2f)
                val fmt = remember { SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()) }
                ChatStore.sessions.toList().forEach { s ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(Surface1)
                            .border(1.dp, Gold.copy(alpha = if (s.id == ChatStore.currentId) 0.7f else 0.14f), shape)
                            .clickable { ChatStore.open(s.id); showHistory = false }.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            JText(s.title, 14, Ivory, FontWeight.Medium); JText(fmt.format(Date(s.ts)), 11, Muted)
                        }
                        Icon(Icons.Outlined.Delete, "Delete", tint = Muted, modifier = Modifier.size(20.dp).clickable { ChatStore.delete(s.id) })
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Bubble(Msg("Good evening, ${Prefs.userName.ifBlank { "there" }}. How may I assist you? You can type, attach a photo, or tap the mic for a live call.", false), 290)
                msgs.forEachIndexed { i, m -> Bubble(m, if (m.user) 260 else 310, i, msgs.lastIndex, scope) { input = it } }
                if (ChatStore.busy) ThinkingCard()
                if (msgs.isEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("What can you do?", "Plan my day", "Tell me a fact").forEach { s ->
                            Box(Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(18.dp)).clickable { send(s) }.padding(horizontal = 12.dp, vertical = 8.dp)) { JText(s, 12, Gold) }
                        }
                    }
                }
            }
        }
        Hairline()
        pending?.let { img ->
            val bmp = remember(img) { BitmapFactory.decodeByteArray(img, 0, img.size) }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                JText("Image attached", 12, Muted, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Close, "Remove", tint = Muted, modifier = Modifier.size(20.dp).clickable { pending = null })
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).background(Surface1, CircleShape).border(1.dp, Gold.copy(alpha = 0.25f), CircleShape)
                .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Image, "Attach photo", tint = Gold, modifier = Modifier.size(20.dp))
            }
            val fieldShape = RoundedCornerShape(26.dp)
            Row(
                Modifier.weight(1f).clip(fieldShape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.22f), fieldShape).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Ivory, fontSize = 14.sp), cursorBrush = SolidColor(Gold), maxLines = 4,
                    decorationBox = { inner -> Box { if (input.isEmpty()) JText("Ask Jarvis anything…", 14, Muted); inner() } }
                )
                Icon(Icons.Outlined.Mic, "Live call", tint = Gold, modifier = Modifier.size(22.dp).clickable { onNav("live") })
            }
            Box(Modifier.size(46.dp).background(goldBrush(), CircleShape).clickable { val t = input; input = ""; send(t) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Send, "Send", tint = Bg, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** The model's live thinking and the answer as it is being written. */
@Composable
private fun ThinkingCard() {
    val inf = rememberInfiniteTransition(label = "think")
    val pulse by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    var secs by remember { mutableStateOf(0) }
    var open by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { secs = ((System.currentTimeMillis() - ChatStore.startedAt) / 1000).toInt(); delay(500) } }
    val writing = ChatStore.streaming.isNotEmpty()
    if (!writing || ChatStore.thinking.isNotBlank()) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), shape).padding(12.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable { open = !open }) {
                Box(Modifier.size(9.dp).background(Gold.copy(alpha = pulse), CircleShape))
                JText(if (writing) "Thought, now writing…" else "Thinking… ${secs}s", 12, Gold, FontWeight.Medium, modifier = Modifier.weight(1f))
                JText(ChatStore.lastModel, 10, Muted)
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
            if (open) {
                if (ChatStore.thinking.isNotBlank()) JText(ChatStore.thinking.trim().takeLast(900), 12, Muted, lh = 17)
                else if (!writing) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.9f, 0.7f, 0.5f).forEach { w -> Box(Modifier.fillMaxWidth(w).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Surface2.copy(alpha = pulse))) }
                }
            }
        }
    }
    if (writing) Bubble(Msg(ChatStore.streaming, false), 310)
}

@Composable
private fun ActionBar(m: Msg, index: Int, last: Int, scope: CoroutineScope, onEdit: (String) -> Unit) {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var feedback by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    fun copy() {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("message", m.text))
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }
    @Composable fun Act(icon: ImageVector, tint: Color = Muted, onClick: () -> Unit) =
        Icon(icon, null, tint = tint, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { onClick() }.padding(7.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Act(Icons.Outlined.ContentCopy) { copy() }
        if (m.user) {
            Act(Icons.Outlined.Edit) { ChatStore.takeBack(index)?.let { onEdit(it.text) } }
            Act(Icons.Outlined.Replay) { val t = ChatStore.takeBack(index); if (t != null) scope.launch { ChatStore.ask(t.text, t.image) } }
        } else {
            Act(if (playing) Icons.Outlined.StopCircle else Icons.Outlined.VolumeUp, if (playing) Gold else Muted) {
                if (playing) { job?.cancel(); playing = false }
                else job = scope.launch { playing = true; try { Speaker.speak(m.text.take(1500)) } finally { playing = false } }
            }
            Act(Icons.Outlined.ThumbUp, if (m.rating > 0) Gold else Muted) { ChatStore.rate(m, if (m.rating > 0) 0 else 1) }
            Act(Icons.Outlined.ThumbDown, if (m.rating < 0) Gold else Muted) { if (m.rating < 0) m.rating = 0 else feedback = true }
            if (index == last) Act(Icons.Outlined.Refresh) { scope.launch { ChatStore.regenerate() } }
        }
    }
    if (feedback) AlertDialog(
        onDismissRequest = { feedback = false },
        confirmButton = { JText("Send", 14, Gold, FontWeight.SemiBold, modifier = Modifier.clickable { ChatStore.rate(m, -1, note); note = ""; feedback = false }.padding(12.dp)) },
        dismissButton = { JText("Cancel", 14, Muted, modifier = Modifier.clickable { feedback = false }.padding(12.dp)) },
        containerColor = Surface1,
        title = { JText("What went wrong?", 16, Ivory, FontWeight.SemiBold) },
        text = { JField(note, { note = it }, "Tell us what was wrong (optional)", Modifier.fillMaxWidth()) }
    )
}

@Composable
private fun Bubble(m: Msg, maxW: Int, index: Int = -1, last: Int = -2, scope: CoroutineScope? = null, onEdit: (String) -> Unit = {}) {
    val user = m.user
    var showThought by remember { mutableStateOf(false) }
    val shape = if (user) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Column(
            Modifier.widthIn(max = maxW.dp).clip(shape)
                .then(if (user) Modifier.background(goldBrush()) else Modifier.background(Surface1).border(1.dp, (if (m.error) Color(0xFFE0665C) else Gold).copy(alpha = if (m.error) 0.5f else 0.14f), shape))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            m.image?.let { img ->
                val bmp = remember(img) { BitmapFactory.decodeByteArray(img, 0, img.size) }
                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            }
            if (m.image == null && m.hadImage) JText("[photo]", 12, if (user) Bg else Muted)
            if (m.text.isNotBlank()) JText(m.text, 14, if (user) Bg else if (m.error) Color(0xFFF0A59E) else Ivory, if (user) FontWeight.Medium else FontWeight.Normal, lh = 21)
            if (!user && !m.error && m.model != null) JText(m.model, 10, Muted)
        }
        if (!m.thought.isNullOrBlank()) {
            JText(if (showThought) "Hide thinking" else "Show thinking", 11, Gold, modifier = Modifier.clickable { showThought = !showThought }.padding(top = 6.dp, start = 4.dp))
            if (showThought) JText(m.thought.trim(), 12, Muted, lh = 17, modifier = Modifier.widthIn(max = maxW.dp).padding(start = 4.dp, top = 4.dp))
        }
        if (scope != null && !m.error && m.text.isNotBlank()) ActionBar(m, index, last, scope, onEdit)
    }
}
