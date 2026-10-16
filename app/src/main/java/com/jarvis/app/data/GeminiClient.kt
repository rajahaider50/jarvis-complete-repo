package com.jarvis.app.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Gemini API client (REST, v1beta). No model names are hard-coded:
 * the list of usable models always comes from models.list for the user's own key.
 */
object GeminiClient {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"

    class GeminiHttpException(val code: Int, msg: String, val retryAfter: Int? = null, val quota: String = "") :
        IOException("HTTP $code: $msg" + (if (quota.isNotBlank()) " [$quota]" else "") + (retryAfter?.let { " · retry in ${it}s" } ?: ""))

    /** One short human reason for a failed request. */
    fun shortReason(e: Throwable): String {
        val h = e as? GeminiHttpException ?: return (e.message ?: "error").take(80)
        return when (h.code) {
            429 -> "quota used up" + (if (h.quota.contains("limit: 0") || h.quota.contains("FreeTier", true)) " (no free quota)" else "") + (h.retryAfter?.let { ", retry in ${it}s" } ?: "")
            503 -> "busy right now"
            404 -> "not available for this key"
            403 -> "no permission"
            else -> "HTTP ${h.code}"
        }
    }

    private fun parseError(code: Int, text: String): GeminiHttpException {
        var msg = text.take(200)
        var retry: Int? = null
        var quota = ""
        try {
            val err = JSONObject(text).getJSONObject("error")
            msg = err.getString("message").substringBefore(" For more information").take(200)
            val details = err.optJSONArray("details")
            if (details != null) for (i in 0 until details.length()) {
                val d = details.getJSONObject(i)
                val type = d.optString("@type")
                if (type.endsWith("RetryInfo")) retry = d.optString("retryDelay").removeSuffix("s").toDoubleOrNull()?.toInt()
                if (type.endsWith("QuotaFailure")) {
                    val v = d.optJSONArray("violations")?.optJSONObject(0)
                    if (v != null) quota = v.optString("quotaId").ifBlank { v.optString("quotaMetric").substringAfterLast('/') }
                }
            }
        } catch (e: Exception) { /* the server did not send JSON: keep the raw text */ }
        return GeminiHttpException(code, msg, retry, quota)
    }

    /** Raw generateContent call (used by the code agent for tool calling). */
    suspend fun generate(key: String, model: String, body: JSONObject, timeoutMs: Int = 120000): JSONObject =
        withContext(Dispatchers.IO) { JSONObject(call("models/$model:generateContent", key, body.toString(), timeoutMs)) }

    private fun contentsOf(turns: List<Turn>): JSONArray {
        val contents = JSONArray()
        turns.forEach { t ->
            val parts = JSONArray()
            if (t.image != null) parts.put(JSONObject().put("inlineData", JSONObject().put("mimeType", "image/jpeg").put("data", Base64.encodeToString(t.image, Base64.NO_WRAP))))
            parts.put(JSONObject().put("text", t.text.ifBlank { "Describe this image." }))
            contents.put(JSONObject().put("role", t.role).put("parts", parts))
        }
        return contents
    }

