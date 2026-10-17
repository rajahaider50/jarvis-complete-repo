package com.jarvis.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real, tiny requests that check whether a provider model actually answers, how fast, and
 * whether it can call tools. Everything reported here comes back from the provider itself,
 * so the UI can mark a model "Verified" instead of guessing.
 */
object ProbeService {
    data class Result(
        val ok: Boolean,
        val latencyMs: Long = 0,
        val tools: Boolean? = null,     // null = not tested / unknown
        val reply: String = "",
        val error: String = ""
    )

    private val tinyTools = JSONArray().put(
        JSONObject()
            .put("type", "function")
            .put("function", JSONObject()
                .put("name", "get_time")
                .put("description", "Get the current time")
                .put("parameters", JSONObject().put("type", "object").put("properties", JSONObject())))
    )

    /** Sends one short message and measures the round-trip. Never fakes a result: a failure is reported as a failure. */
    suspend fun probe(p: CustomProvider, modelId: String, testTools: Boolean = false): Result = withContext(Dispatchers.IO) {
        val target = p.copy(modelId = modelId.trim())
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", "Reply with the single word OK."))
        val body = JSONObject().put("model", target.modelId).put("messages", messages).put("stream", false).put("max_tokens", 64)
        if (testTools) body.put("tools", tinyTools).put("tool_choice", "auto")
        val started = System.currentTimeMillis()
        try {
            val j = OpenAiCompat.completions(target, body)
            val ms = System.currentTimeMillis() - started
            val msg = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: JSONObject()
            val calls = msg.optJSONArray("tool_calls")
            val tools: Boolean? = if (testTools) (calls != null && calls.length() > 0) else null
            val reply = msg.optString("content").trim()
            Result(ok = true, latencyMs = ms, tools = tools, reply = reply.take(120))
        } catch (e: Exception) {
            AppLog.w(e)
            Result(ok = false, latencyMs = System.currentTimeMillis() - started, error = e.message ?: "Probe failed")
        }
    }
}
