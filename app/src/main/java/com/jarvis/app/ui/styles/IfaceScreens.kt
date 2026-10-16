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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.Stats
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

// ================================================================================================
// The premium interface engine (Palettes.engine == 5). Every extra style picks its own
// layout archetype + navigation bar + corner radius, so no two styles look alike.
// ================================================================================================

private fun curCfg(): IfaceCfg = IfaceCatalog.cfgFor(Prefs.uiStyle)
private fun cornerDp(c: Int): Int = intArrayOf(8, 16, 26, 40)[c.coerceIn(0, 3)]

private fun greeting(): String = "Good to see you" + if (Prefs.userName.isNotBlank()) ", ${Prefs.userName}" else ""

private fun homeActions(): List<Triple<ImageVector, String, String>> = listOf(
    Triple(Icons.Filled.Call, "Live call", "live"),
    Triple(Icons.Filled.Chat, "Chat", "chat"),
    Triple(Icons.Filled.Dashboard, "Control", "control"),
    Triple(Icons.Filled.RecordVoiceOver, "Voices", "voices")
)

@Composable
private fun IfaceOrb(h: HomeUi, size: Dp) {
    Box(Modifier.clickable { h.greet() }) {
        SiriOrb(size, if (h.talking) 0.5f else 0.1f, if (h.talking) 3 else if (h.stage < 5) 2 else 0)
    }
}

// ------------------------------------------------------------------------------------------------
// HOME
// ------------------------------------------------------------------------------------------------
@Composable
fun IfaceHome(h: HomeUi) {
    val cfg = curCfg()
    when (cfg.layout) {
        0 -> HomeSpotlight(h, cfg)
        1 -> HomeCommand(h, cfg)
        2 -> HomeDashboard(h, cfg)
        else -> HomeList(h, cfg)
    }
}

