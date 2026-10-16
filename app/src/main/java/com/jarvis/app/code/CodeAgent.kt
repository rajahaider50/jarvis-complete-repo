package com.jarvis.app.code

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.CustomProvider
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.GeminiClient
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.ModelRouter
import com.jarvis.app.data.OpenAiCompat
import com.jarvis.app.data.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * The coding / document agent. It uses the model you chose in Chat (a Gemini model or a custom provider) with tool calling:
 * every file is written in the project's editor instead of the chat, and the work is shown as live thinking.
 */
object CodeAgent {
    class Step(val kind: String, val title: String) {
        var state by mutableStateOf(0)          // 0 working, 1 done, 2 failed
        var detail by mutableStateOf("")
    }

    class Run(val project: Workspace.Project, val prompt: String, val taskId: String, val resumed: Boolean = false) {
        val steps = mutableStateListOf<Step>()
        val notes = mutableStateListOf<String>()
        var thought by mutableStateOf("")
        var summary by mutableStateOf("")
        var error by mutableStateOf("")
        var status by mutableStateOf(0)         // 0 working, 1 done, 2 failed, 3 stopped, 4 waiting for network
        var model by mutableStateOf("")
        var editorPath by mutableStateOf("")
        var editorText by mutableStateOf("")
        var editorSeq by mutableStateOf(0)
        val outputs = mutableStateListOf<String>()
        fun showInEditor(path: String, text: String) { editorPath = path; editorText = text.take(20000); editorSeq++ }
    }

    class Call(val id: String, val name: String, val args: JSONObject)
    class Turn(val text: String, val calls: List<Call>)

    private interface Backend {
        val label: String
        suspend fun begin(system: String, user: String)
        suspend fun step(onThought: (String) -> Unit): Turn
        fun results(list: List<Pair<Call, String>>)
        fun say(text: String)
    }

    // ------------------------------------------------------------------ Gemini
    private class GeminiBackend(val key: String, val models: List<String>) : Backend {
        private val contents = JSONArray()
        private var system = ""
        private var idx = 0
        override val label: String get() = models.getOrElse(idx) { "" }

        override suspend fun begin(system: String, user: String) { this.system = system; say(user) }
        override fun say(text: String) { contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", text)))) }

        override fun results(list: List<Pair<Call, String>>) {
            val parts = JSONArray()
            list.forEach { (c, r) -> parts.put(JSONObject().put("functionResponse", JSONObject().put("name", c.name).put("response", JSONObject().put("result", r)))) }
            contents.put(JSONObject().put("role", "user").put("parts", parts))
        }

        override suspend fun step(onThought: (String) -> Unit): Turn {
            var last: Exception? = null
            while (idx < models.size) {
                try {
                    val body = JSONObject().put("contents", contents)
                        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                        .put("tools", JSONArray().put(AgentTools.gemini()))
                        .put("generationConfig", JSONObject().put("thinkingConfig", JSONObject().put("includeThoughts", true)))
                    val j = try { GeminiClient.generate(key, models[idx], body) }
                    catch (e: GeminiClient.GeminiHttpException) {
                        if (e.code == 400) { body.remove("generationConfig"); GeminiClient.generate(key, models[idx], body) } else throw e
                    }
                    val content = j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content") ?: throw IOException("The model sent an empty answer")
                    val parts = content.optJSONArray("parts") ?: JSONArray()
                    val text = StringBuilder(); val calls = ArrayList<Call>()
                    for (k in 0 until parts.length()) {
                        val p = parts.getJSONObject(k)
                        val fc = p.optJSONObject("functionCall")
                        if (fc != null) calls.add(Call("c$k", fc.getString("name"), fc.optJSONObject("args") ?: JSONObject()))
                        else if (p.optBoolean("thought", false)) onThought(p.optString("text"))
                        else text.append(p.optString("text"))
                    }
                    contents.put(content)
                    return Turn(text.toString(), calls)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    last = e
                    val code = (e as? GeminiClient.GeminiHttpException)?.code
                    AppLog.i("CodeAgent", "${models[idx]}: ${GeminiClient.shortReason(e)}")
                    if (code == 503) { delay(1500); idx++; continue }
                    if (code == 429 || code == 404 || code == 400) { idx++; continue }
                    throw e
                }
            }
            throw last ?: IOException("No Gemini model is available")
        }
    }

