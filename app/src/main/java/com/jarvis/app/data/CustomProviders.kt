package com.jarvis.app.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import com.jarvis.app.audio.WavUtil
import com.jarvis.app.live.PhoneTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** An AI model from any provider that speaks the OpenAI-compatible chat API (OpenAI, OpenRouter, Groq, DeepSeek, Mistral, Together, Ollama, LM Studio ...). */
data class CustomProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val headers: String = "",         // optional extra headers, one per line:  Name: value
    val systemPrompt: String = "",    // optional: replaces the default system prompt
    val temperature: String = "",     // optional
    val vision: Boolean = false,      // sends photos to this model
    val fallback: Boolean = false,    // used automatically when Gemini fails
    val tools: Boolean = true,        // may use phone tools (screen control, apps, weather ...) during calls
    val sttModel: String = "",        // speech-to-text model for voice calls, e.g. whisper-1
    val ttsModel: String = "",        // text-to-speech model, e.g. tts-1
    val ttsVoices: String = "",       // the provider's own voices, comma separated
    val ttsVoice: String = "",        // the voice in use
    val speechFrom: String = ""       // take speech-to-text / voices from another provider (its id)
) {
    fun voiceList(): List<String> = ttsVoices.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
}

object CustomProviders {
    val list = mutableStateListOf<CustomProvider>()
    private var file: File? = null

    class Preset(val name: String, val baseUrl: String, val model: String, val stt: String = "", val tts: String = "", val voices: String = "")

    val presets = listOf(
        Preset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "whisper-1", "tts-1", "alloy, ash, coral, echo, fable, nova, onyx, sage, shimmer"),
        Preset("OpenRouter", "https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free"),
        Preset("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "whisper-large-v3-turbo", "playai-tts", "Arista-PlayAI, Atlas-PlayAI, Basil-PlayAI, Briggs-PlayAI, Calum-PlayAI, Celeste-PlayAI, Chip-PlayAI, Cillian-PlayAI, Fritz-PlayAI, Gail-PlayAI, Indigo-PlayAI, Mason-PlayAI, Mikail-PlayAI, Quinn-PlayAI"),
        Preset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
        Preset("Mistral", "https://api.mistral.ai/v1", "mistral-small-latest"),
        Preset("Together", "https://api.together.xyz/v1", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
        Preset("Ollama (PC on your Wi-Fi)", "http://192.168.1.2:11434/v1", "llama3.2"),
        Preset("Other", "https://", "")
    )

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "providers.json"); file = f
        try {
            if (f.exists()) {
                val a = JSONArray(f.readText())
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    list.add(CustomProvider(o.getString("id"), o.getString("n"), o.getString("u"), o.optString("k"), o.getString("m"),
                        o.optString("h"), o.optString("s"), o.optString("t"), o.optBoolean("v"), o.optBoolean("f"),
                        o.optBoolean("tl", true), o.optString("stt"), o.optString("tts"), o.optString("tvs"), o.optString("tv"), o.optString("sf")))
                }
            }
        } catch (e: Exception) { AppLog.w(e) }
    }

    private fun save() {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("n", it.name).put("u", it.baseUrl).put("k", it.apiKey).put("m", it.modelId)
            .put("h", it.headers).put("s", it.systemPrompt).put("t", it.temperature).put("v", it.vision).put("f", it.fallback)
            .put("tl", it.tools).put("stt", it.sttModel).put("tts", it.ttsModel).put("tvs", it.ttsVoices).put("tv", it.ttsVoice).put("sf", it.speechFrom)) }
        try { file?.writeText(a.toString()) } catch (e: Exception) { AppLog.w(e) }
    }

    fun upsert(p: CustomProvider) {
        val i = list.indexOfFirst { it.id == p.id }
        if (i >= 0) list[i] = p else list.add(p)
        save()
    }

    fun delete(id: String) {
        list.removeAll { it.id == id }
        if (Prefs.manualModel == "custom:$id") { Prefs.manualModel = ""; Prefs.save() }
        save()
    }

    /** The provider chosen in the chat model menu, or null when a Gemini model (or Auto) is selected. */
    fun selected(): CustomProvider? =
        if (Prefs.manualModel.startsWith("custom:")) list.firstOrNull { it.id == Prefs.manualModel.removePrefix("custom:") } else null

    fun fallbacks(): List<CustomProvider> = list.filter { it.fallback }

    /** The provider whose speech-to-text and voices are used for [p] (itself, unless it borrows another one). */
    fun speechOf(p: CustomProvider): CustomProvider =
        if (p.speechFrom.isNotBlank()) list.firstOrNull { it.id == p.speechFrom } ?: p else p

    /** A provider can run a voice call when it can hear (its own STT or a Gemini key) . */
    fun canCall(p: CustomProvider): Boolean = speechOf(p).sttModel.isNotBlank() || Prefs.apiKey.isNotBlank()

    fun callProvider(): CustomProvider? = list.firstOrNull { it.id == Prefs.callProvider }
}

