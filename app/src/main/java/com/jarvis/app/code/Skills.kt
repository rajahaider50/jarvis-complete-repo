package com.jarvis.app.code

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Skills are short instruction packs. The agent sees only each skill's name and one line; when a task needs one it opens the full
 * instructions with use_skill. You can teach it new skills in Settings > Skills, install the bundled pack, or import SKILL.md files
 * from a .zip or a raw GitHub link.
 */
object Skills {
    class Skill(
        val id: String, name: String, description: String, instructions: String, enabled: Boolean = true,
        val category: String = "", val phase: String = "", val source: String = "user"
    ) {
        var name by mutableStateOf(name)
        var description by mutableStateOf(description)
        var instructions by mutableStateOf(instructions)
        var enabled by mutableStateOf(enabled)
    }

    val list = mutableStateListOf<Skill>()
    private var file: File? = null

    private val DOCS = """
make_document(path, kind, spec): kind is pdf, png, docx, xlsx or pptx; spec is a JSON STRING.
PDF / PNG: {"pages":[{"width":595,"height":842,"background":"#ffffff","elements":[ ... ]}]}  Units are points, origin top-left, y grows downward (A4 = 595x842). Add more pages for long files. Keep everything inside the page.
Elements:
 {"type":"rect","x":0,"y":0,"w":595,"h":120,"fill":"#1F3864","radius":0,"stroke":"#000000","strokeWidth":1,"gradient":{"from":"#1F3864","to":"#2F5496","angle":90}}
 {"type":"circle","x":60,"y":60,"r":30,"fill":"#ffffff"}   (x,y = centre)
 {"type":"line","x1":40,"y1":140,"x2":555,"y2":140,"color":"#cccccc","width":1}
 {"type":"text","x":40,"y":30,"w":400,"text":"Hello","size":28,"color":"#ffffff","bold":true,"italic":false,"font":"sans|serif|mono","align":"left|center|right","lineHeight":1.15}
 {"type":"icon","name":"mail","x":40,"y":200,"size":24,"color":"#1F3864"}  names: check close star heart mail phone home person lock arrow search bolt download cloud calendar location
 {"type":"path","d":"M12 2L2 22h20z","x":40,"y":300,"scale":3,"fill":"#e63946"}  (your own logo/icon as SVG path data in a 24x24 box, scaled)
 {"type":"image","x":40,"y":300,"w":100,"h":100,"data":"<base64 png or jpeg>"}
 {"type":"table","x":40,"y":400,"colWidths":[150,200,150],"rowHeight":28,"rows":[["Name","Role","City"],["Ali","Dev","Lahore"]],"header":true,"headerFill":"#1F4E79"}
Draw logos with circles, rects and paths; use colours with good contrast; for Urdu/Arabic text use "align":"right" and a text box width.
DOCX: {"title":"Report","blocks":[{"type":"heading","level":1,"text":"Intro"},{"type":"paragraph","text":"...","bold":false,"italic":false,"align":"left","size":12,"color":"#222222"},{"type":"bullets","items":["a","b"]},{"type":"numbered","items":["a","b"]},{"type":"table","rows":[["H1","H2"],["a","b"]],"header":true},{"type":"pagebreak"}]}
XLSX: {"sheets":[{"name":"Budget","columns":[24,14],"header":true,"rows":[["Item","Cost"],["Rent",500],["Food",250],["Total",{"f":"SUM(B2:B3)","bold":true}]]}]}  (numbers stay numbers; formulas start without =)
PPTX (slide is 960x540 points): {"slides":[{"background":"#0B1F3A","title":"Title","titleColor":"#ffffff","subtitle":"...","bullets":["a","b"],"color":"#ffffff","shapes":[{"type":"rect|roundrect|ellipse|text","x":40,"y":400,"w":200,"h":60,"fill":"#2F5496","text":"Label","size":20,"color":"#ffffff","bold":true,"align":"center","middle":true}]}]}
For csv, md, txt, html, svg, json, xml and code files use write_file instead.
""".trimIndent()

