package com.jarvis.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.live.CursorController
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.CallHost
import com.jarvis.app.live.Diag
import com.jarvis.app.live.MicCaptureHub
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.GoldButton
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.OutlineButton
import com.jarvis.app.ui.components.ScreenScaffold
import com.jarvis.app.ui.components.SubHeader
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Diagnostics (bugfix guide Phase 1). One screen with the live evidence: microphone state + which app holds it,
 * websocket state, playback underruns, server interrupts, reconnects, tool calls, frames sent/dropped and cursor
 * windows. Plus an isolation "Cursor Test" (draw a cursor at the centre then the 4 corners, then clear) and a log
 * dump you can copy or save to a file.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, onNav: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    val stamp = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }

    ScreenScaffold(null, {}) {
        SubHeader("Diagnostics", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                item { Card("Microphone") {
                    Row(Modifier.fillMaxWidth()) { JText("state", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.micState, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("source", 12, Muted, modifier = Modifier.weight(1f)); JText(if (MicCaptureHub.isRunning) MicCaptureHub.sourceName else Diag.micSource, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("level (peak)", 12, Muted, modifier = Modifier.weight(1f)); JText(String.format(Locale.US, "%.3f", Diag.micLevel), 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("silent now", 12, Muted, modifier = Modifier.weight(1f)); JText(if (MicCaptureHub.isRunning) (if (MicCaptureHub.silent) "YES" else "no") else "-", 12, if (MicCaptureHub.silent) Gold else Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("zero-runs / recoveries", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.micZeroRuns} / ${Diag.micRecoveries}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("opens / releases", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.micOpens} / ${Diag.micReleases}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("another app on mic", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.foreignRecorder, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("echo cancel (AEC)", 12, Muted, modifier = Modifier.weight(1f)); JText("avail=${MicCaptureHub.aecAvailable} on=${MicCaptureHub.aecEnabled} lost=${Diag.micAecLost}", 12, if (MicCaptureHub.aecEnabled) Ivory else Gold) }
                    Row(Modifier.fillMaxWidth()) { JText("audio route", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.lastRoute, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("echo guard (half-duplex)", 12, Muted, modifier = Modifier.weight(1f)); JText("${Prefs.halfDuplex}${CallHost.client?.let { if (it.echoGuard) " · holding mic back now" else "" } ?: ""}", 12, Ivory) }
                    OutlineButton("Check who holds the mic", null, {
                        Diag.foreignRecorder = MicCaptureHub.foreignRecorder()
                        note = "Mic check: ${Diag.foreignRecorder}"
                    }, Modifier.padding(top = 6.dp))
                } }

                item { Card("Connection") {
                    Row(Modifier.fillMaxWidth()) { JText("websocket", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.wsState, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("playback underruns", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.underruns}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("server interrupts", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.interrupted}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("reconnects", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.reconnects}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("last close", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.lastClose, 12, Ivory) }
                } }

                item { Card("Tools & frames") {
                    Row(Modifier.fillMaxWidth()) { JText("tool calls", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.toolCalls}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("last tool", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.lastTool, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("frames sent / dropped", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.framesSent} / ${Diag.framesDropped}", 12, Ivory) }
                    OutlineButton("Save one share frame to a file", null, {
                        scope.launch {
                            note = "Saving a frame…"
                            val p = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.jarvis.app.live.ScreenShareService.saveDebugFrame(ctx) }
                            note = if (p != null) "Frame saved: $p — open it and check whether the cursor/grid is visible in it." else "Could not grab a frame (Android 11+ and the accessibility service are needed)."
                        }
                    }, Modifier.padding(top = 6.dp))
                } }

                item { Card("Cursor & accessibility") {
                    Row(Modifier.fillMaxWidth()) { JText("accessibility", 12, Muted, modifier = Modifier.weight(1f)); JText(if (JarvisAccessibilityService.enabled) "connected" else "OFF", 12, if (JarvisAccessibilityService.enabled) Ivory else Gold) }
                    Row(Modifier.fillMaxWidth()) { JText("cursor windows", 12, Muted, modifier = Modifier.weight(1f)); JText("${CursorController.positions().size}", 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("cursor add ok / fail", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.cursorAdds} / ${Diag.cursorAddFails}", 12, Ivory) }
                    JText(CursorController.list(), 11, Muted)
                } }

                item { Card("Pointer & gestures (Masla A)") {
                    Row(Modifier.fillMaxWidth()) { JText("gestures ok / bad", 12, Muted, modifier = Modifier.weight(1f)); JText("${Diag.gesturesCompleted} / ${Diag.gesturesFailed}", 12, if (Diag.gesturesFailed > 0) Gold else Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("last gesture outcome", 12, Muted, modifier = Modifier.weight(1f)); JText(Diag.lastGesture, 12, Ivory) }
                    Row(Modifier.fillMaxWidth()) { JText("move mode (hand drag)", 12, Muted, modifier = Modifier.weight(1f)); JText(if (CursorController.moveMode) "ON" else "OFF (cursors never eat a tap)", 12, if (CursorController.moveMode) Gold else Ivory) }
                    OutlineButton(if (CursorController.moveMode) "Turn move mode OFF" else "Turn move mode ON", null, {
                        CursorController.setMove(!CursorController.moveMode)
                        note = if (CursorController.moveMode) "Move mode ON: you can drag cursors by hand." else "Move mode OFF: cursors are NOT_TOUCHABLE again."
                    }, Modifier.padding(vertical = 4.dp))
                    JText(JarvisAccessibilityService.gestureCapabilities(), 11, Muted)
                    JText(CursorController.windowReport(), 11, Muted)
                    GoldButton("Tap Test (real taps, ±65 targets)", null, { onNav("taptest") }, Modifier.padding(top = 6.dp))
                } }

                item {
                    GoldButton("Cursor Test (centre + 4 corners)", null, {
                        if (!JarvisAccessibilityService.enabled) { note = "Accessibility service is OFF. Turn it on in Android Settings > Accessibility." }
                        else scope.launch {
                            note = "Cursor test running…"
                            CursorController.clear()
                            delay(150)
                            val spots = listOf(0f to 0f, -80f to 80f, 80f to 80f, -80f to -80f, 80f to -80f)
                            spots.forEachIndexed { i, (x, y) ->
                                val id = CursorController.add(x, y, "T${i + 1}")
                                note = if (id > 0) "Cursor #$id at ${x}, ${y}" else "add failed (see log)"
                                Diag.log("CursorTest", "step $i add($x,$y) -> id=$id")
                                delay(700)
                            }
                            delay(700)
                            CursorController.clear()
                            Diag.log("CursorTest", "cleared")
                            note = "Cursor test done. If nothing appeared, the overlay failed (see log above)."
                        }
                    }, Modifier.padding(top = 4.dp))
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlineButton("Copy logs", null, {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("jarvis-diag", Diag.dump()))
                            note = "Diagnostics copied."
                        }, Modifier.weight(1f))
                        OutlineButton("Save logs", null, {
                            try {
                                val dir = File(ctx.filesDir, "diag").apply { mkdirs() }
                                val f = File(dir, "diag_${stamp.format(System.currentTimeMillis())}.txt")
                                f.writeText(Diag.dump())
                                note = "Saved: ${f.absolutePath}"
                            } catch (e: Exception) { note = "Save failed: ${e.message}" }
                        }, Modifier.weight(1f))
                        OutlineButton("Clear log", null, { Diag.events.clear(); note = "Log cleared." }, Modifier.weight(1f))
                    }
                    if (note.isNotBlank()) {
                        JText(note, 11, GoldLight, modifier = Modifier.padding(top = 6.dp))
                    }
                }

                item { JText("Events (newest last)", 12, Gold, FontWeight.Medium, modifier = Modifier.padding(top = 6.dp)) }
                items(Diag.events.toList()) { ev ->
                    val hhmmss = remember(ev.t) { SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(ev.t)) }
                    JText("$hhmmss [${ev.thread}] ${ev.msg}", 10, Muted)
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface2)
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).padding(12.dp)
    ) {
        JText(title, 12, Gold, FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}
