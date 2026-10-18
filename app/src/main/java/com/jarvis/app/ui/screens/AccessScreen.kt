package com.jarvis.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.delay

private class Access(val title: String, val sub: String, val ok: Boolean, val fix: () -> Unit)

/** One place for every permission the assistant needs. Tap a row that is not allowed to fix it. */
@Composable
fun AccessScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    fun open(a: String, pkg: Boolean = true) = DeviceControl.open(ctx, if (pkg) Intent(a, Uri.parse("package:${ctx.packageName}")) else Intent(a))
    val assistant = try { Settings.Secure.getString(ctx.contentResolver, "assistant").orEmpty().startsWith(ctx.packageName) } catch (e: Exception) { false }
    val battery = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)

    val rows = remember(tick) {
        listOf(
            Access("Microphone", "Live calls and the wake word", granted(Manifest.permission.RECORD_AUDIO)) { request.launch(Manifest.permission.RECORD_AUDIO) },
            Access("Camera", "Show things to the assistant", granted(Manifest.permission.CAMERA)) { request.launch(Manifest.permission.CAMERA) },
            Access("Notifications", "Call and listening status", Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) { request.launch(Manifest.permission.POST_NOTIFICATIONS) },
            Access("Location", "Weather where you are", granted(Manifest.permission.ACCESS_COARSE_LOCATION)) { request.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            Access("Display over other apps", "Floating call bubble and the assistant panel", Settings.canDrawOverlays(ctx)) { open(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) },
            Access("Screen control (Accessibility)", "Read the screen, tap, type, scroll, Back and Home", JarvisAccessibilityService.enabled) { open(Settings.ACTION_ACCESSIBILITY_SETTINGS, false) },
            Access("Default assistant app", "Open from the power button or home gesture, like Google Assistant", assistant) { open(Settings.ACTION_VOICE_INPUT_SETTINGS, false) },
            Access("Battery: no restrictions", "Keeps the wake word alive in the background", battery) { open(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) },
            Access("Change system settings", "Brightness control", DeviceControl.canWriteSettings(ctx)) { DeviceControl.askWriteSettings(ctx) },
            Access("Answer & end phone calls", "\"Jarvis, end the call\" / \"answer the call\"", Build.VERSION.SDK_INT < 26 || granted(Manifest.permission.ANSWER_PHONE_CALLS)) { request.launch(Manifest.permission.ANSWER_PHONE_CALLS) },
            Access("Lock the screen (device admin)", "\"Jarvis, lock the screen\". Only the lock right, nothing else. Remove it here before uninstalling the app", com.jarvis.app.assistant.DeviceAdmin.isActive(ctx)) { ctx.startActivity(com.jarvis.app.assistant.DeviceAdmin.activateIntent(ctx)) },
            Access("Do Not Disturb access", "Silent mode control", DeviceControl.dndAllowed(ctx)) { open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, false) }
        )
    }
    val shape = RoundedCornerShape(20.dp)
    ScreenScaffold(null, {}) {
        SubHeader("Access & permissions", onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JText("${rows.count { it.ok }} of ${rows.size} allowed. Tap a red row to allow it.", 12, Muted)
            Column(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.14f), shape).padding(horizontal = 16.dp)) {
                rows.forEachIndexed { i, r ->
                    if (i > 0) Hairline(0.1f)
                    Row(
                        Modifier.fillMaxWidth().then(if (!r.ok) Modifier.clickable { r.fix() } else Modifier).padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(r.title, 14, Ivory, FontWeight.Medium); JText(r.sub, 11, Muted, lh = 15) }
                        Icon(if (r.ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, tint = if (r.ok) Gold else Color(0xFFE0665C), modifier = Modifier.size(20.dp))
                    }
                }
            }
            JText("Google's own \"OK Google\" hotword is private to Google. The default assistant app + the offline wake word in Assistant settings are the official ways for another app.", 11, Muted, lh = 16)

            // Round 3 / B9: keep the accessibility service (and the call) alive on aggressive OEM skins (MIUI/HyperOS,
            // EMUI, ColorOS…). Without this the service is killed in the background and every cursor/tap tool then
            // answers "service OFF".
            Column(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.14f), shape).padding(16.dp)) {
                JText("Keep JARVIS alive (MIUI / HyperOS, EMUI, ColorOS, One UI)", 13, Gold, FontWeight.Medium)
                JText("These phones kill background apps and turn the accessibility service off. Do all four:", 11, Muted, lh = 16)
                JText("1. Battery > no restrictions (red row above).\n2. App info > Battery > Unrestricted / \"No restrictions\".\n3. Autostart / \"Auto-launch\": allow JARVIS (Xiaomi: Security app > Permissions > Autostart; Huawei: App launch; Oppo/Realme: Startup manager).\n4. Lock JARVIS in the recents screen (padlock) so \"clear all\" does not kill it.", 11, Ivory, lh = 17)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("App info", null, { open(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) }, Modifier.weight(1f))
                    OutlineButton("Battery", null, { open(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
