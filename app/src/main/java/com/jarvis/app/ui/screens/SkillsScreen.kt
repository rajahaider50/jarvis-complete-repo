package com.jarvis.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var url by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateListOf<String>() }

    val zipper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            msg = ""; ok = null
            try {
                val n = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { Skills.importZip(it) } ?: 0 }
                msg = "$n skills imported from the zip."; ok = n > 0
            } catch (e: Exception) { AppLog.w(e); msg = e.message ?: "Could not read the zip"; ok = false }
        }
    }

    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JText("The code agent sees each skill's name and one line. When a task needs one, it opens the full instructions. Turn skills off to save time, or add your own.", 12, Muted, lh = 17)

        JText("JARVIS SKILL PACK", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JText(if (Skills.packCount() == 0) "The bundled pack has 95 skills (voice, phone control, productivity, research and more). Install it, then enable only the groups you want." else "${Skills.packCount()} pack skills installed. They start disabled; enable the groups you need.", 12, Muted, lh = 17)
        OutlineButton(if (busy) "Working…" else if (Skills.packCount() == 0) "Install the 95-skill pack" else "Refresh the skill pack", Icons.Outlined.Download, {
            if (!busy) { busy = true; Skills.installPack(ctx, force = true); busy = false; msg = "Pack ready: ${Skills.packCount()} skills."; ok = true }
        }, Modifier.fillMaxWidth())
        if (Skills.packCount() > 0) {
            JText("GROUPS · tap to enable or disable a whole group", 10, Gold, FontWeight.Medium, ls = 1.5f)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Skills.phases.forEach { ph ->
                    val items = Skills.list.filter { it.phase == ph }
                    val on = items.isNotEmpty() && items.all { it.enabled }
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) Gold.copy(alpha = 0.22f) else Surface1).border(1.dp, Gold.copy(alpha = if (on) 0.9f else 0.3f), RoundedCornerShape(16.dp))
                        .clickable { Skills.setPhaseEnabled(ph, !on) }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                        JText(ph.replace("phase-", "").replace('-', ' ') + " (${items.count { it.enabled }}/${items.size})", 12, if (on) GoldLight else Ivory)
                    }
                }
            }
        }

        JText("IMPORT A SKILL", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(url, { url = it }, "https://raw.githubusercontent.com/.../SKILL.md", Modifier.fillMaxWidth())
        OutlineButton(if (busy) "Working…" else "Import SKILL.md from this link", Icons.Outlined.Link, {
            if (!busy && url.startsWith("http")) scope.launch {
                busy = true; msg = ""; ok = null
                try { val s = Skills.importFromUrl(url); msg = if (s != null) "Imported \"${s.name}\"." else "That file has no SKILL.md header."; ok = s != null }
                catch (e: Exception) { AppLog.w(e); msg = e.message ?: "Import failed"; ok = false }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        OutlineButton("Import a skills .zip", Icons.Outlined.FolderZip, { zipper.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, Modifier.fillMaxWidth())
        if (msg.isNotEmpty()) JText(msg, 12, if (ok == true) Gold else Color2, lh = 17)

        val mine = Skills.list.filter { it.source != "pack" }
        JText("MY SKILLS", 11, Gold, FontWeight.Medium, ls = 2.2f)
        mine.forEach { s ->
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
        OutlineButton("Restore built-in skills", Icons.Outlined.Restore, { Skills.restoreDefaults() }, Modifier.fillMaxWidth())

        if (Skills.packCount() > 0) {
            JText("SKILL PACK", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Skills.phases.forEach { ph ->
                val items = Skills.list.filter { it.phase == ph }
                val open = ph in expanded
                Row(Modifier.fillMaxWidth().clip(card).background(Surface2.copy(alpha = 0.5f)).clickable { if (open) expanded.remove(ph) else expanded.add(ph) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    JText(ph.replace("phase-", "").replace('-', ' '), 13, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f))
                    JText("${items.count { it.enabled }}/${items.size}", 11, Muted)
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
                if (open) items.forEach { s ->
                    Column(Modifier.fillMaxWidth().clip(card).background(Surface1).border(1.dp, Gold.copy(alpha = if (s.enabled) 0.3f else 0.1f), card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                JText(s.name, 14, Ivory, FontWeight.SemiBold)
                                if (s.category.isNotBlank()) JText(s.category, 10, Gold)
                                JText(s.description, 11, Muted, lh = 15)
                            }
                            JToggle(s.enabled) { s.enabled = it; Skills.save() }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private val Color2 = androidx.compose.ui.graphics.Color(0xFFE0665C)

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
