package com.jarvis.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jarvis.app.data.Greeting
import com.jarvis.app.data.GreetingPlayer
import com.jarvis.app.data.Startup
import com.jarvis.app.data.DeviceStats
import com.jarvis.app.data.Langs
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import com.jarvis.app.ui.styles.AuroraHome
import com.jarvis.app.ui.styles.HomeUi
import com.jarvis.app.ui.styles.NovaHome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Boot { var greeted = false; var spoken = false }

@Composable
fun HomeScreen(onNav: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf(DeviceStats.read(ctx)) }
    val needsLoad = remember { !Startup.ready || Greeting.key != Greeting.currentKey() }
    var loading by remember { mutableStateOf(needsLoad) }
    var pct by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("Starting") }
    var stage by remember { mutableStateOf(if (Boot.greeted) 5 else 0) }
    var talking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { while (true) { stats = withContext(Dispatchers.IO) { DeviceStats.read(ctx) }; delay(5000) } }

    fun greet() { talking = true; GreetingPlayer.play(scope) { talking = false } }

    LaunchedEffect(Unit) {
        if (needsLoad) {
            Startup.prepare(ctx) { p, l -> pct = p; label = l }
            // Loading is at 100%: the voice starts right now, together with the dashboard animation
            if (Prefs.greetOnHome) { Boot.spoken = true; greet() }
            delay(350); loading = false
        } else if (Prefs.greetOnHome && !Boot.spoken) { Boot.spoken = true; greet() }
        if (!Boot.greeted) { delay(100); stage = 1; delay(380); stage = 2; delay(380); stage = 3; delay(380); stage = 4; delay(380); stage = 5; Boot.greeted = true }
    }
    DisposableEffect(Unit) { onDispose { GreetingPlayer.stop() } }

    if (loading) LoadingDialog(pct, label)

    val ui = HomeUi(stats, stage, talking, { greet() }, onNav)
    ScreenScaffold("home", onNav) {
        if (Palettes.style != 0) Box(Modifier.weight(1f).fillMaxWidth()) { com.jarvis.app.ui.styles.StyledHome(ui) }
        else Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val col = this
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    JText("Good to see you", 13, Muted)
                    JText(Prefs.userName.ifBlank { "Welcome" }, 26, Ivory, FontWeight.Medium, serif = true)
                }
                Box(Modifier.size(44.dp).background(Surface1, CircleShape).border(1.dp, Gold.copy(alpha = 0.25f), CircleShape).clickable { greet() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.VolumeUp, null, tint = Gold, modifier = Modifier.size(20.dp))
                }
            }
            // ---- animated boot: orb + rotating rings ----
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                col.AnimatedVisibility(stage >= 1, enter = scaleIn(tween(900), initialScale = 0.3f) + fadeIn(tween(900))) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { greet() }) {
                        BootRings(stage < 5 || talking)
                        SiriOrb(190.dp, if (talking) 0.5f else 0.1f, if (talking) 3 else if (stage < 5) 2 else 0)
                    }
                }
            }
            AnimatedVisibility(stage >= 2, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    JText("${Prefs.assistantName.uppercase()} SYSTEM ONLINE", 14, Gold, FontWeight.Medium, ls = 3f)
                    JText(if (ModelCatalog.ok == true) "Gemini link · connected" else if (Prefs.apiKey.isBlank()) "Gemini link · add API key in Profile" else "Gemini link · ${ModelCatalog.status.ifBlank { "connecting…" }}", 11, Muted)
                    JText("Voice · ${Prefs.voiceLabel} · ${Langs.of(Prefs.lang).label}", 11, Muted)
                    Greeting.error?.let { JText("Voice unavailable: ${it.take(110)}", 10, Color(0xFFE0665C), lh = 14) }
                }
            }
            AnimatedVisibility(stage >= 2, enter = fadeIn(tween(600))) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(64.dp).shadow(18.dp, CircleShape, ambientColor = Gold, spotColor = Gold).background(goldBrush(), CircleShape).clickable { onNav("live") },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Mic, "Live call", tint = Bg, modifier = Modifier.size(26.dp)) }
                }
            }
            // ---- real device stats (equal tiles) ----
            AnimatedVisibility(stage >= 3, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    JText("DEVICE STATUS", 11, Gold, FontWeight.Medium, ls = 2.2f)
                    val hot = stats.tempC >= 40f
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(Icons.Outlined.BatteryFull, "Battery", stats.battery.toFloat(), "%", if (stats.charging) "Charging" else "On battery", stats.battery / 100f, stats.battery < 20, Modifier.weight(1f))
                        StatTile(Icons.Outlined.Thermostat, "Heat", stats.tempC, "°C", "Thermal: ${stats.thermal}", (stats.tempC / 50f).coerceIn(0f, 1f), hot, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(Icons.Outlined.Storage, "Storage free", stats.storageFreeGb, " GB", "of ${"%.0f".format(stats.storageTotalGb)} GB", 1f - stats.storageFreeGb / stats.storageTotalGb.coerceAtLeast(1f), stats.storageFreeGb < 3f, Modifier.weight(1f))
                        StatTile(Icons.Outlined.Memory, "Memory free", stats.ramFreeGb, " GB", "of ${"%.1f".format(stats.ramTotalGb)} GB", 1f - stats.ramFreeGb / stats.ramTotalGb.coerceAtLeast(1f), stats.ramFreeGb < 0.6f, Modifier.weight(1f))
                    }
                }
            }
            AnimatedVisibility(stage >= 4, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    JText("QUICK ACTIONS", 11, Gold, FontWeight.Medium, ls = 2.2f)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QTile(Icons.Outlined.Call, "Live Call", "Talk to Jarvis", Modifier.weight(1f)) { onNav("live") }
                        QTile(Icons.Outlined.ChatBubbleOutline, "Chat", "Text & images", Modifier.weight(1f)) { onNav("chat") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QTile(Icons.Outlined.GridView, "Control", "Phone controls", Modifier.weight(1f)) { onNav("control") }
                        QTile(Icons.Outlined.RecordVoiceOver, "Voices", "16 voices", Modifier.weight(1f)) { onNav("voices") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BootRings(fast: Boolean) {
    val inf = rememberInfiniteTransition(label = "rings")
    val a by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(if (fast) 2200 else 9000, easing = LinearEasing)), label = "a")
    Canvas(Modifier.size(250.dp)) {
        val c = center
        for (i in 0 until 3) {
            val rr = size.minDimension / 2f * (0.80f + i * 0.095f)
            val dir = if (i % 2 == 0) 1f else -1f
            drawArc(
                (if (i == 1) GoldLight else Gold).copy(alpha = 0.55f - i * 0.12f), startAngle = a * dir + i * 70f, sweepAngle = 80f + i * 25f,
                useCenter = false, topLeft = Offset(c.x - rr, c.y - rr), size = Size(rr * 2, rr * 2), style = Stroke(width = 2f * density)
            )
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: Float, unit: String, sub: String, frac: Float, warn: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val v by animateFloatAsState(value, tween(1400), label = "v")
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), tween(1400), label = "f")
    val col = if (warn) Color(0xFFE0665C) else Gold
    Column(
        modifier.height(104.dp).clip(shape).background(Surface1).border(1.dp, col.copy(alpha = if (warn) 0.5f else 0.16f), shape).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = col, modifier = Modifier.size(16.dp)); JText(label, 11, Muted)
        }
        JText((if (unit == "%") "%d".format(v.toInt()) else "%.1f".format(v)) + unit, 22, Ivory, FontWeight.Medium, serif = true)
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Surface2)) {
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(col))
        }
        JText(sub, 10, Muted)
    }
}

@Composable
private fun QTile(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.height(72.dp).clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.14f), shape).clickable { onClick() }.padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        GoldIconBox(icon)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(title, 14, Ivory, FontWeight.Medium); JText(sub, 11, Muted) }
    }
}

@Composable
private fun LoadingDialog(pct: Int, label: String) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) { LoadingPanel(pct, label) }
    }
}