@Composable
private fun ActionCircles(onNav: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        homeActions().forEach { (icon, label, route) ->
            Column(Modifier.width(76.dp).clickable { onNav(route) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(60.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, label, tint = Gold, modifier = Modifier.size(26.dp)) }
                JText(label, 12, Ivory, FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatRows(s: Stats) {
    BarRow(Icons.Filled.BatteryFull, "Battery", "${s.battery}% · ${if (s.charging) "charging" else "on battery"}", s.battery / 100f, s.battery < 20)
    BarRow(Icons.Filled.Storage, "Storage", "%.0f of %.0f GB free".format(s.storageFreeGb, s.storageTotalGb), 1f - s.storageFreeGb / s.storageTotalGb.coerceAtLeast(1f), s.storageFreeGb < 3f)
    BarRow(Icons.Filled.Memory, "Memory", "%.1f of %.1f GB free".format(s.ramFreeGb, s.ramTotalGb), 1f - s.ramFreeGb / s.ramTotalGb.coerceAtLeast(1f), s.ramFreeGb < 0.6f)
    BarRow(Icons.Filled.Thermostat, "Temperature", "%.0f°C · ${s.thermal}".format(s.tempC), s.tempC / 50f, s.tempC >= 40f)
}

/** layout 0 · centred big orb, greeting and a health list. */
@Composable
private fun HomeSpotlight(h: HomeUi, cfg: IfaceCfg) {
    val shape = RoundedCornerShape(cornerDp(cfg.corner).dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IfaceOrb(h, 140.dp) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            JText(greeting(), 22, Ivory, FontWeight.SemiBold, align = TextAlign.Center)
            JText("${Prefs.assistantName} is online", 13, Gold, FontWeight.Medium)
            JText(gemStatus(), 11, Muted, lh = 15, align = TextAlign.Center)
        }
        ActionCircles(h.onNav)
        SectionLabel("Phone health")
        Column(Modifier.fillMaxWidth().clip(shape).background(Surface1)) { StatRows(h.stats) }
    }
}

/** layout 1 · search bar, hero card, action pills and ring stats. */
@Composable
private fun HomeCommand(h: HomeUi, cfg: IfaceCfg) {
    val card = RoundedCornerShape(cornerDp(cfg.corner).dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Surface2).clickable { h.onNav("chat") }.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(22.dp))
            JText("Ask ${Prefs.assistantName} anything", 15, Muted, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).background(Gold, CircleShape).clickable { h.onNav("live") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Mic, "Live call", tint = Bg, modifier = Modifier.size(22.dp)) }
        }
        Row(Modifier.fillMaxWidth().clip(card).background(Gold.copy(alpha = 0.12f)).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IfaceOrb(h, 96.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JText(greeting(), 18, Ivory, FontWeight.SemiBold)
                JText("${Prefs.assistantName} is online", 13, Gold, FontWeight.Medium)
                JText(gemStatus(), 11, Muted, lh = 15)
            }
        }
        homeActions().chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, label, route) ->
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(cornerDp(cfg.corner).dp)).background(Surface1).clickable { h.onNav(route) }.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(36.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp)) }
                        JText(label, 14, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        SectionLabel("Phone health")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RingTile("Battery", "${h.stats.battery}%", if (h.stats.charging) "charging" else "on battery", h.stats.battery / 100f, h.stats.battery < 20, Icons.Filled.BatteryFull, modifier = Modifier.weight(1f))
            RingTile("Storage", "%.0f GB".format(h.stats.storageFreeGb), "free", 1f - h.stats.storageFreeGb / h.stats.storageTotalGb.coerceAtLeast(1f), h.stats.storageFreeGb < 3f, Icons.Filled.Storage, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BigTile(label: String, value: String, sub: String, icon: ImageVector, shape: Shape, modifier: Modifier) {
    Column(modifier.clip(shape).background(Surface1).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(22.dp))
        JText(value, 22, Ivory, FontWeight.SemiBold)
        JText(label, 12, Muted)
        JText(sub, 10, Muted)
    }
}

/** layout 2 · 2x2 stat tiles on top, hero card, action circles. */
@Composable
private fun HomeDashboard(h: HomeUi, cfg: IfaceCfg) {
    val s = h.stats
    val card = RoundedCornerShape(cornerDp(cfg.corner).dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigTile("Battery", "${s.battery}%", if (s.charging) "charging" else "on battery", Icons.Filled.BatteryFull, card, Modifier.weight(1f))
            BigTile("Storage", "%.0f GB".format(s.storageFreeGb), "free", Icons.Filled.Storage, card, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigTile("Memory", "%.1f GB".format(s.ramFreeGb), "free", Icons.Filled.Memory, card, Modifier.weight(1f))
            BigTile("Temp", "%.0f°C".format(s.tempC), s.thermal, Icons.Filled.Thermostat, card, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().clip(card).background(Gold.copy(alpha = 0.10f)).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IfaceOrb(h, 84.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JText(greeting(), 17, Ivory, FontWeight.SemiBold)
                JText(gemStatus(), 11, Muted, lh = 15)
            }
        }
        ActionCircles(h.onNav)
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Surface1).padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        JText(value, 15, Ivory, FontWeight.SemiBold)
        JText(label, 10, Muted)
    }
}

/** layout 3 · compact header with small orb, action list rows and stat chips. */
@Composable
private fun HomeList(h: HomeUi, cfg: IfaceCfg) {
    val s = h.stats
    val card = RoundedCornerShape(cornerDp(cfg.corner).dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JText(greeting(), 20, Ivory, FontWeight.SemiBold)
                JText("${Prefs.assistantName} is online", 13, Gold, FontWeight.Medium)
                JText(gemStatus(), 11, Muted, lh = 15)
            }
            IfaceOrb(h, 64.dp)
        }
        Column(Modifier.fillMaxWidth().clip(card).background(Surface1)) {
            homeActions().forEachIndexed { i, (icon, label, route) ->
                if (i > 0) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Surface2))
                Row(Modifier.fillMaxWidth().clickable { h.onNav(route) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp)) }
                    JText(label, 15, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = Muted)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Battery", "${s.battery}%", Modifier.weight(1f))
            StatChip("Storage", "%.0fG".format(s.storageFreeGb), Modifier.weight(1f))
            StatChip("Memory", "%.1fG".format(s.ramFreeGb), Modifier.weight(1f))
            StatChip("Temp", "%.0f°".format(s.tempC), Modifier.weight(1f))
        }
    }
}

// ------------------------------------------------------------------------------------------------
// CONTROL
// ------------------------------------------------------------------------------------------------
@Composable
fun IfaceControl(onNav: (String) -> Unit) {
    val cfg = curCfg()
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
    val onMedia: (Float) -> Unit = { media = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
    val onRing: (Float) -> Unit = { ring = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_RING, it) }
    val onBright: (Float) -> Unit = { if (canWrite) { bright = it; DeviceControl.setBrightness(ctx, it) } else DeviceControl.askWriteSettings(ctx) }

    when (cfg.layout) {
        0 -> ControlTiles(cfg, tiles, actions, canWrite, media, ring, bright, onMedia, onRing, onBright, onNav)
        1 -> ControlGrid(cfg, tiles, actions, canWrite, media, ring, bright, onMedia, onRing, onBright, onNav)
        else -> ControlList(cfg, tiles, actions, canWrite, media, ring, bright, onMedia, onRing, onBright, onNav)
    }
}

