package com.jarvis.app.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.R
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

@Composable
fun ProfileScreen(onNav: (String) -> Unit) {
    if (Palettes.style != 0) {
        ScreenScaffold("profile", onNav) {
            Box(Modifier.weight(1f).fillMaxWidth()) { com.jarvis.app.ui.styles.StyledSettings(onNav) }
        }
        return
    }
    val name = Prefs.userName.ifBlank { "Your Name" }
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    ScreenScaffold("profile", onNav) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                JText("Profile", 28, Ivory, FontWeight.Medium, serif = true)
            }
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(68.dp).background(Surface1, CircleShape).border(2.dp, Gold, CircleShape), contentAlignment = Alignment.Center) {
                    JText(initials.ifBlank { "J" }, 24, Gold, FontWeight.Medium, serif = true, ls = 1f)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    JText(name, 21, Ivory, FontWeight.Medium, serif = true)
                    JText("PLATINUM MEMBER", 11, Gold, FontWeight.Medium, ls = 2f)
                }
            }
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp).fillMaxWidth().height(160.dp)
                    .shadow(20.dp, cardShape, ambientColor = Gold, spotColor = Gold).clip(cardShape)
                    .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.22f).compositeOver(Color(0xFF0F0F11)), Color(0xFF0F0F11))))
                    .border(1.dp, Gold.copy(alpha = 0.5f), cardShape).padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    JText("${Prefs.assistantName.uppercase()} PRIVILEGE", 11, Gold, FontWeight.Medium, ls = 3f)
                    JIcon(R.drawable.ic_badge, Gold, 26.dp)
                }
                JText("Platinum", 34, GoldLight, FontWeight.Medium, serif = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    JText(if (ModelCatalog.ok == true) "Gemini: connected" else "Gemini: not connected", 11, Ivory)
                    JText("Voice: ${Prefs.voiceLabel}", 11, Muted)
                }
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JText("PREFERENCES", 11, Gold, FontWeight.Medium, ls = 2.2f)
                val listShape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.fillMaxWidth().clip(listShape).background(Surface1)
                        .border(1.dp, Gold.copy(alpha = 0.14f), listShape).padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    listOf(
                        Triple(R.drawable.ic_pref_api, "Gemini API", "api"),
                        Triple(R.drawable.ic_pref_voice, "Voice", "voices"),
                        Triple(R.drawable.ic_pref_palette, "Appearance", "themes"),
                        Triple(R.drawable.ic_pref_palette, "Theme Lab", "themelab"),
                        Triple(R.drawable.ic_pref_memory, "Memory & Instructions", "memory"),
                        Triple(R.drawable.ic_pref_wake, "Assistant name & wake phrase", "assistant")
                    ).forEachIndexed { i, (icon, label, route) ->
                        if (i > 0) Hairline(0.12f)
                        PrefRow(icon, label) { onNav(route) }
                    }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Code, "Code & documents workspace") { onNav("code") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Extension, "Skills") { onNav("skills") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Layers, "Interface style") { onNav("uistyle") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Security, "Access & permissions") { onNav("access") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Terminal, "Console (errors)") { onNav("console") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.GpsFixed, "Cursor Lab (AI cursor)") { onNav("cursorlab") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Bookmark, "Cursor memory (click lessons)") { onNav("cursormemory") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.BugReport, "Diagnostics (mic · call · cursor)") { onNav("diagnostics") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.CloudSync, "Brain server (memory · learning)") { onNav("server") }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.AutoAwesome, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("AI control in calls", 15, Ivory)
                            JText("Let ${Prefs.assistantName} change orb, theme, recording by voice", 11, Muted)
                        }
                        JToggle(Prefs.aiControl) { Prefs.aiControl = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.GpsFixed, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("AI cursor control", 15, Ivory)
                            JText("Let ${Prefs.assistantName} move a cursor and tap the screen for you", 11, Muted)
                        }
                        JToggle(Prefs.aiCursor) { Prefs.aiCursor = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.CloudSync, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Keep the call alive", 15, Ivory)
                            JText("Resume automatically if the connection drops (session resumption)", 11, Muted)
                        }
                        JToggle(Prefs.liveResume) { Prefs.liveResume = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.Compress, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Compress long calls", 15, Ivory)
                            JText("Let the server shrink the context so long calls keep going", 11, Muted)
                        }
                        JToggle(Prefs.liveCompress) { Prefs.liveCompress = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.RecordVoiceOver, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Let me interrupt", 15, Ivory)
                            JText("Speak over ${Prefs.assistantName} to cut in. Turn off if echo keeps cutting it off on speaker", 11, Muted)
                        }
                        JToggle(Prefs.liveBargeIn) { Prefs.liveBargeIn = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val steps = listOf(40, 55, 70, 85, 100)
                            Prefs.callVolume = steps[(steps.indexOf(Prefs.callVolume).let { if (it < 0) 2 else it } + 1) % steps.size]
                            Prefs.save()
                            com.jarvis.app.audio.AudioRouter.applyVolume()
                        }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.VolumeUp, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Call volume", 15, Ivory)
                            JText("Set once when the call starts. It is no longer forced to maximum (max volume aimed at the same mic is the worst case for echo). Tap to change", 11, Muted)
                        }
                        JText("${Prefs.callVolume}%", 14, Gold, FontWeight.Medium)
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.Headphones, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Auto earpiece", 15, Ivory)
                            JText("Hold the phone at your ear for 1.5 s to move to the earpiece; moving away returns to the speaker at once", 11, Muted)
                        }
                        JToggle(Prefs.autoEarpiece) { Prefs.autoEarpiece = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            Prefs.halfDuplex = when (Prefs.halfDuplex) { "auto" -> "on"; "on" -> "off"; else -> "auto" }
                            Prefs.save()
                        }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.Hearing, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Echo guard (half-duplex)", 15, Ivory)
                            JText("While ${Prefs.assistantName} speaks the mic is not sent upstream, so the speaker cannot interrupt it. A clearly loud voice still gets through. Auto = only without headphones and without echo cancellation", 11, Muted)
                        }
                        JText(Prefs.halfDuplex.replaceFirstChar { it.uppercase() }, 13, Gold, FontWeight.Medium)
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            Prefs.shareQuality = if (Prefs.shareQuality == "fast") "detailed" else "fast"
                            Prefs.save()
                        }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.ScreenShare, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Screen-share detail", 15, Ivory)
                            JText("Detailed = 1280 px frames so ${Prefs.assistantName} can read small buttons (800 px when the phone is hot). Fast = 720 px, lighter on data", 11, Muted)
                        }
                        JText(Prefs.shareQuality.replaceFirstChar { it.uppercase() }, 13, Gold, FontWeight.Medium)
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.VolumeUp, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Phone recorders can hear ${Prefs.assistantName}", 15, Ivory)
                            JText("Plays ${Prefs.assistantName}'s voice on the media stream so the phone's own screen recorder (choose \"Media sounds\") records both voices. Off = voice-call stream: Android hides it from recorders, but earpiece/BT-call routing is exact", 11, Muted)
                        }
                        JToggle(Prefs.shareToRecorders) { Prefs.shareToRecorders = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            Prefs.recBothVoices = !Prefs.recBothVoices
                            Prefs.save()
                        }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.RecordVoiceOver, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("Call recording voices", 15, Ivory)
                            JText("Both voices = ${Prefs.assistantName}'s own Record button saves my voice AND ${Prefs.assistantName}'s voice in the video (kept in sync with the picture). My voice only = just the microphone", 11, Muted)
                        }
                        JText(if (Prefs.recBothVoices) "Both voices" else "My voice only", 13, Gold, FontWeight.Medium)
                    }
                    Hairline(0.12f)
                    JText(
                        "Using Android's own screen recorder? In its dialog turn ON \"Media sounds\" (that carries ${Prefs.assistantName}'s voice) and, if you also turn on Microphone, Android may give the mic to the recorder — ${Prefs.assistantName} then shows a red \"microphone is silent\" warning and takes the mic back by itself once the recorder stops.",
                        11, Muted, lh = 16, modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Hairline(0.12f)
                    JText(
                        "Quick A/B test for stalling: run the same call twice — once with headphones (echo canceller works) and once on speaker — then turn \"Let me interrupt\" off and on. If the interruptions stop with headphones or with interrupt off, it was echo, not the AI.",
                        11, Muted, lh = 16, modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrefRowV(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        GoldIconBox(icon, box = 38, iconSize = 22, radius = 12)
        JText(label, 15, Ivory, modifier = Modifier.weight(1f))
        JIcon(R.drawable.ic_chevron, Muted, 18.dp)
    }
}

@Composable
private fun PrefRow(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        GoldIconBoxRes(icon, box = 38, iconSize = 22, radius = 12)
        JText(label, 15, Ivory, modifier = Modifier.weight(1f))
        JIcon(R.drawable.ic_chevron, Muted, 18.dp)
    }
}
