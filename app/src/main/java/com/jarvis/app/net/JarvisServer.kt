package com.jarvis.app.net

import com.jarvis.app.data.Prefs
import com.jarvis.app.live.Diag
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The app's ONE door to the owner's own "brain server" (Phase 4).
 *
 * Rules kept from the rest of the app:
 *  - real HTTP only ([HttpURLConnection], the same class the provider code uses) — never a fake reply;
 *  - the app must work exactly as before when no server is set or reachable, so every call here returns a
 *    [Result] instead of throwing and the callers decide what to do;
 *  - every result (ok or failure) is written to [Diag] as evidence (`JARVIS_DIAG: srv: …`) and shown on
 *    Profile > Brain server and on the Diagnostics screen.
 *
 * Call these from a background thread (the UI screens use `withContext(Dispatchers.IO)`).
 */
object JarvisServer {

    /** One reply. [body] is the real JSON (null when the call never reached a server). */
    data class Result(val ok: Boolean, val status: Int, val body: JSONObject?, val error: String = "") {
        fun str(key: String, fallback: String = ""): String = body?.optString(key, fallback) ?: fallback
        fun int(key: String): Int = body?.optInt(key, 0) ?: 0
        fun obj(key: String): JSONObject? = body?.optJSONObject(key)
        fun arr(key: String): JSONArray? = body?.optJSONArray(key)
    }

    private val stamp = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun configured(): Boolean = Prefs.serverUrl.isNotBlank()

    /** Usable = the switch is on AND a URL is set. Nothing else in the app depends on this. */
    fun usable(): Boolean = Prefs.serverEnabled && configured()

    fun base(): String = Prefs.serverUrl.trim().trimEnd('/')

    // ------------------------------------------------------------------ public calls
    /** `GET /health` — always open, no token needed. Also fills the evidence counters. */
    fun health(): Result {
        val r = request("GET", "/health")
        if (r.ok) {
            Diag.srvVersion = r.str("version")
            Diag.srvMemories = r.int("memories")
            Diag.srvSkills = r.int("skills")
            Diag.srvJobs = r.int("jobs_queued")
            Diag.srvMood = r.str("mood")
            val cfg = r.obj("config")
            Diag.log("srv", "health ok v=${Diag.srvVersion} memories=${Diag.srvMemories} skills=${Diag.srvSkills}" +
                    " jobs=${Diag.srvJobs} mood=${Diag.srvMood}" +
                    " auth=${cfg?.optBoolean("auth_required")} llm=${cfg?.optBoolean("llm_ready")}")
        }
        return r
    }

    /** `GET /api/state` — counts + affect + learner status (needs the token when the server has one). */
    fun state(): Result {
        val r = request("GET", "/api/state")
        if (r.ok) {
            r.obj("counts")?.let {
                Diag.srvMemories = it.optInt("memories", Diag.srvMemories)
                Diag.srvSkills = it.optInt("skills", Diag.srvSkills)
            }
            r.obj("affect")?.let { Diag.srvMood = it.optString("mood", Diag.srvMood) }
            r.obj("learner")?.let { l ->
                Diag.srvLearner = (if (l.optBoolean("running")) "on" else "off") +
                        "/${l.optInt("interval_s")}s queued=${l.optInt("queued")}" +
                        " done=${l.optInt("done")} failed=${l.optInt("failed")}"
            }
            Diag.log("srv", "state ok memories=${Diag.srvMemories} skills=${Diag.srvSkills} mood=${Diag.srvMood} learner=${Diag.srvLearner}")
        }
        return r
    }

    /** `POST /api/memories` — one durable note (used by the memory sync in S3). */
    fun addMemory(text: String, kind: String = "note", tags: String = ""): Result =
        request("POST", "/api/memories", JSONObject().put("text", text).put("kind", kind).put("tags", tags).put("source", "app"))

    /** `POST /api/recall` — real ranked recall from the server's memory (S3). */
    fun recall(query: String, limit: Int = 8): Result =
        request("POST", "/api/recall", JSONObject().put("query", query).put("limit", limit))

    /** `POST /api/learn` — queue a real learning job (distil memory + write a skill while the owner is busy). */
    fun learn(text: String, task: String = "", observation: String = "", name: String = "", skill: Boolean = true): Result =
        request("POST", "/api/learn", JSONObject()
            .put("text", text).put("task", task).put("observation", observation)
            .put("name", name).put("skill", skill), 15000)

    /** `GET /api/jobs` — the queue with each job's real result/error. */
    fun jobs(limit: Int = 20): Result = request("GET", "/api/jobs?limit=$limit")

    /** `POST /api/affect` — record something that really happened (S6). */
    fun affectEvent(kind: String, weight: Double = 1.0, note: String = ""): Result =
        request("POST", "/api/affect", JSONObject().put("kind", kind).put("weight", weight).put("note", note))

    /** `GET /api/skills` — the server's skill book (S5). */
    fun skills(): Result = request("GET", "/api/skills")

    // ------------------------------------------------------------------ transport
    private fun request(method: String, path: String, body: JSONObject? = null, readMs: Int = 12000): Result {
        val url = base()
        Diag.srvUrl = url
        if (url.isBlank()) {
            note("not configured", "No server URL set (Profile > Brain server)")
            return Result(false, 0, null, "no server URL set")
        }
        Diag.srvCalls++
        var c: HttpURLConnection? = null
        return try {
            val conn = URL(url + path).openConnection() as HttpURLConnection
            c = conn
            conn.connectTimeout = 6000
            conn.readTimeout = readMs
            conn.requestMethod = method
            conn.setRequestProperty("Accept", "application/json")
            if (Prefs.serverToken.isNotBlank()) conn.setRequestProperty("X-Jarvis-Token", Prefs.serverToken.trim())
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conn.connect()
            if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try { JSONObject(text) } catch (e: Exception) { null }
            if (code in 200..299 && json != null && json.optBoolean("ok", true)) {
                Diag.srvState = "ok"
                Diag.srvLastError = ""
                Diag.srvLastOk = stamp.format(Date())
                Result(true, code, json)
            } else {
                val msg = if (json != null) json.optString("error", text.take(180)) else text.take(180)
                val what = when (code) {
                    401, 403 -> "HTTP $code (token mismatch?)"
                    else -> "HTTP $code"
                }
                note(what, msg.ifBlank { "no message" })
                Result(false, code, json, "$what: ${msg.ifBlank { "no message" }}")
            }
        } catch (e: Exception) {
            note(e.javaClass.simpleName, e.message ?: "unreachable")
            Result(false, 0, null, "${e.javaClass.simpleName}: ${e.message ?: "unreachable"}")
        } finally {
            try { c?.disconnect() } catch (x: Exception) { /* nothing else to do */ }
        }
    }

    /** Records the real failure on screen + in logcat, so nothing is silently swallowed. */
    private fun note(what: String, why: String) {
        Diag.srvState = what
        Diag.srvLastError = why
        Diag.log("srv", "$what -> $why")
    }
}