/** Talks to any OpenAI-compatible server: chat, tool calling, speech-to-text, text-to-speech and the model list. */
object OpenAiCompat {
    private fun base(p: CustomProvider) = p.baseUrl.trim().trimEnd('/').removeSuffix("/chat/completions")

    private fun open(p: CustomProvider, path: String, post: Boolean, readMs: Int): HttpURLConnection {
        val c = URL(base(p) + path).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = readMs
        if (p.apiKey.isNotBlank()) c.setRequestProperty("Authorization", "Bearer ${p.apiKey.trim()}")
        c.setRequestProperty("Content-Type", "application/json")
        p.headers.lines().forEach { l ->
            val i = l.indexOf(':')
            if (i > 0) c.setRequestProperty(l.substring(0, i).trim(), l.substring(i + 1).trim())
        }
        if (post) { c.requestMethod = "POST"; c.doOutput = true } else c.requestMethod = "GET"
        return c
    }

    private fun errorText(c: HttpURLConnection, p: CustomProvider, code: Int): String {
        val text = c.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        val msg = try {
            val e = JSONObject(text).opt("error")
            if (e is JSONObject) e.optString("message") else e?.toString() ?: text.take(200)
        } catch (x: Exception) { text.take(200) }
        return "${p.name}: HTTP $code: ${msg.take(240)}"
    }

    private fun ensureOk(c: HttpURLConnection, p: CustomProvider) {
        val code = c.responseCode
        if (code !in 200..299) throw IOException(errorText(c, p, code))
    }

    private fun read(c: HttpURLConnection, p: CustomProvider): String {
        ensureOk(c, p)
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun buildMessages(p: CustomProvider, system: String, turns: List<GeminiClient.Turn>): JSONArray {
        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", p.systemPrompt.ifBlank { system }))
        turns.forEach { t ->
            val role = if (t.role == "user") "user" else "assistant"
            if (t.image != null && p.vision) {
                val parts = JSONArray()
                parts.put(JSONObject().put("type", "text").put("text", t.text.ifBlank { "Describe this image." }))
                parts.put(JSONObject().put("type", "image_url").put("image_url",
                    JSONObject().put("url", "data:image/jpeg;base64," + Base64.encodeToString(t.image, Base64.NO_WRAP))))
                msgs.put(JSONObject().put("role", role).put("content", parts))
            } else msgs.put(JSONObject().put("role", role).put("content", t.text.ifBlank { "Hello" }))
        }
        return msgs
    }

    private fun contentOf(m: JSONObject): String = when (val content = m.opt("content")) {
        is String -> content
        is JSONArray -> (0 until content.length()).joinToString("") { content.optJSONObject(it)?.optString("text").orEmpty() }
        else -> ""
    }.trim()

    suspend fun chat(p: CustomProvider, system: String, turns: List<GeminiClient.Turn>): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("model", p.modelId.trim()).put("messages", buildMessages(p, system, turns)).put("stream", false)
        p.temperature.toDoubleOrNull()?.let { body.put("temperature", it) }
        val c = open(p, "/chat/completions", true, 90000)
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val j = JSONObject(read(c, p))
        val reply = contentOf(j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: JSONObject())
        if (reply.isEmpty()) throw IOException("${p.name}: empty reply")
        reply
    }

    /** Raw /chat/completions call (used by the code agent for tool calling). */
    suspend fun completions(p: CustomProvider, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val c = open(p, "/chat/completions", true, 120000)
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        JSONObject(read(c, p))
    }