    private fun builtins() = listOf(
        Skill("documents", "Documents & PDFs", "How to make PDF, PNG, DOCX, XLSX and PPTX files with logos, icons, boxes and colours", DOCS),
        Skill("webdesign", "Website design", "Responsive, clean HTML/CSS/JS websites",
            "Make mobile-first pages: <meta name=viewport>, semantic tags, CSS variables for colours, flexbox/grid, readable font sizes, good contrast, buttons that look tappable.\nSplit into index.html, style.css, script.js (+ assets/) for sites; link them with relative paths and check each link exists.\nNo build step: the site must open by double-clicking index.html. Add alt text, a footer and a clear call-to-action."),
        Skill("python", "Python scripts", "Clean, runnable Python projects",
            "Use a main() with if __name__ == '__main__'. Put dependencies in requirements.txt. Keep modules small, add type hints and short docstrings, handle errors with clear messages. Add a README with how to run it."),
        Skill("android", "Android (Kotlin + Compose)", "Android apps with Kotlin and Jetpack Compose",
            "Use Gradle Kotlin DSL, one module 'app', package folders matching the package name, AndroidManifest.xml with every activity/service and permission. UI in Compose, state in remember/ViewModel. Keep every referenced resource (drawable, string, xml) in res/."),
        Skill("bugfix", "Bug fixing", "Find and fix bugs with small, safe edits",
            "Read the relevant files first. State the cause in one line. Fix with the smallest edit (str_replace or replace_lines), never rewrite a whole file for a small bug. Re-read the edited region, search_text for other places with the same mistake, then run verify_project."),
        Skill("readme", "README & docs", "Clear README files and documentation",
            "README.md: title, one-line purpose, features, install, run, usage example, folder structure, licence. Short sentences, copy-pasteable commands."),
        Skill("review", "Code review & security", "Review code for mistakes, weak spots and security problems",
            "Check: unhandled errors, null/empty cases, hard-coded secrets, unsafe user input (injection, XSS), missing validation, duplicated code, unclear names. List findings by severity with file:line, then fix the important ones."),
        Skill("excel", "Data to Excel", "Turn data into tidy spreadsheets",
            "One sheet per topic, a header row, sensible column widths, numbers as numbers, totals as formulas (SUM, AVERAGE), consistent units. Use use_skill documents for the XLSX format."),
        Skill("urdu", "Urdu & Arabic documents", "Right-to-left text in files",
            "For Urdu/Arabic/Persian text: PDF text elements use \"align\":\"right\" with a wide w; DOCX and PPTX switch to right-to-left automatically. Keep lines short, leave generous margins, use size 16 or more for body text.")
    )

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "skills.json"); file = f
        try {
            if (f.exists()) {
                val a = JSONArray(f.readText())
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    list.add(Skill(o.getString("id"), o.getString("n"), o.optString("d"), o.optString("i"),
                        o.optBoolean("e", true), o.optString("c"), o.optString("p"), o.optString("src", "user")))
                }
            }
        } catch (e: Exception) { AppLog.w(e) }
        if (list.isEmpty()) { list.addAll(builtins()); save() }
    }

    fun save() {
        val a = JSONArray(); list.forEach { a.put(JSONObject().put("id", it.id).put("n", it.name).put("d", it.description).put("i", it.instructions)
            .put("e", it.enabled).put("c", it.category).put("p", it.phase).put("src", it.source)) }
        try { file?.writeText(a.toString()) } catch (e: Exception) { AppLog.w(e) }
    }

    fun add(name: String, description: String, instructions: String) {
        list.add(Skill("c" + System.currentTimeMillis(), name.trim().ifBlank { "My skill" }, description.trim(), instructions.trim())); save()
    }

    fun delete(s: Skill) { list.remove(s); save() }

    /** Brings back any built-in skill that was deleted. */
    fun restoreDefaults() { builtins().forEach { b -> if (list.none { it.id == b.id }) list.add(b) }; save() }

    fun find(name: String): Skill? = list.firstOrNull { it.enabled && (it.id.equals(name, true) || it.name.equals(name, true)) }

    // ---------------------------------------------------------------- pack + import
    val phases: List<String> get() = list.filter { it.source == "pack" && it.phase.isNotBlank() }.map { it.phase }.distinct().sorted()

    fun setPhaseEnabled(phase: String, on: Boolean) { list.filter { it.phase == phase }.forEach { it.enabled = on }; save() }

    fun packCount(): Int = list.count { it.source == "pack" }

    /** Installs (or refreshes) the bundled 95-skill pack. Pack skills start disabled so you enable only what you need. */
    fun installPack(ctx: Context, force: Boolean = false): Int {
        val text = try { ctx.assets.open("skills_pack.json").bufferedReader().use { it.readText() } } catch (e: Exception) { AppLog.w(e); return 0 }
        val root = try { JSONObject(text) } catch (e: Exception) { AppLog.w(e); return 0 }
        val ver = root.optString("version")
        val mark = File(ctx.filesDir, "skills_pack_version.txt")
        if (!force && mark.exists() && mark.readText().trim() == ver && list.any { it.source == "pack" }) return 0
        list.removeAll { it.source == "pack" }
        val arr = root.optJSONArray("skills") ?: JSONArray()
        var n = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id").ifBlank { "p$i" }
            if (list.any { it.id == "pack:$id" }) continue
            list.add(Skill("pack:$id", o.optString("name").ifBlank { id }, o.optString("description"),
                o.optString("instructions"), enabled = false, category = o.optString("category"), phase = o.optString("phase"), source = "pack"))
            n++
        }
        try { mark.writeText(ver) } catch (e: Exception) { AppLog.w(e) }
        save()
        return n
    }

    /** Reads one SKILL.md (YAML frontmatter + body) and adds/updates it. */
    fun importMarkdown(text: String, source: String = "import"): Skill? {
        val m = Regex("^---\\s*\\n(.*?)\\n---", RegexOption.DOT_MATCHES_ALL).find(text) ?: return null
        var id = ""; var desc = ""; var phase = ""; var cat = ""; var title = ""
        m.groupValues[1].split("\n").forEach { line ->
            val i = line.indexOf(':'); if (i > 0) {
                val k = line.substring(0, i).trim().lowercase(); val v = line.substring(i + 1).trim()
                when (k) { "name" -> id = v; "description" -> desc = v; "phase" -> phase = v; "category" -> cat = v; "title" -> title = v }
            }
        }
        if (id.isBlank()) return null
        val s = Skill(id, title.ifBlank { id.replace('-', ' ').replaceFirstChar { it.uppercase() } }, desc, text, true, cat, phase, source)
        val at = list.indexOfFirst { it.id == s.id }
        if (at >= 0) list[at] = s else list.add(s)
        save()
        return s
    }

    /** Imports every SKILL.md inside a .zip. */
    fun importZip(input: InputStream, source: String = "zip"): Int {
        var n = 0
        try {
            ZipInputStream(input).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    if (e.name.endsWith("SKILL.md", true)) {
                        if (importMarkdown(z.readBytes().toString(Charsets.UTF_8), source) != null) n++
                    }
                    z.closeEntry(); e = z.nextEntry
                }
            }
        } catch (e: Exception) { AppLog.w(e) }
        return n
    }

    /** Imports one SKILL.md from a raw URL (e.g. a GitHub raw link). */
    suspend fun importFromUrl(url: String): Skill? = withContext(Dispatchers.IO) {
        val u = url.trim(); if (!u.startsWith("http")) throw IllegalArgumentException("The link must start with http")
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000
        val text = try {
            if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        importMarkdown(text, "github")
    }

    fun promptBlock(): String {
        val on = list.filter { it.enabled }
        if (on.isEmpty()) return ""
        val shown = on.take(80)
        val more = if (on.size > shown.size) "\n(+${on.size - shown.size} more enabled skills)" else ""
        return "Skills you can open with use_skill(name):\n" + shown.joinToString("\n") { "- ${it.id}: ${it.description}" } + more
    }
}
