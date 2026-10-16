package com.jarvis.app.ui.screens

import com.jarvis.app.data.AppLog
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.jarvis.app.audio.AudioRouter
import com.jarvis.app.audio.Route
import com.jarvis.app.data.ImageUtil
import com.jarvis.app.data.Langs
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.CallEngine
import com.jarvis.app.live.CallHost
import com.jarvis.app.live.LiveClient
import com.jarvis.app.live.ScreenShareService
import com.jarvis.app.live.WakeService
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import java.util.concurrent.Executors

private fun has(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

@Composable
fun LiveVoiceScreen(onClose: () -> Unit, onChat: () -> Unit, onApi: () -> Unit) {
    val ctx = LocalContext.current
    val live = CallHost.client
    var closed by remember { mutableStateOf(false) }
    var hadCall by remember { mutableStateOf(false) }
    val close: () -> Unit = { if (!closed) { closed = true; onClose() } }
    LaunchedEffect(live) { if (live != null) hadCall = true else if (hadCall) close() }              // the call was ended by voice ("end call") or from the bubble
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) CallHost.begin(ctx) else close()
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)                           // draw the screen first: no freeze when the mic button is tapped
        if (CallHost.client != null) return@LaunchedEffect      // coming back from the floating bubble: the call is already running
        if (has(ctx, Manifest.permission.RECORD_AUDIO)) CallHost.begin(ctx) else micPerm.launch(Manifest.permission.RECORD_AUDIO)
    }
    BackHandler { CallHost.end(ctx, showPopup = false); close() }
    if (live == null) {
        Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) { SiriOrb(160.dp, 0.1f, 2) }
        return
    }
    LiveCallContent(live, close, { CallHost.end(ctx, showPopup = false); close(); onChat() }, onApi)
}

@Composable
private fun LiveCallContent(live: CallEngine, onClose: () -> Unit, onChat: () -> Unit, onApi: () -> Unit) {
    val ctx = LocalContext.current
    var cam by remember { mutableStateOf(0) }      // 0 off, 1 front, 2 back
    var share by remember { mutableStateOf(false) }
    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }

    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) cam = 1 }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val btPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val d = r.data
        if (r.resultCode == Activity.RESULT_OK && d != null) {
            ScreenShareService.frameSink = { live.sendJpeg(it) }
            ScreenShareService.start(ctx, r.resultCode, d)
            share = true
        }
    }
    LaunchedEffect(Unit) { while (true) { overlayOk = Settings.canDrawOverlays(ctx); kotlinx.coroutines.delay(1000) } }
    DisposableEffect(Unit) {
        // Leaving the screen does NOT hang up (Home keeps the call alive as a bubble); only the screen-share hook is released.
        onDispose { ScreenShareService.frameSink = null }
    }

    val mode = when {
        live.state != CallEngine.State.LIVE -> 2
        live.speaking -> 3
        else -> 1
    }
    val level = if (live.speaking) live.outLevel else live.micLevel
    val title = when (live.state) {
        CallEngine.State.CONNECTING -> "Connecting…"
        CallEngine.State.ERROR -> "Call stopped"
        CallEngine.State.IDLE -> "Starting…"
        CallEngine.State.LIVE -> if (live.muted) "Muted" else if (live.speaking) "Speaking…" else "Listening…"
    }

    Column(Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            RoundBtn(Icons.Outlined.Close, 42, Surface1, Ivory) { CallHost.end(ctx, showPopup = false); onClose() }
            Row(
                Modifier.clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = 0.12f))
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(if (live.state == CallEngine.State.LIVE) Gold else Muted, CircleShape))
                JText("LIVE · ${Prefs.assistantName.uppercase()}", 11, Gold, FontWeight.Medium, ls = 1.6f)
            }
            RoundBtn(Icons.Outlined.ChatBubbleOutline, 42, Surface1, Ivory) { onChat() }
        }
        // language chips: tap at any time, the call updates instantly
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Langs.all.forEach { l ->
                val on = Prefs.lang == l.code
                Box(
                    Modifier.clip(RoundedCornerShape(18.dp))
                        .then(if (on) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)))
                        .clickable { if (!on) live.switchLanguage(l.code) }.padding(horizontal = 14.dp, vertical = 7.dp)
                ) { JText(l.label, 12, if (on) Bg else Gold, if (on) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
        if (!overlayOk) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.12f))
                    .clickable { try { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))) } catch (e: Exception) { AppLog.w(e); } }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.PictureInPicture, null, tint = Gold, modifier = Modifier.size(15.dp))
                JText("Keep this call floating when you leave the app · Allow", 11, GoldLight)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                SiriOrb(240.dp, level, mode)
                VoiceWave(level, live.state == CallEngine.State.LIVE && (live.speaking || !live.muted))
                JText(title, 22, Ivory, FontWeight.Medium, serif = true)
                Column(Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (live.userText.isNotBlank()) JText("“${live.userText.trim()}”", 14, GoldLight, lh = 21, align = TextAlign.Center)
                    if (live.aiText.isNotBlank()) JText(live.aiText.trim(), 14, Ivory, lh = 21, align = TextAlign.Center)
                }
                live.error?.let {
                    JText(it, 12, Color(0xFFE0665C), align = TextAlign.Center, lh = 18)
                    if (live.state == CallEngine.State.ERROR) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton("Retry", Icons.Outlined.Refresh, { live.start() })
                            com.jarvis.app.data.CustomProviders.list.firstOrNull { com.jarvis.app.data.CustomProviders.canCall(it) }?.let { p ->
                                OutlineButton("Call with ${p.name}", Icons.Outlined.SwapHoriz, { Prefs.callProvider = p.id; Prefs.save(); CallHost.end(ctx, showPopup = false); CallHost.begin(ctx) })
                            }
                            if (Prefs.apiKey.isBlank() || !ModelCatalog.ready) OutlineButton("API settings", null, onApi)
                        }
                    }
                }
                JText("${live.modelName.ifBlank { "Gemini Live" }} · ${Prefs.voiceLabel}", 11, Muted)
            }
            if (cam != 0) {
                CameraPip(front = cam == 1, onFrame = { live.sendJpeg(it) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(96.dp, 128.dp)
                        .clip(RoundedCornerShape(16.dp)).border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp)))
            }
            if (share) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFC4483F).copy(alpha = 0.9f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) { Icon(Icons.Outlined.ScreenShare, null, tint = Ivory, modifier = Modifier.size(14.dp)); JText("Sharing screen", 11, Ivory, FontWeight.Medium) }
            }
        }
        RouteChips { if (Build.VERSION.SDK_INT >= 31 && !has(ctx, Manifest.permission.BLUETOOTH_CONNECT)) btPerm.launch(Manifest.permission.BLUETOOTH_CONNECT) }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
        ) {
            RoundBtn(if (live.muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, 52, if (live.muted) Gold.copy(alpha = 0.3f) else Surface1, Ivory) { live.toggleMute() }
            RoundBtn(if (cam == 0) Icons.Outlined.VideocamOff else Icons.Outlined.Videocam, 52, if (cam != 0) Gold.copy(alpha = 0.3f) else Surface1, Ivory) {
                if (cam != 0) cam = 0 else if (has(ctx, Manifest.permission.CAMERA)) cam = 1 else camPerm.launch(Manifest.permission.CAMERA)
            }
            if (cam != 0) RoundBtn(Icons.Outlined.FlipCameraAndroid, 52, Surface1, Ivory) { cam = if (cam == 1) 2 else 1 }
            RoundBtn(Icons.Outlined.ScreenShare, 52, if (share) Gold.copy(alpha = 0.3f) else Surface1, Ivory) {
                if (share) { ScreenShareService.stop(ctx); share = false }
                else {
                    if (Build.VERSION.SDK_INT >= 33 && !has(ctx, Manifest.permission.POST_NOTIFICATIONS)) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    val mpm = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    capture.launch(mpm.createScreenCaptureIntent())
                }
            }
            RoundBtn(Icons.Outlined.Close, 64, Color(0xFFC4483F), Ivory) { CallHost.end(ctx, showPopup = false); onClose() }
        }
    }
}

