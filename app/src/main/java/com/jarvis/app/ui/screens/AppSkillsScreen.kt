package com.jarvis.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.appskills.AppSkillAction
import com.jarvis.app.appskills.AppSkillPlayer
import com.jarvis.app.appskills.AppSkillProfile
import com.jarvis.app.appskills.AppSkillRecorder
import com.jarvis.app.appskills.AppSkills
import com.jarvis.app.live.JarvisAccessibilityService
import com.jarvis.app.ui.components.GoldButton
import com.jarvis.app.ui.components.JField
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.OutlineButton
import com.jarvis.app.ui.components.ScreenScaffold
import com.jarvis.app.ui.components.SubHeader
import com.jarvis.app.ui.theme.Gold
import com.jarvis.app.ui.theme.Ivory
import com.jarvis.app.ui.theme.Muted
import com.jarvis.app.ui.theme.Surface1

@Composable
fun AppSkillsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    AppSkills.init(ctx)
    var tick by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<AppSkillProfile?>(null) }
    var creating by remember { mutableStateOf(false) }
    val profiles = remember(tick) { AppSkills.all() }

    ScreenScaffold(null, {}) {
        SubHeader("App Skills", { if (editing != null || creating) { editing = null; creating = false } else onBack() })
        if (editing != null || creating) {
            Editor(editing ?: AppSkillProfile(name = "New App Skill")) { saved ->
                AppSkills.save(saved); editing = null; creating = false; tick++
            }
        } else {
            ListUi(profiles, onCreate = { creating = true }, onEdit = { editing = it.copy(actions = it.actions.map { a -> a.copy() }.toMutableList()) }, onRefresh = { tick++ })
        }
    }
}

@Composable
private fun ColumnScope.ListUi(profiles: List<AppSkillProfile>, onCreate: () -> Unit, onEdit: (AppSkillProfile) -> Unit, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf("") }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JText("Record real UI actions per app and replay them through Accessibility. Use 2-3 draggable cursors, record taps/swipes/text/waits, then save as a profile.", 12, Muted, lh = 17)
        if (!JarvisAccessibilityService.enabled) {
            OutlineButton("Enable Accessibility service", null, {
                try { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
            }, Modifier.fillMaxWidth())
        }
        GoldButton("Create App Skill", Icons.Outlined.Add, onCreate, Modifier.fillMaxWidth())
        if (msg.isNotBlank()) JText(msg, 12, Gold)
        profiles.forEach { p ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText(p.name, 15, Ivory, FontWeight.SemiBold)
                JText("${p.appLabel.ifBlank { p.packageName.ifBlank { "No package set" } }} · ${p.actions.size} actions", 12, Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("Run", Icons.Outlined.PlayArrow, {
                        msg = "Running ${p.name}..."
                        AppSkillPlayer.play(ctx, p) { r -> msg = r.message; onRefresh() }
                    }, Modifier.weight(1f))
                    OutlineButton("Edit", Icons.Outlined.Edit, { onEdit(p) }, Modifier.weight(1f))
                    OutlineButton("Delete", Icons.Outlined.Delete, { AppSkills.delete(p.id); onRefresh() }, Modifier.weight(1f))
                }
            }
        }
        if (profiles.isEmpty()) JText("No App Skills yet. Create one for an app and record its UI steps.", 12, Muted)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ColumnScope.Editor(initial: AppSkillProfile, onSave: (AppSkillProfile) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var pkg by remember { mutableStateOf(initial.packageName) }
    var appLabel by remember { mutableStateOf(initial.appLabel) }
    var desc by remember { mutableStateOf(initial.description) }
    var actions by remember { mutableStateOf(initial.actions.toMutableList()) }
    var msg by remember { mutableStateOf("") }

    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JText("PROFILE", 11, Gold, FontWeight.Medium, ls = 2.2f)
        JField(name, { name = it }, "Profile name", Modifier.fillMaxWidth())
        JField(appLabel, { appLabel = it }, "App name (e.g. WhatsApp)", Modifier.fillMaxWidth())
        JField(pkg, { pkg = it }, "Package name (e.g. com.whatsapp)", Modifier.fillMaxWidth())
        JField(desc, { desc = it }, "Description / what this skill does", Modifier.fillMaxWidth(), singleLine = false, minHeight = 72.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton("Record actions", Icons.Outlined.RadioButtonChecked, {
                val p = initial.copy(name = name, packageName = pkg.trim(), appLabel = appLabel.trim(), description = desc.trim(), actions = actions)
                AppSkills.save(p)
                AppSkillRecorder.start(ctx, p)
                msg = "Recorder overlay opened. Record actions, save in overlay, then reopen profile."
            }, Modifier.weight(1f))
            OutlineButton("Capture foreground app", null, {
                pkg = JarvisAccessibilityService.foregroundPackage().orEmpty()
                if (appLabel.isBlank()) appLabel = pkg.substringAfterLast('.')
            }, Modifier.weight(1f))
        }

        JText("ACTIONS (${actions.size})", 11, Gold, FontWeight.Medium, ls = 2.2f)
        actions.forEachIndexed { i, a ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).border(1.dp, Gold.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                JText("${i + 1}. ${a.type} ${a.label}", 13, Ivory, FontWeight.Medium)
                JText("x=${"%.1f".format(a.x)} y=${"%.1f".format(a.y)} to=${"%.1f".format(a.toX)},${"%.1f".format(a.toY)} delay=${a.delayMs}ms", 11, Muted)
                JField(a.label, { v -> actions = actions.toMutableList().also { it[i] = it[i].copy(label = v) } }, "Label", Modifier.fillMaxWidth())
                JField(a.text, { v -> actions = actions.toMutableList().also { it[i] = it[i].copy(text = v) } }, "Text (for text action)", Modifier.fillMaxWidth())
                JField(a.verifyText, { v -> actions = actions.toMutableList().also { it[i] = it[i].copy(verifyText = v) } }, "Optional verify text", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton("Up", null, { if (i > 0) actions = actions.toMutableList().also { it.add(i - 1, it.removeAt(i)) } }, Modifier.weight(1f))
                    OutlineButton("Down", null, { if (i < actions.lastIndex) actions = actions.toMutableList().also { it.add(i + 1, it.removeAt(i)) } }, Modifier.weight(1f))
                    OutlineButton("Delete", Icons.Outlined.Delete, { actions = actions.toMutableList().also { it.removeAt(i) } }, Modifier.weight(1f))
                }
            }
        }
        OutlineButton("Add manual wait", null, { actions = actions.toMutableList().also { it.add(AppSkillAction(type = "wait", label = "Wait", durationMs = 800)) } }, Modifier.fillMaxWidth())
        if (msg.isNotBlank()) JText(msg, 12, Gold)
        GoldButton("Save profile", null, {
            if (name.isBlank()) { msg = "Profile name is required"; return@GoldButton }
            val p = initial.copy(name = name.trim(), packageName = pkg.trim(), appLabel = appLabel.trim(), description = desc.trim(), actions = actions)
            onSave(p)
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
    }
}
