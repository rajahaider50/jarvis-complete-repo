package com.jarvis.app.ui.styles

import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlin.math.sin

/** PULSE: pure-black HUD look. Monospace text, sharp corners, a raised center "talk" button, tabs inside the settings page. */
@Composable
fun PText(text: String, size: Int, color: Color = Ivory, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier, align: TextAlign? = null) =
    Text(text, modifier = modifier, color = color, fontSize = size.sp, fontWeight = weight, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, textAlign = align)

private val Sharp4 = RoundedCornerShape(4.dp)

@Composable
fun PulseNav(current: String, onNav: (String) -> Unit) {
    val left = listOf(Triple("home", "HOME", Icons.Sharp.Home), Triple("chat", "AI", Icons.Sharp.AutoAwesome))
    val right = listOf(Triple("control", "CTRL", Icons.Sharp.Tune), Triple("profile", "ME", Icons.Sharp.Person))
    Box(Modifier.fillMaxWidth().height(84.dp)) {
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp).background(Bg)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gold.copy(alpha = 0.35f)))
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                left.forEach { PulseTab(it, current, onNav, Modifier.weight(1f)) }
                Spacer(Modifier.width(84.dp))
                right.forEach { PulseTab(it, current, onNav, Modifier.weight(1f)) }
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter).size(68.dp).border(2.dp, Gold, CircleShape).padding(5.dp).background(Gold, CircleShape).clickable { onNav("live") },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Sharp.Mic, "Talk", tint = Bg, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun PulseTab(t: Triple<String, String, ImageVector>, current: String, onNav: (String) -> Unit, modifier: Modifier) {
    val active = t.first == current
    Column(modifier.clickable { if (!active) onNav(t.first) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.width(26.dp).height(2.dp).background(if (active) Gold else Color.Transparent))
        Icon(t.third, t.second, tint = if (active) Gold else Muted, modifier = Modifier.size(22.dp))
        PText(t.second, 9, if (active) Gold else Muted)
    }
}

@Composable
fun PulseHeader(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable { onBack() }) {
            Icon(Icons.Sharp.ArrowBack, "Back", tint = Gold, modifier = Modifier.size(18.dp)); PText("BACK", 11, Gold, FontWeight.Bold)
        }
        PText(title.uppercase(), 22, Ivory, FontWeight.Bold)
        Box(Modifier.width(56.dp).height(3.dp).background(Gold))
    }
}

