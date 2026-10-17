package com.jarvis.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.code.CodeAgent
import com.jarvis.app.code.TaskStateStore
import com.jarvis.app.code.Workspace
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.CustomProviders
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private val CodeBg = Color(0xFF0B0B0D)
private val CodeText = Color(0xFFE6E1D6)
private val CodeDim = Color(0xFF6F6B62)
private val Red = Color(0xFFE0665C)
private val TEXT_EXT = setOf("txt", "md", "csv", "json", "html", "htm", "css", "js", "ts", "py", "kt", "java", "xml", "svg", "yaml", "yml", "gradle", "kts", "sh", "c", "cpp", "h", "php", "sql", "log", "ini", "toml")

@Composable
private fun Mono(text: String, size: Int, color: Color, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace, softWrap = false, maxLines = 1)

private fun viewable(f: File) = f.extension.lowercase().let { it in TEXT_EXT || it in setOf("pdf", "png", "jpg", "jpeg") }

@Composable
private fun rememberSaveFile(): (File) -> Unit {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val f = pending; pending = null
        if (uri != null && f != null) try {
            ctx.contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }
            Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { AppLog.w(e); Toast.makeText(ctx, "Could not save: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    return { f -> pending = f; launcher.launch(f.name) }
}

@Composable
private fun NameDialog(title: String, hint: String, onOk: (String) -> Unit, onCancel: () -> Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel, containerColor = Surface1,
        title = { JText(title, 16, Ivory, FontWeight.SemiBold) },
        text = { JField(v, { v = it }, hint, Modifier.fillMaxWidth()) },
        confirmButton = { JText("OK", 14, Gold, FontWeight.SemiBold, modifier = Modifier.clickable { if (v.isNotBlank()) onOk(v.trim()) }.padding(12.dp)) },
        dismissButton = { JText("Cancel", 14, Muted, modifier = Modifier.clickable { onCancel() }.padding(12.dp)) }
    )
}

/** Code & documents workspace: the agent writes real files in an editor, you can browse, edit and download them. */
@Composable
fun CodeScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var viewing by remember { mutableStateOf<File?>(null) }
    ScreenScaffold(null, {}) {
        val v = viewing
        if (v != null) FileViewer(v) { viewing = null }
        else {
            SubHeader("Workspace", onBack)
            ProjectBar()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Agent", "Files", "Output").forEachIndexed { i, label ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .then(if (sel) Modifier.background(goldBrush()) else Modifier.border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(18.dp)))
                            .clickable { tab = i }.padding(vertical = 9.dp), contentAlignment = Alignment.Center
                    ) { JText(label, 13, if (sel) Bg else Gold, if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) { 0 -> AgentTab { viewing = it }; 1 -> FilesTab { tab = 2 }; else -> OutputTab { viewing = it } }
        }
    }
}

