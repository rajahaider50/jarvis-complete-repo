package com.jarvis.app.code

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Projects on the phone: folders of real files that you and the AI can create, edit, delete, check and export. */
object Workspace {
    class Project(val id: String, var name: String)
    class Entry(val path: String, val isDir: Boolean, val size: Long)

    val projects = mutableStateListOf<Project>()
    var current by mutableStateOf<Project?>(null)
    /** Bumped on every change so screens redraw. */
    var tick by mutableStateOf(0)

    private lateinit var root: File
    private const val MAX_FILE = 600_000L
    private const val MAX_ZIP_ENTRIES = 3000
    private const val MAX_ZIP_BYTES = 60_000_000L

    fun init(ctx: Context) {
        if (::root.isInitialized) return
        root = File(ctx.filesDir, "projects").apply { mkdirs() }
        try {
            val f = File(root, "index.json")
            if (f.exists()) {
                val a = JSONArray(f.readText())
                for (i in 0 until a.length()) projects.add(Project(a.getJSONObject(i).getString("id"), a.getJSONObject(i).getString("name")))
            }
        } catch (e: Exception) { AppLog.w(e) }
        if (projects.isEmpty()) create("My project")
        current = projects.first()
    }

    private fun saveIndex() {
        val a = JSONArray(); projects.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
        try { File(root, "index.json").writeText(a.toString()) } catch (e: Exception) { AppLog.w(e) }
    }

    fun create(name: String): Project {
        val p = Project(System.currentTimeMillis().toString(), name.trim().ifBlank { "New project" })
        projects.add(p); File(root, p.id).mkdirs(); saveIndex(); current = p; tick++
        return p
    }

    fun rename(p: Project, name: String) { p.name = name.trim().ifBlank { p.name }; saveIndex(); tick++ }

    fun deleteProject(p: Project) {
        File(root, p.id).deleteRecursively(); File(root, "out_" + p.id).deleteRecursively()
        projects.remove(p); if (projects.isEmpty()) create("My project")
        if (current == p) current = projects.first()
        saveIndex(); tick++
    }

    fun dir(p: Project): File = File(root, p.id).apply { mkdirs() }
    fun outDir(p: Project): File = File(root, "out_" + p.id).apply { mkdirs() }

    /** A path inside the project. Anything that would leave the project folder is refused. */
    fun file(p: Project, rel: String): File {
        val base = dir(p).canonicalFile
        val f = File(base, rel.trim().trimStart('/')).canonicalFile
        if (f != base && !f.path.startsWith(base.path + File.separator)) throw IOException("Path is outside the project: $rel")
        return f
    }

    fun tree(p: Project): List<Entry> {
        val base = dir(p)
        return base.walkTopDown().filter { it != base }.map { Entry(it.relativeTo(base).path, it.isDirectory, if (it.isFile) it.length() else 0) }
            .sortedBy { it.path.lowercase() }.toList()
    }

    fun filesOnly(p: Project): List<Entry> = tree(p).filter { !it.isDir }

    fun read(p: Project, rel: String, from: Int = 0, to: Int = 0): String {
        val f = file(p, rel)
        if (!f.isFile) throw IOException("No such file: $rel")
        if (f.length() > MAX_FILE) throw IOException("File is too large to open (${f.length() / 1000} KB)")
        val lines = f.readText().split("\n")
        if (from <= 0 && to <= 0) return lines.joinToString("\n")
        val s = (if (from > 0) from else 1).coerceAtMost(lines.size)
        val e = (if (to > 0) to else lines.size).coerceAtMost(lines.size)
        return lines.subList(s - 1, e).joinToString("\n")
    }

    fun write(p: Project, rel: String, content: String) {
        val f = file(p, rel)
        if (content.length > MAX_FILE) throw IOException("File too large")
        f.parentFile?.mkdirs(); f.writeText(content); tick++
    }

    fun mkdir(p: Project, rel: String) { file(p, rel).mkdirs(); tick++ }