@Composable
private fun IfaceSlider(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp)); JText(label, 14, Ivory, FontWeight.Medium); Spacer(Modifier.weight(1f)); JText("${(value * 100).toInt()}%", 12, Muted)
        }
        Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Surface2))
    }
}

@Composable
private fun SliderCard(cfg: IfaceCfg, canWrite: Boolean, media: Float, ring: Float, bright: Float, onMedia: (Float) -> Unit, onRing: (Float) -> Unit, onBright: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(cornerDp(cfg.corner).dp)).background(Surface1).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IfaceSlider(Icons.Filled.VolumeUp, "Media volume", media, onMedia)
        IfaceSlider(Icons.Filled.NotificationsActive, "Ringer volume", ring, onRing)
        IfaceSlider(Icons.Filled.LightMode, if (canWrite) "Brightness" else "Brightness (tap to allow)", bright, onBright)
    }
}

@Composable
private fun ScreenControlRow(onNav: (String) -> Unit, cfg: IfaceCfg) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(cornerDp(cfg.corner).dp)).background(Surface1).clickable { onNav("access") }.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Accessibility, null, tint = Gold, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f)) {
            JText("Screen control", 15, Ivory, FontWeight.SemiBold)
            JText(if (JarvisAccessibilityService.enabled) "On" else "Off · set up in Access & permissions", 12, Muted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Muted)
    }
}

private typealias Tile = Triple<ImageVector, String, Pair<String, Boolean>>

/** control layout 0 · rounded Material tiles, two per row. */
@Composable
private fun ControlTiles(cfg: IfaceCfg, tiles: List<Tile>, actions: List<() -> Unit>, canWrite: Boolean, media: Float, ring: Float, bright: Float, onMedia: (Float) -> Unit, onRing: (Float) -> Unit, onBright: (Float) -> Unit, onNav: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        JText("Control", 30, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        tiles.chunked(2).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, (icon, title, st) ->
                    val on = st.second
                    Row(
                        Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(cornerDp(cfg.corner).dp)).background(if (on) Gold else Surface1).clickable { actions[r * 2 + c]() }.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).background(if (on) Bg.copy(alpha = 0.18f) else Surface2, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (on) Bg else Gold, modifier = Modifier.size(22.dp)) }
                        Column { JText(title, 13, if (on) Bg else Ivory, FontWeight.SemiBold); JText(st.first, 11, if (on) Bg.copy(alpha = 0.8f) else Muted) }
                    }
                }
            }
        }
        SliderCard(cfg, canWrite, media, ring, bright, onMedia, onRing, onBright)
        ScreenControlRow(onNav, cfg)
        Spacer(Modifier.height(8.dp))
    }
}

/** control layout 1 · square icon grid, three per row. */
@Composable
private fun ControlGrid(cfg: IfaceCfg, tiles: List<Tile>, actions: List<() -> Unit>, canWrite: Boolean, media: Float, ring: Float, bright: Float, onMedia: (Float) -> Unit, onRing: (Float) -> Unit, onBright: (Float) -> Unit, onNav: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        JText("Control", 30, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        tiles.chunked(3).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, (icon, title, st) ->
                    val on = st.second
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(cornerDp(cfg.corner).dp)).background(if (on) Gold else Surface1).clickable { actions[r * 3 + c]() }.padding(vertical = 16.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(icon, null, tint = if (on) Bg else Gold, modifier = Modifier.size(26.dp))
                        JText(title, 11, if (on) Bg else Ivory, FontWeight.Medium, align = TextAlign.Center, lh = 14)
                    }
                }
                if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        SliderCard(cfg, canWrite, media, ring, bright, onMedia, onRing, onBright)
        ScreenControlRow(onNav, cfg)
        Spacer(Modifier.height(8.dp))
    }
}

