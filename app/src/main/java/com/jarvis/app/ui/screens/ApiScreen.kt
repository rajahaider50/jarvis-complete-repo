package com.jarvis.app.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ApiScreen(onBack: () -> Unit, onProviders: () -> Unit) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(Prefs.apiKey) }
    var show by remember { mutableStateOf(false) }

    ScreenScaffold(null, {}) {
        SubHeader("Gemini API", onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (ModelCatalog.status.isNotEmpty()) {
                val c = when (ModelCatalog.ok) { true -> Gold; false -> Color(0xFFE0665C); else -> Muted }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.1f)).border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(c, CircleShape))
                    JText(ModelCatalog.status, 13, if (ModelCatalog.ok == true) GoldLight else Ivory, FontWeight.Medium, lh = 19, modifier = Modifier.weight(1f))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("API KEY", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JField(key, { key = it }, "Paste your Gemini API key", Modifier.fillMaxWidth(), secret = !show, trailing = {
                    Icon(Icons.Outlined.Visibility, "Show", tint = if (show) Gold else Muted, modifier = Modifier.size(20.dp).clickable { show = !show })
                })
                JText("Free key: aistudio.google.com → Get API key", 11, Muted)
            }
            GoldButton(if (ModelCatalog.loading) "Loading…" else "Save & Connect", Icons.Outlined.Refresh, {
                if (!ModelCatalog.loading && key.isNotBlank()) scope.launch { Prefs.apiKey = key.trim(); Prefs.save(); ModelCatalog.refresh() }
            }, Modifier.fillMaxWidth())

            JText("AUTOMATIC MODELS", 11, Gold, FontWeight.Medium, ls = 2.2f)
            JText("Nothing to select: Jarvis reads the real model list of your key and picks the best available model for each job.", 11, Muted, lh = 16)
            val card = RoundedCornerShape(18.dp)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 14.dp)) {
                AutoRow("Quick questions", ModelCatalog.pick(ModelCatalog.Task.FAST)?.id)
                Hairline(0.1f)
                AutoRow("Photos & vision", ModelCatalog.pick(ModelCatalog.Task.IMAGE)?.id)
                Hairline(0.1f)
                AutoRow("Complex reasoning", ModelCatalog.pick(ModelCatalog.Task.COMPLEX)?.id)
                Hairline(0.1f)
                AutoRow("Voice (speech)", ModelCatalog.ttsModel())
                Hairline(0.1f)
                AutoRow("Live call", ModelCatalog.liveModel())
            }

            JText("LIVE CALL MODELS", 11, Gold, FontWeight.Medium, ls = 2.2f)
            JText("In this order the live call connects to these Gemini Live models. The top two are tried at the same time for the fastest start; if one fails the call moves to the next by itself.", 11, Muted, lh = 16)
            var manualLive by remember { mutableStateOf("") }
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 14.dp)) {
                val ids = (Prefs.liveModels.toList() + ModelCatalog.live.map { it.id }).distinct()   // saved order first, then the rest
                if (ids.isEmpty()) JText("Connect first to see the Live models of your key.", 12, Muted, modifier = Modifier.padding(vertical = 12.dp))
                ids.forEach { id ->
                    val pos = Prefs.liveModels.indexOf(id)
                    val noAudio = ModelCatalog.isNoAudio(id)          // answered "AUDIO is not supported" (code 1007)
                    val on = pos >= 0 && !noAudio
                    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clickable(enabled = !noAudio) {
                            if (pos >= 0) Prefs.liveModels.remove(id) else Prefs.liveModels.add(id); Prefs.save()
                        }) { JRadio(on) }
                        Column(Modifier.weight(1f)) {
                            JText(id, 13, if (noAudio) Muted else Ivory)
                            if (noAudio) JText("No audio support (code 1007): this model cannot carry a live call, so it is skipped", 10, Muted)
                        }
                        if (on) {
                            JText("#${pos + 1}", 11, Gold)
                            Icon(Icons.Outlined.KeyboardArrowUp, "Up", tint = if (pos > 0) Gold else Muted, modifier = Modifier.size(20.dp).clickable {
                                if (pos > 0) { val t = Prefs.liveModels[pos - 1]; Prefs.liveModels[pos - 1] = id; Prefs.liveModels[pos] = t; Prefs.save() }
                            })
                            Icon(Icons.Outlined.KeyboardArrowDown, "Down", tint = if (pos < Prefs.liveModels.size - 1) Gold else Muted, modifier = Modifier.size(20.dp).clickable {
                                if (pos < Prefs.liveModels.size - 1) { val t = Prefs.liveModels[pos + 1]; Prefs.liveModels[pos + 1] = id; Prefs.liveModels[pos] = t; Prefs.save() }
                            })
                        }
                    }
                    if (id != ids.last()) Hairline(0.1f)
                }
                Hairline(0.1f)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    JField(manualLive, { manualLive = it }, "Add a model id manually", Modifier.weight(1f))
                    Icon(Icons.Outlined.Add, "Add", tint = Gold, modifier = Modifier.size(24.dp).clickable {
                        val id = manualLive.trim().removePrefix("models/")
                        if (id.isNotBlank() && !Prefs.liveModels.contains(id)) { Prefs.liveModels.add(id); Prefs.save(); manualLive = "" }
                    })
                }
            }

            JText("ALL MODELS OF YOUR KEY", 11, Gold, FontWeight.Medium, ls = 2.2f)
            JText("These are the real models Google gives your key. Auto picks one for each job and switches to the next one by itself when a model is out of quota. Tap a chat model only if you want to force it.", 11, Muted, lh = 16)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth().clickable { Prefs.manualModel = ""; Prefs.save() }.padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    JRadio(Prefs.manualModel.isBlank()); JText("Auto (recommended)", 14, Ivory, FontWeight.Medium)
                }
                ModelCatalog.chat.forEach { m ->
                    Hairline(0.1f)
                    Row(Modifier.fillMaxWidth().clickable { Prefs.manualModel = m.id; Prefs.save() }.padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        JRadio(Prefs.manualModel == m.id); JText(m.id, 13, Ivory)
                    }
                }
                if (ModelCatalog.chat.isEmpty()) JText("Connect first to see the models.", 12, Muted, modifier = Modifier.padding(vertical = 12.dp))
            }
            OutlineButton(if (ModelCatalog.probing) "Testing every model…" else "Test every chat model", Icons.Outlined.NetworkCheck, { scope.launch { ModelCatalog.diagnose() } }, Modifier.fillMaxWidth())
            if (ModelCatalog.probes.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModelCatalog.probes.forEach { pr ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (pr.result == null) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, tint = if (pr.result == null) Gold else Color(0xFFE0665C), modifier = Modifier.size(16.dp))
                            JText(pr.id, 12, Ivory, modifier = Modifier.weight(1f)); JText(pr.result ?: "works", 11, if (pr.result == null) Gold else Muted)
                        }
                    }
                    if (!ModelCatalog.probing && ModelCatalog.probes.none { it.result == null })
                        JText("No model of this key answers. The Google project behind the key has no usable quota: create the key in a NEW project, turn on billing, or use a custom provider.", 11, Color(0xFFE0665C), lh = 15)
                    else if (!ModelCatalog.probing) JText("Working models are tried first from now on.", 11, Muted)
                }
            }
            if (ModelCatalog.tts.isNotEmpty() || ModelCatalog.live.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    ModelCatalog.tts.forEach { m -> Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { JText("voice", 10, Gold); JText(m.id, 12, Ivory) } }
                    ModelCatalog.live.forEach { m -> Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { JText("live", 10, Gold); JText(m.id, 12, Ivory) } }
                }
            }

            JText("WEB SEARCH", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    JText("Google Search for answers", 14, Ivory, FontWeight.Medium)
                    JText("Off by default: many free keys have no quota for it and every message fails with error 429. Opening Chrome and searching through screen control works without it.", 11, Muted, lh = 15)
                }
                JToggle(Prefs.webSearch) { Prefs.webSearch = it; Prefs.save() }
            }

            JText("OTHER AI PROVIDERS", 11, Gold, FontWeight.Medium, ls = 2.2f)
            OutlineButton("Custom providers (OpenAI, OpenRouter, Groq, DeepSeek, Ollama …)", Icons.Outlined.AddCircleOutline, { onProviders() }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AutoRow(label: String, model: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        JText(label, 13, Ivory)
        JText(model ?: "not available", 11, if (model != null) Gold else Muted, modifier = Modifier.padding(start = 12.dp))
    }
}