@Composable
fun PulseHome(h: HomeUi) {
    val s = h.stats
    val accent = Gold
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(0f, 6.283f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (com.jarvis.app.data.ModelCatalog.ok == true) Gold else Warn, CircleShape))
                PText(if (com.jarvis.app.data.ModelCatalog.ok == true) "ONLINE" else "NO LINK", 11, Gold, FontWeight.Bold)
            }
            PText(Prefs.assistantName.uppercase(), 11, Muted)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.clickable { h.greet() }) { SiriOrb(250.dp, if (h.talking) 0.6f else 0.12f, if (h.talking) 3 else if (h.stage < 5) 2 else 0) }
        }
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val n = 36
            val gap = size.width / n
            val amp = if (h.talking) 0.95f else 0.25f
            for (i in 0 until n) {
                val hgt = size.height * (0.12f + amp * 0.6f * (0.5f + 0.5f * sin(phase + i * 0.5f)))
                val x = gap * i + gap / 2
                drawLine(accent, Offset(x, size.height / 2 - hgt / 2), Offset(x, size.height / 2 + hgt / 2), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Square)
            }
        }
        PText("TAP ORB: BRIEFING  ·  CENTER KEY: TALK", 10, Muted, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        AnimatedVisibility(h.stage >= 2, enter = fadeIn(tween(500)) + slideInHorizontally(tween(500)) { it / 3 }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PText("// TELEMETRY", 11, Gold, FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card("BATTERY", "${s.battery}%", if (s.charging) "CHARGING" else "ON BATTERY", s.battery / 100f, s.battery < 20)
                    Card("HEAT", "%.0f°".format(s.tempC), s.thermal.uppercase(), s.tempC / 50f, s.tempC >= 40f)
                    Card("STORAGE", "%.0fG".format(s.storageFreeGb), "OF %.0fG FREE".format(s.storageTotalGb), 1f - s.storageFreeGb / s.storageTotalGb.coerceAtLeast(1f), s.storageFreeGb < 3f)
                    Card("MEMORY", "%.1fG".format(s.ramFreeGb), "OF %.1fG FREE".format(s.ramTotalGb), 1f - s.ramFreeGb / s.ramTotalGb.coerceAtLeast(1f), s.ramFreeGb < 0.6f)
                }
            }
        }
        AnimatedVisibility(h.stage >= 3, enter = fadeIn(tween(500))) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PText("// SHORTCUTS", 11, Gold, FontWeight.Bold)
                listOf(
                    listOf(Triple(Icons.Sharp.Call, "LIVE CALL", "live"), Triple(Icons.Sharp.Chat, "CHAT", "chat")),
                    listOf(Triple(Icons.Sharp.Tune, "CONTROL", "control"), Triple(Icons.Sharp.Terminal, "CONSOLE", "console"))
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (icon, label, route) ->
                            Row(Modifier.weight(1f).height(64.dp).border(1.dp, Gold.copy(alpha = 0.5f), Sharp4).clickable { h.onNav(route) }.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp)); PText(label, 12, Ivory, FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(label: String, value: String, sub: String, frac: Float, warn: Boolean) {
    val col = if (warn) Warn else Gold
    Column(Modifier.width(140.dp).border(1.dp, col.copy(alpha = 0.55f), Sharp4).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PText(label, 10, Muted)
        PText(value, 30, col, FontWeight.Bold)
        PText(sub, 9, Muted)
        Box(Modifier.fillMaxWidth().height(4.dp).background(Surface2)) { Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(col)) }
    }
}

@Composable
private fun SegmentBar(value: Float, onChange: (Float) -> Unit) {
    val n = 14
    Row(
        Modifier.fillMaxWidth().height(34.dp)
            .pointerInput(Unit) { detectTapGestures { o -> onChange((o.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) } },
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 0 until n) Box(Modifier.weight(1f).fillMaxHeight().background(if ((i + 1f) / n <= value + 0.001f) Gold else Surface2))
    }
}