@Composable
private fun ProjectBar() {
    var menu by remember { mutableStateOf(false) }
    var newDlg by remember { mutableStateOf(false) }
    var delDlg by remember { mutableStateOf(false) }
    var provMenu by remember { mutableStateOf(false) }
    val p = Workspace.current
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), shape).clickable { menu = true }.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Folder, null, tint = Gold, modifier = Modifier.size(20.dp))
                    JText(p?.name ?: "-", 14, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    Workspace.projects.toList().forEach { pr -> DropdownMenuItem(text = { JText(pr.name, 13, if (pr == p) Gold else Ivory) }, onClick = { Workspace.current = pr; menu = false }) }
                    DropdownMenuItem(text = { JText("+ New project", 13, Gold, FontWeight.Medium) }, onClick = { menu = false; newDlg = true })
                }
            }
            Icon(Icons.Outlined.DeleteOutline, "Delete project", tint = Muted, modifier = Modifier.size(24.dp).clickable { delDlg = true })
        }
        Box(Modifier.fillMaxWidth()) {
            val manual = Prefs.workspaceModel.startsWith("custom:")
            val label = if (manual) CustomProviders.list.firstOrNull { it.id == Prefs.workspaceModel.removePrefix("custom:") }?.let { it.name + " · " + it.modelId } ?: "Manual" else "Auto · all providers, step by step"
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), shape).clickable { provMenu = true }.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.SmartToy, null, tint = Gold, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    JText(if (manual) "MANUAL MODEL" else "AUTO MODEL", 9, Muted, FontWeight.Medium, ls = 1.6f)
                    JText(label, 12, Ivory, FontWeight.Medium)
                }
                Icon(Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = provMenu, onDismissRequest = { provMenu = false }) {
                DropdownMenuItem(text = { JText("Auto — every provider, step by step", 13, if (!manual) Gold else Ivory) }, onClick = { Prefs.workspaceModel = ""; Prefs.save(); provMenu = false })
                if (CustomProviders.list.isEmpty()) {
                    DropdownMenuItem(text = { JText("No providers yet — add one in Settings > Providers", 12, Muted) }, onClick = { provMenu = false })
                } else CustomProviders.list.toList().forEach { p ->
                    DropdownMenuItem(text = { JText("${p.name} · ${p.modelId}", 13, if (Prefs.workspaceModel == "custom:${p.id}") Gold else Ivory) }, onClick = { Prefs.workspaceModel = "custom:${p.id}"; Prefs.save(); provMenu = false })
                }
            }
        }
    }
    if (newDlg) NameDialog("New project", "Project name", { Workspace.create(it); newDlg = false }, { newDlg = false })
    if (delDlg && p != null) AlertDialog(
        onDismissRequest = { delDlg = false }, containerColor = Surface1,
        title = { JText("Delete \"${p.name}\"?", 16, Ivory, FontWeight.SemiBold) },
        text = { JText("All its files and downloads are removed from the phone.", 13, Muted) },
        confirmButton = { JText("Delete", 14, Red, FontWeight.SemiBold, modifier = Modifier.clickable { Workspace.deleteProject(p); delDlg = false }.padding(12.dp)) },
        dismissButton = { JText("Cancel", 14, Muted, modifier = Modifier.clickable { delDlg = false }.padding(12.dp)) }
    )
}

// ------------------------------------------------------------------------------------ Agent
@Composable
private fun ColumnScope.AgentTab(onView: (File) -> Unit) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val attached = remember { mutableStateListOf<String>() }
    val p = Workspace.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val pr = Workspace.current
        if (pr != null) uris.forEach { u ->
            try {
                val name = ctx.contentResolver.query(u, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null } ?: "file"
                ctx.contentResolver.openInputStream(u)?.let { Workspace.importFile(pr, name, it) }
                attached.add(name)
            } catch (e: Exception) { AppLog.w(e); Toast.makeText(ctx, "Could not add the file: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val runs = CodeAgent.runs.filter { it.project == p }
    val scroll = rememberScrollState()
    val last = runs.lastOrNull()
    LaunchedEffect(runs.size, last?.steps?.size, last?.status, last?.editorSeq) { scroll.animateScrollTo(scroll.maxValue) }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val pending = TaskStateStore.pending
        if (pending != null && !CodeAgent.busy) {
            val card = RoundedCornerShape(16.dp)
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = 0.4f), card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("UNFINISHED TASK", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JText(pending.originalGoal.take(180), 13, Ivory, lh = 19)
                JText("Saved ${pending.completedFiles.size} file(s) · status: ${pending.status}", 11, Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton("Resume task", Icons.Outlined.PlayArrow, { CodeAgent.resumeTask() }, Modifier.weight(1f))
                    OutlineButton("Discard", Icons.Outlined.Delete, { CodeAgent.discardPending() }, Modifier.weight(1f))
                }
            }
        }
        if (runs.isEmpty()) {
            JText("Tell the agent what to build or fix. It writes the files in an editor (not in the chat), checks that everything is connected, and gives you a zip or a single file to download. Attach a zip or files with the clip to start from your own work.", 13, Muted, lh = 19)
            listOf("Build a responsive landing page website for my business", "Make a one-page PDF CV with a logo and icons", "Create an Excel monthly budget with formulas", "Find and fix the bugs in my project").forEach { s ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).clickable { input = s }.padding(12.dp)) { JText(s, 13, Gold) }
            }
        } else runs.forEach { RunCard(it, onView) }
        Spacer(Modifier.height(8.dp))
    }
    if (attached.isNotEmpty()) JText("Added: " + attached.joinToString(", "), 11, Gold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Surface1, CircleShape).border(1.dp, Gold.copy(alpha = 0.25f), CircleShape).clickable { picker.launch(arrayOf("*/*")) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AttachFile, "Add files or a zip", tint = Gold, modifier = Modifier.size(20.dp))
        }
        val fieldShape = RoundedCornerShape(26.dp)
        Box(Modifier.weight(1f).clip(fieldShape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.22f), fieldShape).padding(horizontal = 14.dp, vertical = 12.dp)) {
            BasicTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Ivory, fontSize = 14.sp), cursorBrush = SolidColor(Gold), maxLines = 5,
                decorationBox = { inner -> Box { if (input.isEmpty()) JText("Describe what to build or fix…", 14, Muted); inner() } }
            )
        }
        val busy = CodeAgent.busy
        Box(
            Modifier.size(46.dp).background(if (busy) Brush1(Red) else goldBrush(), CircleShape)
                .clickable { if (busy) CodeAgent.stop() else if (input.isNotBlank()) { CodeAgent.start(input, attached.toList()); input = ""; attached.clear() } },
            contentAlignment = Alignment.Center
        ) { Icon(if (busy) Icons.Outlined.Stop else Icons.Outlined.Send, if (busy) "Stop" else "Send", tint = Bg, modifier = Modifier.size(20.dp)) }
    }
}