    // ------------------------------------------------------------------ custom provider (OpenAI-compatible)
    private class ProviderBackend(val p: CustomProvider) : Backend {
        private val msgs = JSONArray()
        private var model: String = CustomProviders.modelCandidates(p).firstOrNull() ?: p.modelId
        override val label: String get() = p.name + " · " + model

        override suspend fun begin(system: String, user: String) {
            msgs.put(JSONObject().put("role", "system").put("content", system)); say(user)
        }
        override fun say(text: String) { msgs.put(JSONObject().put("role", "user").put("content", text)) }
        override fun results(list: List<Pair<Call, String>>) {
            list.forEach { (c, r) -> msgs.put(JSONObject().put("role", "tool").put("tool_call_id", c.id).put("content", r)) }
        }

        override suspend fun step(onThought: (String) -> Unit): Turn {
            val routes = ModelRouter.rank(p, ModelRouter.Task.CODING, (msgs.toString().length / 4).coerceAtLeast(1), true)
            var last: Exception? = null
            for (route in routes) {
                val started = System.currentTimeMillis()
                try {
                    model = route.model.id
                    val active = route.provider
                    val body = JSONObject().put("model", model.trim()).put("messages", msgs).put("tools", AgentTools.openAi()).put("tool_choice", "auto").put("stream", false)
                    active.temperature.toDoubleOrNull()?.let { body.put("temperature", it) }
                    val j = OpenAiCompat.completions(active, body)
                    val m = j.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    if (!m.isNull("reasoning_content")) onThought(m.optString("reasoning_content"))
                    else if (!m.isNull("reasoning")) onThought(m.optString("reasoning"))
                    val calls = ArrayList<Call>()
                    val tc = m.optJSONArray("tool_calls")
                    if (tc != null) for (k in 0 until tc.length()) {
                        val o = tc.getJSONObject(k); val f = o.getJSONObject("function")
                        val args = try { JSONObject(f.optString("arguments").ifBlank { "{}" }) } catch (e: Exception) { JSONObject() }
                        calls.add(Call(o.optString("id", "c$k"), f.getString("name"), args))
                    }
                    msgs.put(m)
                    ModelRouter.markSuccess(route, System.currentTimeMillis() - started, msgs.toString().length / 4)
                    val text = if (m.isNull("content")) "" else m.optString("content")
                    return Turn(text, calls)
                } catch (e: Exception) {
                    last = e
                    if (e.message?.contains("HTTP 4") == true && e.message?.contains("tool", true) == true)
                        last = IOException("${p.name}: this model does not support tool calling, which the code agent needs. Choose another model in Chat.")
                    if (ModelRouter.markFailure(route, e)) {
                        AppLog.i("Router", "${route.provider.name} · ${route.model.id} failed for coding; switching to next scored model")
                        continue
                    }
                    throw last
                }
            }
            throw last ?: IOException("No eligible coding model is available; wait for cooldown or refresh providers.")
        }
    }

    // ------------------------------------------------------------------ the run
    val runs = mutableStateListOf<Run>()
    var current by mutableStateOf<Run?>(null)
    val busy: Boolean get() = current?.status == 0
    private var job: Job? = null
    private var appContext: Context? = null
    private var networkPauseRequested = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private class NetworkUnavailableException : IOException("Network unavailable")

    fun init(ctx: Context) { appContext = ctx.applicationContext }

    fun onNetworkChanged(connected: Boolean) {
        if (!connected && busy) {
            networkPauseRequested = true
            job?.cancel()
        } else if (connected && current?.status == 4 && !busy) {
            resume()
        }
    }

