package com.jarvis.app.ui.styles

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

/** AURORA · Home: one big hero card with the orb and a "Talk now" pill, quick chips, ring gauges. */
@Composable
fun AuroraHome(h: HomeUi) {
    val shape = RoundedCornerShape(32.dp)
    val name = Prefs.userName.ifBlank { "there" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Brush.linearGradient(listOf(GoldLight, GoldDark)), CircleShape), contentAlignment = Alignment.Center) {
                    JText(name.take(1).uppercase(), 19, Bg, FontWeight.SemiBold)
                }
                Column { JText("Hello,", 12, Muted); JText(name, 19, Ivory, FontWeight.SemiBold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassCircle(Icons.Rounded.VolumeUp) { h.greet() }
                GlassCircle(Icons.Rounded.Terminal) { h.onNav("console") }
            }
        }
        AnimatedVisibility(h.stage >= 1, enter = fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.85f)) {
            Box(
                Modifier.fillMaxWidth().clip(shape)
                    .background(Brush.verticalGradient(listOf(Gold.copy(alpha = 0.30f).compositeOver(Surface1), Surface1)))
                    .border(1.dp, Gold.copy(alpha = 0.3f), shape).padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.clickable { h.greet() }) { SiriOrb(170.dp, if (h.talking) 0.5f else 0.1f, if (h.talking) 3 else if (h.stage < 5) 2 else 0) }
                    JText("${Prefs.assistantName} is online", 19, Ivory, FontWeight.SemiBold)
                    JText(gemStatus(), 12, Muted)
                    Row(
                        Modifier.clip(RoundedCornerShape(30.dp)).background(goldBrush()).clickable { h.onNav("live") }.padding(horizontal = 28.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Mic, null, tint = Bg, modifier = Modifier.size(22.dp))
                        JText("Talk now", 15, Bg, FontWeight.SemiBold)
                    }
                }
            }
        }
        AnimatedVisibility(h.stage >= 2, enter = fadeIn(tween(600))) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple(Icons.Rounded.ChatBubble, "Chat", "chat"), Triple(Icons.Rounded.Tune, "Control", "control"),
                    Triple(Icons.Rounded.RecordVoiceOver, "Voices", "voices"), Triple(Icons.Rounded.Palette, "Look", "themes"),
                    Triple(Icons.Rounded.Layers, "Interface", "uistyle")
                ).forEach { (icon, label, route) ->
                    Row(
                        Modifier.clip(RoundedCornerShape(24.dp)).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                            .clickable { h.onNav(route) }.padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                    ) { Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp)); JText(label, 13, Ivory, FontWeight.Medium) }
                }
            }
        }
        AnimatedVisibility(h.stage >= 3, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }) {
            val s = h.stats
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Your phone")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RingTile("Battery", "${s.battery}%", if (s.charging) "Charging" else "On battery", s.battery / 100f, s.battery < 20, Icons.Rounded.BatteryFull, Modifier.weight(1f))
                    RingTile("Heat", "%.0f°C".format(s.tempC), s.thermal, (s.tempC / 50f), s.tempC >= 40f, Icons.Rounded.Thermostat, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RingTile("Storage free", "%.0f GB".format(s.storageFreeGb), "of %.0f GB".format(s.storageTotalGb), 1f - s.storageFreeGb / s.storageTotalGb.coerceAtLeast(1f), s.storageFreeGb < 3f, Icons.Rounded.Storage, Modifier.weight(1f))
                    RingTile("Memory free", "%.1f GB".format(s.ramFreeGb), "of %.1f GB".format(s.ramTotalGb), 1f - s.ramFreeGb / s.ramTotalGb.coerceAtLeast(1f), s.ramFreeGb < 0.6f, Icons.Rounded.Memory, Modifier.weight(1f))
                }
            }
        }
    }
}

