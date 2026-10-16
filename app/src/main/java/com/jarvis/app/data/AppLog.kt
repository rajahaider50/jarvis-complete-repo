package com.jarvis.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * The app-wide console. Every handled error (every catch block), every crash and the important events are recorded here
 * with the time, the file + line + function where it happened, the message and the full stack trace.
 * The log is kept on the phone, so crashes survive a restart. Open it from Profile > Console.
 */
object AppLog {
    enum class Level { INFO, WARN, ERROR }

    data class Entry(
        val id: Long, val time: Long, val level: Level,
        val where: String, val message: String, val stack: String, val count: Int = 1
    )

    val entries = mutableStateListOf<Entry>()

    private const val MAX = 400
    private var file: File? = null
    private val ids = AtomicLong(1)
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    private val saveTask = Runnable { writeAsync() }

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.applicationContext.filesDir, "console.json")
        file = f
        try {
            if (f.exists()) {
                val a = JSONArray(f.readText())
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    entries.add(Entry(ids.getAndIncrement(), o.getLong("t"), Level.valueOf(o.getString("l")), o.getString("w"), o.getString("m"), o.optString("s"), o.optInt("c", 1)))
                }
            }
        } catch (e: Exception) { /* a damaged log file is simply ignored */ }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val crash = Entry(ids.getAndIncrement(), System.currentTimeMillis(), Level.ERROR, "CRASH · " + locate(e), "${e.javaClass.simpleName}: ${e.message} (thread ${t.name})", trace(e))
                writeNow(entries.toList() + crash)
            } catch (x: Exception) { }
            previous?.uncaughtException(t, e)
        }
        i("App", "Console started")
    }

    // ---------- writing ----------
    fun i(tag: String, msg: String) = add(Level.INFO, tag, msg, "")
    fun w(e: Throwable) = add(Level.WARN, locate(e), "${e.javaClass.simpleName}: ${e.message}", trace(e))
    fun e(tag: String, e: Throwable) = add(Level.ERROR, locate(e), "[$tag] ${e.javaClass.simpleName}: ${e.message}", trace(e))
    fun e(tag: String, msg: String) = add(Level.ERROR, tag, msg, "")

    private fun add(level: Level, where: String, message: String, stack: String) {
        val entry = Entry(ids.getAndIncrement(), System.currentTimeMillis(), level, where, message.take(2000), stack.take(6000))
        main.post { push(entry) }
    }

    private fun push(e: Entry) {
        val last = entries.lastOrNull()
        if (last != null && last.level == e.level && last.where == e.where && last.message == e.message && e.time - last.time < 5000) {
            entries[entries.lastIndex] = last.copy(count = last.count + 1, time = e.time)   // same error repeating: count it, do not flood
        } else {
            entries.add(e)
            if (entries.size > MAX) entries.removeAt(0)
        }
        main.removeCallbacks(saveTask)
        main.postDelayed(saveTask, 800)
    }

    // ---------- managing ----------
    fun remove(idsToRemove: Collection<Long>) {
        main.post { entries.removeAll { it.id in idsToRemove }; writeAsync() }
    }

    fun clear() { main.post { entries.clear(); writeAsync() } }

    fun time(e: Entry): String = fmt.format(Instant.ofEpochMilli(e.time))

    /** One entry as plain text, ready to paste to anyone. */
    fun text(e: Entry): String = buildString {
        append("[").append(time(e)).append("] ").append(e.level.name)
        if (e.count > 1) append(" x").append(e.count)
        append("\n").append(e.where).append("\n").append(e.message)
        if (e.stack.isNotBlank()) append("\n").append(e.stack)
    }

    fun textAll(list: List<Entry>): String = list.joinToString("\n\n----------------------------------------\n\n") { text(it) }

    // ---------- helpers ----------
    private fun locate(e: Throwable): String {
        val f = e.stackTrace.firstOrNull { it.className.startsWith("com.jarvis.app") && !it.className.contains("AppLog") }
        return if (f != null) "${f.fileName}:${f.lineNumber} · ${f.className.substringAfterLast('.')}.${f.methodName}" else e.javaClass.simpleName
    }

    private fun trace(e: Throwable): String = e.stackTrace.take(14).joinToString("\n") { "  at $it" }

    private fun json(list: List<Entry>): String {
        val a = JSONArray()
        list.takeLast(MAX).forEach {
            a.put(JSONObject().put("t", it.time).put("l", it.level.name).put("w", it.where).put("m", it.message).put("s", it.stack).put("c", it.count))
        }
        return a.toString()
    }

    private fun writeAsync() {
        val snapshot = entries.toList()
        io.execute { writeNow(snapshot) }
    }

    private fun writeNow(list: List<Entry>) {
        try { file?.writeText(json(list)) } catch (e: Exception) { }
    }
}