    private fun systemPrompt(p: Workspace.Project): String =
        "You are a coding and document agent inside a phone app. You work in the project \"${p.name}\" with tools, and the user watches an editor, not a chat.\n" +
            "RULES:\n1. Never put code or file contents in your replies. Put them into files with write_file. Replies are one short sentence of progress.\n" +
            "2. First call list_files, and read_file for existing files the task touches. Say your plan in at most 4 short lines.\n" +
            "3. Create files with write_file (complete content). Change existing files with str_replace or replace_lines: read the part first, then edit. Fix a bug with the smallest edit.\n" +
            "4. For PDF, PNG, DOCX, XLSX or PPTX call use_skill documents first, then make_document. For csv, md, txt, html, svg, json and code use write_file.\n" +
            "5. Every file, import, link, script, style and image you reference must exist. When all files are written call verify_project and fix every problem.\n" +
            "6. End with finish(summary, deliver): deliver=zip for projects with many files, file (+file_path) for one deliverable, none if nothing is to be downloaded. Write the summary in the user's language, 2-4 lines, naming the files and how to use them.\n" +
            "7. Do not invent facts. If the task is vague, choose sensible defaults and mention them in the summary.\n" + Skills.promptBlock()

    private suspend fun makeBackend(): Backend {
        CustomProviders.selected()?.let { return ProviderBackend(it) }
        val key = Prefs.apiKey.trim()
        if (key.isEmpty()) throw IOException("Add your Gemini API key, or choose a custom provider in the Chat model menu.")
        if (!ModelCatalog.ready) ModelCatalog.refresh()
        val models = ModelCatalog.chatCandidates(ModelCatalog.Task.COMPLEX).map { it.id }.take(8)
        if (models.isEmpty()) throw IOException("No Gemini chat model was found for this key.")
        return GeminiBackend(key, models)
    }

    fun start(prompt: String, attached: List<String>) {
        val p = Workspace.current ?: return
        if (busy || prompt.isBlank()) return
        startInternal(p, prompt.trim(), attached, null)
    }

    fun resume() {
        val saved = TaskStateStore.pending ?: return
        val p = Workspace.projects.firstOrNull { it.id == saved.projectId } ?: return
        if (busy) return
        Workspace.current = p
        startInternal(p, saved.originalGoal, emptyList(), saved)
    }

    private fun startInternal(p: Workspace.Project, prompt: String, attached: List<String>, saved: TaskStateStore.State?) {
        networkPauseRequested = false
        val state = saved ?: TaskStateStore.begin(p, prompt)
        if (saved != null) TaskStateStore.resume(saved)
        val run = Run(p, prompt, state.taskId, saved != null)
        runs.add(run); current = run
        if (!NetworkMonitor.online) {
            run.status = 4; run.error = "Waiting for network…"
            job = scope.launch { TaskStateStore.waitForNetwork(state, p, "Task queued while network is unavailable") }
            return
        }
        keepAlive()
        job = scope.launch {
            try { execute(run, attached, state) }
            catch (e: CancellationException) {
                if (networkPauseRequested || !NetworkMonitor.online) {
                    run.status = 4; run.error = "Waiting for network…"; withContext(NonCancellable) { TaskStateStore.waitForNetwork(state, p, "Network lost; task paused safely") }
                } else { run.status = 3; withContext(NonCancellable) { TaskStateStore.pause(state, p, "Stopped before completion") } }
                stopKeepAlive()
            }
            catch (e: Exception) {
                if (!NetworkMonitor.online) { run.status = 4; run.error = "Waiting for network…"; withContext(NonCancellable) { TaskStateStore.waitForNetwork(state, p, "Network lost during request") } }
                else { AppLog.e("CodeAgent", e); run.error = e.message ?: "The agent stopped"; run.status = 2; withContext(NonCancellable) { TaskStateStore.pause(state, p, run.error) } }
                stopKeepAlive()
            }
        }
    }