/** AURORA · Control: round toggle buttons, big glass sliders, accessibility switch. */
@Composable
fun AuroraControl(onNav: (String) -> Unit) {
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
    val shape = RoundedCornerShape(28.dp)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        JText("Control", 30, Ivory, FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        val toggles: List<Triple<ImageVector, String, Pair<Boolean, () -> Unit>>> = listOf(
            Triple(Icons.Rounded.FlashlightOn, "Torch", DeviceControl.torchOn to { DeviceControl.setTorch(ctx, !DeviceControl.torchOn); Unit }),
            Triple(Icons.Rounded.Wifi, "Wi-Fi", wifi to { DeviceControl.openWifi(ctx) }),
            Triple(Icons.Rounded.Bluetooth, "Bluetooth", bt to { DeviceControl.openBluetooth(ctx) }),
            Triple(Icons.Rounded.DoNotDisturbOn, "Silent", dnd to { DeviceControl.setDnd(ctx, !dnd); tick++; Unit }),
            Triple(Icons.Rounded.AirplanemodeActive, "Airplane", air to { DeviceControl.openAirplane(ctx) }),
            Triple(Icons.Rounded.LocationOn, "Location", false to { DeviceControl.openLocation(ctx) })
        )
        toggles.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { (icon, label, st) ->
                    Column(Modifier.width(92.dp).clickable { st.second() }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(if (st.first) goldBrush() else Brush.linearGradient(listOf(Surface2, Surface2)))
                                .border(1.dp, Gold.copy(alpha = if (st.first) 0f else 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(icon, label, tint = if (st.first) Bg else Muted, modifier = Modifier.size(28.dp)) }
                        JText(label, 12, if (st.first) Ivory else Muted, FontWeight.Medium)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.18f), shape).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AuroraSlider(Icons.Rounded.VolumeUp, "Media", media) { media = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
            AuroraSlider(Icons.Rounded.NotificationsActive, "Ringer", ring) { ring = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_RING, it) }
            AuroraSlider(Icons.Rounded.LightMode, if (canWrite) "Brightness" else "Brightness · tap to allow", bright) {
                if (canWrite) { bright = it; DeviceControl.setBrightness(ctx, it) } else DeviceControl.askWriteSettings(ctx)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.18f), shape).clickable { onNav("access") }.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Accessibility, null, tint = Gold, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                JText("Screen control", 15, Ivory, FontWeight.SemiBold)
                JText(if (JarvisAccessibilityService.enabled) "On · the assistant can read and tap the screen" else "Off · tap to allow in Access & permissions", 11, Muted)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AuroraSlider(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp)); JText(label, 14, Ivory, FontWeight.Medium); Spacer(Modifier.weight(1f)); JText("${(value * 100).toInt()}%", 12, Muted)
        }
        Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = GoldLight, activeTrackColor = Gold, inactiveTrackColor = Surface2))
    }
}

/** AURORA · Settings: every area is its own big tile that opens its own page. */
@Composable
fun AuroraSettings(onNav: (String) -> Unit) {
    val name = Prefs.userName.ifBlank { "Your name" }
    val tiles = listOf(
        Triple(Icons.Rounded.Code, "Workspace" to "Code, files & documents", "code"),
        Triple(Icons.Rounded.Extension, "Skills" to "Teach the agent", "skills"),
        Triple(Icons.Rounded.AutoAwesome, "Assistant" to "Name & wake word", "assistant"),
        Triple(Icons.Rounded.RecordVoiceOver, "Voice" to "${Prefs.voiceLabel}", "voices"),
        Triple(Icons.Rounded.Palette, "Appearance" to "Colors & orb", "themes"),
        Triple(Icons.Rounded.Layers, "Interface" to "Switch the whole design", "uistyle"),
        Triple(Icons.Rounded.Memory, "Memory" to "Name & instructions", "memory"),
        Triple(Icons.Rounded.Key, "Gemini API" to "Key & models", "api"),
        Triple(Icons.Rounded.Security, "Access" to "Permissions & screen control", "access"),
        Triple(Icons.Rounded.Terminal, "Console" to "Errors & events", "console")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        JText("Settings", 30, Ivory, FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(Brush.linearGradient(listOf(GoldLight, GoldDark)), CircleShape), contentAlignment = Alignment.Center) {
                JText(name.take(1).uppercase(), 26, Bg, FontWeight.Bold)
            }
            Column { JText(name, 20, Ivory, FontWeight.SemiBold); JText("${Prefs.assistantName} · ${Prefs.voiceLabel}", 12, Muted) }
        }
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { (icon, text, route) ->
                    val shape = RoundedCornerShape(26.dp)
                    Column(
                        Modifier.weight(1f).height(124.dp).clip(shape)
                            .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.16f).compositeOver(Surface1), Surface1)))
                            .border(1.dp, Gold.copy(alpha = 0.2f), shape).clickable { onNav(route) }.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(Modifier.size(40.dp).background(Gold.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Gold, modifier = Modifier.size(22.dp)) }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(text.first, 15, Ivory, FontWeight.SemiBold); JText(text.second, 11, Muted, lh = 14) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