    /** Chat that shows the answer as it arrives (and the reasoning, for models that send it). */
    suspend fun chatStream(
        p: CustomProvider, system: String, turns: List<GeminiClient.Turn>, onThought: (String) -> Unit, onText: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("model", p.modelId.trim()).put("messages", buildMessages(p, system, turns)).put("stream", true)
        p.temperature.toDoubleOrNull()?.let { body.put("temperature", it) }
        val c = open(p, "/chat/completions", true, 120000)
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        ensureOk(c, p)
        val sb = StringBuilder()
        val raw = StringBuilder()
        c.inputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (!line.startsWith("data:")) { raw.append(line); continue }
                val js = line.removePrefix("data:").trim()
                if (js.isEmpty() || js == "[DONE]") continue
                val d = (try { JSONObject(js).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") } catch (e: Exception) { null }) ?: continue
                val r = if (!d.isNull("reasoning_content")) d.optString("reasoning_content") else if (!d.isNull("reasoning")) d.optString("reasoning") else ""
                if (r.isNotEmpty() && r != "null") onThought(r)
                val t = if (d.isNull("content")) "" else d.optString("content")
                if (t.isNotEmpty()) { sb.append(t); onText(t) }
            }
        }
        if (sb.isEmpty() && raw.isNotEmpty()) {          // the server ignored "stream" and sent one plain answer
            val m = try { JSONObject(raw.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message") } catch (e: Exception) { null }
            if (m != null) { val t = contentOf(m); sb.append(t); onText(t) }
        }
        val out = sb.toString().trim()
        if (out.isEmpty()) throw IOException("${p.name}: empty reply")
        out
    }

    /** Chat with the phone tools available: the model may call them (several steps), then gives the final answer. */
    suspend fun chatWithTools(
        p: CustomProvider, system: String, turns: List<GeminiClient.Turn>, exec: (String, JSONObject) -> String
    ): String = withContext(Dispatchers.IO) {
        val msgs = buildMessages(p, system, turns)
        var useTools = p.tools
        repeat(6) {
            val body = JSONObject().put("model", p.modelId.trim()).put("messages", msgs).put("stream", false)
            if (useTools) body.put("tools", PhoneTools.openAi()).put("tool_choice", "auto")
            p.temperature.toDoubleOrNull()?.let { body.put("temperature", it) }
            val c = open(p, "/chat/completions", true, 90000)
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            if (code in listOf(400, 404, 422) && useTools) {
                AppLog.i("Call", "${p.name}: ${errorText(c, p, code).take(160)} → trying without phone tools")
                useTools = false
                return@repeat
            }
            val m = JSONObject(read(c, p)).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val calls = m.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                val reply = contentOf(m)
                if (reply.isEmpty()) throw IOException("${p.name}: empty reply")
                return@withContext reply
            }
            msgs.put(m)
            for (i in 0 until calls.length()) {
                val tc = calls.getJSONObject(i)
                val f = tc.getJSONObject("function")
                val args = try { JSONObject(f.optString("arguments").ifBlank { "{}" }) } catch (e: Exception) { JSONObject() }
                val result = try { exec(f.getString("name"), args) } catch (e: Exception) { AppLog.w(e); "error: ${e.message}" }
                AppLog.i("Call", "tool ${f.optString("name")} → ${result.take(80)}")
                msgs.put(JSONObject().put("role", "tool").put("tool_call_id", tc.optString("id")).put("content", result))
            }
        }
        throw IOException("${p.name}: too many tool steps")
    }

    /** Speech to text (Whisper-style: POST /audio/transcriptions). */
    suspend fun transcribe(p: CustomProvider, wav: ByteArray, lang: String): String = withContext(Dispatchers.IO) {
        val boundary = "----jarvis" + System.currentTimeMillis()
        val c = open(p, "/audio/transcriptions", true, 60000)
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        c.outputStream.use { out ->
            fun field(name: String, value: String) { out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray()) }
            field("model", p.sttModel.trim())
            if (lang.isNotBlank()) field("language", lang)
            field("response_format", "json")
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n".toByteArray())
            out.write(wav)
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        JSONObject(read(c, p)).optString("text").trim()
    }

    /** Text to speech with the provider's own voice (POST /audio/speech). Returns 16-bit mono PCM at 24 kHz. */
    suspend fun speech(p: CustomProvider, text: String): ByteArray = withContext(Dispatchers.IO) {
        val voice = p.ttsVoice.trim().ifBlank { p.voiceList().firstOrNull() ?: "alloy" }
        val body = JSONObject().put("model", p.ttsModel.trim()).put("input", text.take(1500)).put("voice", voice).put("response_format", "wav")
        val c = open(p, "/audio/speech", true, 60000)
        c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        ensureOk(c, p)
        WavUtil.toPcm24k(c.inputStream.use { it.readBytes() })
    }

    /** Asks the server which model ids it offers, so you can pick one instead of typing it. */
    suspend fun fetchModels(p: CustomProvider): List<String> = withContext(Dispatchers.IO) {
        val j = JSONObject(read(open(p, "/models", false, 20000), p))
        val arr = j.optJSONArray("data") ?: JSONArray()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("id")?.takeIf { s -> s.isNotBlank() } }.sorted()
    }
}