    fun stop() { networkPauseRequested = false; job?.cancel(); current?.let { if (it.status == 0) it.status = 3 } }

    private fun keepAlive() {
        val c = appContext ?: return
        try { if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(Intent(c, CodeTaskService::class.java)) else c.startService(Intent(c, CodeTaskService::class.java)) }
        catch (e: Exception) { AppLog.w(e) }
    }

    private fun stopKeepAlive() { try { val c = appContext ?: return; c.stopService(Intent(c, CodeTaskService::class.java)) } catch (e: Exception) { AppLog.w(e) } }

    private suspend fun execute(run: Run, attached: List<String>, state: TaskStateStore.State) {
        val p = run.project
        val backend = makeBackend()
        run.model = backend.label
        val files = Workspace.tree(p).take(60).joinToString(", ") { it.path }.ifBlank { "(empty)" }
        val goal = if (run.resumed) TaskStateStore.resumePrompt(state) else run.prompt
        backend.begin(systemPrompt(p), goal + (if (attached.isNotEmpty()) "\n\nThe user added these files to the project: ${attached.joinToString()}" else "") + "\n\nProject files now: $files")
        TaskStateStore.record(state, p, if (run.resumed) "Resumed task" else "Started task", run.editorPath, run.editorText)
        var fixRounds = 0; var done = false; var empty = 0
        for (round in 1..45) {
            if (!NetworkMonitor.online) throw NetworkUnavailableException()
            val t = backend.step { run.thought += it }
            run.model = backend.label
            if (t.text.isNotBlank()) run.notes.add(t.text.trim().take(400))
            TaskStateStore.record(state, p, t.text, run.editorPath, run.editorText)
            if (t.calls.isEmpty()) {
                if (t.text.isBlank() && ++empty < 2) { backend.say("Continue: use the tools to do the work and call finish when everything is done."); continue }
                if (run.summary.isBlank()) run.summary = t.text.trim()
                break
            }
            val results = ArrayList<Pair<Call, String>>()
            for (c in t.calls) {
                val step = Step(c.name, AgentTools.title(c.name, c.args)); run.steps.add(step)
                val r = if (c.name == "finish") {
                    val issues = Workspace.verify(p)
                    if (issues.isNotEmpty() && fixRounds < 2) { fixRounds++; "NOT FINISHED. The check found problems, fix them and call finish again:\n" + issues.joinToString("\n") }
                    else { run.summary = c.args.optString("summary"); deliver(run, c.args); done = true; "Finished." }
                } else AgentTools.exec(p, c.name, c.args, run)
                step.state = if (r.startsWith("ERROR") || r.startsWith("NOT FINISHED")) 2 else 1
                step.detail = r.take(240)
                results.add(c to r)
                TaskStateStore.record(state, p, "${c.name}: ${r.take(400)}", run.editorPath, run.editorText)
            }
            backend.results(results)
            if (done) break
        }
        if (run.summary.isBlank()) run.summary = if (done) "Done." else "The work stopped before the agent said it was finished. Everything written so far is in the project."
        if (!done && run.outputs.isEmpty()) deliver(run, JSONObject().put("deliver", "zip"))
        run.status = 1
        TaskStateStore.complete(state)
        stopKeepAlive()
    }

    /** Puts the result where you can view / download it: a single file, or a zip of the project. */
    private fun deliver(run: Run, a: JSONObject) {
        val p = run.project
        val files = Workspace.filesOnly(p)
        if (files.isEmpty()) return
        var mode = a.optString("deliver").lowercase()
        if (mode.isBlank()) mode = if (files.size == 1) "file" else "zip"
        if (mode == "none") return
        try {
            if (mode == "file") {
                val path = a.optString("file_path").ifBlank { files.last().path }
                run.outputs.add(Workspace.copyToOutput(p, path).name)
            } else run.outputs.add(Workspace.exportZip(p).name)
        } catch (e: Exception) { AppLog.w(e); run.outputs.add(Workspace.exportZip(p).name) }
    }
}
