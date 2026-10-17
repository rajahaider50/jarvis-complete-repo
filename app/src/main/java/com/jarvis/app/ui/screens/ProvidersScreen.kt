package com.jarvis.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.audio.PcmPlayer
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.CustomProvider
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.GeminiClient
import com.jarvis.app.data.ModelCatalogFile
import com.jarvis.app.data.OpenAiCompat
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.ProbeService
import com.jarvis.app.data.ProviderModel
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var importMsg by remember { mutableStateOf("") }
    var importOk by remember { mutableStateOf<Boolean?>(null) }
    var catUrl by remember { mutableStateOf(Prefs.catalogUrl) }
    var catMsg by remember { mutableStateOf("") }
    var catOk by remember { mutableStateOf<Boolean?>(null) }
    var catBusy by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importMsg = ""; importOk = null
            try {
                val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "" }
                val s = CustomProviders.importJson(text)
                importMsg = "${s.count} models imported from ${s.providerName}."; importOk = true
            } catch (e: Exception) { AppLog.w(e); importMsg = e.message ?: "Could not import that file"; importOk = false }
        }
    }
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
                JText(p.modelId, 13, GoldLight)
                JText(p.baseUrl, 11, Muted)
                val ms = CustomProviders.modelsOf(p)
                JText("${ms.size} model${if (ms.size == 1) "" else "s"} known · ${if (p.freeOnly) "free only" else "all models"}", 11, Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton(if (sel) "Use Auto" else "Use in chat", null, { Prefs.manualModel = if (sel) "" else "custom:${p.id}"; Prefs.save() }, Modifier.weight(1f))
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
        if (importMsg.isNotEmpty()) JText(importMsg, 12, if (importOk == true) Gold else Color(0xFFE0665C), lh = 17)
        OutlineButton("Import models from .json", Icons.Outlined.FileOpen, {
            importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
        }, Modifier.fillMaxWidth())
        JText("Pick a provider .json file (with baseURL, apiKey and a models list). Every model in it is added at once, ready to use in Chat.", 11, Muted, lh = 15)
        JText("MODEL CATALOG", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JText("Offline facts (context size, tool support) for well-known models. Currently ${ModelCatalogFile.count()} models from the ${ModelCatalogFile.source} catalog v${ModelCatalogFile.version}. Paste a URL to a newer model_catalog.json to update it.", 11, Muted, lh = 15)
        JField(catUrl, { catUrl = it }, "https://example.com/model_catalog.json (optional)", Modifier.fillMaxWidth())
        OutlineButton(if (catBusy) "Updating…" else "Save URL and update now", Icons.Outlined.Download, {
            if (!catBusy) scope.launch {
                catBusy = true; catMsg = ""; catOk = null
                Prefs.catalogUrl = catUrl.trim(); Prefs.save()
                if (catUrl.isBlank()) { catMsg = "URL cleared. The bundled catalog stays in use."; catOk = true }
                else try { val n = ModelCatalogFile.update(ctx, catUrl); catMsg = "Catalog updated: $n models (v${ModelCatalogFile.version})."; catOk = true }
                catch (e: Exception) { AppLog.w(e); catMsg = e.message ?: "Could not update the catalog"; catOk = false }
                catBusy = false
            }
        }, Modifier.fillMaxWidth())
        if (catMsg.isNotEmpty()) JText(catMsg, 12, if (catOk == true) Gold else Color(0xFFE0665C), lh = 17)
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
    var modelsList by remember { mutableStateOf(start.models) }
    var freeOnly by remember { mutableStateOf(start.freeOnly) }
    var mId by remember { mutableStateOf("") }
    var mCtx by remember { mutableStateOf("") }
    var mTier by remember { mutableStateOf("") }
    var mTools by remember { mutableStateOf<Boolean?>(null) }
    var mFree by remember { mutableStateOf<Boolean?>(null) }
    var probing by remember { mutableStateOf("") }
    val exists = CustomProviders.list.any { it.id == start.id }

    fun current() = CustomProvider(start.id, name.trim().ifBlank { "My provider" }, url.trim(), key.trim(), model.trim(), headers, system, temp, vision, backup, tools, stt.trim(), ttsM.trim(), voices, voice, borrow, modelsList, freeOnly = freeOnly)

    fun markVerified(id: String, toolsResult: Boolean?) {
        modelsList = modelsList.map { if (it.id == id) it.copy(source = "probe", supportsTools = toolsResult ?: it.supportsTools, lastVerifiedAt = System.currentTimeMillis()) else it }
    }

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
        OutlineButton(if (busy) "Please wait…" else "Fetch the model list from this server", Icons.Outlined.Refresh, {
            if (!busy) scope.launch {
                busy = true; result = ""; ok = null
                try {
                    modelsList = OpenAiCompat.fetchModelDetails(current())
                    val free = modelsList.count { it.isFree == true }
                    result = "${modelsList.size} models found · $free free. Tap one to use it."; ok = true
                    if (url.isNotBlank()) CustomProviders.upsert(current())
                } catch (e: Exception) { AppLog.w(e); result = e.message ?: "Could not read the list"; ok = false }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        SwitchRow("Free models only (recommended)", freeOnly) { freeOnly = it }
        JText(if (freeOnly) "Only models the server reports as free (price 0) are used. Unverified models stay listed but are held back." else "Every model may be used, including paid ones.", 11, Muted, lh = 15)
        if (modelsList.isNotEmpty()) {
            JText("MODELS · TAP TO USE, ⓘ TO TEST", 11, Gold, FontWeight.Medium, ls = 2.2f)
            listOf("Free" to modelsList.filter { it.isFree == true }, "Paid" to modelsList.filter { it.isFree == false }, "Unverified" to modelsList.filter { it.isFree == null }).forEach { (group, items) ->
                if (items.isNotEmpty()) {
                    JText("$group · ${items.size}", 11, Muted, FontWeight.Medium)
                    items.forEach { m ->
                        ModelRow(m, m.id == model, probing == m.id, onClick = { model = m.id }, onProbe = {
                            if (probing.isBlank() && url.isNotBlank()) scope.launch {
                                probing = m.id; result = ""; ok = null
                                val r = ProbeService.probe(current(), m.id, testTools = true)
                                markVerified(m.id, r.tools)
                                CustomProviders.upsert(current())
                                result = if (r.ok) "Verified in ${r.latencyMs} ms" + (r.tools?.let { t -> if (t) " · supports tools" else " · no tool calling" } ?: "") + (if (r.reply.isNotBlank()) " · \"${r.reply}\"" else "")
                                else "Test failed: ${r.error}"
                                ok = r.ok; probing = ""
                            }
                        })
                    }
                }
            }
        }
        JText("ADD A MODEL BY HAND", 11, Gold, FontWeight.Medium, ls = 2.2f)
        Field("MODEL ID", mId, { mId = it }, "e.g. some-org/some-model:free")
        Field("CONTEXT TOKENS (optional)", mCtx, { mCtx = it }, "e.g. 128000")
        Field("QUALITY TIER 1-5 (optional)", mTier, { mTier = it }, "1 small · 5 flagship")
        if (mTools != null) SwitchRow("Supports tool calling", mTools == true) { mTools = it } else SwitchRow("Supports tool calling (unknown)", false) { mTools = it }
        if (mFree != null) SwitchRow("Free model", mFree == true) { mFree = it } else SwitchRow("Free model (unknown)", false) { mFree = it }
        OutlineButton("Add this model to the list", Icons.Outlined.Add, {
            val id = mId.trim()
            if (id.isBlank()) { result = "Type the model id first."; ok = false }
            else {
                val nm = ProviderModel(id, source = "manual", contextWindow = mCtx.trim().toIntOrNull(),
                    supportsTools = mTools, qualityTier = mTier.trim().toIntOrNull()?.coerceIn(1, 5), isFree = mFree)
                modelsList = (modelsList.filter { it.id != id } + nm)
                if (model.isBlank()) model = id
                mId = ""; mCtx = ""; mTier = ""; mTools = null; mFree = null
                if (url.isNotBlank()) CustomProviders.upsert(current())
                result = "Model added."; ok = true
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
        GoldButton("Save", Icons.Outlined.Check, {
            if (url.isNotBlank() && model.isNotBlank()) { CustomProviders.upsert(current()); onDone() } else { result = "Base URL and Model ID are required."; ok = false }
        }, Modifier.fillMaxWidth())
        if (exists) OutlineButton("Delete this provider", Icons.Outlined.Delete, { CustomProviders.delete(start.id); onDone() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, hint: String, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JText(label, 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(value, onChange, hint, Modifier.fillMaxWidth(), singleLine = singleLine, minHeight = if (singleLine) 0.dp else 72.dp)
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

@Composable
private fun ModelRow(m: ProviderModel, selected: Boolean, testing: Boolean, onClick: () -> Unit, onProbe: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val ctxText = m.contextWindow?.let { if (it >= 1000) "${it / 1000}k" else "$it" } ?: "?"
    val toolsText = when (m.supportsTools) { true -> "tools"; false -> "no tools"; null -> "tools?" }
    val meta = buildString {
        append(ctxText); append(" · "); append(toolsText)
        if (m.lastVerifiedAt > 0) append(" · verified")
    }
    Row(Modifier.fillMaxWidth().clip(shape).background(if (selected) Gold.copy(alpha = 0.14f) else Surface1)
        .border(if (selected) 1.5.dp else 1.dp, Gold.copy(alpha = if (selected) 0.8f else 0.16f), shape)
        .clickable(enabled = !testing) { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            JText(m.id, 13, if (selected) GoldLight else Ivory)
            JText(meta, 10, Muted)
        }
        if (m.isFree == true) JText("free", 10, Gold)
        if (testing) Icon(Icons.Outlined.Refresh, "Testing", tint = Gold, modifier = Modifier.size(18.dp))
        else Icon(Icons.Outlined.Info, if (m.lastVerifiedAt > 0) "Verified · test again" else "Test this model", tint = if (m.lastVerifiedAt > 0) Gold else Muted,
            modifier = Modifier.size(18.dp).clickable { onProbe() })
    }
}
