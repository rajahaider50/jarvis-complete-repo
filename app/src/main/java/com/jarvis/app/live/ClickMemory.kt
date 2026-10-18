package com.jarvis.app.live

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Click-lesson memory for the AI cursor. It is FILE based (no Room): a machine file `ui_clicks.json`, a
 * human-readable `ui_clicks.md` the user can read/edit, and an append-only `ui_clicks.journal` used to rebuild
 * the JSON if it is ever lost or corrupted. It NEVER stores screen text, passwords or payment content, and it
 * refuses to learn anything from a banking / payment / password screen.
 */
object ClickMemory {
    private const val MAX_LESSONS = 400
    private const val CLOSE = 6f                 // two lessons closer than this are the "same" spot
    private var root: File? = null
    private var appCtx: Context? = null

    /** Bumped on every write so the memory screen refreshes. */
    var revision by mutableStateOf(0)
        private set

    /** Phrases the user corrected the AI with during THIS call (kept only in memory). */
    val sessionNotes = mutableStateListOf<String>()

    data class Lesson(
        var pkg: String,
        var screen: String,
        var target: String,
        var x: Float,
        var y: Float,
        var w: Int,
        var h: Int,
        var density: Float,
        var anchor: String,
        var ok: Int,
        var fail: Int,
        var last: Long,
        var source: String,
        var note: String
    )

