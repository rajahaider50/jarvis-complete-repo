package com.jarvis.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.code.Skills
import com.jarvis.app.data.AppLog
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.launch

/** Teach the agent new skills: short instruction packs it opens when a task needs them. */
@Composable
fun SkillsScreen(onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Skills.Skill?>(null) }
    var creating by remember { mutableStateOf(false) }
    ScreenScaffold(null, {}) {
        SubHeader("Skills", { if (editing != null || creating) { editing = null; creating = false } else onBack() })
        if (editing != null || creating) SkillEditor(editing) { editing = null; creating = false }
        else SkillList({ editing = it }, { creating = true })
    }
}

@Composable
private fun ColumnScope.SkillList(onEdit: (Skills.Skill) -> Unit, onAdd: () -> Unit) {
    val card = RoundedCornerShape(18.dp)
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JText("The code agent sees each skill's name and one line. When a task needs one, it opens the full instructions. Turn skills off to save time, or add your own.", 12, Muted, lh = 17)
        Skills.list.toList().forEach { s ->
            Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = if (s.enabled) 0.3f else 0.1f), card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { JText(s.name, 15, Ivory, FontWeight.SemiBold); JText(s.description, 12, Muted, lh = 16) }
                    JToggle(s.enabled) { s.enabled = it; Skills.save() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("Edit", Icons.Outlined.Edit, { onEdit(s) }, Modifier.weight(1f))
                    OutlineButton("Delete", Icons.Outlined.Delete, { Skills.delete(s) }, Modifier.weight(1f))
                }
            }
        }
        GoldButton("Add a skill", Icons.Outlined.Add, { onAdd() }, Modifier.fillMaxWidth())
        SkillImportBox()
        OutlineButton("Restore built-in skills", Icons.Outlined.Restore, { Skills.restoreDefaults() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnScope.SkillImportBox() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Skills.ImportPreview?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).border(1.dp, Gold.copy(alpha = 0.22f), RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JText("IMPORT FROM GITHUB", 11, Gold, FontWeight.Medium, ls = 2f)
        JText("Paste a raw SKILL.md, GitHub folder/repository, or HTTPS zip URL. Downloaded files are untrusted text and never executed.", 11, Muted, lh = 16)
        JField(url, { url = it }, "https://github.com/owner/repo/tree/main/skill", Modifier.fillMaxWidth())
        if (preview == null) {
            OutlineButton(if (busy) "Downloading…" else "Preview skill", Icons.Outlined.Download, {
                if (!busy && url.isNotBlank()) scope.launch {
                    busy = true
                    try { preview = Skills.preview(url) }
                    catch (e: Exception) { AppLog.w(e); Toast.makeText(ctx, e.message ?: "Could not import skill", Toast.LENGTH_LONG).show() }
                    finally { busy = false }
                }
            }, Modifier.fillMaxWidth())
        } else {
            JText(preview!!.name, 15, Ivory, FontWeight.SemiBold)
            JText(preview!!.description, 12, Muted, lh = 16)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldButton("Save imported skill", Icons.Outlined.Check, { Skills.saveImported(preview!!); preview = null; url = "" }, Modifier.weight(1f))
                OutlineButton("Cancel", Icons.Outlined.Close, { preview = null }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.SkillEditor(skill: Skills.Skill?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(skill?.name ?: "") }
    var desc by remember { mutableStateOf(skill?.description ?: "") }
    var text by remember { mutableStateOf(skill?.instructions ?: "") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try { text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty().take(40000) }
        catch (e: Exception) { AppLog.w(e); Toast.makeText(ctx, "Could not read the file", Toast.LENGTH_LONG).show() }
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JText("NAME", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(name, { name = it }, "e.g. Landing pages", Modifier.fillMaxWidth())
        JText("ONE LINE: WHEN TO USE IT", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(desc, { desc = it }, "e.g. Marketing pages with a hero, features and contact form", Modifier.fillMaxWidth())
        JText("INSTRUCTIONS", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(text, { text = it }, "Write the rules, steps and examples the agent should follow", Modifier.fillMaxWidth(), singleLine = false, minHeight = 220.dp)
        OutlineButton("Load instructions from a text file", Icons.Outlined.FileUpload, { picker.launch(arrayOf("text/*", "application/octet-stream")) }, Modifier.fillMaxWidth())
        GoldButton("Save skill", Icons.Outlined.Check, {
            if (name.isBlank() || text.isBlank()) Toast.makeText(ctx, "Name and instructions are needed", Toast.LENGTH_SHORT).show()
            else { if (skill == null) Skills.add(name, desc, text) else { skill.name = name; skill.description = desc; skill.instructions = text; Skills.save() }; onDone() }
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}
