package com.jarvis.app.ui.styles

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** FOLIO: a warm, paper-like magazine. Serif headlines, navigation as text tabs at the TOP (no bottom bar), numbers and rules instead of cards. */
@Composable
fun FolioTabs(current: String, onNav: (String) -> Unit) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())).uppercase()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp)) {
        JText(date, 10, Muted, ls = 1.6f)
        JText(Prefs.assistantName, 30, Ivory, FontWeight.SemiBold, serif = true)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            listOf("home" to "Home", "chat" to "Assistant", "control" to "Control", "profile" to "Profile").forEach { (route, label) ->
                val active = route == current
                Column(Modifier.clickable { if (!active) onNav(route) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    JText(label, 14, if (active) Ivory else Muted, if (active) FontWeight.SemiBold else FontWeight.Normal)
                    Box(Modifier.height(2.dp).width(if (active) 26.dp else 0.dp).background(Gold))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Hairline(0.5f)
    }
}

@Composable
fun FolioHeader(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        JText("← Back", 13, Gold, FontWeight.Medium, modifier = Modifier.clickable { onBack() })
        JText(title, 34, Ivory, FontWeight.SemiBold, serif = true, lh = 40)
        Hairline(0.5f)
    }
}

@Composable
private fun Figure(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        JText(value, 34, Ivory, FontWeight.SemiBold, serif = true)
        JText(label, 10, Muted)
    }
}

@Composable
fun FolioHome(h: HomeUi) {
    val s = h.stats
    val hour = LocalTime.now().hour
    val greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val who = Prefs.userName.let { if (it.isBlank()) "." else ", $it." }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("$greeting$who", 36, Ivory, FontWeight.SemiBold, serif = true, lh = 42)
                JText(gemStatus(), 12, Muted)
                Row(Modifier.clickable { h.onNav("live") }, horizontalArrangement = Arrangement.spacedBy(6.dp)) { JText("Start a live call", 15, Gold, FontWeight.Medium); JText("→", 15, Gold) }
            }
            Box(Modifier.clickable { h.greet() }) { SiriOrb(112.dp, if (h.talking) 0.5f else 0.1f, if (h.talking) 3 else if (h.stage < 5) 2 else 0) }
        }
        Hairline(0.5f)
        JText("KEY FIGURES", 10, Muted, ls = 2f)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Figure("${s.battery}", "% battery", Modifier.weight(1f))
            Box(Modifier.fillMaxHeight().width(1.dp).background(Gold.copy(alpha = 0.3f)))
            Figure("%.0f".format(s.storageFreeGb), "GB free", Modifier.weight(1f))
            Box(Modifier.fillMaxHeight().width(1.dp).background(Gold.copy(alpha = 0.3f)))
            Figure("%.1f".format(s.ramFreeGb), "GB memory", Modifier.weight(1f))
            Box(Modifier.fillMaxHeight().width(1.dp).background(Gold.copy(alpha = 0.3f)))
            Figure("%.0f°".format(s.tempC), "heat", Modifier.weight(1f))
        }
        Hairline(0.5f)
        JText("NEXT", 10, Muted, ls = 2f)
        listOf("Ask a question in chat" to "chat", "Choose a voice" to "voices", "Change the colors" to "themes", "Switch the interface style" to "uistyle", "Open the console" to "console").forEach { (label, route) ->
            Column(Modifier.clickable { h.onNav(route) }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { JText(label, 19, Ivory, serif = true); JText("→", 18, Gold) }
                Hairline(0.18f)
            }
        }
    }
}

