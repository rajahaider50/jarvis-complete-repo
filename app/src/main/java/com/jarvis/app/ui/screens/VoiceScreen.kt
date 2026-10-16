package com.jarvis.app.ui.screens

import com.jarvis.app.data.AppLog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jarvis.app.audio.PcmPlayer
import com.jarvis.app.audio.Speaker
import com.jarvis.app.data.CustomVoice
import com.jarvis.app.data.Langs
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.Voices
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val VOICES = listOf(
    "Zephyr" to "Bright", "Puck" to "Upbeat", "Charon" to "Informative", "Kore" to "Firm",
    "Fenrir" to "Excitable", "Leda" to "Youthful", "Orus" to "Firm", "Aoede" to "Breezy",
    "Iapetus" to "Clear", "Algieba" to "Smooth", "Erinome" to "Clear", "Algenib" to "Gravelly",
    "Achird" to "Friendly", "Sulafat" to "Warm", "Gacrux" to "Mature", "Despina" to "Smooth"
)

@Composable
fun VoiceScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var open by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var phase by remember { mutableStateOf("") }
    var modelLabel by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var pcm by remember { mutableStateOf<ByteArray?>(null) }
    var title by remember { mutableStateOf("") }
    var addOpen by remember { mutableStateOf(false) }
    val lang = Langs.of(Prefs.lang)

    fun preview(base: String, style: String, label: String) {
        job?.cancel()
        open = true; errorMsg = null; progress = 0f; playing = false; pcm = null; title = label
        modelLabel = "Choosing the best voice model…"
        val sample = Langs.sampleFor(Prefs.lang, Voices.isFemale(base), Prefs.assistantName)
        job = scope.launch {
            try {
                var result: ByteArray? = null
                var failure: String? = null
                var done = false
                launch {
                    try { result = Speaker.synth(sample, base, style) { m -> modelLabel = m } }   // model is chosen automatically, falls back by itself
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { AppLog.w(e); failure = e.message ?: "unknown error" }
                    done = true
                }
                var p = 0f
                phase = "Sending request"
                while (!done) {                       // real wait: the bar creeps up until the audio arrives
                    p += (92f - p) * 0.035f + 0.12f
                    if (p > 92f) p = 92f
                    progress = p / 100f
                    phase = if (p < 25f) "Sending request" else "Generating voice"
                    delay(60)
                }
                val data = result
                if (data != null) {
                    phase = "Audio received"
                    while (p < 100f) { p = minOf(100f, p + 4f); progress = p / 100f; delay(16) }   // signal arrived: jump to 100
                    pcm = data; phase = "Playing"; playing = true
                    PcmPlayer.play(data)
                    playing = false; phase = "Done"
                } else {
                    progress = 0f
                    errorMsg = "Voice could not be generated. $failure"
                    phase = "Failed"
                }
            } catch (e: CancellationException) { playing = false; throw e }
        }
    }

    ScreenScaffold(null, {}) {
        SubHeader("Voice", onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JText("LANGUAGE", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Langs.all.forEach { l ->
                    val on = Prefs.lang == l.code
                    Box(
                        Modifier.clip(RoundedCornerShape(18.dp))
                            .then(if (on) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)))
                            .clickable { Prefs.lang = l.code; Prefs.save() }.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { JText(l.label, 12, if (on) Bg else Gold, if (on) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }
            JText("Selected: ${Prefs.voiceLabel}. Tap a voice to select it and hear a ${lang.english} sample. The voice model is chosen automatically.", 11, Muted, lh = 16)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    JText("Speak the briefing on Home", 14, Ivory, FontWeight.Medium)
                    JText("Plays instantly when you open Home", 11, Muted)
                }
                JToggle(Prefs.greetOnHome) { Prefs.greetOnHome = it; Prefs.save() }
            }
            JText("JARVIS VOICES · ${VOICES.size}", 11, Gold, FontWeight.Medium, ls = 2.2f)
            VOICES.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (name, style) ->
                        VoiceCard(name, style + " · " + (if (Voices.isFemale(name)) "Female" else "Male"), Prefs.voice == name && Prefs.voiceStyle.isBlank(), false, Modifier.weight(1f), onDelete = null) {
                            Prefs.voice = name; Prefs.voiceLabel = name; Prefs.voiceStyle = ""; Prefs.save(); preview(name, "", name)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            JText("CUSTOM VOICES", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Prefs.customVoices.toList().chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cv ->
                        VoiceCard(cv.name, "${cv.base} · custom", Prefs.voiceLabel == cv.name && Prefs.voiceStyle == cv.style, true, Modifier.weight(1f), onDelete = { Prefs.customVoices.remove(cv); Prefs.save() }) {
                            Prefs.voice = cv.base; Prefs.voiceLabel = cv.name; Prefs.voiceStyle = cv.style; Prefs.save(); preview(cv.base, cv.style, cv.name)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            OutlineButton("Add custom voice", Icons.Outlined.Add, { addOpen = true }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }

    if (open) {
        Dialog(onDismissRequest = { job?.cancel(); open = false }) {
            val shape = RoundedCornerShape(28.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.4f), shape).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JText("VOICE PREVIEW", 11, Gold, FontWeight.Medium, ls = 2.4f)
                SiriOrb(150.dp, if (playing) 0.55f else 0.1f, if (playing) 3 else 2)
                JText(title, 22, Ivory, FontWeight.Medium, serif = true)
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Gold.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) { Icon(Icons.Outlined.Memory, null, tint = Gold, modifier = Modifier.size(14.dp)); JText(modelLabel, 11, GoldLight) }
                JText("${(progress * 100).toInt()}%", 40, Ivory, FontWeight.Medium, serif = true)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Surface2)) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(goldBrush()))
                }
                JText(phase, 12, Muted)
                errorMsg?.let { JText(it, 11, Color(0xFFE0665C), align = TextAlign.Center, lh = 16) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (playing) OutlineButton("Stop", Icons.Outlined.Stop, { job?.cancel(); playing = false; phase = "Stopped" })
                    else OutlineButton("Play again", Icons.Outlined.PlayArrow, {
                        val d = pcm
                        if (d == null) { preview(Prefs.voice, Prefs.voiceStyle, Prefs.voiceLabel); return@OutlineButton }
                        job?.cancel()
                        job = scope.launch {
                            try {
                                playing = true; phase = "Playing"
                                PcmPlayer.play(d)
                                playing = false; phase = "Done"
                            } catch (e: CancellationException) { playing = false; throw e }
                        }
                    })
                    OutlineButton("Close", null, { job?.cancel(); open = false })
                }
            }
        }
    }

    if (addOpen) AddVoiceDialog(onDismiss = { addOpen = false }) { cv -> Prefs.customVoices.add(cv); Prefs.save(); addOpen = false }
}