    /** Screens where we must never learn or remember a tap. */
    private val blockedPackages = listOf(
        "com.android.settings", "com.google.android.settings",
        "com.android.vending", "com.google.android.apps.walletnfcrel",
        "com.paypal.android.p2pmobile", "com.android.chrome.incognito"
    )
    private val blockedWords = listOf(
        "bank", "banking", "wallet", "payment", "paytm", "phonepe", "gpay", "upi",
        "password", "passcode", "pin", "otp", "1password", "bitwarden", "keepass", "locker", "card"
    )

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        if (root == null) root = File(ctx.applicationContext.filesDir, "memory").apply { mkdirs() }
        consolidate()
    }

    private fun rootDir(): File? = root
    private fun jsonFile(): File? { val r = root ?: return null; return File(r, "ui_clicks.json") }
    private fun mdFile(): File? { val r = root ?: return null; return File(r, "ui_clicks.md") }
    private fun journalFile(): File? { val r = root ?: return null; return File(r, "ui_clicks.journal") }

    /** True when this foreground package / label looks sensitive — we then never learn from it. */
    fun isProtected(pkg: String?, label: String? = null): Boolean {
        val p = pkg.orEmpty().lowercase()
        val l = label.orEmpty().lowercase()
        if (blockedPackages.any { p == it || p.startsWith(it) }) return true
        if (blockedWords.any { p.contains(it) }) return true
        if (l.isNotEmpty() && blockedWords.any { l.contains(it) }) return true
        return false
    }

    // ---------- load / save ----------

    @Synchronized
    fun load(): MutableList<Lesson> {
        val f = jsonFile() ?: return mutableListOf()
        if (!f.isFile) return mutableListOf()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Lesson(
                    pkg = o.optString("pkg"), screen = o.optString("screen"), target = o.optString("target"),
                    x = o.optDouble("x", 0.0).toFloat(), y = o.optDouble("y", 0.0).toFloat(),
                    w = o.optInt("w", 0), h = o.optInt("h", 0), density = o.optDouble("density", 1.0).toFloat(),
                    anchor = o.optString("anchor", "screen"), ok = o.optInt("ok", 0), fail = o.optInt("fail", 0),
                    last = o.optLong("last", 0L), source = o.optString("source", ""), note = o.optString("note", "")
                )
            }.toMutableList()
        } catch (e: Exception) { AppLog.w(e); mutableListOf() }
    }

    @Synchronized
    private fun saveAll(list: MutableList<Lesson>) {
        try {
            val arr = JSONArray()
            list.forEach { l ->
                arr.put(JSONObject().apply {
                    put("pkg", l.pkg); put("screen", l.screen); put("target", l.target)
                    put("x", l.x.toDouble()); put("y", l.y.toDouble()); put("w", l.w); put("h", l.h)
                    put("density", l.density.toDouble()); put("anchor", l.anchor)
                    put("ok", l.ok); put("fail", l.fail); put("last", l.last); put("source", l.source); put("note", l.note)
                })
            }
            jsonFile()?.writeText(arr.toString(1))
            writeMd(list)
            revision++
        } catch (e: Exception) { AppLog.w(e) }
    }

    private fun writeMd(list: MutableList<Lesson>) {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append("# UI click lessons\n\n")
        sb.append("Human-readable copy. Newest first. Edit freely - the app reads ui_clicks.json.\n")
        sb.append("Coordinates: centre (0,0), X right +, Y up +, range -100..+100.\n\n")
        list.sortedByDescending { it.last }.forEach { l ->
            sb.append("- `").append(l.pkg).append("`")
            if (l.screen.isNotBlank()) sb.append(" / ").append(l.screen)
            sb.append(" -> **").append(l.target).append("**")
            sb.append(" @ x=").append(fmt(l.x)).append(", y=").append(fmt(l.y))
            sb.append("  (ok=").append(l.ok).append(" fail=").append(l.fail)
            sb.append(", anchor=").append(l.anchor).append(", src=").append(l.source)
            sb.append(", ").append(df.format(Date(l.last))).append(")")
            if (l.note.isNotBlank()) sb.append(" - ").append(l.note.replace("\n", " "))
            sb.append("\n")
        }
        mdFile()?.writeText(sb.toString())
    }

    private fun fmt(v: Float) = String.format(Locale.US, "%.2f", v)

    // ---------- learn ----------

    /**
     * Records (or reinforces) a lesson. Is a no-op on a protected screen, or if the target label is empty.
     * source is one of: ai_success, user_correction, user_marked, manual.
     */
    @Synchronized
    fun remember(pkg: String?, target: String?, x: Float, y: Float, anchor: String = "screen", source: String = "ai_success", note: String = ""): String {
        val r = root
        val t = target?.trim().orEmpty()
        if (r == null) return "memory not ready"
        if (t.isEmpty()) return "no target label"
        if (isProtected(pkg, t)) return "this looks like a private screen; nothing saved"
        if (JarvisAccessibilityService.enabled && JarvisAccessibilityService.isSensitiveScreen()) return "a password field is open; nothing saved"
        val s = if (JarvisAccessibilityService.enabled) CursorGeometry.screenSize(JarvisAccessibilityService.inst!!) else CursorGeometry.ScreenSize(0, 0)
        val density = appCtx?.resources?.displayMetrics?.density ?: 1f
        val list = load()
        val same = list.firstOrNull { it.pkg == pkg.orEmpty() && it.target.equals(t, true) && kotlin.math.abs(it.x - x) < CLOSE && kotlin.math.abs(it.y - y) < CLOSE }
        if (same != null) {
            same.ok += if (source == "ai_success") 1 else 0
            same.fail += if (source == "ai_fail") 1 else 0
            same.x = x; same.y = y; same.last = System.currentTimeMillis(); same.note = note.ifBlank { same.note }
            if (source == "user_correction" || source == "user_marked") same.source = source
            same.anchor = anchor
        } else {
            list.add(Lesson(pkg.orEmpty(), "", t, x, y, s.w, s.h, density, anchor,
                ok = if (source == "ai_success") 1 else 0, fail = if (source == "ai_fail") 1 else 0,
                last = System.currentTimeMillis(), source = source, note = note))
        }
        prune(list)
        saveAll(list)
        appendJournal(pkg.orEmpty(), t, x, y, anchor, source, note)
        return "remembered '$t' at ${fmt(x)}, ${fmt(y)}"
    }

    /** When the user corrects a spot, drop any auto lesson for the same target that is now wrong. */
    @Synchronized
    fun correct(pkg: String?, target: String?, oldX: Float, oldY: Float, newX: Float, newY: Float, note: String = ""): String {
        val list = load()
        val t = target?.trim().orEmpty()
        list.removeAll { it.pkg == pkg.orEmpty() && it.target.equals(t, true) && kotlin.math.abs(it.x - oldX) < CLOSE && kotlin.math.abs(it.y - oldY) < CLOSE }
        saveAll(list)
        val r = remember(pkg, target, newX, newY, "screen", "user_correction", note)
        sessionNotes.add("Corrected '${t.ifBlank { "tap" }}' to ${fmt(newX)}, ${fmt(newY)}")
        return r
    }

    /** Records that the last ai_success lesson at this spot turned out wrong. */
    @Synchronized
    fun fail(pkg: String?, x: Float, y: Float) {
        val list = load()
        val l = list.minByOrNull { kotlin.math.abs(it.x - x) + kotlin.math.abs(it.y - y) }
        if (l != null && kotlin.math.abs(l.x - x) < CLOSE && kotlin.math.abs(l.y - y) < CLOSE) {
            l.fail += 1; l.last = System.currentTimeMillis(); saveAll(list); appendJournal(pkg.orEmpty(), l.target, x, y, l.anchor, "ai_fail", "")
        }
    }

    private fun prune(list: MutableList<Lesson>) {
        if (list.size <= MAX_LESSONS) return
        val keep = list.sortedByDescending { it.ok * 3 - it.fail * 2 + it.last / 1_000_000_000L }.take(MAX_LESSONS)
        list.clear(); list.addAll(keep)
    }

    // ---------- recall ----------

    /** Best remembered spots for a query in the foreground app, as text for the model. */
    @Synchronized
    fun recall(query: String?, pkg: String?): String {
        val list = load()
        if (list.isEmpty()) return "No click lessons yet."
        val q = query.orEmpty().trim().lowercase()
        val p = pkg.orEmpty().trim().lowercase()
        val scored = list
            .filter { p.isBlank() || it.pkg.equals(p, true) || p.contains(it.pkg.lowercase()) || it.pkg.lowercase().contains(p) }
            .map { l ->
                var sc = l.ok * 3 - l.fail * 4
                if (q.isNotBlank()) {
                    val t = (l.target + " " + l.screen + " " + l.note).lowercase()
                    if (t.contains(q)) sc += 8
                    q.split(' ', '_').filter { it.length > 2 }.forEach { if (t.contains(it)) sc += 2 }
                }
                sc to l
            }
            .sortedByDescending { it.first }
        val top = scored.take(6).map { it.second }
        if (top.isEmpty()) return "No lessons for '${query.orEmpty()}' in this app."
        return top.joinToString("\n") { l ->
            "- ${l.target} @ x=${fmt(l.x)}, y=${fmt(l.y)}${if (l.screen.isNotBlank()) " (${l.screen})" else ""} ok=${l.ok} fail=${l.fail}"
        }
    }

    /** Lessons for the screen the user is looking at (shown in the Cursor memory screen). */
    fun all(): List<Lesson> = load().sortedByDescending { it.last }

    @Synchronized
    fun delete(index: Int) { val list = load(); if (index in list.indices) { list.removeAt(index); saveAll(list) } }

    @Synchronized
    fun clearAll() { saveAll(mutableListOf()) }

    /** Lessons learned during this call (shown as a small list at the end). */
    fun sessionText(): String = if (sessionNotes.isEmpty()) "" else sessionNotes.joinToString("\n") { "- $it" }

    /** Called at the end of a call: prune + rewrite the readable file. */
    @Synchronized
    fun consolidate() {
        if (root == null) return
        val list = load()
        if (list.isEmpty() && (journalFile()?.isFile != true)) return
        prune(list)
        saveAll(list)
        try { journalFile()?.writeText("") } catch (e: Exception) { }
    }

    private fun appendJournal(pkg: String, target: String, x: Float, y: Float, anchor: String, source: String, note: String) {
        try {
            val o = JSONObject().apply {
                put("t", System.currentTimeMillis()); put("pkg", pkg); put("target", target)
                put("x", x.toDouble()); put("y", y.toDouble()); put("anchor", anchor); put("source", source); put("note", note)
            }
            journalFile()?.appendText(o.toString() + "\n")
        } catch (e: Exception) { AppLog.w(e) }
    }
}