private fun Brush1(c: Color) = androidx.compose.ui.graphics.Brush.linearGradient(listOf(c, c))

@Composable
private fun RunCard(run: CodeAgent.Run, onView: (File) -> Unit) {
    val inf = rememberInfiniteTransition(label = "run")
    val pulse by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "p")
    var open by remember { mutableStateOf(true) }
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)).background(goldBrush()).padding(horizontal = 14.dp, vertical = 11.dp)) { JText(run.prompt, 14, Bg, FontWeight.Medium, lh = 20) }
        }
        Column(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), shape).padding(12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.clickable { open = !open }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(9.dp).background(if (run.status == 0) Gold.copy(alpha = pulse) else if (run.status == 1) Gold else Red, CircleShape))
                JText(when (run.status) { 0 -> "Working… ${run.steps.size} steps"; 1 -> "Done · ${run.steps.size} steps"; 2 -> "Stopped with an error"; else -> "Stopped" }, 12, Gold, FontWeight.Medium, modifier = Modifier.weight(1f))
                if (run.model.isNotBlank()) JText(run.model.take(26), 10, Muted)
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
            if (open) {
                if (run.thought.isNotBlank()) JText(run.thought.trim().takeLast(600), 12, Muted, lh = 17)
                run.notes.toList().takeLast(3).forEach { JText(it, 12, Ivory, lh = 17) }
                run.steps.toList().takeLast(14).forEach { st ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                            when (st.state) {
                                0 -> CircularProgressIndicator(Modifier.size(13.dp), color = Gold, strokeWidth = 2.dp)
                                1 -> Icon(Icons.Outlined.CheckCircle, null, tint = Gold, modifier = Modifier.size(15.dp))
                                else -> Icon(Icons.Outlined.ErrorOutline, null, tint = Red, modifier = Modifier.size(15.dp))
                            }
                        }
                        Column { JText(st.title, 12, Ivory); if (st.state == 2) JText(st.detail.take(160), 10, Red, lh = 14) }
                    }
                }
            }
        }
        if (run.editorPath.isNotBlank()) EditorPanel(run)
        if (run.summary.isNotBlank()) JText(run.summary, 14, Ivory, lh = 21)
        if (run.error.isNotBlank()) JText(run.error, 12, Red, lh = 17)
        run.outputs.toList().forEach { name -> OutputRow(File(Workspace.outDir(run.project), name), onView) }
    }
}