    fun delete(p: Project, rel: String) {
        val f = file(p, rel)
        if (f == dir(p).canonicalFile) throw IOException("Cannot delete the project folder")
        if (!f.exists()) throw IOException("No such path: $rel")
        f.deleteRecursively(); tick++
    }

    fun move(p: Project, from: String, to: String) {
        val a = file(p, from); val b = file(p, to)
        if (!a.exists()) throw IOException("No such path: $from")
        b.parentFile?.mkdirs(); if (!a.renameTo(b)) throw IOException("Could not move $from"); tick++
    }

    fun replaceLines(p: Project, rel: String, start: Int, end: Int, text: String): String {
        val lines = read(p, rel).split("\n").toMutableList()
        if (start < 1 || start > lines.size + 1 || end < start - 1 || end > lines.size) throw IOException("Line range $start-$end is outside the file (${lines.size} lines)")
        repeat(end - start + 1) { if (start - 1 < lines.size) lines.removeAt(start - 1) }
        lines.addAll(start - 1, if (text.isEmpty()) emptyList() else text.split("\n"))
        write(p, rel, lines.joinToString("\n"))
        return "Replaced lines $start-$end, the file now has ${lines.size} lines"
    }

    fun strReplace(p: Project, rel: String, old: String, new: String): String {
        val t = read(p, rel)
        val n = Regex(Regex.escape(old)).findAll(t).count()
        if (n == 0) throw IOException("The text to replace was not found in $rel")
        if (n > 1) throw IOException("The text to replace appears $n times in $rel: make it longer so it is unique")
        write(p, rel, t.replace(old, new)); return "Edited $rel"
    }

    fun search(p: Project, q: String): String {
        val out = StringBuilder()
        for (e in filesOnly(p)) {
            if (e.size > MAX_FILE) continue
            val f = file(p, e.path)
            try { f.readLines().forEachIndexed { i, l -> if (l.contains(q, true) && out.length < 3000) out.append("${e.path}:${i + 1}: ${l.trim().take(120)}\n") } } catch (x: Exception) { /* binary file */ }
        }
        return out.toString().ifBlank { "No matches for \"$q\"" }
    }

    // ---------- import / export ----------
    fun importFile(p: Project, name: String, input: InputStream) {
        val safe = name.substringAfterLast('/')
        if (safe.endsWith(".zip", true)) importZip(p, input) else { val f = file(p, safe); input.use { i -> f.outputStream().use { i.copyTo(it) } }; tick++ }
    }

    fun importZip(p: Project, input: InputStream) {
        var count = 0; var bytes = 0L
        ZipInputStream(input).use { z ->
            var e: ZipEntry? = z.nextEntry
            while (e != null) {
                if (++count > MAX_ZIP_ENTRIES) throw IOException("The zip has too many files")
                val name = e.name
                if (!name.startsWith("__MACOSX") && !name.contains("..")) {
                    val f = file(p, name)
                    if (e.isDirectory) f.mkdirs() else {
                        f.parentFile?.mkdirs()
                        f.outputStream().use { o -> val buf = ByteArray(32768); while (true) { val n = z.read(buf); if (n < 0) break; bytes += n; if (bytes > MAX_ZIP_BYTES) throw IOException("The zip is too big"); o.write(buf, 0, n) } }
                    }
                }
                e = z.nextEntry
            }
        }
        tick++
    }

    /** Zips the whole project into the project's output folder and returns the file. */
    fun exportZip(p: Project): File {
        val out = File(outDir(p), p.name.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "project" } + ".zip")
        val base = dir(p)
        ZipOutputStream(out.outputStream()).use { z ->
            base.walkTopDown().filter { it.isFile }.forEach { f ->
                z.putNextEntry(ZipEntry(f.relativeTo(base).path.replace('\\', '/'))); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
        }
        tick++
        return out
    }

    fun copyToOutput(p: Project, rel: String): File {
        val src = file(p, rel); val out = File(outDir(p), src.name)
        src.copyTo(out, overwrite = true); tick++
        return out
    }

