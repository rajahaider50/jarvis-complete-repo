package com.jarvis.app.code

import android.content.Context
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
import com.jarvis.app.data.ProviderModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    class Run(val project: Workspace.Project, val prompt: String) {
        val steps = mutableStateListOf<Step>()
        val notes = mutableStateListOf<String>()
        var thought by mutableStateOf("")
        var summary by mutableStateOf("")
        var error by mutableStateOf("")
        var status by mutableStateOf(0)         // 0 working, 1 done, 2 failed, 3 stopped
        var model by mutableStateOf("")
        var editorPath by mutableStateOf("")
        var editorText by mutableStateOf("")
        var editorSeq by mutableStateOf(0)
        var state: TaskStateStore.State? = null
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

    // ------------------------------------------------------------------ custom provider (OpenAI-compatible), with router failover
    private class ProviderBackend(private val candidates: List<ModelRouter.Candidate>) : Backend {
        private val msgs = JSONArray()
        private var idx = 0
        private val cand get() = candidates[idx]
        private val p get() = cand.provider
        private val model get() = cand.model.id
        private val est get() = 4000
        override val label: String get() = p.name + " · " + model

        override suspend fun begin(system: String, user: String) {
            msgs.put(JSONObject().put("role", "system").put("content", system)); say(user)
        }
        override fun say(text: String) { msgs.put(JSONObject().put("role", "user").put("content", text)) }
        override fun results(list: List<Pair<Call, String>>) {
            list.forEach { (c, r) -> msgs.put(JSONObject().put("role", "tool").put("tool_call_id", c.id).put("content", r)) }
        }

        override suspend fun step(onThought: (String) -> Unit): Turn {
            var last: Exception? = null
            while (idx < candidates.size) {
                val body = JSONObject().put("model", model.trim()).put("messages", msgs).put("tools", AgentTools.openAi()).put("tool_choice", "auto").put("stream", false)
                p.temperature.toDoubleOrNull()?.let { body.put("temperature", it) }
                val started = System.currentTimeMillis()
                try {
                    val j = OpenAiCompat.completions(p, body)
                    ModelRouter.markSuccess(cand, System.currentTimeMillis() - started, est)
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
                    val text = if (m.isNull("content")) "" else m.optString("content")
                    return Turn(text, calls)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    last = e
                    val retry = ModelRouter.markFailure(cand, e)
                    if (e.message?.contains("HTTP 4") == true && e.message?.contains("tool", true) == true && !retry)
                        throw IOException("${p.name}: this model does not support tool calling, which the code agent needs. Choose another model in Chat.")
                    if (retry && idx + 1 < candidates.size) { AppLog.i("Router", "${p.name} · $model: ${e.message} → next model"); idx++; continue }
                    throw e
                }
            }
            throw last ?: IOException("No provider model is available")
        }
    }

    // ------------------------------------------------------------------ the run
    val runs = mutableStateListOf<Run>()
    var current by mutableStateOf<Run?>(null)
    val busy: Boolean get() = current?.status == 0
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun systemPrompt(p: Workspace.Project): String =
        "You are a coding and document agent inside a phone app. You work in the project \"${p.name}\" with tools, and the user watches an editor, not a chat.\n" +
            "RULES:\n1. Never put code or file contents in your replies. Put them into files with write_file. Replies are one short sentence of progress.\n" +
            "2. First call list_files, and read_file for existing files the task touches. Say your plan in at most 4 short lines.\n" +
            "3. Create files with write_file (complete content). Change existing files with str_replace or replace_lines: read the part first, then edit. Fix a bug with the smallest edit.\n" +
            "4. For PDF, PNG, DOCX, XLSX or PPTX call use_skill documents first, then make_document. For csv, md, txt, html, svg, json and code use write_file.\n" +
            "5. Every file, import, link, script, style and image you reference must exist. When all files are written call verify_project and fix every problem.\n" +
            "6. End with finish(summary, deliver): deliver=zip for projects with many files, file (+file_path) for one deliverable, none if nothing is to be downloaded. Write the summary in the user's language, 2-4 lines, naming the files and how to use them.\n" +
            "7. Do not invent facts. If the task is vague, choose sensible defaults and mention them in the summary.\n" + Skills.promptBlock()

    private suspend fun makeBackend(prompt: String): Backend {
        val custom = CustomProviders.selected()
        if (custom != null) {
            val est = ModelRouter.estimateTokens(listOf(GeminiClient.Turn("user", prompt)), "")
            val cands = ModelRouter.rank(custom, ModelRouter.Task.CODING, est, needsTools = true)
            if (cands.isNotEmpty()) return ProviderBackend(cands)
            val m = custom.models.firstOrNull { it.id == custom.modelId } ?: ProviderModel(custom.modelId, source = "manual")
            return ProviderBackend(listOf(ModelRouter.Candidate(custom, m, 0.0)))
        }
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
        val run = Run(p, prompt.trim())
        runs.add(run); current = run
        job = scope.launch {
            try { execute(run, attached) }
            catch (e: CancellationException) { run.status = 3; run.state?.let { TaskStateStore.pause(it, p, "Stopped by the user.") }; throw e }
            catch (e: Exception) { AppLog.e("CodeAgent", e); run.error = e.message ?: "The agent stopped"; run.status = 2; run.state?.let { TaskStateStore.pause(it, p, "Interrupted: ${e.message}") } }
        }
    }

    /** Restarts the last unfinished task from its saved checkpoint, keeping the original goal. */
    fun resumeTask() {
        val st = TaskStateStore.pending ?: return
        if (busy) return
        val p = Workspace.projects.firstOrNull { it.id == st.projectId } ?: return
        Workspace.current = p
        val run = Run(p, st.originalGoal)
        run.state = st
        runs.add(run); current = run
        job = scope.launch {
            try { execute(run, emptyList(), st) }
            catch (e: CancellationException) { run.status = 3; TaskStateStore.pause(st, p, "Stopped by the user."); throw e }
            catch (e: Exception) { AppLog.e("CodeAgent", e); run.error = e.message ?: "The agent stopped"; run.status = 2; TaskStateStore.pause(st, p, "Interrupted: ${e.message}") }
        }
    }

    fun discardPending() = TaskStateStore.pending?.let { TaskStateStore.complete(it) }

    fun stop() { job?.cancel(); current?.let { if (it.status == 0) it.status = 3 } }

    private suspend fun execute(run: Run, attached: List<String>, resume: TaskStateStore.State? = null) {
        val p = run.project
        val state = resume ?: TaskStateStore.begin(p, run.prompt)
        run.state = state
        if (resume != null) TaskStateStore.resume(state)
        val backend = makeBackend(run.prompt)
        run.model = backend.label
        val files = Workspace.tree(p).take(60).joinToString(", ") { it.path }.ifBlank { "(empty)" }
        backend.begin(systemPrompt(p), run.prompt + (if (attached.isNotEmpty()) "\n\nThe user added these files to the project: ${attached.joinToString()}" else "") + "\n\nProject files now: $files")
        if (resume != null) backend.say(TaskStateStore.resumePrompt(state))
        var fixRounds = 0; var done = false; var empty = 0
        for (round in 1..45) {
            val t = backend.step { run.thought += it }
            run.model = backend.label
            if (t.text.isNotBlank()) run.notes.add(t.text.trim().take(400))
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
            }
            backend.results(results)
            TaskStateStore.checkpoint(state, p, run.editorPath, run.editorText)
            if (done) break
        }
        if (run.summary.isBlank()) run.summary = if (done) "Done." else "The work stopped before the agent said it was finished. Everything written so far is in the project."
        if (!done && run.outputs.isEmpty()) deliver(run, JSONObject().put("deliver", "zip"))
        run.status = 1
        if (done) TaskStateStore.complete(state) else TaskStateStore.pause(state, p, "Reached the step limit before finish. Everything so far is saved.")
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
