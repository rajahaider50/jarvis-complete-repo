package com.jarvis.app.code

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException

/** Topic-based markdown memory. It never runs content and rejects obvious secret material. */
object MemoryStore {
    private const val MAX = 120_000L
    private var root: File? = null

    /** Bumped on every write so the Memory screen can show new memory the moment the agent adds it. */
    var revision by mutableStateOf(0)
        private set

    fun init(ctx: Context) { if (root == null) root = File(ctx.applicationContext.filesDir, "memory").apply { mkdirs() } }

    /** Every memory topic file with its size in bytes (path, bytes) — newest activity can be reviewed by the user. */
    fun entries(): List<Pair<String, Long>> {
        val r = root ?: return emptyList()
        return r.walkTopDown().filter { it.isFile && it.extension.equals("md", true) }
            .map { it.relativeTo(r).path to it.length() }.sortedBy { it.first }.toList()
    }

    fun list(): String {
        val r = root ?: throw IOException("Memory is not initialized")
        val names = r.walkTopDown().filter { it.isFile && it.extension.equals("md", true) }.map { it.relativeTo(r).path }.sorted().toList()
        return names.joinToString("\n").ifBlank { "No memory files yet." }
    }

    fun read(path: String): String {
        val f = safe(path)
        if (!f.isFile) throw IOException("Memory file does not exist: $path")
        if (f.length() > MAX) throw IOException("Memory file is too large")
        return f.readText()
    }

    fun write(path: String, content: String): String {
        checkContent(content)
        val f = safe(path)
        if (content.length.toLong() > MAX) throw IOException("Memory content is too large")
        f.parentFile?.mkdirs(); f.writeText(content); return "Saved memory/${f.relativeTo(root!!).path}".also { revision++ }
    }

    fun append(path: String, content: String): String {
        checkContent(content)
        val old = if (safe(path).isFile) read(path) else ""
        return write(path, (old + if (old.isEmpty() || old.endsWith("\n")) "" else "\n" + content).take(MAX.toInt()))
    }

    fun replace(path: String, old: String, new: String): String {
        if (old.isBlank()) throw IOException("old_text cannot be blank")
        val text = read(path); val count = Regex(Regex.escape(old)).findAll(text).count()
        if (count != 1) throw IOException("old_text must appear exactly once (found $count)")
        return write(path, text.replace(old, new))
    }

    private fun safe(path: String): File {
        val r = root ?: throw IOException("Memory is not initialized")
        val clean = path.trim().replace('\\', '/')
        if (clean.isBlank() || !clean.endsWith(".md", true) || clean.contains("..") || clean.startsWith('/') || clean.contains(Regex("[^A-Za-z0-9._/-]"))) throw IOException("Memory path must be a relative .md topic path")
        val f = File(r, clean).canonicalFile
        if (!f.path.startsWith(r.canonicalPath + File.separator)) throw IOException("Memory path is outside storage")
        return f
    }

    private fun checkContent(content: String) {
        val lower = content.lowercase()
        if (Regex("(sk-[A-Za-z0-9]{12,}|AIza[0-9A-Za-z_-]{20,}|bearer\\s+[A-Za-z0-9._-]{12,}|api[_ -]?key\\s*[:=])").containsMatchIn(content) || "password=" in lower || "secret=" in lower)
            throw IOException("Memory refuses probable secrets; store durable non-sensitive facts only")
    }
}
