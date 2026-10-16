package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.audio.PcmPlayer
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.CustomProvider
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.GeminiClient
import com.jarvis.app.data.OpenAiCompat
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.ProviderModel
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.launch

/** Add models from any other AI provider. Anything that offers an OpenAI-compatible API works. */
@Composable
fun ProvidersScreen(onBack: () -> Unit) {
    var editing by remember { mutableStateOf<CustomProvider?>(null) }
    ScreenScaffold(null, {}) {
        SubHeader(if (editing == null) "Custom providers" else "Provider", if (editing == null) onBack else ({ editing = null }))
        val e = editing
        if (e == null) ProviderList(onEdit = { editing = it }) else ProviderEditor(e, onDone = { editing = null })
    }
}

@Composable
private fun ColumnScope.ProviderList(onEdit: (CustomProvider) -> Unit) {
    val card = RoundedCornerShape(18.dp)
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JText("Add a model from any other AI website. Pick it from the model menu in Chat, or mark it as a backup that is used automatically when Gemini fails. Voice and live calls always use Gemini.", 12, Muted, lh = 17)
        CustomProviders.list.forEach { p ->
            val sel = Prefs.manualModel == "custom:${p.id}"
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(if (sel) 2.dp else 1.dp, Gold.copy(alpha = if (sel) 0.9f else 0.16f), card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JText(p.name, 16, Ivory, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (p.fallback) JText("backup", 10, Gold)
                    if (sel) JText("in use", 10, Gold)
                }
                val visible = CustomProviders.eligibleModels(p)
                val unknown = CustomProviders.unverifiedModels(p)
                JText(if (p.modelId.isBlank()) "AUTO · ${visible.size} eligible models" else p.modelId, 13, GoldLight)
                JText(p.baseUrl, 11, Muted)
                if (p.models.isNotEmpty() || p.modelIds.isNotEmpty()) {
                    JText("${if (p.freeOnly) "FREE MODELS" else "MODELS"} · tap Auto or one model", 10, Gold, FontWeight.Medium, ls = 1.5f)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (p.modelId.isBlank()) Gold.copy(alpha = 0.22f) else Surface2)
                            .border(1.dp, Gold.copy(alpha = if (p.modelId.isBlank()) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                            .clickable { CustomProviders.upsert(p.copy(modelId = "")); if (Prefs.manualModel != "custom:${p.id}") { Prefs.manualModel = "custom:${p.id}"; Prefs.save() } }
                            .padding(horizontal = 12.dp, vertical = 7.dp)) { JText("Auto", 12, if (p.modelId.isBlank()) GoldLight else Ivory) }
                        visible.take(80).forEach { m ->
                            val id = m.id
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (p.modelId == id) Gold.copy(alpha = 0.22f) else Surface2)
                                .border(1.dp, Gold.copy(alpha = if (p.modelId == id) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                                .clickable { CustomProviders.upsert(p.copy(modelId = id)); Prefs.manualModel = "custom:${p.id}"; Prefs.save() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)) { JText(id, 12, if (p.modelId == id) GoldLight else Ivory) }
                        }
                    }
                    if (unknown.isNotEmpty()) JText("UNVERIFIED · ${unknown.size} hidden by Free only", 10, Muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton(if (sel) "Use Gemini Auto" else "Use in chat", null, { Prefs.manualModel = if (sel) "" else "custom:${p.id}"; Prefs.save() }, Modifier.weight(1f))
                    OutlineButton("Edit", Icons.Outlined.Edit, { onEdit(p) }, Modifier.weight(1f))
                }
                val sp = CustomProviders.speechOf(p)
                if (sp.ttsModel.isNotBlank() && sp.voiceList().isNotEmpty()) {
                    JText("VOICES · tap one to hear it", 10, Gold, FontWeight.Medium, ls = 1.5f)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sp.voiceList().forEach { v ->
                            val on = (sp.ttsVoice.ifBlank { sp.voiceList().first() }) == v
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) Gold.copy(alpha = 0.22f) else Surface2).border(1.dp, Gold.copy(alpha = if (on) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                                .clickable {
                                    val chosen = sp.copy(ttsVoice = v)
                                    CustomProviders.upsert(chosen); note = ""
                                    scope.launch {
                                        try { PcmPlayer.play(OpenAiCompat.speech(chosen, "Hello, this is my voice.")) }
                                        catch (e: Exception) { AppLog.w(e); note = e.message ?: "The voice could not be played" }
                                    }
                                }.padding(horizontal = 12.dp, vertical = 7.dp)) { JText(v, 12, if (on) GoldLight else Ivory) }
                        }
                    }
                }
                val calling = Prefs.callProvider == p.id
                OutlineButton(if (calling) "Voice calls use this provider · tap for Gemini Live" else "Use for voice calls", Icons.Outlined.Call,
                    { Prefs.callProvider = if (calling) "" else p.id; Prefs.save() }, Modifier.fillMaxWidth())
                if (calling && !CustomProviders.canCall(p)) JText("It needs a speech-to-text model (edit the provider) or a Gemini key to hear you.", 11, Color(0xFFE0665C), lh = 15)
            }
        }
        if (note.isNotEmpty()) JText(note, 12, Color(0xFFE0665C), lh = 17)
        GoldButton("Add provider", Icons.Outlined.Add, { onEdit(CustomProvider(System.currentTimeMillis().toString(), "", "https://", "", "")) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnScope.ProviderEditor(start: CustomProvider, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(start.name) }
    var url by remember { mutableStateOf(start.baseUrl) }
    var key by remember { mutableStateOf(start.apiKey) }
    var model by remember { mutableStateOf(start.modelId) }
    var headers by remember { mutableStateOf(start.headers) }
    var system by remember { mutableStateOf(start.systemPrompt) }
    var temp by remember { mutableStateOf(start.temperature) }
    var vision by remember { mutableStateOf(start.vision) }
    var backup by remember { mutableStateOf(start.fallback) }
    var showKey by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf(start.tools) }
    var stt by remember { mutableStateOf(start.sttModel) }
    var ttsM by remember { mutableStateOf(start.ttsModel) }
    var voices by remember { mutableStateOf(start.ttsVoices) }
    var voice by remember { mutableStateOf(start.ttsVoice) }
    var borrow by remember { mutableStateOf(start.speechFrom) }
    var result by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(start.models.ifEmpty { start.modelIds.map { ProviderModel(it, source = "cache") } }) }
    var freeOnly by remember { mutableStateOf(start.freeOnly) }
    var manualId by remember { mutableStateOf("") }
    var manualContext by remember { mutableStateOf("") }
    var manualTools by remember { mutableStateOf(true) }
    var manualFree by remember { mutableStateOf(true) }
    var manualTier by remember { mutableStateOf("") }
    var dailyLimit by remember { mutableStateOf(if (start.dailyLimitTokens > 0) start.dailyLimitTokens.toString() else "") }
    val exists = CustomProviders.list.any { it.id == start.id }

    fun current(models: List<ProviderModel> = found) = CustomProvider(start.id, name.trim().ifBlank { "My provider" }, url.trim(), key.trim(), model.trim(), headers, system, temp, vision, backup, tools, stt.trim(), ttsM.trim(), voices, voice, borrow, models.map { it.id }, freeOnly, models, dailyLimit.toLongOrNull()?.coerceAtLeast(0L) ?: 0L)

    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JText("START FROM", 11, Gold, FontWeight.Medium, ls = 2.2f)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomProviders.presets.forEach { pr ->
                Box(Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable {
                        name = if (pr.name == "Other") name else pr.name; url = pr.baseUrl; if (pr.model.isNotBlank()) model = pr.model
                        if (pr.stt.isNotBlank()) { stt = pr.stt; ttsM = pr.tts; voices = pr.voices; voice = pr.voices.substringBefore(',').trim() }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)) { JText(pr.name, 12, Gold) }
            }
        }
        Field("NAME", name, { name = it }, "e.g. OpenRouter")
        Field("BASE URL", url, { url = it }, "https://api.example.com/v1")
        JText("The address before /chat/completions. Paste the full address too, it is cleaned automatically.", 11, Muted, lh = 15)
        JText("API KEY", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(key, { key = it }, "Key (leave empty for a local server)", Modifier.fillMaxWidth(), secret = !showKey, trailing = {
            Icon(Icons.Outlined.Visibility, "Show", tint = if (showKey) Gold else Muted, modifier = Modifier.size(20.dp).clickable { showKey = !showKey })
        })
        Field("MODEL ID", model, { model = it }, "e.g. gpt-4o-mini")
        SwitchRow("Free models only (unknown prices stay unverified)", freeOnly) { freeOnly = it }
        Field("DAILY TOKEN LIMIT (OPTIONAL)", dailyLimit, { dailyLimit = it.filter(Char::isDigit) }, "0 = unlimited")
        OutlineButton(if (busy) "Please wait…" else "Fetch the model list from this server", Icons.Outlined.Refresh, {
            if (!busy) scope.launch {
                busy = true; result = ""; ok = null
                try { found = OpenAiCompat.fetchModelCatalog(current()); if (model !in found.map { it.id }) model = ""; result = "${found.size} models found. Free: ${found.count { it.isFree == true }} · Unverified: ${found.count { it.isFree == null }}"; ok = true }
                catch (e: Exception) { AppLog.w(e); result = e.message ?: "Could not read the list"; ok = false }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        if (found.isNotEmpty()) {
            val shown = found.filter { !freeOnly || it.isFree == true }
            JText("FREE · ${found.count { it.isFree == true }}", 10, Gold, FontWeight.Medium, ls = 1.5f)
            ModelChips(shown, model) { model = it }
            val unverified = found.filter { it.isFree == null }
            if (unverified.isNotEmpty()) {
                JText("UNVERIFIED · ${unverified.size} (shown when Free only is off)", 10, Muted, FontWeight.Medium, ls = 1.2f)
                if (!freeOnly) ModelChips(unverified, model) { model = it }
            }
        }
        JText("MANUAL MODEL OVERRIDE", 11, Gold, FontWeight.Medium, ls = 2.2f)
        Field("MODEL ID", manualId, { manualId = it }, "provider/model-id")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("CONTEXT", manualContext, { manualContext = it }, "tokens", Modifier.weight(1f))
            Field("QUALITY 1-5", manualTier, { manualTier = it }, "unknown", Modifier.weight(1f))
        }
        SwitchRow("Manual model is free", manualFree) { manualFree = it }
        SwitchRow("Manual model supports tools", manualTools) { manualTools = it }
        OutlineButton("Add manual model", Icons.Outlined.Add, {
            if (manualId.isNotBlank()) {
                found = (found.filterNot { it.id == manualId.trim() } + ProviderModel(manualId.trim(), manualFree, manualContext.toIntOrNull(), manualTools, manualTier.toIntOrNull()?.coerceIn(1, 5), "manual", System.currentTimeMillis())).sortedBy { it.id }
                model = manualId.trim(); manualId = ""
            }
        }, Modifier.fillMaxWidth())
        JText("VOICE CALLS", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JText("To call this model by voice it must hear you and speak. Fill the provider's own speech models, or borrow them from another provider. Without a speech-to-text model Gemini is used to hear you; without a voice model Gemini's voice speaks.", 11, Muted, lh = 15)
        Field("SPEECH-TO-TEXT MODEL", stt, { stt = it }, "whisper-1  /  whisper-large-v3-turbo")
        Field("TEXT-TO-SPEECH MODEL", ttsM, { ttsM = it }, "tts-1  /  playai-tts")
        Field("VOICES (comma separated)", voices, { voices = it }, "alloy, echo, nova, onyx")
        val voiceChoices = voices.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (voiceChoices.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            voiceChoices.forEach { v ->
                val on = (voice.ifBlank { voiceChoices.first() }) == v
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) Gold.copy(alpha = 0.22f) else Surface1).border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { voice = v }.padding(horizontal = 12.dp, vertical = 7.dp)) { JText(v, 12, if (on) GoldLight else Ivory) }
            }
        }
        val others = CustomProviders.list.filter { it.id != start.id }
        if (others.isNotEmpty()) {
            JText("BORROW SPEECH FROM", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("" to "This provider") + others.map { it.id to it.name }).forEach { (id, label) ->
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (borrow == id) Gold.copy(alpha = 0.22f) else Surface1).border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { borrow = id }.padding(horizontal = 12.dp, vertical = 7.dp)) { JText(label, 12, if (borrow == id) GoldLight else Ivory) }
                }
            }
        }
        SwitchRow("Can use phone tools during calls", tools) { tools = it }
        if (ttsM.isNotBlank()) OutlineButton(if (busy) "Please wait…" else "Test the voice", Icons.Outlined.VolumeUp, {
            if (!busy) scope.launch {
                busy = true; result = ""; ok = null
                try { PcmPlayer.play(OpenAiCompat.speech(CustomProviders.speechOf(current()), "Hello, this is my voice.")); result = "Voice works."; ok = true }
                catch (e: Exception) { AppLog.w(e); result = e.message ?: "Voice failed"; ok = false }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        JText("OPTIONAL", 11, Gold, FontWeight.Medium, ls = 2.2f)
        Field("EXTRA HEADERS", headers, { headers = it }, "HTTP-Referer: https://myapp.com", singleLine = false)
        Field("SYSTEM PROMPT (replaces the default)", system, { system = it }, "You are a helpful assistant.", singleLine = false)
        Field("TEMPERATURE", temp, { temp = it }, "0.7")
        SwitchRow("This model understands photos", vision) { vision = it }
        SwitchRow("Use as backup when Gemini fails", backup) { backup = it }

        if (result.isNotEmpty()) {
            val c = when (ok) { true -> Gold; false -> Color(0xFFE0665C); else -> Muted }
            JText(result, 12, c, lh = 17)
        }
        OutlineButton(if (busy) "Testing…" else "Test connection", Icons.Outlined.NetworkCheck, {
            if (!busy) scope.launch {
                busy = true; result = ""; ok = null
                try {
                    val r = OpenAiCompat.chat(current(), "You are a test. Answer very briefly.", listOf(GeminiClient.Turn("user", "Reply with the word OK.", null)))
                    result = "Works. The model answered: ${r.take(120)}"; ok = true
                } catch (e: Exception) { AppLog.w(e); result = e.message ?: "Failed"; ok = false }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        GoldButton(if (busy) "Saving…" else "Save", Icons.Outlined.Check, {
            if (!busy && url.isNotBlank()) scope.launch {
                busy = true; result = ""; ok = null
                val base = current()
                try {
                    found = OpenAiCompat.fetchModelCatalog(base)
                    if (model.isNotBlank() && model !in found.map { it.id }) model = ""
                    val saved = current(found)
                    if (saved.modelId.isBlank() && found.isEmpty()) throw IllegalStateException("No models returned. Add a manual Model ID or check the server.")
                    CustomProviders.upsert(saved); result = "Saved. ${found.size} models cached for this provider."; ok = true; onDone()
                } catch (e: Exception) {
                    AppLog.w(e); CustomProviders.upsert(base); result = e.message ?: "Saved, but the model list could not be read."; ok = false
                }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        if (exists) OutlineButton("Delete this provider", Icons.Outlined.Delete, { CustomProviders.delete(start.id); onDone() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModelChips(models: List<ProviderModel>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        models.take(80).forEach { m ->
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (m.id == selected) Gold.copy(alpha = 0.22f) else Surface1)
                .border(1.dp, Gold.copy(alpha = if (m.id == selected) 0.8f else 0.3f), RoundedCornerShape(16.dp))
                .clickable { onSelect(m.id) }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                JText(m.id, 12, if (m.id == selected) GoldLight else Ivory)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JText(label, 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(value, onChange, hint, modifier.fillMaxWidth(), singleLine = singleLine, minHeight = if (singleLine) 0.dp else 72.dp)
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), shape).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        JText(label, 14, Ivory, modifier = Modifier.weight(1f))
        JToggle(on) { onChange(it) }
    }
}
