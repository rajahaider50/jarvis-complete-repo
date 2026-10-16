package com.jarvis.app.ui.styles

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

/** NOVA · Home: search-style "Ask" bar, a tonal assistant card, circular quick actions and a phone-health list. */
@Composable
fun NovaHome(h: HomeUi) {
    val s = h.stats
    val card = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(32.dp)).background(Surface2).clickable { h.onNav("chat") }.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(22.dp))
            JText("Ask ${Prefs.assistantName} anything", 15, Muted, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).background(Gold, CircleShape).clickable { h.onNav("live") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, "Live call", tint = Bg, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(card).background(Gold.copy(alpha = 0.12f)).padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.clickable { h.greet() }) { SiriOrb(96.dp, if (h.talking) 0.5f else 0.1f, if (h.talking) 3 else if (h.stage < 5) 2 else 0) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JText("Good to see you${if (Prefs.userName.isNotBlank()) ", " + Prefs.userName else ""}", 18, Ivory, FontWeight.SemiBold)
                JText("${Prefs.assistantName} is online", 13, Gold, FontWeight.Medium)
                JText(gemStatus(), 11, Muted, lh = 15)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                Triple(Icons.Filled.Call, "Live call", "live"), Triple(Icons.Filled.Chat, "Chat", "chat"),
                Triple(Icons.Filled.Dashboard, "Control", "control"), Triple(Icons.Filled.RecordVoiceOver, "Voices", "voices")
            ).forEach { (icon, label, route) ->
                Column(Modifier.width(76.dp).clickable { h.onNav(route) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(60.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, label, tint = Gold, modifier = Modifier.size(26.dp)) }
                    JText(label, 12, Ivory, FontWeight.Medium)
                }
            }
        }
        SectionLabel("Phone health")
        Column(Modifier.fillMaxWidth().clip(card).background(Surface1)) {
            BarRow(Icons.Filled.BatteryFull, "Battery", "${s.battery}% · ${if (s.charging) "charging" else "on battery"}", s.battery / 100f, s.battery < 20)
            BarRow(Icons.Filled.Storage, "Storage", "%.0f of %.0f GB free".format(s.storageFreeGb, s.storageTotalGb), 1f - s.storageFreeGb / s.storageTotalGb.coerceAtLeast(1f), s.storageFreeGb < 3f)
            BarRow(Icons.Filled.Memory, "Memory", "%.1f of %.1f GB free".format(s.ramFreeGb, s.ramTotalGb), 1f - s.ramFreeGb / s.ramTotalGb.coerceAtLeast(1f), s.ramFreeGb < 0.6f)
            BarRow(Icons.Filled.Thermostat, "Temperature", "%.0f°C · ${s.thermal}".format(s.tempC), s.tempC / 50f, s.tempC >= 40f)
        }
    }
}