@Composable
private fun CameraPip(front: Boolean, onFrame: (ByteArray) -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val owner = ctx as? LifecycleOwner
    val previewView = remember { PreviewView(ctx) }
    DisposableEffect(front) {
        var provider: ProcessCameraProvider? = null
        val exec = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            try {
                val p = future.get(); provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
                var last = 0L
                analysis.setAnalyzer(exec) { img ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - last >= 1000) {
                            last = now
                            onFrame(ImageUtil.toJpeg(img.toBitmap(), 640, 60, img.imageInfo.rotationDegrees))
                        }
                    } catch (e: Exception) { AppLog.w(e); } finally { img.close() }
                }
                p.unbindAll()
                if (owner != null) p.bindToLifecycle(owner, if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) { AppLog.w(e); }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { try { provider?.unbindAll() } catch (e: Exception) { AppLog.w(e); }; exec.shutdown() }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun RoundBtn(icon: ImageVector, size: Int, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).background(bg, CircleShape).border(1.dp, Gold.copy(alpha = 0.25f), CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (size > 60) 28.dp else 22.dp)) }
}

@Composable
private fun RouteChips(onNeedBtPermission: () -> Unit) {
    val items = buildList {
        add(Triple(Route.AUTO, "Auto", Icons.Outlined.AutoAwesome))
        add(Triple(Route.SPEAKER, "Speaker", Icons.Outlined.VolumeUp))
        add(Triple(Route.EARPIECE, "Ear", Icons.Outlined.Hearing))
        if (AudioRouter.hasBt) add(Triple(Route.BLUETOOTH, "Bluetooth", Icons.Outlined.Bluetooth))
        if (AudioRouter.hasWired) add(Triple(Route.WIRED, "Wired", Icons.Outlined.Headphones))
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        items.forEach { (r, label, icon) ->
            val on = AudioRouter.selected == r
            Row(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .then(if (on) Modifier.background(Gold.copy(alpha = 0.18f)).border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(16.dp)) else Modifier.border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(16.dp)))
                    .clickable { if (r == Route.BLUETOOTH) onNeedBtPermission(); AudioRouter.select(r) }.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(15.dp))
                JText(if (r == Route.AUTO) "Auto · ${AudioRouter.active.name.lowercase()}" else label, 11, if (on) GoldLight else Muted)
            }
        }
    }
}
