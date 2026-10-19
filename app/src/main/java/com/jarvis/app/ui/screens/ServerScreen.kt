package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.Diag
import com.jarvis.app.net.JarvisServer
import com.jarvis.app.ui.components.GoldButton
import com.jarvis.app.ui.components.JField
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.JToggle
import com.jarvis.app.ui.components.OutlineButton
import com.jarvis.app.ui.components.ScreenScaffold
import com.jarvis.app.ui.components.SubHeader
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.GoldLight
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Profile > Brain server (Phase 4 step S2).
 *
 * Shows the REAL state of the owner's own server: the URL in use, the last HTTP result, the version and
 * counts the server itself returned, its mood model, and the learning queue. Nothing here is invented:
 * when the server is off the page says "not reachable" with the real exception/status text and the app
 * keeps running exactly as before (local memory only).
 */
@Composable
fun ServerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Prefs.serverUrl) }
    var token by remember { mutableStateOf(Prefs.serverToken) }
    var enabled by remember { mutableStateOf(Prefs.serverEnabled) }
    var syncMem by remember { mutableStateOf(Prefs.serverSyncMemory) }
    var teach by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Set the URL of your server, paste the same JARVIS_TOKEN, then Test connection.") }
    var busy by remember { mutableStateOf(false) }
    var jobs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    fun saveAll() {
        Prefs.serverUrl = url.trim()
        Prefs.serverToken = token.trim()
        Prefs.serverEnabled = enabled
        Prefs.serverSyncMemory = syncMem
        Prefs.save()
    }

    fun test() {
        saveAll()
        busy = true
        scope.launch {
            note = "Contacting ${Prefs.serverUrl.trim().ifBlank { "(no URL)" }} …"
            val r = withContext(Dispatchers.IO) { JarvisServer.health() }
            val r2 = if (r.ok) withContext(Dispatchers.IO) { JarvisServer.state() } else r
            note = when {
                r.ok && r2.ok -> "Connected to v${Diag.srvVersion}: ${Diag.srvMemories} memories, " +
                        "${Diag.srvSkills} skills, ${Diag.srvJobs} jobs queued, mood " +
                        "${Diag.srvMood.ifBlank { "-" }}, learner ${Diag.srvLearner.ifBlank { "-" }}."
                r.ok -> "Reached the server (v${Diag.srvVersion}) but ${r2.error}. " +
                        "Set the same token as JARVIS_TOKEN on the server."
                else -> "Not reachable — ${r.error}. The app keeps working on local memory."
            }
            busy = false
        }
    }

    fun loadJobs() {
        saveAll()
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { JarvisServer.jobs(10) }
            if (!r.ok) {
                note = "Could not read the queue: ${r.error}"
            } else {
                val a = r.arr("items")
                jobs = if (a == null) emptyList() else (0 until a.length()).map { a.getJSONObject(it) }
                note = "Queue read: ${jobs.size} job(s)."
            }
            busy = false
        }
    }

    fun sendLesson() {
        saveAll()
        val text = teach.trim()
        if (text.isBlank()) {
            note = "Write what JARVIS should learn first."
            return
        }
        busy = true
        scope.launch {
            note = "Queueing the learning job…"
            val r = withContext(Dispatchers.IO) { JarvisServer.learn(text = text) }
            note = if (r.ok) "Queued job #${r.int("job")} (${r.int("queued")} waiting). " +
                    "The server distils it in the background — refresh the queue in a few seconds."
            else "Could not queue: ${r.error}"
            if (r.ok) teach = ""
            busy = false
        }
    }

    ScreenScaffold(null, {}) {
        SubHeader("Brain server", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    JText("Your own server (memory · skills · background learning). Optional: with no server " +
                            "set, JARVIS uses only the phone's memory, exactly as before.", 12, Muted)
                }

                item {
                    Card("Connection") {
                        JField(url, { url = it }, "http://192.168.1.5:8787  or  https://your-app.fly.dev")
                        JField(token, { token = it }, "JARVIS_TOKEN (same string as on the server)", secret = true)
                        SwitchRow("Use the brain server", enabled) { enabled = it; saveAll() }
                        SwitchRow("Sync memory with it (S3)", syncMem) { syncMem = it; saveAll() }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlineButton("Test connection", null, { if (!busy) test() }, Modifier.weight(1f))
                            OutlineButton("Refresh state", null, { if (!busy) test() }, Modifier.weight(1f))
                        }
                        JText(note, 11, if (Diag.srvState == "ok") GoldLight else Muted, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                item {
                    Card("Live state (from the server itself)") {
                        StatRow("last call", Diag.srvState)
                        StatRow("URL in use", Diag.srvUrl.ifBlank { "(none)" })
                        StatRow("calls made", "${Diag.srvCalls}")
                        StatRow("version", Diag.srvVersion.ifBlank { "-" })
                        StatRow("memories · skills", "${Diag.srvMemories} · ${Diag.srvSkills}")
                        StatRow("jobs queued", "${Diag.srvJobs}")
                        StatRow("mood (affect)", Diag.srvMood.ifBlank { "-" })
                        StatRow("learner", Diag.srvLearner.ifBlank { "-" })
                        StatRow("last success", Diag.srvLastOk.ifBlank { "-" })
                        if (Diag.srvLastError.isNotBlank()) StatRow("last error", Diag.srvLastError, warn = true)
                    }
                }

                item {
                    Card("Teach JARVIS (background learning)") {
                        JText("Write a task or anything to remember. The server distils it into lessons and " +
                                "writes a skill while you are busy — this is the real job queue, not a mock.", 11, Muted)
                        JField(teach, { teach = it }, "e.g. build a dynamic 3D website like this sample",
                            singleLine = false, minHeight = 70.dp, modifier = Modifier.padding(top = 6.dp))
                        GoldButton(if (busy) "Working…" else "Queue a learning job", null, { if (!busy) sendLesson() },
                            Modifier.padding(top = 8.dp))
                        OutlineButton("Read the queue", null, { if (!busy) loadJobs() }, Modifier.padding(top = 8.dp))
                    }
                }

                if (jobs.isNotEmpty()) {
                    item {
                        Card("Learning jobs (real rows)") {
                            jobs.forEach { j ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        JText("#${j.optInt("id")} · ${j.optString("kind")}", 12, Gold, FontWeight.Medium,
                                            modifier = Modifier.weight(1f))
                                        JText(j.optString("status"), 11,
                                            if (j.optString("status") == "done") GoldLight else Muted)
                                    }
                                    val detail = if (j.optString("status") == "failed") j.optString("error")
                                    else j.optString("result")
                                    if (detail.isNotBlank()) JText(detail, 11, Muted)
                                }
                            }
                        }
                    }
                }
                item { Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {} }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        JText(label, 14, Ivory, modifier = Modifier.weight(1f))
        JToggle(on, onChange)
    }
}

@Composable
private fun StatRow(label: String, value: String, warn: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        JText(label, 12, Muted, modifier = Modifier.weight(1f))
        JText(value, 12, if (warn) Gold else Ivory)
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