/** NOVA · Control: Material-style tonal tiles with the state written out, then sliders. */
@Composable
fun NovaControl(onNav: (String) -> Unit) {
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
    val tiles: List<Triple<ImageVector, String, Pair<String, Boolean>>> = listOf(
        Triple(Icons.Filled.FlashlightOn, "Flashlight", (if (DeviceControl.torchOn) "On" else "Off") to DeviceControl.torchOn),
        Triple(Icons.Filled.Wifi, "Wi-Fi", (if (wifi) "On" else "Off") to wifi),
        Triple(Icons.Filled.Bluetooth, "Bluetooth", (if (bt) "On" else "Off") to bt),
        Triple(Icons.Filled.DoNotDisturbOn, "Do not disturb", (if (dnd) "On" else "Off") to dnd),
        Triple(Icons.Filled.AirplanemodeActive, "Airplane mode", (if (air) "On" else "Off") to air),
        Triple(Icons.Filled.LocationOn, "Location", "Open settings" to false)
    )
    val actions = listOf<() -> Unit>(
        { DeviceControl.setTorch(ctx, !DeviceControl.torchOn) }, { DeviceControl.openWifi(ctx) }, { DeviceControl.openBluetooth(ctx) },
        { DeviceControl.setDnd(ctx, !dnd); tick++ }, { DeviceControl.openAirplane(ctx) }, { DeviceControl.openLocation(ctx) }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        JText("Control", 30, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        tiles.chunked(2).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, (icon, title, st) ->
                    val on = st.second
                    Row(
                        Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(36.dp)).background(if (on) Gold else Surface1).clickable { actions[r * 2 + c]() }.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).background(if (on) Bg.copy(alpha = 0.18f) else Surface2, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = if (on) Bg else Gold, modifier = Modifier.size(22.dp))
                        }
                        Column { JText(title, 13, if (on) Bg else Ivory, FontWeight.SemiBold); JText(st.first, 11, if (on) Bg.copy(alpha = 0.8f) else Muted) }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Surface1).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NovaSlider(Icons.Filled.VolumeUp, "Media volume", media) { media = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
            NovaSlider(Icons.Filled.NotificationsActive, "Ringer volume", ring) { ring = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_RING, it) }
            NovaSlider(Icons.Filled.LightMode, if (canWrite) "Brightness" else "Brightness (tap to allow)", bright) {
                if (canWrite) { bright = it; DeviceControl.setBrightness(ctx, it) } else DeviceControl.askWriteSettings(ctx)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Surface1).clickable { onNav("access") }.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Accessibility, null, tint = Gold, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                JText("Screen control", 15, Ivory, FontWeight.SemiBold)
                JText(if (JarvisAccessibilityService.enabled) "On" else "Off · set up in Access & permissions", 12, Muted)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Muted)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NovaSlider(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp)); JText(label, 14, Ivory, FontWeight.Medium); Spacer(Modifier.weight(1f)); JText("${(value * 100).toInt()}%", 12, Muted)
        }
        Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Surface2))
    }
}

/** NOVA · Account: a searchable list in grouped cards, the way Google's own settings look. */
@Composable
fun NovaSettings(onNav: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val name = Prefs.userName.ifBlank { "Your name" }
    data class Row3(val icon: ImageVector, val title: String, val sub: String, val route: String)
    val groups = listOf(
        "Assistant" to listOf(
            Row3(Icons.Filled.Code, "Code & documents workspace", "Build files, fix bugs, make PDFs", "code"),
            Row3(Icons.Filled.Extension, "Skills", "Teach the agent new skills", "skills"),
            Row3(Icons.Filled.AutoAwesome, "Assistant & wake word", "${Prefs.assistantName} · offline wake word", "assistant"),
            Row3(Icons.Filled.RecordVoiceOver, "Voice", Prefs.voiceLabel, "voices"),
            Row3(Icons.Filled.Memory, "Memory & instructions", "Your name and what to remember", "memory")
        ),
        "Personalize" to listOf(
            Row3(Icons.Filled.Palette, "Colors & orb", "Theme and voice orb style", "themes"),
            Row3(Icons.Filled.Layers, "Interface style", "Switch the whole app design", "uistyle")
        ),
        "System" to listOf(
            Row3(Icons.Filled.Key, "Gemini API", "Key and models", "api"),
            Row3(Icons.Filled.Security, "Access & permissions", "Microphone, screen control, assistant app", "access"),
            Row3(Icons.Filled.Terminal, "Console", "Errors, crashes and events", "console")
        )
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Gold, CircleShape), contentAlignment = Alignment.Center) { JText(name.take(1).uppercase(), 24, Bg, FontWeight.SemiBold) }
            Column { JText(name, 22, Ivory, FontWeight.SemiBold); JText("Account & settings", 12, Muted) }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Surface2).padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = Muted, modifier = Modifier.size(20.dp))
            JField(query, { query = it }, "Search settings", Modifier.weight(1f))
        }
        groups.forEach { (title, rows) ->
            val shown = rows.filter { query.isBlank() || it.title.contains(query, true) || it.sub.contains(query, true) }
            if (shown.isNotEmpty()) {
                SectionLabel(title)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Surface1)) {
                    shown.forEachIndexed { i, r ->
                        if (i > 0) Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(Surface2))
                        Row(Modifier.fillMaxWidth().clickable { onNav(r.route) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(r.icon, null, tint = Gold, modifier = Modifier.size(21.dp)) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(r.title, 15, Ivory, FontWeight.Medium); JText(r.sub, 12, Muted) }
                            Icon(Icons.Filled.ChevronRight, null, tint = Muted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