@Composable
fun PulseControl(onNav: (String) -> Unit) {
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
    val rows = listOf(
        Triple(Icons.Sharp.FlashlightOn, "TORCH", DeviceControl.torchOn), Triple(Icons.Sharp.Wifi, "WI-FI", wifi),
        Triple(Icons.Sharp.Bluetooth, "BLUETOOTH", bt), Triple(Icons.Sharp.DoNotDisturbOn, "SILENT", dnd),
        Triple(Icons.Sharp.AirplanemodeActive, "AIRPLANE", air), Triple(Icons.Sharp.LocationOn, "LOCATION", false)
    )
    val actions = listOf<() -> Unit>(
        { DeviceControl.setTorch(ctx, !DeviceControl.torchOn) }, { DeviceControl.openWifi(ctx) }, { DeviceControl.openBluetooth(ctx) },
        { DeviceControl.setDnd(ctx, !dnd); tick++ }, { DeviceControl.openAirplane(ctx) }, { DeviceControl.openLocation(ctx) }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { PText("CONTROL", 24, Ivory, FontWeight.Bold); Box(Modifier.width(56.dp).height(3.dp).background(Gold)) }
        rows.forEachIndexed { i, (icon, label, on) ->
            Row(Modifier.fillMaxWidth().height(60.dp).border(1.dp, Gold.copy(alpha = if (on) 0.6f else 0.2f), Sharp4).clickable { actions[i]() }, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(if (on) Gold else Color.Transparent))
                Spacer(Modifier.width(14.dp))
                Icon(icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                PText(label, 13, Ivory, FontWeight.Bold, Modifier.weight(1f))
                PText(if (on) "ON" else "OFF", 11, if (on) Gold else Muted)
                Spacer(Modifier.width(14.dp))
            }
        }
        PText("// LEVELS", 11, Gold, FontWeight.Bold, Modifier.padding(top = 6.dp))
        PText("MEDIA ${(media * 100).toInt()}%", 11, Muted)
        SegmentBar(media) { media = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
        PText("RINGER ${(ring * 100).toInt()}%", 11, Muted)
        SegmentBar(ring) { ring = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_RING, it) }
        PText(if (canWrite) "BRIGHTNESS ${(bright * 100).toInt()}%" else "BRIGHTNESS · TAP BAR TO ALLOW", 11, Muted)
        SegmentBar(bright) { if (canWrite) { bright = it; DeviceControl.setBrightness(ctx, it) } else DeviceControl.askWriteSettings(ctx) }
        Row(Modifier.fillMaxWidth().border(1.dp, Gold.copy(alpha = 0.4f), Sharp4).clickable { onNav("access") }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Sharp.Accessibility, null, tint = Gold, modifier = Modifier.size(22.dp))
            PText("SCREEN CONTROL  " + if (com.jarvis.app.live.JarvisAccessibilityService.enabled) "[ON]" else "[OFF] TAP TO SET UP", 11, Ivory, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun PulseSettings(onNav: (String) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val name = Prefs.userName.ifBlank { "UNNAMED" }
    data class R(val icon: ImageVector, val title: String, val sub: String, val route: String)
    val groups = listOf(
        "ASSISTANT" to listOf(R(Icons.Sharp.Code, "WORKSPACE", "CODE, FILES & DOCUMENTS", "code"), R(Icons.Sharp.Extension, "SKILLS", "TEACH THE AGENT", "skills"), R(Icons.Sharp.AutoAwesome, "ASSISTANT & WAKE", Prefs.assistantName, "assistant"), R(Icons.Sharp.RecordVoiceOver, "VOICE", Prefs.voiceLabel, "voices"), R(Icons.Sharp.Memory, "MEMORY", "NAME & INSTRUCTIONS", "memory")),
        "LOOK" to listOf(R(Icons.Sharp.Palette, "COLORS & ORB", "THEME", "themes"), R(Icons.Sharp.Layers, "INTERFACE STYLE", "SWITCH THE WHOLE APP", "uistyle")),
        "SYSTEM" to listOf(R(Icons.Sharp.Key, "GEMINI API", "KEY & MODELS", "api"), R(Icons.Sharp.Security, "ACCESS", "PERMISSIONS & SCREEN CONTROL", "access"), R(Icons.Sharp.Terminal, "CONSOLE", "ERRORS & EVENTS", "console"))
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { PText("PROFILE", 24, Ivory, FontWeight.Bold); Box(Modifier.width(56.dp).height(3.dp).background(Gold)) }
        Column(Modifier.fillMaxWidth().border(1.dp, Gold.copy(alpha = 0.6f), Sharp4).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PText("OPERATOR", 10, Muted); PText(name.uppercase(), 24, Gold, FontWeight.Bold); PText("UNIT ${Prefs.assistantName.uppercase()}  ·  VOICE ${Prefs.voiceLabel.uppercase()}", 10, Muted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            groups.forEachIndexed { i, g ->
                Column(Modifier.clickable { tab = i }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PText(g.first, 12, if (tab == i) Gold else Muted, FontWeight.Bold)
                    Box(Modifier.width(34.dp).height(2.dp).background(if (tab == i) Gold else Color.Transparent))
                }
            }
        }
        groups[tab].second.forEachIndexed { i, r ->
            Row(Modifier.fillMaxWidth().border(1.dp, Gold.copy(alpha = 0.25f), Sharp4).clickable { onNav(r.route) }.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PText("%02d".format(i + 1), 12, Gold, FontWeight.Bold)
                Icon(r.icon, null, tint = Ivory, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { PText(r.title, 13, Ivory, FontWeight.Bold); PText(r.sub, 9, Muted) }
                Icon(Icons.Sharp.ArrowForward, null, tint = Gold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
