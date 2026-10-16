package com.jarvis.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.Langs
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.Startup
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun StepHeader(step: Int, title: String, sub: String) {
    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..4) Box(Modifier.width(if (i == step) 28.dp else 14.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) Gold else Surface2))
        }
        JText("STEP $step OF 4", 11, Gold, FontWeight.Medium, ls = 2.4f, modifier = Modifier.padding(top = 10.dp))
        JText(title, 28, Ivory, FontWeight.Medium, serif = true)
        JText(sub, 13, Muted, lh = 19)
    }
}

@Composable
fun LanguageScreen(onNext: () -> Unit) {
    ScreenScaffold(null, {}) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepHeader(1, "Choose your language", "Jarvis will speak and listen in this language. You can change it any time.")
            Spacer(Modifier.height(6.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Langs.all.forEach { l ->
                    val on = Prefs.lang == l.code
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(if (on) Gold.copy(alpha = 0.12f) else Surface1)
                            .border(1.dp, Gold.copy(alpha = if (on) 0.8f else 0.14f), shape)
                            .clickable { Prefs.lang = l.code; Prefs.save() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        JText(l.label, 17, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f))
                        JText(l.english, 12, Muted)
                        JRadio(on)
                    }
                }
            }
        }
        Box(Modifier.padding(24.dp)) { GoldButton("Continue", Icons.Outlined.ArrowForward, onNext, Modifier.fillMaxWidth()) }
    }
}

@Composable
fun PermissionsScreen(onNext: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        val owner = ctx as? LifecycleOwner
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    val runtime = buildList {
        add(Manifest.permission.RECORD_AUDIO); add(Manifest.permission.CAMERA)
        add(Manifest.permission.ACCESS_COARSE_LOCATION); add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val rows = remember(tick) {
        listOf(
            Triple("Microphone", "Talk to Jarvis, live calls", granted(Manifest.permission.RECORD_AUDIO)),
            Triple("Camera", "Show things to Jarvis", granted(Manifest.permission.CAMERA)),
            Triple("Location", "Today's weather", granted(Manifest.permission.ACCESS_COARSE_LOCATION)),
            Triple("Notifications", "Screen-share & wake-word status", Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)),
            Triple("Nearby devices", "Bluetooth headset audio", Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_CONNECT))
        )
    }
    val overlay = remember(tick) { Settings.canDrawOverlays(ctx) }
    val write = remember(tick) { DeviceControl.canWriteSettings(ctx) }
    val dnd = remember(tick) { DeviceControl.dndAllowed(ctx) }

    ScreenScaffold(null, {}) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepHeader(2, "Permissions", "Allow these so every feature works. You can change them later in Control.")
            Spacer(Modifier.height(4.dp))
            val card = RoundedCornerShape(18.dp)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 16.dp)) {
                rows.forEachIndexed { i, (n, d, ok) -> if (i > 0) Hairline(0.1f); PRow(n, d, ok, null) }
            }
            JText("SPECIAL ACCESS (opens Android settings)", 11, Gold, FontWeight.Medium, ls = 2f, modifier = Modifier.padding(top = 6.dp))
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.16f), card).padding(horizontal = 16.dp)) {
                PRow("Display over other apps", "Jarvis opens over any screen (wake word)", overlay) {
                    DeviceControl.open(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                }
                Hairline(0.1f)
                PRow("Change system settings", "Brightness control", write) { DeviceControl.askWriteSettings(ctx) }
                Hairline(0.1f)
                PRow("Do Not Disturb access", "Silent mode control", dnd) { DeviceControl.setDnd(ctx, false) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GoldButton("Allow all permissions", null, { launcher.launch(runtime.toTypedArray()) }, Modifier.fillMaxWidth())
            OutlineButton("Continue", Icons.Outlined.ArrowForward, onNext, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PRow(name: String, desc: String, ok: Boolean, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (!ok && onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(name, 14, Ivory, FontWeight.Medium); JText(desc, 11, Muted) }
        Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (ok) Gold else Muted, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ApiSetupScreen(onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(Prefs.apiKey) }
    var show by remember { mutableStateOf(false) }
    ScreenScaffold(null, {}) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StepHeader(3, "Connect Gemini", "Paste your Gemini API key. Models are detected automatically from your key.")
            JField(key, { key = it }, "Gemini API key", Modifier.fillMaxWidth().padding(top = 8.dp), secret = !show, trailing = {
                Icon(Icons.Outlined.Visibility, "Show", tint = if (show) Gold else Muted, modifier = Modifier.size(20.dp).clickable { show = !show })
            })
            JText("Free key: aistudio.google.com → Get API key", 11, Muted)
            if (ModelCatalog.status.isNotEmpty()) {
                val c = when (ModelCatalog.ok) { true -> Gold; false -> Color(0xFFE0665C); else -> Muted }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.1f)).border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) { Box(Modifier.size(8.dp).background(c, CircleShape)); JText(ModelCatalog.status, 13, Ivory, lh = 19, modifier = Modifier.weight(1f)) }
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GoldButton(if (ModelCatalog.loading) "Connecting…" else "Save & Connect", Icons.Outlined.Refresh, {
                if (!ModelCatalog.loading && key.isNotBlank()) scope.launch { Prefs.apiKey = key.trim(); Prefs.save(); ModelCatalog.refresh() }
            }, Modifier.fillMaxWidth())
            OutlineButton(if (ModelCatalog.ok == true) "Continue" else "Skip for now", Icons.Outlined.ArrowForward, onNext, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var pct by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("Starting") }
    LaunchedEffect(Unit) {
        Startup.prepare(ctx) { p, l -> pct = p; label = l }
        Prefs.onboarded = true; Prefs.save()
        delay(600)
        onDone()
    }
    ScreenScaffold(null, {}) { LoadingPanel(pct, label, "Installing your system · step 4 of 4") }
}