@Composable
private fun EditorPanel(run: CodeAgent.Run) {
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(CodeBg).border(1.dp, Gold.copy(alpha = 0.25f), shape)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Color(0xFFFF5F57), Color(0xFFFEBC2E), Color(0xFF28C840)).forEach { Box(Modifier.size(8.dp).background(it, CircleShape)) }
            Mono(run.editorPath, 11, Gold, Modifier.padding(start = 6.dp).weight(1f))
            if (run.status == 0) Mono("writing…", 10, CodeDim)
        }
        TypedCode(run.editorText, run.editorSeq)
    }
}

@Composable
private fun TypedCode(text: String, seq: Int) {
    var shown by remember(seq) { mutableStateOf(0) }
    LaunchedEffect(seq) {
        val n = text.length
        val steps = 36
        val total = (n * 1.5).toLong().coerceIn(300, 2600)
        for (i in 1..steps) { shown = (n.toLong() * i / steps).toInt(); delay(total / steps) }
        shown = n
    }
    val lines = text.take(shown).split("\n")
    val first = (lines.size - 14).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 10.dp)) {
        lines.drop(first).forEachIndexed { k, l ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Mono("%3d".format(first + k + 1), 11, CodeDim)
                Mono(l.take(70), 11, CodeText)
            }
        }
    }
}

// ------------------------------------------------------------------------------------ Files
@Composable
private fun ColumnScope.FilesTab(goOutput: () -> Unit) {
    val p = Workspace.current ?: return
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<String?>(null) }
    var dlg by remember { mutableStateOf(0) }
    val tick = Workspace.tick
    val entries = remember(tick, p) { Workspace.tree(p) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { u ->
            try {
                val name = ctx.contentResolver.query(u, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null } ?: "file"
                ctx.contentResolver.openInputStream(u)?.let { Workspace.importFile(p, name, it) }
            } catch (e: Exception) { AppLog.w(e); Toast.makeText(ctx, "Could not import: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val o = open
    if (o != null) { FileEditor(p, o) { open = null }; return }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlineButton("New file", Icons.Outlined.NoteAdd, { dlg = 1 })
        OutlineButton("New folder", Icons.Outlined.CreateNewFolder, { dlg = 2 })
        OutlineButton("Import", Icons.Outlined.FileUpload, { picker.launch(arrayOf("*/*")) })
        OutlineButton("Make ZIP", Icons.Outlined.FolderZip, { try { Workspace.exportZip(p); goOutput() } catch (e: Exception) { AppLog.w(e); Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() } })
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (entries.isEmpty()) JText("No files yet. Ask the agent to build something, or import your own.", 13, Muted)
        entries.forEach { e ->
            val depth = e.path.count { it == '/' }
            Row(
                Modifier.fillMaxWidth().clickable { if (!e.isDir) open = e.path }.padding(start = (depth * 16).dp, top = 9.dp, bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (e.isDir) Icons.Outlined.Folder else Icons.Outlined.Description, null, tint = if (e.isDir) Gold else Muted, modifier = Modifier.size(20.dp))
                JText(e.path.substringAfterLast('/'), 14, Ivory, modifier = Modifier.weight(1f))
                if (!e.isDir) JText(if (e.size > 1000) "${e.size / 1000} KB" else "${e.size} B", 11, Muted)
                Icon(Icons.Outlined.Close, "Delete", tint = Muted, modifier = Modifier.size(18.dp).clickable { try { Workspace.delete(p, e.path) } catch (x: Exception) { AppLog.w(x) } })
            }
        }
    }
    if (dlg != 0) NameDialog(if (dlg == 1) "New file" else "New folder", if (dlg == 1) "e.g. src/main.py" else "e.g. assets", { n ->
        try { if (dlg == 1) { Workspace.write(p, n, ""); open = n } else Workspace.mkdir(p, n) } catch (x: Exception) { AppLog.w(x); Toast.makeText(ctx, x.message, Toast.LENGTH_LONG).show() }
        dlg = 0
    }, { dlg = 0 })
}

@Composable
private fun ColumnScope.FileEditor(p: Workspace.Project, path: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember(path) { mutableStateOf(try { Workspace.read(p, path) } catch (e: Exception) { "" }) }
    val readable = remember(path) { try { Workspace.read(p, path); true } catch (e: Exception) { false } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(24.dp).clickable { onClose() })
        JText(path, 13, Gold, FontWeight.Medium, modifier = Modifier.weight(1f))
        if (readable) Icon(Icons.Outlined.Save, "Save", tint = Gold, modifier = Modifier.size(24.dp).clickable { try { Workspace.write(p, path, text); Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show() } catch (e: Exception) { AppLog.w(e) } })
        Icon(Icons.Outlined.DeleteOutline, "Delete", tint = Muted, modifier = Modifier.size(24.dp).clickable { try { Workspace.delete(p, path); onClose() } catch (e: Exception) { AppLog.w(e) } })
    }
    Hairline()
    if (!readable) JText("This file is binary or too large to edit here. Use Make ZIP to download it.", 13, Muted, modifier = Modifier.padding(16.dp))
    else Box(Modifier.weight(1f).fillMaxWidth().background(CodeBg).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(12.dp)) {
        BasicTextField(value = text, onValueChange = { text = it }, textStyle = TextStyle(color = CodeText, fontSize = 12.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(Gold), modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 300.dp))
    }
}

// ------------------------------------------------------------------------------------ Output
@Composable
private fun ColumnScope.OutputTab(onView: (File) -> Unit) {
    val p = Workspace.current ?: return
    val tick = Workspace.tick
    val files = remember(tick, p) { Workspace.outputs(p) }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JText("Files ready to view or download. A zip cannot be viewed, only saved; text files, PDFs and images can be viewed first.", 12, Muted, lh = 17)
        if (files.isEmpty()) JText("Nothing here yet. When the agent finishes, its result appears here.", 13, Muted)
        files.forEach { OutputRow(it, onView) }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun OutputRow(f: File, onView: (File) -> Unit) {
    val save = rememberSaveFile()
    val shape = RoundedCornerShape(16.dp)
    val ext = f.extension.lowercase()
    Row(Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), shape).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(when (ext) { "zip" -> Icons.Outlined.FolderZip; "pdf" -> Icons.Outlined.PictureAsPdf; "png", "jpg" -> Icons.Outlined.Image; else -> Icons.Outlined.Description }, null, tint = Gold, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f)) { JText(f.name, 14, Ivory, FontWeight.Medium); JText(if (f.length() > 1000) "${f.length() / 1000} KB" else "${f.length()} B", 11, Muted) }
        if (viewable(f)) Icon(Icons.Outlined.Visibility, "View", tint = Gold, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { onView(f) }.padding(6.dp))
        Icon(Icons.Outlined.Download, "Download", tint = Gold, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { save(f) }.padding(6.dp))
    }
}