@Composable
private fun VoiceCard(name: String, style: String, selected: Boolean, custom: Boolean, modifier: Modifier, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.clip(shape).background(if (selected) Gold.copy(alpha = 0.12f) else Surface1)
            .border(1.dp, Gold.copy(alpha = if (selected) 0.8f else 0.14f), shape).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).then(if (selected) Modifier.background(goldBrush()) else Modifier.background(Gold.copy(alpha = 0.12f))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.PlayArrow, "Preview", tint = if (selected) Bg else Gold, modifier = Modifier.size(18.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            JText(name, 14, Ivory, FontWeight.Medium); JText(style, 11, if (selected) Gold else Muted)
        }
        if (custom && onDelete != null) Icon(Icons.Outlined.Close, "Delete", tint = Muted, modifier = Modifier.size(16.dp).clickable { onDelete() })
        else if (selected) Icon(Icons.Outlined.Check, null, tint = Gold, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun AddVoiceDialog(onDismiss: () -> Unit, onAdd: (CustomVoice) -> Unit) {
    var name by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("") }
    var base by remember { mutableStateOf("Zephyr") }
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.4f), shape).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JText("CUSTOM VOICE", 11, Gold, FontWeight.Medium, ls = 2.4f)
            JField(name, { name = it }, "Voice name, e.g. Royal Butler", Modifier.fillMaxWidth())
            JText("Base voice", 12, Muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VOICES.forEach { (n, _) ->
                    val on = base == n
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp)).then(if (on) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp)))
                            .clickable { base = n }.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) { JText(n, 12, if (on) Bg else Gold) }
                }
            }
            JField(style, { style = it }, "Style, e.g. Speak slowly in a deep, formal, calm tone", Modifier.fillMaxWidth(), singleLine = false, minHeight = 80.dp)
            GoldButton("Save voice", null, { if (name.isNotBlank()) onAdd(CustomVoice(name.trim(), base, style.trim())) }, Modifier.fillMaxWidth())
        }
    }
}