    private fun streamOnce(key: String, model: String, body: JSONObject, onThought: (String) -> Unit, onText: (String) -> Unit): String {
        val c = URL("$BASE/models/$model:streamGenerateContent?alt=sse").openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 120000
        c.setRequestProperty("x-goog-api-key", key); c.setRequestProperty("Content-Type", "application/json")
        c.requestMethod = "POST"; c.doOutput = true
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        if (code !in 200..299) throw parseError(code, c.errorStream?.bufferedReader()?.use { it.readText() } ?: "")
        val sb = StringBuilder()
        c.inputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (!line.startsWith("data:")) continue
                val js = line.removePrefix("data:").trim()
                if (js.isEmpty()) continue
                val parts = (try { JSONObject(js).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") } catch (e: Exception) { null }) ?: continue
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    val t = p.optString("text", "")
                    if (t.isEmpty()) continue
                    if (p.optBoolean("thought", false)) onThought(t) else { sb.append(t); onText(t) }
                }
            }
        }
        val out = sb.toString().trim()
        if (out.isEmpty()) throw IOException("The model returned an empty answer")
        return out
    }

    /** Chat that shows the answer while it is being written, and the model's own thought summary while it thinks. */
    suspend fun chatStream(
        key: String, model: String, system: String, turns: List<Turn>, search: Boolean,
        onThought: (String) -> Unit, onText: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        var thinking = true
        var useSearch = search
        for (attempt in 0..3) {
            val body = JSONObject().put("contents", contentsOf(turns))
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            if (useSearch) body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
            if (thinking) body.put("generationConfig", JSONObject().put("thinkingConfig", JSONObject().put("includeThoughts", true)))
            try { return@withContext streamOnce(key, model, body, onThought, onText) }
            catch (e: GeminiHttpException) {
                if (e.code == 400 && thinking) { thinking = false; continue }                                // this model has no thought summaries
                if (useSearch && (e.code == 400 || e.code == 403 || e.code == 429)) { useSearch = false; continue }   // no search quota: answer without it
                throw e
            }
        }
        throw IOException("The model did not answer")
    }

    data class ModelInfo(val id: String, val display: String, val methods: List<String>)

    private fun call(path: String, key: String, body: String?, readMs: Int = 90000): String {
        val conn = URL("$BASE/$path").openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = readMs
        conn.setRequestProperty("x-goog-api-key", key)
        conn.setRequestProperty("Content-Type", "application/json")
        if (body != null) {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        } else {
            conn.requestMethod = "GET"
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw parseError(code, text)
        return text
    }

    suspend fun listModels(key: String): List<ModelInfo> = withContext(Dispatchers.IO) {
        val out = mutableListOf<ModelInfo>()
        var token: String? = null
        do {
            val path = "models?pageSize=1000" + (token?.let { "&pageToken=$it" } ?: "")
            val j = JSONObject(call(path, key, null))
            val arr = j.optJSONArray("models") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                val ms = m.optJSONArray("supportedGenerationMethods")
                val methods = ArrayList<String>()
                if (ms != null) for (k in 0 until ms.length()) methods.add(ms.getString(k))
                out.add(ModelInfo(m.getString("name").removePrefix("models/"), m.optString("displayName"), methods))
            }
            val next = j.optString("nextPageToken", "")
            token = if (next.isEmpty()) null else next
        } while (token != null)
        out
    }

    private val notChat = listOf("tts", "image", "live", "audio", "embedding", "robotics", "computer", "imagen", "veo", "native")
    fun isChat(m: ModelInfo) = m.id.startsWith("gemini") && "generateContent" in m.methods && notChat.none { it in m.id }
    fun isLive(m: ModelInfo) = "bidiGenerateContent" in m.methods
    fun isTts(m: ModelInfo) = "tts" in m.id && "generateContent" in m.methods

    fun sortChat(l: List<ModelInfo>) = l.sortedByDescending { it.id }

    fun pickDefault(chat: List<ModelInfo>): ModelInfo =
        chat.firstOrNull { "flash" in it.id && "lite" !in it.id && "preview" !in it.id && "exp" !in it.id }
            ?: chat.firstOrNull { "flash" in it.id && "lite" !in it.id && "preview" !in it.id && "exp" !in it.id }
            ?: chat.firstOrNull { "flash" in it.id }
            ?: chat.first()

    fun pickTts(tts: List<ModelInfo>): ModelInfo? = tts.firstOrNull { "flash" in it.id } ?: tts.firstOrNull()

    data class Turn(val role: String, val text: String, val image: ByteArray? = null)

    suspend fun chat(key: String, model: String, system: String, turns: List<Turn>, search: Boolean = false): String =
        withContext(Dispatchers.IO) {
            val contents = JSONArray()
            turns.forEach { t ->
                val parts = JSONArray()
                if (t.image != null) parts.put(JSONObject().put("inlineData",
                    JSONObject().put("mimeType", "image/jpeg").put("data", Base64.encodeToString(t.image, Base64.NO_WRAP))))
                parts.put(JSONObject().put("text", t.text.ifBlank { "Describe this image." }))
                contents.put(JSONObject().put("role", t.role).put("parts", parts))
            }
            val base = JSONObject()
                .put("contents", contents)
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            val withSearch = JSONObject(base.toString()).put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
            val path = "models/$model:generateContent"
            val j = if (search) {
                try { JSONObject(call(path, key, withSearch.toString())) }
                catch (e: GeminiHttpException) { AppLog.w(e); JSONObject(call(path, key, base.toString())) }   // search not included in this key/plan: answer without it
            } else JSONObject(call(path, key, base.toString()))
            val parts = j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val sb = StringBuilder()
            if (parts != null) for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (!p.optBoolean("thought", false)) sb.append(p.optString("text", ""))
            }
            val reply = sb.toString().trim()
            if (reply.isEmpty()) throw IOException("Empty reply (the answer may have been blocked)")
            reply
        }

    /** A tiny test request. Returns null when the model works, otherwise the reason it does not. */
    suspend fun probe(key: String, model: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Reply with OK")))))
                .put("generationConfig", JSONObject().put("maxOutputTokens", 16)).toString()
            call("models/$model:generateContent", key, body, 20000)
            null
        } catch (e: Exception) { shortReason(e) }
    }

    /** Speech-to-text with a Gemini model (used by custom-provider calls that have no speech model of their own). */
    suspend fun transcribe(key: String, model: String, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val parts = JSONArray()
            .put(JSONObject().put("inlineData", JSONObject().put("mimeType", "audio/wav").put("data", Base64.encodeToString(wav, Base64.NO_WRAP))))
            .put(JSONObject().put("text", "Transcribe this speech exactly, in its original language. Reply with only the transcript, nothing else. If there is no speech, reply with an empty string."))
        val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", parts))).toString()
        val j = JSONObject(call("models/$model:generateContent", key, body, 40000))
        val p = j.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val sb = StringBuilder()
        if (p != null) for (i in 0 until p.length()) sb.append(p.getJSONObject(i).optString("text", ""))
        sb.toString().trim()
    }

    /** Returns raw PCM (16-bit, mono, 24 kHz) of the spoken text. */
    suspend fun tts(key: String, model: String, voice: String, text: String): ByteArray =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))
                .put("generationConfig", JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put("speechConfig", JSONObject().put("voiceConfig",
                        JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice)))))
                .toString()
            val j = JSONObject(call("models/$model:generateContent", key, body, 45000))
            val cand = j.optJSONArray("candidates")?.optJSONObject(0)
            val parts = cand?.optJSONObject("content")?.optJSONArray("parts")
            var data: String? = null
            if (parts != null) for (i in 0 until parts.length()) {
                val d = parts.getJSONObject(i).optJSONObject("inlineData")?.optString("data")
                if (!d.isNullOrEmpty()) { data = d; break }
            }
            if (data == null) throw IOException("No audio returned (" + (cand?.optString("finishReason") ?: "empty reply") + ")")
            Base64.decode(data, Base64.DEFAULT)
        }
}
