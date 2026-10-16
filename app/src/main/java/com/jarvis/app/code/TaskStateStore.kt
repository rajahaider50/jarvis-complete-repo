package com.jarvis.app.code

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Crash-safe local state for the one coding task that may need to resume. */
object TaskStateStore {
    data class FileState(val path: String, val hash: String, val size: Long, val updatedAt: Long)
    data class State(
        val taskId: String,
        val projectId: String,
        val projectName: String,
        val originalGoal: String,
        var status: String = "running",
        var currentFile: String = "",
        var partialContent: String = "",
        val plan: MutableList<String> = mutableListOf(),
        val completedFiles: MutableList<FileState> = mutableListOf(),
        val decisions: MutableMap<String, String> = linkedMapOf(),
        val conversation: MutableList<String> = mutableListOf(),
        var updatedAt: Long = System.currentTimeMillis()
    )

    var pending by mutableStateOf<State?>(null)
        private set
    private var file: File? = null

    fun init(ctx: android.content.Context) {
        if (file != null) return
        file = File(ctx.filesDir, "code_task_state.json")
        load()
    }

    private fun load() {
        val f = file ?: return
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText())
            pending = State(
                o.getString("id"), o.getString("pid"), o.getString("pn"), o.getString("goal"),
                o.optString("status", "paused"), o.optString("current"), o.optString("partial"),
                o.optJSONArray("plan").strings(), o.optJSONArray("files").fileStates(),
                o.optJSONObject("decisions").stringsMap(), o.optJSONArray("conversation").strings().toMutableList(),
                o.optLong("updated", System.currentTimeMillis())
            )
        } catch (e: Exception) { AppLog.w(e); pending = null }
    }

    fun begin(project: Workspace.Project, goal: String): State {
        val s = State(System.currentTimeMillis().toString(), project.id, project.name, goal.trim())
        pending = s; save(s); return s
    }

    fun resume(s: State) { s.status = "running"; s.updatedAt = System.currentTimeMillis(); pending = s; save(s) }

    suspend fun checkpoint(s: State, project: Workspace.Project, currentFile: String = "", partial: String = "") = withContext(Dispatchers.IO) {
        s.status = "running"
        s.currentFile = currentFile
        s.partialContent = partial.takeLast(60000)
        s.completedFiles.clear()
        Workspace.filesOnly(project).forEach { e ->
            try { s.completedFiles.add(FileState(e.path, hash(Workspace.file(project, e.path)), e.size, System.currentTimeMillis())) }
            catch (x: Exception) { AppLog.w(x) }
        }
        s.updatedAt = System.currentTimeMillis(); pending = s; save(s)
    }

    suspend fun record(s: State, project: Workspace.Project, note: String, currentFile: String = "", partial: String = "") {
        if (note.isNotBlank()) { s.conversation.add(note.take(500)); while (s.conversation.size > 80) s.conversation.removeAt(0) }
        checkpoint(s, project, currentFile, partial)
    }

    suspend fun pause(s: State, project: Workspace.Project, reason: String) {
        s.status = "paused"; s.conversation.add(reason.take(500)); checkpoint(s, project, s.currentFile, s.partialContent); s.status = "paused"; save(s)
    }

    fun complete(s: State) {
        s.status = "completed"; s.updatedAt = System.currentTimeMillis(); pending = null
        try { file?.delete() } catch (e: Exception) { AppLog.w(e) }
    }

    fun resumePrompt(s: State): String = buildString {
        append("RESUME THE SAME TASK. Do not change the original goal or project.\n")
        append("ORIGINAL GOAL (immutable): ").append(s.originalGoal).append("\n")
        append("TASK STATUS: ").append(s.status).append("\n")
        val completed = s.completedFiles.joinToString { it.path }.ifBlank { "(none)" }
        append("FILES COMPLETED: ").append(completed).append("\n")
        append("CURRENTLY WRITING: ").append(s.currentFile.ifBlank { "(none)" }).append("\n")
        if (s.partialContent.isNotBlank()) append("CONTENT SO FAR TAIL (continue, do not repeat):\n").append(s.partialContent.takeLast(4000)).append("\n")
        if (s.conversation.isNotEmpty()) append("LAST CHECKPOINT NOTES:\n").append(s.conversation.takeLast(8).joinToString("\n")).append("\n")
        append("Continue from the saved state and finish this same task only.")
    }

    private fun hash(f: File): String = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
        .joinToString("") { "%02x".format(it) }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    private fun JSONArray?.fileStates(): MutableList<FileState> = if (this == null) mutableListOf() else (0 until length()).mapNotNull { i -> optJSONObject(i)?.let { FileState(it.optString("p"), it.optString("h"), it.optLong("s"), it.optLong("t")) } }.toMutableList()
    private fun JSONObject?.stringsMap(): MutableMap<String, String> = linkedMapOf<String, String>().also { out -> if (this != null) keys().forEach { k -> out[k] = optString(k) } }

    private fun save(s: State) {
        val files = JSONArray(); s.completedFiles.forEach { files.put(JSONObject().put("p", it.path).put("h", it.hash).put("s", it.size).put("t", it.updatedAt)) }
        val decisions = JSONObject(); s.decisions.forEach { (k, v) -> decisions.put(k, v) }
        val o = JSONObject().put("id", s.taskId).put("pid", s.projectId).put("pn", s.projectName).put("goal", s.originalGoal)
            .put("status", s.status).put("current", s.currentFile).put("partial", s.partialContent)
            .put("plan", JSONArray(s.plan)).put("files", files).put("decisions", decisions).put("conversation", JSONArray(s.conversation)).put("updated", s.updatedAt)
        try { file?.writeText(o.toString()) } catch (e: Exception) { AppLog.w(e) }
    }
}