@Composable
fun FolioControl(onNav: (String) -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { DeviceControl.init(ctx); while (true) { kotlinx.coroutines.delay(1500); tick++ } }
    val wifi = remember(tick) { DeviceControl.wifiOn(ctx) }
    val bt = remember(tick) { DeviceControl.bluetoothOn(ctx) }
    val air = remember(tick) { DeviceControl.airplaneOn(ctx) }
    val dnd = remember(tick) { DeviceControl.dndOn(ctx) }
    val canWrite = remember(tick) { DeviceControl.canWriteSettings(ctx) }
    var media by remember(tick) { mutableStateOf(DeviceControl.volume(ctx, AudioManager.STREAM_MUSIC)) }
    var ring by remember(tick) { mutableStateOf(DeviceControl.volume(ctx, AudioManager.STREAM_RING)) }
    var bright by remember(tick) { mutableStateOf(DeviceControl.brightness(ctx)) }
    val rows: List<Triple<ImageVector, String, Boolean>> = listOf(
        Triple(Icons.TwoTone.FlashlightOn, "Flashlight", DeviceControl.torchOn), Triple(Icons.TwoTone.Wifi, "Wi-Fi", wifi),
        Triple(Icons.TwoTone.Bluetooth, "Bluetooth", bt), Triple(Icons.TwoTone.DoNotDisturbOn, "Do not disturb", dnd),
        Triple(Icons.TwoTone.AirplanemodeActive, "Airplane mode", air), Triple(Icons.TwoTone.LocationOn, "Location", false)
    )
    val actions = listOf<() -> Unit>(
        { DeviceControl.setTorch(ctx, !DeviceControl.torchOn) }, { DeviceControl.openWifi(ctx) }, { DeviceControl.openBluetooth(ctx) },
        { DeviceControl.setDnd(ctx, !dnd); tick++ }, { DeviceControl.openAirplane(ctx) }, { DeviceControl.openLocation(ctx) }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        JText("Switches", 30, Ivory, FontWeight.SemiBold, serif = true, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        rows.forEachIndexed { i, (icon, label, on) ->
            Column(Modifier.clickable { actions[i]() }) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
                    JText(label, 19, Ivory, serif = true, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp)).then(if (on) Modifier.background(Gold) else Modifier.border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))).padding(horizontal = 14.dp, vertical = 5.dp)
                    ) { JText(if (on) "On" else "Off", 12, if (on) Bg else Muted, FontWeight.Medium) }
                }
                Hairline(0.18f)
            }
        }
        JText("Levels", 30, Ivory, FontWeight.SemiBold, serif = true, modifier = Modifier.padding(top = 18.dp))
        FolioLevel("Media volume", media) { media = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
        FolioLevel("Ringer volume", ring) { ring = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_RING, it) }
        FolioLevel(if (canWrite) "Brightness" else "Brightness (tap to allow)", bright) { if (canWrite) { bright = it; DeviceControl.setBrightness(ctx, it) } else DeviceControl.askWriteSettings(ctx) }
        Column(Modifier.clickable { onNav("access") }.padding(top = 10.dp)) {
            Hairline(0.5f)
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                JText("Screen control · " + if (JarvisAccessibilityService.enabled) "on" else "off", 17, Ivory, serif = true); JText("→", 18, Gold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FolioLevel(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row { JText(label, 15, Ivory, serif = true, modifier = Modifier.weight(1f)); JText("${(value * 100).toInt()}%", 12, Muted) }
        Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Surface2))
    }
}

@Composable
fun FolioSettings(onNav: (String) -> Unit) {
    var open by remember { mutableStateOf(0) }
    val name = Prefs.userName.ifBlank { "Reader" }
    data class R(val title: String, val sub: String, val route: String)
    val groups = listOf(
        "The assistant" to listOf(R("Code & documents workspace", "Build files, fix bugs, make PDFs", "code"), R("Skills", "Teach the agent new skills", "skills"), R("Assistant & wake word", "${Prefs.assistantName}, how it wakes up", "assistant"), R("Voice", Prefs.voiceLabel, "voices"), R("Memory", "Your name and what to remember", "memory")),
        "Appearance" to listOf(R("Colors & orb", "Theme and orb style", "themes"), R("Interface style", "Change the whole design of the app", "uistyle")),
        "System" to listOf(R("Gemini API", "Key, models and other providers", "api"), R("Access & permissions", "Microphone, screen control, assistant app", "access"), R("Console", "Errors, crashes and events", "console"))
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        JText("Profile", 36, Ivory, FontWeight.SemiBold, serif = true, modifier = Modifier.padding(top = 12.dp))
        JText("Prepared for $name", 12, Muted, modifier = Modifier.padding(bottom = 10.dp))
        groups.forEachIndexed { i, (title, rows) ->
            Hairline(0.5f)
            Row(Modifier.fillMaxWidth().clickable { open = if (open == i) -1 else i }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                JText(title, 24, Ivory, serif = true); JText(if (open == i) "–" else "+", 24, Gold)
            }
            if (open == i) rows.forEach { r ->
                Row(Modifier.fillMaxWidth().clickable { onNav(r.route) }.padding(start = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(r.title, 17, Ivory, FontWeight.Medium); JText(r.sub, 12, Muted) }
                    JText("→", 17, Gold)
                }
            }
        }
        Hairline(0.5f)
        Spacer(Modifier.height(16.dp))
    }
}
