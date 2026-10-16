package com.jarvis.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
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
import androidx.core.content.ContextCompat
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Langs
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.live.VoskModels
import com.jarvis.app.live.WakeService
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AssistantScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(Prefs.assistantName) }
    var phrase by remember { mutableStateOf(Prefs.wakePhrase) }
    var on by remember { mutableStateOf(Prefs.wakeEnabled) }
    var msg by remember { mutableStateOf("") }
    var micOk by remember { mutableStateOf(false) }
    var overlayOk by remember { mutableStateOf(false) }
    var batteryOk by remember { mutableStateOf(false) }
    var assistantOk by remember { mutableStateOf(false) }
    var a11yOk by remember { mutableStateOf(false) }
    var engineErr by remember { mutableStateOf<String?>(null) }
    var key by remember { mutableStateOf(Prefs.pvKey) }
    val scope = rememberCoroutineScope()
    val pickPpn = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input -> java.io.File(ctx.filesDir, "wake.ppn").outputStream().use { input.copyTo(it) } }
                Prefs.pvKeyword = "custom"; Prefs.save(); msg = "Custom wake word imported. Press Save."
            } catch (e: Exception) { AppLog.w(e); msg = "Could not read that file." }
        }
    }
    // refresh the permission status every second so it updates as soon as you come back from Settings
    LaunchedEffect(Unit) {
        while (true) {
            micOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            overlayOk = Settings.canDrawOverlays(ctx)
            batteryOk = (ctx.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
            assistantOk = try { Settings.Secure.getString(ctx.contentResolver, "assistant").orEmpty().startsWith(ctx.packageName) } catch (e: Exception) { false }
            a11yOk = JarvisAccessibilityService.enabled
            engineErr = WakeService.engineError
            delay(1000)
        }
    }

    fun apply() {
        Prefs.assistantName = name.trim().ifBlank { "Jarvis" }
        Prefs.wakePhrase = phrase.trim()
        Prefs.pvKey = key.trim()
        Prefs.wakeEnabled = on && (phrase.isNotBlank() || Prefs.wakeEngine == "porcupine")
        Prefs.save()
        if (Prefs.wakeEnabled) {
            if (!micOk) { msg = "Allow the microphone first (Control > Permissions)."; Prefs.wakeEnabled = false; on = false; Prefs.save(); return }
            WakeService.stop(ctx)
            WakeService.start(ctx)
            if (!overlayOk) {
                msg = "Saved. One more step: allow “Display over other apps” for ${Prefs.assistantName}, then come back. Without it Android will not let the assistant open over other apps."
                DeviceControl.open(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            } else msg = "Listening in the background for “${Prefs.wakePhrase}”."
        } else { WakeService.stop(ctx); msg = "Saved. Wake phrase is off." }
    }

    ScreenScaffold(null, {}) {
        SubHeader("Assistant", onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("ASSISTANT NAME", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JField(name, { name = it }, "Jarvis", Modifier.fillMaxWidth())
                JText("Any name you like. It is used in chat, calls and the spoken briefing.", 11, Muted, lh = 16)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("WAKE ENGINE", 11, Gold, FontWeight.Medium, ls = 2.2f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("vosk" to "Offline phrase · no key", "porcupine" to "Offline · Picovoice key", "basic" to "Basic · beeps").forEach { (id, label) ->
                        val sel = Prefs.wakeEngine == id
                        Box(
                            Modifier.clip(RoundedCornerShape(18.dp))
                                .then(if (sel) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)))
                                .clickable { Prefs.wakeEngine = id; Prefs.save() }.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { JText(label, 12, if (sel) Bg else Gold, if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                    }
                }
                if (Prefs.wakeEngine == "vosk") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoskModels.all.forEach { (code, info) ->
                            val sel = Prefs.voskLang == code
                            Box(
                                Modifier.clip(RoundedCornerShape(16.dp)).then(if (sel) Modifier.background(Gold.copy(alpha = 0.22f)) else Modifier)
                                    .border(1.dp, Gold.copy(alpha = if (sel) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { Prefs.voskLang = code; Prefs.save() }.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) { JText(info.label, 12, if (sel) GoldLight else Muted) }
                        }
                    }
                    val ready = VoskModels.ready(ctx, Prefs.voskLang)
                    if (ready) JText("Offline voice model: ready ✓", 12, Gold, FontWeight.Medium)
                    else if (VoskModels.progress >= 0f) JText("Downloading… ${(VoskModels.progress * 100).toInt()}%", 12, GoldLight)
                    else OutlineButton("Download offline voice model (about 40 MB)", null, { scope.launch { VoskModels.download(ctx, Prefs.voskLang) } }, Modifier.fillMaxWidth())
                    VoskModels.error?.let { JText(it, 11, Color(0xFFE0665C), lh = 16) }
                    JText("Type your wake phrase below (English or Hindi words, for example “my app” or “hello computer”). It listens only for that phrase, on the phone itself: no internet, no key, no start/stop sounds. Common words work best; Urdu is not available in this engine.", 11, Muted, lh = 16)
                    engineErr?.let { JText(it, 11, Color(0xFFE0665C), lh = 16) }
                } else if (Prefs.wakeEngine == "porcupine") {
                    JField(key, { key = it }, "Picovoice AccessKey (free)", Modifier.fillMaxWidth())
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("JARVIS", "COMPUTER", "ALEXA", "HEY_GOOGLE", "OK_GOOGLE", "HEY_SIRI", "PICOVOICE", "PORCUPINE", "TERMINATOR", "AMERICANO", "BLUEBERRY", "BUMBLEBEE", "GRAPEFRUIT", "GRASSHOPPER").forEach { kw ->
                            val sel = Prefs.pvKeyword == kw
                            Box(
                                Modifier.clip(RoundedCornerShape(16.dp))
                                    .then(if (sel) Modifier.background(Gold.copy(alpha = 0.22f)) else Modifier)
                                    .border(1.dp, Gold.copy(alpha = if (sel) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { Prefs.pvKeyword = kw; Prefs.save() }.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) { JText(kw.replace('_', ' ').lowercase(), 12, if (sel) GoldLight else Muted) }
                        }
                    }
                    OutlineButton(if (Prefs.pvKeyword == "custom") "Custom .ppn word: imported (tap to replace)" else "Use my own word (.ppn file)", null, { pickPpn.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth())
                    JText("Listens fully on the phone: no internet, no start/stop sounds, very little battery. Get the free key at console.picovoice.ai; the same site makes a custom English word (.ppn) such as your own name. Urdu words are not supported by this engine.", 11, Muted, lh = 16)
                    engineErr?.let { JText(it, 11, Color(0xFFE0665C), lh = 16) }
                } else {
                    JText("Basic mode uses Android's speech recognizer in a loop. It needs internet and may click on and off.", 11, Muted, lh = 16)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("WAKE PHRASE", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JField(phrase, { phrase = it }, "e.g. Hey Jarvis", Modifier.fillMaxWidth())
                JText("Say it out loud while the app is closed and the assistant opens over your screen, like Siri. You can use your own name or command.", 11, Muted, lh = 16)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("WAKE PHRASE LANGUAGE", 11, Gold, FontWeight.Medium, ls = 2.2f)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("auto" to "Auto") + Langs.all.map { it.code to it.label }).forEach { (code, label) ->
                        val sel = Prefs.wakeLang == code
                        Box(
                            Modifier.clip(RoundedCornerShape(18.dp))
                                .then(if (sel) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)))
                                .clickable { Prefs.wakeLang = code; Prefs.save(); if (Prefs.wakeEnabled) { WakeService.stop(ctx); WakeService.start(ctx) } }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { JText(label, 12, if (sel) Bg else Gold, if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                    }
                }
                JText("Pick the language you speak the phrase in. Auto guesses it from how you typed the phrase (English letters = English, اردو letters = Urdu).", 11, Muted, lh = 16)
            }
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), shape).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    JText("Listen in the background", 14, Ivory, FontWeight.Medium)
                    JText("Runs a small foreground service", 11, Muted)
                }
                JToggle(on) { on = it }
            }
            val card = RoundedCornerShape(18.dp)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.14f), card).padding(horizontal = 16.dp)) {
                StatusRow("Microphone", micOk)
                Hairline(0.1f)
                Row(Modifier.fillMaxWidth().clickable { if (!overlayOk) DeviceControl.open(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))) }) { Box(Modifier.weight(1f)) { StatusRow("Display over other apps", overlayOk) } }
                Hairline(0.1f)
                Row(Modifier.fillMaxWidth().clickable { if (!assistantOk) DeviceControl.open(ctx, Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }) { Box(Modifier.weight(1f)) { StatusRow("Default assistant app", assistantOk) } }
                Hairline(0.1f)
                Row(Modifier.fillMaxWidth().clickable { if (!a11yOk) DeviceControl.open(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Box(Modifier.weight(1f)) { StatusRow("Screen control (Accessibility)", a11yOk) } }
                Hairline(0.1f)
                Row(Modifier.fillMaxWidth().clickable { if (!batteryOk) DeviceControl.open(ctx, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) }) { Box(Modifier.weight(1f)) { StatusRow("Battery: no restrictions", batteryOk) } }
            }
            if (msg.isNotEmpty()) JText(msg, 12, GoldLight, lh = 18)
            JText("Notes: the wake phrase uses the phone's speech recognizer in a loop (needs internet, uses battery and shows the mic indicator). The tiny start/stop sound of the recognizer is muted for a moment on each cycle, but a few phones may still play it. For it to open while the app is closed, allow all three items above.", 11, Muted, lh = 16)
            GoldButton("Save", null, { apply() }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusRow(name: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        JText(name, 14, Ivory, modifier = Modifier.weight(1f))
        Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, tint = if (ok) Gold else Color(0xFFE0665C), modifier = Modifier.size(18.dp))
        JText(if (ok) "Allowed" else "Tap to allow", 11, if (ok) Gold else Color(0xFFE0665C))
    }
}
