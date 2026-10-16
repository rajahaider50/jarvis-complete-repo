package com.jarvis.app.code

import com.jarvis.app.data.AppLog
import org.json.JSONArray
import org.json.JSONObject

/** The tools the code agent works with: a real project folder, an editor, document makers, skills and a checker. */
object AgentTools {
    /** name, description, parameters as (name, type, required) */
    class T(val name: String, val desc: String, val props: List<Triple<String, String, Boolean>> = emptyList())

    private fun s(n: String, req: Boolean = true) = Triple(n, "STRING", req)
    private fun i(n: String, req: Boolean = true) = Triple(n, "INTEGER", req)

    val all = listOf(
        T("list_files", "Show every file and folder of the project with sizes"),
        T("read_file", "Read a file. Lines are numbered. Optional start_line / end_line to read a part", listOf(s("path"), i("start_line", false), i("end_line", false))),
        T("write_file", "Create a file (folders are created automatically) or replace it completely with this content", listOf(s("path"), s("content"))),
        T("replace_lines", "Replace lines start_line..end_line (1-based, inclusive) with new_text. Use an empty new_text to delete the lines; end_line = start_line - 1 inserts before start_line", listOf(s("path"), i("start_line"), i("end_line"), s("new_text"))),
        T("str_replace", "Replace one exact piece of text (it must appear exactly once) with new text", listOf(s("path"), s("old_text"), s("new_text"))),
        T("delete_path", "Delete a file or a whole folder", listOf(s("path"))),
        T("make_dir", "Create a folder", listOf(s("path"))),
        T("move_path", "Move or rename a file or folder", listOf(s("from"), s("to"))),
        T("search_text", "Find text in all files (shows file:line)", listOf(s("query"))),
        T("make_document", "Create a PDF, PNG, DOCX, XLSX or PPTX file from a JSON description. Call use_skill documents first to learn the format", listOf(s("path"), s("kind"), s("spec"))),
        T("use_skill", "Open the full instructions of a skill", listOf(s("name"))),
        T("verify_project", "Check the project for missing files, broken links and imports, bad JSON and unbalanced brackets"),
        T("finish", "Finish the work. deliver: zip (many files), file (one file, give file_path) or none", listOf(s("summary"), s("deliver", false), s("file_path", false)))
    )

    private fun ty(t: String) = if (t == "INTEGER") "integer" else "string"

    fun gemini(): JSONObject {
        val arr = JSONArray()
        all.forEach { t ->
            val o = JSONObject().put("name", t.name).put("description", t.desc)
            if (t.props.isNotEmpty()) {
                val p = JSONObject(); t.props.forEach { (n, ty, _) -> p.put(n, JSONObject().put("type", ty)) }
                o.put("parameters", JSONObject().put("type", "OBJECT").put("properties", p).put("required", JSONArray(t.props.filter { it.third }.map { it.first })))
            }
            arr.put(o)
        }
        return JSONObject().put("functionDeclarations", arr)
    }

    fun openAi(): JSONArray {
        val arr = JSONArray()
        all.forEach { t ->
            val p = JSONObject(); t.props.forEach { (n, type, _) -> p.put(n, JSONObject().put("type", ty(type))) }
            val params = JSONObject().put("type", "object").put("properties", p)
            if (t.props.any { it.third }) params.put("required", JSONArray(t.props.filter { it.third }.map { it.first }))
            arr.put(JSONObject().put("type", "function").put("function", JSONObject().put("name", t.name).put("description", t.desc).put("parameters", params)))
        }
        return arr
    }

    /** A short human title for a step, shown in the thinking panel. */
    fun title(name: String, a: JSONObject): String = when (name) {
        "list_files" -> "Looking at the project files"
        "read_file" -> "Reading ${a.optString("path")}"
        "write_file" -> "Writing ${a.optString("path")}"
        "replace_lines" -> "Editing lines ${a.optInt("start_line")}-${a.optInt("end_line")} of ${a.optString("path")}"
        "str_replace" -> "Editing ${a.optString("path")}"
        "delete_path" -> "Deleting ${a.optString("path")}"
        "make_dir" -> "Creating folder ${a.optString("path")}"
        "move_path" -> "Moving ${a.optString("from")} to ${a.optString("to")}"
        "search_text" -> "Searching for \"${a.optString("query").take(30)}\""
        "make_document" -> "Creating ${a.optString("kind").uppercase()} ${a.optString("path")}"
        "use_skill" -> "Using skill ${a.optString("name")}"
        "verify_project" -> "Checking that all files are connected"
        "finish" -> "Finishing"
        else -> name
    }

    private fun numbered(text: String, from: Int): String {
        val lines = text.split("\n")
        return lines.take(400).mapIndexed { k, l -> "%4d| %s".format(from + k, l) }.joinToString("\n") + if (lines.size > 400) "\n... (${lines.size - 400} more lines: read again with start_line)" else ""
    }

    /** Runs one tool. Errors are returned as text starting with ERROR so the model can correct itself. */
    fun exec(p: Workspace.Project, name: String, a: JSONObject, run: CodeAgent.Run): String = try {
        when (name) {
            "list_files" -> Workspace.tree(p).joinToString("\n") { (if (it.isDir) "[dir] " else "") + it.path + (if (!it.isDir) "  (${it.size} bytes)" else "") }.ifBlank { "The project is empty." }
            "read_file" -> { val st = a.optInt("start_line", 0); numbered(Workspace.read(p, a.getString("path"), st, a.optInt("end_line", 0)), if (st > 0) st else 1) }
            "write_file" -> {
                val c = a.getString("content"); Workspace.write(p, a.getString("path"), c)
                run.showInEditor(a.getString("path"), c)
                "Wrote ${a.getString("path")} (${c.split("\n").size} lines)"
            }
            "replace_lines" -> {
                val r = Workspace.replaceLines(p, a.getString("path"), a.getInt("start_line"), a.getInt("end_line"), a.optString("new_text"))
                run.showInEditor(a.getString("path"), Workspace.read(p, a.getString("path"))); r
            }
            "str_replace" -> {
                val r = Workspace.strReplace(p, a.getString("path"), a.getString("old_text"), a.optString("new_text"))
                run.showInEditor(a.getString("path"), Workspace.read(p, a.getString("path"))); r
            }
            "delete_path" -> { Workspace.delete(p, a.getString("path")); "Deleted ${a.getString("path")}" }
            "make_dir" -> { Workspace.mkdir(p, a.getString("path")); "Created ${a.getString("path")}" }
            "move_path" -> { Workspace.move(p, a.getString("from"), a.getString("to")); "Moved" }
            "search_text" -> Workspace.search(p, a.getString("query"))
            "make_document" -> {
                val spec = JSONObject(a.getString("spec"))
                val f = Workspace.file(p, a.getString("path"))
                DocGen.make(a.getString("kind"), spec, f); Workspace.tick++
                run.showInEditor(a.getString("path"), "// ${a.getString("kind").uppercase()} built from this description\n" + spec.toString(2))
                "Created ${a.getString("path")} (${f.length() / 1000} KB)"
            }
            "use_skill" -> Skills.find(a.getString("name"))?.instructions ?: "No skill called \"${a.getString("name")}\". " + Skills.promptBlock()
            "verify_project" -> Workspace.verify(p).let { if (it.isEmpty()) "No problems found." else "Problems:\n" + it.joinToString("\n") }
            else -> "ERROR: unknown tool $name"
        }
    } catch (e: Exception) {
        AppLog.i("CodeAgent", "$name failed: ${e.message}")
        "ERROR: ${e.message ?: e.javaClass.simpleName}"
    }
}