/** control layout 2 · list rows with a state pill. */
@Composable
private fun ControlList(cfg: IfaceCfg, tiles: List<Tile>, actions: List<() -> Unit>, canWrite: Boolean, media: Float, ring: Float, bright: Float, onMedia: (Float) -> Unit, onRing: (Float) -> Unit, onBright: (Float) -> Unit, onNav: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        JText("Control", 30, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(cornerDp(cfg.corner).dp)).background(Surface1)) {
            tiles.forEachIndexed { i, (icon, title, st) ->
                if (i > 0) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Surface2))
                val on = st.second
                Row(Modifier.fillMaxWidth().clickable { actions[i]() }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        JText(title, 15, Ivory, FontWeight.Medium)
                        JText(st.first, 12, if (on) Gold else Muted)
                    }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Gold else Surface2).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        JText(if (on) "On" else "Off", 11, if (on) Bg else Muted, FontWeight.SemiBold)
                    }
                }
            }
        }
        SliderCard(cfg, canWrite, media, ring, bright, onMedia, onRing, onBright)
        ScreenControlRow(onNav, cfg)
        Spacer(Modifier.height(8.dp))
    }
}

// ------------------------------------------------------------------------------------------------
// SETTINGS
// ------------------------------------------------------------------------------------------------
private data class SetRow(val icon: ImageVector, val title: String, val sub: String, val route: String)

private fun settingGroups(): List<Pair<String, List<SetRow>>> = listOf(
    "Assistant" to listOf(
        SetRow(Icons.Filled.Code, "Code & documents workspace", "Build files, fix bugs, make PDFs", "code"),
        SetRow(Icons.Filled.Extension, "Skills", "Teach the agent new skills", "skills"),
        SetRow(Icons.Filled.AutoAwesome, "Assistant & wake word", "${Prefs.assistantName} · offline wake word", "assistant"),
        SetRow(Icons.Filled.RecordVoiceOver, "Voice", Prefs.voiceLabel, "voices"),
        SetRow(Icons.Filled.Memory, "Memory & instructions", "Your name and what to remember", "memory")
    ),
    "Personalize" to listOf(
        SetRow(Icons.Filled.Palette, "Colors & orb", "Theme and voice orb style", "themes"),
        SetRow(Icons.Filled.Layers, "Interface style", "Switch the whole app design", "uistyle")
    ),
    "System" to listOf(
        SetRow(Icons.Filled.Key, "Gemini API", "Key and models", "api"),
        SetRow(Icons.Filled.Security, "Access & permissions", "Microphone, screen control, assistant app", "access"),
        SetRow(Icons.Filled.Terminal, "Console", "Errors, crashes and events", "console")
    )
)

@Composable
fun IfaceSettings(onNav: (String) -> Unit) {
    val cfg = curCfg()
    when (cfg.layout) {
        0 -> SettingsGrouped(onNav, cfg, true)
        1 -> SettingsProfile(onNav, cfg)
        else -> SettingsTiles(onNav, cfg)
    }
}

