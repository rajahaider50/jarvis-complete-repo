package com.jarvis.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Msg(
    val text: String, val user: Boolean, val error: Boolean = false,
    val image: ByteArray? = null, val hadImage: Boolean = false, val model: String? = null,
    val thought: String? = null
) {
    var rating by mutableStateOf(0)        // 1 liked, -1 disliked
}
data class SessionMeta(val id: String, var title: String, var ts: Long)

object ChatStore {
    val messages = mutableStateListOf<Msg>()
    val sessions = mutableStateListOf<SessionMeta>()
    var currentId by mutableStateOf("")
    var busy by mutableStateOf(false)
    var lastModel by mutableStateOf("")
    var thinking by mutableStateOf("")       // live thought summary of the model while it works
    var streaming by mutableStateOf("")      // the answer as it is being written
    var startedAt by mutableStateOf(0L)
    var lastError: String? = null
    private lateinit var dir: File

    fun init(ctx: Context) {
        dir = ctx.filesDir
        sessions.clear()
        try {
            val a = JSONArray(File(dir, "chat_index.json").takeIf { it.exists() }?.readText() ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i); sessions.add(SessionMeta(o.getString("id"), o.getString("t"), o.getLong("ts")))
            }
        } catch (e: Exception) { AppLog.w(e); }
        if (sessions.isEmpty()) newSession() else open(sessions[0].id)
    }

    private fun saveIndex() {
        val a = JSONArray(); sessions.forEach { a.put(JSONObject().put("id", it.id).put("t", it.title).put("ts", it.ts)) }
        File(dir, "chat_index.json").writeText(a.toString())
    }

    private fun saveCurrent() {
        if (currentId.isEmpty()) return
        val a = JSONArray()
        messages.forEach {
            a.put(JSONObject().put("t", it.text).put("u", it.user).put("e", it.error)
                .put("i", it.hadImage || it.image != null).put("m", it.model ?: ""))
        }
        File(dir, "chat_$currentId.json").writeText(a.toString())
        saveIndex()
    }

    fun newSession() {
        if (currentId.isNotEmpty() && messages.isNotEmpty()) saveCurrent()
        if (currentId.isNotEmpty() && messages.isEmpty()) return
        val id = System.currentTimeMillis().toString()
        sessions.add(0, SessionMeta(id, "New chat", System.currentTimeMillis()))
        currentId = id; messages.clear(); saveIndex()
    }

    fun open(id: String) {
        if (currentId.isNotEmpty() && currentId != id) saveCurrent()
        currentId = id; messages.clear()
        try {
            val f = File(dir, "chat_$id.json")
            if (f.exists()) {
                val a = JSONArray(f.readText())
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    messages.add(Msg(o.getString("t"), o.getBoolean("u"), o.optBoolean("e"), null, o.optBoolean("i"), o.optString("m").ifEmpty { null }))
                }
            }
        } catch (e: Exception) { AppLog.w(e); }
    }

    fun delete(id: String) {
        File(dir, "chat_$id.json").delete()
        sessions.removeAll { it.id == id }
        if (id == currentId) { currentId = ""; if (sessions.isEmpty()) newSession() else open(sessions[0].id) }
        saveIndex()
    }

    fun clearCurrent() { messages.clear(); saveCurrent() }

    fun systemPrompt(voice: Boolean): String {
        val sb = StringBuilder()
        sb.append("You are ${Prefs.assistantName}, a private AI concierge: elegant, calm, precise and discreet. ")
        if (Prefs.userName.isNotBlank()) sb.append("The user's name is ${Prefs.userName}. ")
        if (Prefs.memories.isNotEmpty()) sb.append("Known facts about the user: ${Prefs.memories.joinToString("; ")}. ")
        if (Prefs.instructions.isNotBlank()) sb.append("User's custom instructions: ${Prefs.instructions} ")
        sb.append("Always reply in the same language the user writes. ")
        sb.append(Voices.genderRule())
        sb.append(RealFacts.block())
        sb.append(if (voice) "Answer in one to three short spoken sentences, no markdown." else "Be concise and clear. Use plain text.")
        return sb.toString()
    }

    /** Throws the last answer away and asks the same question again. */
    suspend fun regenerate(): String? {
        val i = messages.indexOfLast { it.user }
        if (i < 0 || busy) return null
        val u = messages[i]
        while (messages.size > i + 1) messages.removeAt(messages.lastIndex)
        saveCurrent()
        return ask(u.text, u.image, addUser = false)
    }

    /** Removes the message at [index] and everything after it, so it can be edited and sent again. */
    fun takeBack(index: Int): Msg? {
        val m = messages.getOrNull(index) ?: return null
        while (messages.size > index) messages.removeAt(messages.lastIndex)
        saveCurrent()
        return m
    }

    fun rate(m: Msg, rating: Int, note: String = "") {
        m.rating = rating
        if (rating != 0) AppLog.i("Feedback", (if (rating > 0) "Liked" else "Disliked") + " · " + (m.model ?: "") + (if (note.isNotBlank()) " · $note" else "") + " · " + m.text.take(80))
    }

    /** Saves one finished turn of a live voice call into the current chat. */
    fun addVoiceTurn(user: String, ai: String) {
        if (currentId.isEmpty()) return
        if (user.isNotBlank()) messages.add(Msg(user, true))
        if (ai.isNotBlank()) messages.add(Msg(ai, false, model = "live"))
        sessions.firstOrNull { it.id == currentId }?.let { if (it.title == "New chat" && user.isNotBlank()) it.title = user.take(40) }
        saveCurrent()
    }

    private fun explain(notes: List<String>, quotaHits: Int, tried: Int): String = when {
        tried == 0 -> "No chat models were found for this key. Open Profile > Gemini API and test the key."
        quotaHits == tried ->
            "Every Gemini model of this key is out of quota ($tried tried). The Google project of this key probably has no free quota (limit 0) or today's limit is used up. " +
                "Fix: create the key in a NEW project at aistudio.google.com, or turn on billing, or add another provider in Profile > Gemini API > Custom providers (Groq and OpenRouter have free keys)."
        else -> notes.take(3).joinToString("\n").ifBlank { "Request failed" }
    }

    private fun fail(m: String) { lastError = m; messages.add(Msg(m, false, true)) }

    private fun providerCode(e: Throwable): Int? = Regex("HTTP (\\d+)").find(e.message.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
    private fun retryableProvider(e: Throwable): Boolean = providerCode(e)?.let { it == 402 || it == 408 || it == 409 || it == 425 || it == 429 || it in 500..599 } == true

    /**
     * Adds the user message and answers it:
     *  1. the model you chose (a Gemini model or one of your custom providers), or Auto = best Gemini model for the task
     *  2. if it fails (quota, overload ...) the next Gemini model is tried automatically
     *  3. then your custom providers marked "backup"
     */
    suspend fun ask(text: String, image: ByteArray? = null, voice: Boolean = false, addUser: Boolean = true): String? {
        if (addUser) messages.add(Msg(text, true, image = image))
        lastError = null
        if (sessions.firstOrNull { it.id == currentId }?.title == "New chat") {
            sessions.firstOrNull { it.id == currentId }?.let { it.title = text.take(40).ifBlank { "Image chat" } }
        }
        val custom = CustomProviders.selected()
        val gemKey = Prefs.apiKey.trim()
        if (custom == null && gemKey.isBlank() && CustomProviders.fallbacks().isEmpty()) {
            fail("Add your Gemini API key in Profile > Gemini API, or add a custom provider."); saveCurrent(); return null
        }
        busy = true; thinking = ""; streaming = ""; startedAt = System.currentTimeMillis()
        return try {
            val src = if (Prefs.rememberChats) messages.takeLast(24) else messages.takeLast(1)
            val turns = src.filter { !it.error }
                .map { GeminiClient.Turn(if (it.user) "user" else "model", it.text, it.image) }
                .dropWhile { it.role == "model" }
            val system = systemPrompt(voice)
            var reply: String? = null
            var used = ""
            var lastErr = ""
            if (custom != null) {
                val routes = ModelRouter.rank(custom, ModelRouter.Task.CHAT, ModelRouter.estimateTokens(turns, text), image != null)
                for (route in routes) {
                    if (reply != null) break
                    lastModel = route.provider.name + " · " + route.model.id; thinking = ""; streaming = ""
                    val started = System.currentTimeMillis()
                    try {
                        val active = route.provider.copy(modelId = route.model.id)
                        reply = OpenAiCompat.chatStream(active, system, turns, { thinking += it }, { streaming += it })
                        used = route.provider.name + " · " + route.model.id
                        ModelRouter.markSuccess(route, System.currentTimeMillis() - started, ModelRouter.estimateTokens(turns, reply ?: ""))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        AppLog.w(e); lastErr = e.message ?: "Request failed"
                        if (ModelRouter.markFailure(route, e)) {
                            AppLog.i("Router", "${route.provider.name} · ${route.model.id} failed (${ModelRouter.httpCode(e) ?: "network"}); switching to next scored model")
                            thinking = "Switched model after a temporary failure…"
                        } else break
                    }
                }
                if (routes.isEmpty()) lastErr = "No eligible provider model is available; refresh the model list or wait for cooldowns."
            } else if (gemKey.isNotBlank()) {
                if (!ModelCatalog.ready) ModelCatalog.refresh()
                val candidates = ModelCatalog.chatCandidates(ModelCatalog.taskFor(text, image != null)).take(10)
                val notes = ArrayList<String>()
                var quotaHits = 0
                var stop = false
                for (m in candidates) {
                    if (stop) break
                    lastModel = m.id; thinking = ""; streaming = ""
                    var tries = 0
                    while (true) {
                        tries++
                        try {
                            reply = GeminiClient.chatStream(gemKey, m.id, system, turns, Prefs.webSearch, { thinking += it }, { streaming += it }); used = m.id
                            if (Prefs.goodChat != m.id) { Prefs.goodChat = m.id; Prefs.save() }      // remember what works: it is tried first next time
                            break
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            val code = (e as? GeminiClient.GeminiHttpException)?.code
                            if (code == 503 && tries < 2) { delay(1500); continue }                  // busy for a moment: ask the same model again
                            AppLog.i("Chat", "${m.id}: ${GeminiClient.shortReason(e)} → next model")
                            notes.add("${m.id}: ${GeminiClient.shortReason(e)}")
                            if (code == 429) quotaHits++
                            if (e.message?.contains("API key", true) == true) stop = true            // wrong key: other models will not help
                            break
                        }
                    }
                    if (reply != null) break
                }
                if (reply == null) {
                    lastErr = explain(notes, quotaHits, candidates.size)
                    AppLog.e("Chat", "All Gemini models failed. " + notes.joinToString("; "))
                }
            }
            if (reply == null) {
                val routes = if (custom == null) ModelRouter.rank(null, ModelRouter.Task.CHAT, ModelRouter.estimateTokens(turns, text), image != null) else emptyList()
                for (route in routes) {
                    if (reply != null) break
                    lastModel = route.provider.name + " · " + route.model.id
                    val started = System.currentTimeMillis()
                    try {
                        reply = OpenAiCompat.chat(route.provider.copy(modelId = route.model.id), system, turns)
                        used = route.provider.name + " · " + route.model.id
                        ModelRouter.markSuccess(route, System.currentTimeMillis() - started, ModelRouter.estimateTokens(turns, reply ?: ""))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        AppLog.w(e); lastErr = e.message ?: "Request failed"
                        if (ModelRouter.markFailure(route, e)) AppLog.i("Router", "${route.provider.name} · ${route.model.id} failed; switching to next scored model") else break
                    }
                }
            }
            val r = reply
            if (r == null) { fail(lastErr.ifBlank { "Request failed" }); saveCurrent(); null }
            else { messages.add(Msg(r, false, model = used, thought = thinking.trim().ifBlank { null })); lastModel = used; saveCurrent(); r }
        } finally { busy = false; thinking = ""; streaming = "" }
    }
}
