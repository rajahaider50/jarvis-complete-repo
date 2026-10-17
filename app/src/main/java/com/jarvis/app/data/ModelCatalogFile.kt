package com.jarvis.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * A small offline catalog of well-known models, so a provider that does not report
 * context size / tool support still shows good values. Values are only ever facts we
 * can stand behind; anything not listed stays `unknown` (null) rather than guessed.
 *
 * Ships bundled in assets/model_catalog.json and can be refreshed from a URL the user sets
 * (Profile > Gemini API > Custom providers > Model catalog). A downloaded copy wins over the bundled one.
 */
object ModelCatalogFile {
    data class Entry(
        val match: String,
        val contextWindow: Int? = null,
        val supportsTools: Boolean? = null,
        val qualityTier: Int? = null,
        val notes: String = ""
    )

    var version: Int = 0
        private set
    var source: String = "bundled"
        private set
    private var entries: List<Entry> = emptyList()
    private var appCtx: Context? = null
    private var file: File? = null

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        file = File(ctx.filesDir, "model_catalog.json")
        load()
    }

    private fun load() {
        val remote = file?.takeIf { it.exists() }?.readText()
        val text = remote ?: try {
            appCtx?.assets?.open("model_catalog.json")?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) { AppLog.w(e); null }
        source = if (remote != null) "downloaded" else "bundled"
        entries = parse(text)
        if (entries.isEmpty()) AppLog.i("Catalog", "model catalog empty")
    }

    private fun parse(text: String?): List<Entry> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val root = JSONObject(text)
            version = root.optInt("version", 0)
            val arr = root.optJSONArray("models") ?: return emptyList()
            val out = ArrayList<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val m = o.optString("match").trim(); if (m.isBlank()) continue
                out.add(Entry(
                    match = m.lowercase(),
                    contextWindow = o.optInt("contextWindow", 0).takeIf { it > 0 },
                    supportsTools = if (o.has("supportsTools")) o.optBoolean("supportsTools") else null,
                    qualityTier = o.optInt("qualityTier", 0).takeIf { it in 1..5 },
                    notes = o.optString("notes")
                ))
            }
            out
        } catch (e: Exception) { AppLog.w(e); emptyList() }
    }

    /** Best-known facts for a model id, or null when the catalog has nothing for it. Exact match wins over "contains". */
    fun lookup(id: String): Entry? {
        val k = id.lowercase()
        entries.firstOrNull { it.match == k }?.let { return it }
        entries.firstOrNull { k.contains(it.match) || it.match.contains(k) }?.let { return it }
        return null
    }

    /** Downloads a fresh catalog from [url] and keeps it for next launches. Returns the number of models, or throws. */
    suspend fun update(ctx: Context, url: String): Int = withContext(Dispatchers.IO) {
        val u = url.trim(); require(u.startsWith("http")) { "The catalog URL must start with http" }
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/json")
        val text = try {
            if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        val parsed = parse(text)
        require(parsed.isNotEmpty()) { "The file has no models" }
        File(ctx.filesDir, "model_catalog.json").writeText(text)
        appCtx = ctx.applicationContext; file = File(ctx.filesDir, "model_catalog.json")
        entries = parsed; source = "downloaded"
        parsed.size
    }

    fun count(): Int = entries.size
}