/** settings layout 0 · search + grouped list cards. */
@Composable
private fun SettingsGrouped(onNav: (String) -> Unit, cfg: IfaceCfg, search: Boolean) {
    val card = RoundedCornerShape(cornerDp(cfg.corner).dp)
    val name = Prefs.userName.ifBlank { "Your name" }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Gold, CircleShape), contentAlignment = Alignment.Center) { JText(name.take(1).uppercase(), 24, Bg, FontWeight.SemiBold) }
            Column { JText(name, 22, Ivory, FontWeight.SemiBold); JText("Account & settings", 12, Muted) }
        }
        if (search) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Surface2).padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = Muted, modifier = Modifier.size(20.dp))
                JField(query, { query = it }, "Search settings", Modifier.weight(1f))
            }
        }
        settingGroups().forEach { (title, rows) ->
            val shown = if (!search) rows else rows.filter { query.isBlank() || it.title.contains(query, true) || it.sub.contains(query, true) }
            if (shown.isNotEmpty()) {
                SectionLabel(title)
                Column(Modifier.fillMaxWidth().clip(card).background(Surface1)) {
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

/** settings layout 1 · profile header + grouped list, no search. */
@Composable
private fun SettingsProfile(onNav: (String) -> Unit, cfg: IfaceCfg) = SettingsGrouped(onNav, cfg, false)

/** settings layout 2 · a two-column grid of setting tiles. */
@Composable
private fun SettingsTiles(onNav: (String) -> Unit, cfg: IfaceCfg) {
    val shape = RoundedCornerShape(cornerDp(cfg.corner).dp)
    val all = settingGroups().flatMap { it.second }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        JText("Settings", 30, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { r ->
                    Column(Modifier.weight(1f).clip(shape).background(Surface1).clickable { onNav(r.route) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(40.dp).background(Gold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(r.icon, null, tint = Gold, modifier = Modifier.size(21.dp)) }
                        JText(r.title, 14, Ivory, FontWeight.Medium, lh = 18)
                        JText(r.sub, 11, Muted, lh = 14)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ------------------------------------------------------------------------------------------------
// NAVIGATION + HEADER
// ------------------------------------------------------------------------------------------------
private val NAV_ITEMS = listOf(
    Triple("home", "Home", Icons.Filled.Home),
    Triple("chat", "Assistant", Icons.Filled.AutoAwesome),
    Triple("control", "Control", Icons.Filled.Dashboard),
    Triple("profile", "Settings", Icons.Filled.AccountCircle)
)

@Composable
fun IfaceNav(current: String, onNav: (String) -> Unit) {
    val cfg = curCfg()
    when (cfg.nav) {
        0 -> NavLabels(current, onNav)
        1 -> NavPill(current, onNav, cfg)
        2 -> NavSegmented(current, onNav, cfg)
        3 -> NavIconBar(current, onNav)
        else -> NavTextTabs(current, onNav)
    }
}

/** nav 0 · flat Material bar with a tonal indicator and labels. */
@Composable
private fun NavLabels(current: String, onNav: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Surface1).padding(top = 10.dp, bottom = 12.dp)) {
        NAV_ITEMS.forEach { (route, label, icon) ->
            val active = route == current
            Column(Modifier.weight(1f).clickable { if (!active) onNav(route) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(64.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (active) Gold.copy(alpha = 0.20f) else Color.Transparent), contentAlignment = Alignment.Center) { Icon(icon, label, tint = if (active) Gold else Muted, modifier = Modifier.size(24.dp)) }
                JText(label, 12, if (active) Ivory else Muted, if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** nav 1 · floating glass pill, active tab shows its name. */
@Composable
private fun NavPill(current: String, onNav: (String) -> Unit, cfg: IfaceCfg) {
    val shape = RoundedCornerShape(cornerDp(cfg.corner).dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().shadow(14.dp, shape, ambientColor = Gold, spotColor = Gold).clip(shape).background(Surface2.copy(alpha = 0.96f)).border(1.dp, Gold.copy(alpha = 0.22f), shape).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            NAV_ITEMS.forEach { (route, label, icon) ->
                val active = route == current
                Row(Modifier.clip(RoundedCornerShape(26.dp)).then(if (active) Modifier.background(goldBrush()) else Modifier).clickable { if (!active) onNav(route) }.padding(horizontal = if (active) 16.dp else 14.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, label, tint = if (active) Bg else Muted, modifier = Modifier.size(22.dp))
                    if (active) JText(label, 13, Bg, FontWeight.SemiBold)
                }
            }
        }
    }
}

/** nav 2 · segmented control in a rounded track. */
@Composable
private fun NavSegmented(current: String, onNav: (String) -> Unit, cfg: IfaceCfg) {
    val gy = cornerDp(cfg.corner)
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(gy.dp)).background(Surface2).padding(4.dp)) {
            NAV_ITEMS.forEach { (route, label, icon) ->
                val active = route == current
                Row(Modifier.weight(1f).clip(RoundedCornerShape((gy - 4).coerceAtLeast(2).dp)).then(if (active) Modifier.background(goldBrush()) else Modifier).clickable { if (!active) onNav(route) }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, label, tint = if (active) Bg else Muted, modifier = Modifier.size(20.dp))
                    if (active) JText(label, 12, Bg, FontWeight.SemiBold)
                }
            }
        }
    }
}

/** nav 3 · compact icon bar with a top indicator, no labels. */
@Composable
private fun NavIconBar(current: String, onNav: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface1)) {
        Hairline()
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp)) {
            NAV_ITEMS.forEach { (route, label, icon) ->
                val active = route == current
                Column(Modifier.weight(1f).clickable { if (!active) onNav(route) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(26.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (active) Gold else Color.Transparent))
                    Icon(icon, label, tint = if (active) Gold else Muted, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** nav 4 · small outlined text/icon tabs. */
@Composable
private fun NavTextTabs(current: String, onNav: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface1)) {
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
            NAV_ITEMS.forEach { (route, label, icon) ->
                val active = route == current
                Column(Modifier.weight(1f).clickable { if (!active) onNav(route) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(icon, label, tint = if (active) Gold else Muted, modifier = Modifier.size(22.dp))
                    JText(label, 11, if (active) Gold else Muted, if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun IfaceHeader(title: String, onBack: () -> Unit) {
    val cfg = curCfg()
    if (cfg.corner <= 1) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(24.dp)) }
            JText(title, 28, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp))
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Surface2, CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(22.dp)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { JText(title, 18, Ivory, FontWeight.SemiBold) }
            Spacer(Modifier.size(42.dp))
        }
    }
}
