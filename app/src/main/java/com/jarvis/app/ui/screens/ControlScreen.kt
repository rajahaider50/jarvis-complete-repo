package com.jarvis.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

@Composable
fun ControlScreen(onNav: (String) -> Unit) {
    if (Palettes.style != 0) {
        ScreenScaffold("control", onNav) {
            Box(Modifier.weight(1f).fillMaxWidth()) { com.jarvis.app.ui.styles.StyledControl(onNav) }
        }
        return
    }
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        DeviceControl.init(ctx)
        val owner = ctx as? LifecycleOwner
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }
    val wifi = remember(tick) { DeviceControl.wifiOn(ctx) }
    val bt = remember(tick) { DeviceControl.bluetoothOn(ctx) }
    val air = remember(tick) { DeviceControl.airplaneOn(ctx) }
    val dnd = remember(tick) { DeviceControl.dndOn(ctx) }
    val dndOk = remember(tick) { DeviceControl.dndAllowed(ctx) }
    val canWrite = remember(tick) { DeviceControl.canWriteSettings(ctx) }
    var media by remember(tick) { mutableStateOf(DeviceControl.volume(ctx, AudioManager.STREAM_MUSIC)) }
    var ring by remember(tick) { mutableStateOf(DeviceControl.volume(ctx, AudioManager.STREAM_RING)) }
    var bright by remember(tick) { mutableStateOf(DeviceControl.brightness(ctx)) }
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    ScreenScaffold("control", onNav) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JText("CONTROL CENTER", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JText("Phone Control", 28, Ivory, FontWeight.Medium, serif = true)
                JText("Real controls for this phone", 13, Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(Icons.Outlined.FlashlightOn, "Flashlight", if (DeviceControl.torchOn) "On" else "Off", DeviceControl.torchOn, Modifier.weight(1f)) { DeviceControl.setTorch(ctx, !DeviceControl.torchOn) }
                Tile(Icons.Outlined.Wifi, "Wi-Fi", if (wifi) "On" else "Off · tap to open", wifi, Modifier.weight(1f)) { DeviceControl.openWifi(ctx) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(Icons.Outlined.Bluetooth, "Bluetooth", if (bt) "On" else "Off · tap to open", bt, Modifier.weight(1f)) { DeviceControl.openBluetooth(ctx) }
                Tile(Icons.Outlined.DoNotDisturbOn, "Do Not Disturb", if (!dndOk) "Tap to allow" else if (dnd) "On" else "Off", dnd, Modifier.weight(1f)) { DeviceControl.setDnd(ctx, !dnd); tick++ }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(Icons.Outlined.AirplanemodeActive, "Airplane mode", if (air) "On" else "Off · tap to open", air, Modifier.weight(1f)) { DeviceControl.openAirplane(ctx) }
                Tile(Icons.Outlined.LocationOn, "Location", "Tap to open", false, Modifier.weight(1f)) { DeviceControl.openLocation(ctx) }
            }
            val card = RoundedCornerShape(20.dp)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.18f), card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SliderRow(Icons.Outlined.VolumeUp, "Media volume", media) { media = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_MUSIC, it) }
                SliderRow(Icons.Outlined.NotificationsActive, "Ringer volume", ring) { ring = it; DeviceControl.setVolume(ctx, AudioManager.STREAM_RING, it) }
                SliderRow(Icons.Outlined.LightMode, if (canWrite) "Brightness" else "Brightness (tap to allow)", bright) {
                    if (canWrite) { bright = it; DeviceControl.setBrightness(ctx, it) } else DeviceControl.askWriteSettings(ctx)
                }
            }
            JText("PERMISSIONS", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.14f), card).padding(horizontal = 16.dp, vertical = 4.dp)) {
                PermRow("Microphone", granted(Manifest.permission.RECORD_AUDIO))
                Hairline(0.1f)
                PermRow("Camera", granted(Manifest.permission.CAMERA))
                Hairline(0.1f)
                PermRow("Notifications", Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS))
                Hairline(0.1f)
                PermRow("Change system settings", canWrite) { DeviceControl.askWriteSettings(ctx) }
                Hairline(0.1f)
                PermRow("Do Not Disturb access", dndOk) { DeviceControl.setDnd(ctx, dnd); tick++ }
            }
            val perms = buildList { add(Manifest.permission.RECORD_AUDIO); add(Manifest.permission.CAMERA); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }
            GoldButton("Grant mic, camera & notifications", null, { permLauncher.launch(perms.toTypedArray()) }, Modifier.fillMaxWidth())
            OutlineButton("Open app settings", null, { DeviceControl.openAppSettings(ctx) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, title: String, sub: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.height(96.dp).clip(shape).background(if (on) Gold.copy(alpha = 0.13f) else Surface1)
            .border(1.dp, Gold.copy(alpha = if (on) 0.6f else 0.14f), shape).clickable { onClick() }.padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            JText(title, 14, Ivory, FontWeight.Medium); JText(sub, 11, if (on) Gold else Muted)
        }
    }
}

@Composable
private fun SliderRow(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp)); JText(label, 13, Ivory); Spacer(Modifier.weight(1f)); JText("${(value * 100).toInt()}%", 12, Muted)
        }
        Slider(value = value, onValueChange = onChange, colors = SliderDefaults.colors(thumbColor = Ivory, activeTrackColor = Gold, inactiveTrackColor = Surface2))
    }
}

@Composable
private fun PermRow(name: String, ok: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null && !ok) Modifier.clickable { onClick() } else Modifier).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        JText(name, 14, Ivory, modifier = Modifier.weight(1f))
        Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, tint = if (ok) Gold else Color(0xFFE0665C), modifier = Modifier.size(18.dp))
        JText(if (ok) "Allowed" else "Not allowed", 11, if (ok) Gold else Color(0xFFE0665C))
    }
}