    fun outputs(p: Project): List<File> = outDir(p).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    // ---------- checking ----------
    /** Looks for the problems that make a project fail: missing files, broken links, bad JSON, unbalanced brackets, empty files. */
    fun verify(p: Project): List<String> {
        val issues = ArrayList<String>()
        val files = filesOnly(p)
        if (files.isEmpty()) return listOf("The project has no files.")
        val exists = { rel: String -> try { file(p, rel).exists() } catch (e: Exception) { false } }
        fun resolveRel(from: String, ref: String): String {
            val dir = from.substringBeforeLast('/', "")
            val clean = ref.substringBefore('?').substringBefore('#')
            val parts = ArrayList<String>(); (if (clean.startsWith("/")) clean.trimStart('/') else if (dir.isEmpty()) clean else "$dir/$clean").split('/').forEach { s -> when (s) { "", "." -> {}; ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex); else -> parts.add(s) } }
            return parts.joinToString("/")
        }
        for (e in files) {
            val ext = e.path.substringAfterLast('.', "").lowercase()
            if (e.size == 0L) { issues.add("${e.path} is empty"); continue }
            if (e.size > MAX_FILE || ext in setOf("png", "jpg", "jpeg", "gif", "pdf", "zip", "docx", "xlsx", "pptx", "ico", "woff", "woff2", "ttf", "mp3", "mp4")) continue
            val text = try { file(p, e.path).readText() } catch (x: Exception) { continue }
            fun local(ref: String) = ref.isNotBlank() && !Regex("^(https?:|data:|mailto:|tel:|javascript:|#|//|\\{|\\$)").containsMatchIn(ref)
            when (ext) {
                "html", "htm" -> Regex("""(?:src|href)\s*=\s*["']([^"']+)["']""").findAll(text).map { it.groupValues[1] }.filter { local(it) }
                    .forEach { if (!exists(resolveRel(e.path, it))) issues.add("${e.path} links to ${it}, which does not exist") }
                "css" -> Regex("""url\(\s*["']?([^"')]+)["']?\s*\)""").findAll(text).map { it.groupValues[1] }.filter { local(it) }
                    .forEach { if (!exists(resolveRel(e.path, it))) issues.add("${e.path} uses ${it}, which does not exist") }
                "js", "mjs", "jsx", "ts", "tsx" -> Regex("""(?:from\s+|require\(\s*|import\(\s*|import\s+)["'](\.{1,2}/[^"']+)["']""").findAll(text).map { it.groupValues[1] }.forEach { ref ->
                    val b = resolveRel(e.path, ref)
                    if (listOf("", ".js", ".ts", ".jsx", ".tsx", ".json", ".css", ".mjs", "/index.js", "/index.ts").none { exists(b + it) }) issues.add("${e.path} imports ${ref}, which does not exist")
                }
                "py" -> Regex("""^\s*(?:from|import)\s+([A-Za-z_][\w.]*)""", RegexOption.MULTILINE).findAll(text).map { it.groupValues[1] }.forEach { mod ->
                    val first = mod.substringBefore('.')
                    val rel = mod.replace('.', '/')
                    val localTop = exists("$first.py") || exists(first)
                    if (localTop && !(exists("$rel.py") || exists("$rel/__init__.py") || exists(rel))) issues.add("${e.path} imports $mod, but that module is missing")
                }
                "json" -> try { JSONObject(text.trim().let { if (it.startsWith("[")) "{\"a\":$it}" else it }) } catch (x: Exception) { issues.add("${e.path} is not valid JSON") }
            }
            if (ext in setOf("js", "ts", "jsx", "tsx", "java", "kt", "c", "cpp", "css", "json", "php", "cs", "go", "rs", "swift", "dart")) {
                val t = text.replace(Regex("""//.*|/\*[\s\S]*?\*/"""), "").replace(Regex(""""(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`[\s\S]*?`"""), "\"\"")
                for ((a, b) in listOf('{' to '}', '(' to ')', '[' to ']')) { val x = t.count { it == a }; val y = t.count { it == b }; if (x != y) issues.add("${e.path} has unbalanced $a$b ($x opening, $y closing)") }
            }
        }
        return issues.distinct().take(30)
    }
}