@Composable
private fun ColumnScope.FileViewer(f: File, onClose: () -> Unit) {
    val save = rememberSaveFile()
    var pages by remember(f) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var text by remember(f) { mutableStateOf("") }
    val ext = f.extension.lowercase()
    LaunchedEffect(f) {
        withContext(Dispatchers.IO) {
            try {
                if (ext == "pdf") {
                    val r = PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY))
                    val out = ArrayList<Bitmap>()
                    for (i in 0 until minOf(r.pageCount, 12)) {
                        val pg = r.openPage(i)
                        val w = 1000; val h = (w.toFloat() * pg.height / pg.width).toInt()
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); bmp.eraseColor(android.graphics.Color.WHITE)
                        pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); pg.close(); out.add(bmp)
                    }
                    r.close(); pages = out
                } else if (ext == "png" || ext == "jpg" || ext == "jpeg") {
                    pages = listOfNotNull(android.graphics.BitmapFactory.decodeFile(f.path))
                } else text = f.readText().take(200_000)
            } catch (e: Exception) { AppLog.w(e); text = "Could not open this file: ${e.message}" }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(24.dp).clickable { onClose() })
        JText(f.name, 14, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Download, "Download", tint = Gold, modifier = Modifier.size(28.dp).clickable { save(f) })
    }
    Hairline()
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pages.forEach { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))) }
        if (text.isNotEmpty()) Box(Modifier.fillMaxWidth().background(CodeBg).horizontalScroll(rememberScrollState()).padding(10.dp)) {
            Text(text, color = CodeText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
