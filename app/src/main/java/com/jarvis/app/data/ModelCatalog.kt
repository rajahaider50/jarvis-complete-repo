package com.jarvis.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.GeminiClient.ModelInfo
import kotlinx.coroutines.CancellationException

/** Real models available for the user's key (from models.list) + automatic per-task selection. */
object ModelCatalog {
    var chat by mutableStateOf(listOf<ModelInfo>())
    var tts by mutableStateOf(listOf<ModelInfo>())
    var live by mutableStateOf(listOf<ModelInfo>())
    var loading by mutableStateOf(false)
    var ok by mutableStateOf<Boolean?>(null)
    var status by mutableStateOf("")

    val ready get() = chat.isNotEmpty()

    enum class Task { FAST, IMAGE, COMPLEX }

    suspend fun refresh() {
        val key = Prefs.apiKey.trim()
        if (key.isEmpty()) { ok = null; status = "Add your API key first"; return }
        loading = true; status = "Contacting Google…"
        try {
            val all = GeminiClient.listModels(key)
            chat = GeminiClient.sortChat(all.filter { GeminiClient.isChat(it) })
            tts = all.filter { GeminiClient.isTts(it) }.sortedByDescending { it.id }
            live = all.filter { GeminiClient.isLive(it) }.sortedByDescending { it.id }
            ok = chat.isNotEmpty()
            status = if (chat.isNotEmpty()) "Connected · ${chat.size} chat · ${tts.size} voice · ${live.size} live models"
            else "This key has no Gemini chat models"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { AppLog.w(e);
            ok = false; status = e.message ?: "Connection failed"
        }
        loading = false
    }

    private fun stable(f: (ModelInfo) -> Boolean): ModelInfo? =
        chat.firstOrNull { f(it) && "preview" !in it.id && "exp" !in it.id } ?: chat.firstOrNull(f)

    fun pick(task: Task): ModelInfo? {
        val m = Prefs.manualModel
        if (m.isNotBlank()) chat.firstOrNull { it.id == m }?.let { return it }
        if (chat.isEmpty()) return null
        return when (task) {
            Task.COMPLEX -> stable { "pro" in it.id } ?: stable { "flash" in it.id && "lite" !in it.id } ?: chat.first()
            Task.IMAGE -> stable { "flash" in it.id && "lite" !in it.id } ?: stable { "pro" in it.id } ?: chat.first()
            Task.FAST -> stable { "flash-lite" in it.id } ?: stable { "flash" in it.id } ?: chat.first()
        }
    }

    private fun rank(m: ModelInfo): Int {
        val id = m.id
        val base = when { "flash-lite" in id -> 0; "flash" in id -> 1; "pro" in id -> 3; else -> 2 }
        return base + if ("preview" in id || "exp" in id) 10 else 0
    }

    /** The best model for the task first, then every other chat model (cheap and stable ones first). Used to fall back on quota errors. */
    fun chatCandidates(task: Task): List<ModelInfo> =
        (listOfNotNull(chat.firstOrNull { it.id == Prefs.goodChat && Prefs.manualModel.isBlank() }, pick(task)) + chat.sortedBy { rank(it) }).distinctBy { it.id }

    class Probe(val id: String, val result: String?)       // result null = works
    var probes by mutableStateOf(listOf<Probe>())
    var probing by mutableStateOf(false)

    /** Sends a tiny request to every chat model of the key and remembers the first one that works. */
    suspend fun diagnose() {
        val key = Prefs.apiKey.trim()
        if (key.isEmpty() || probing) return
        probing = true; probes = emptyList()
        try {
            if (chat.isEmpty()) refresh()
            val out = mutableListOf<Probe>()
            for (m in chat.sortedBy { rank(it) }.take(12)) {
                out.add(Probe(m.id, GeminiClient.probe(key, m.id)))
                probes = out.toList()
            }
            out.firstOrNull { it.result == null }?.let { Prefs.goodChat = it.id; Prefs.save() }
            AppLog.i("API", "Model test: " + out.joinToString { it.id + "=" + (it.result ?: "ok") })
        } finally { probing = false }
    }

    fun taskFor(text: String, hasImage: Boolean): Task {
        if (hasImage) return Task.IMAGE
        val l = text.lowercase()
        val keys = listOf("code", "analy", "explain in detail", "step by step", "compare", "essay", "plan ", "strategy",
            "calculate", "prove", "algorithm", "debug", "research", "تفصیل", "تجزیہ", "کوڈ", "موازنہ")
        return if (text.length > 350 || keys.any { it in l }) Task.COMPLEX else Task.FAST
    }

    /** Every model that may be able to speak, best first. Speaker tries them in this order until one works. */
    fun ttsCandidates(): List<String> {
        val out = ArrayList<String>()
        if (com.jarvis.app.audio.Speaker.lastModel.isNotBlank() && (tts + chat).any { it.id == com.jarvis.app.audio.Speaker.lastModel })
            out.add(com.jarvis.app.audio.Speaker.lastModel)
        tts.filter { "pro" !in it.id }.forEach { out.add(it.id) }
        tts.filter { "pro" in it.id }.forEach { out.add(it.id) }
        chat.filter { "flash" in it.id }.take(2).forEach { out.add(it.id) }   // last resort: newer models that speak without "tts" in the name
        return out.distinct()
    }
    fun ttsModel(): String? = ttsCandidates().firstOrNull()
    /** Live models in the user's saved priority order first, then every other Live model of the key. */
    fun liveCandidates(): List<String> = (Prefs.liveModels.toList() + listOfNotNull(liveModel()) + live.map { it.id }).distinct()
    fun liveModel(): String? = live.firstOrNull { "flash" in it.id }?.id ?: live.firstOrNull()?.id
}
