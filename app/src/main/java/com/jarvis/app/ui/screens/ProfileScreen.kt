package com.jarvis.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.R
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

@Composable
fun ProfileScreen(onNav: (String) -> Unit) {
    if (Palettes.style != 0) {
        ScreenScaffold("profile", onNav) {
            Box(Modifier.weight(1f).fillMaxWidth()) { com.jarvis.app.ui.styles.StyledSettings(onNav) }
        }
        return
    }
    val name = Prefs.userName.ifBlank { "Your Name" }
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    ScreenScaffold("profile", onNav) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                JText("Profile", 28, Ivory, FontWeight.Medium, serif = true)
            }
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(68.dp).background(Surface1, CircleShape).border(2.dp, Gold, CircleShape), contentAlignment = Alignment.Center) {
                    JText(initials.ifBlank { "J" }, 24, Gold, FontWeight.Medium, serif = true, ls = 1f)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    JText(name, 21, Ivory, FontWeight.Medium, serif = true)
                    JText("PLATINUM MEMBER", 11, Gold, FontWeight.Medium, ls = 2f)
                }
            }
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp).fillMaxWidth().height(160.dp)
                    .shadow(20.dp, cardShape, ambientColor = Gold, spotColor = Gold).clip(cardShape)
                    .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.22f).compositeOver(Color(0xFF0F0F11)), Color(0xFF0F0F11))))
                    .border(1.dp, Gold.copy(alpha = 0.5f), cardShape).padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    JText("${Prefs.assistantName.uppercase()} PRIVILEGE", 11, Gold, FontWeight.Medium, ls = 3f)
                    JIcon(R.drawable.ic_badge, Gold, 26.dp)
                }
                JText("Platinum", 34, GoldLight, FontWeight.Medium, serif = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    JText(if (ModelCatalog.ok == true) "Gemini: connected" else "Gemini: not connected", 11, Ivory)
                    JText("Voice: ${Prefs.voiceLabel}", 11, Muted)
                }
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JText("PREFERENCES", 11, Gold, FontWeight.Medium, ls = 2.2f)
                val listShape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.fillMaxWidth().clip(listShape).background(Surface1)
                        .border(1.dp, Gold.copy(alpha = 0.14f), listShape).padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    listOf(
                        Triple(R.drawable.ic_pref_api, "Gemini API", "api"),
                        Triple(R.drawable.ic_pref_voice, "Voice", "voices"),
                        Triple(R.drawable.ic_pref_palette, "Appearance", "themes"),
                        Triple(R.drawable.ic_pref_palette, "Theme Lab", "themelab"),
                        Triple(R.drawable.ic_pref_memory, "Memory & Instructions", "memory"),
                        Triple(R.drawable.ic_pref_wake, "Assistant name & wake phrase", "assistant")
                    ).forEachIndexed { i, (icon, label, route) ->
                        if (i > 0) Hairline(0.12f)
                        PrefRow(icon, label) { onNav(route) }
                    }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Code, "Code & documents workspace") { onNav("code") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Extension, "Skills") { onNav("skills") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Layers, "Interface style") { onNav("uistyle") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Security, "Access & permissions") { onNav("access") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Terminal, "Console (errors)") { onNav("console") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.GpsFixed, "Cursor Lab (AI cursor)") { onNav("cursorlab") }
                    Hairline(0.12f); PrefRowV(Icons.Outlined.Bookmark, "Cursor memory (click lessons)") { onNav("cursormemory") }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.AutoAwesome, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("AI control in calls", 15, Ivory)
                            JText("Let ${Prefs.assistantName} change orb, theme, recording by voice", 11, Muted)
                        }
                        JToggle(Prefs.aiControl) { Prefs.aiControl = it; Prefs.save() }
                    }
                    Hairline(0.12f)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoldIconBox(Icons.Outlined.GpsFixed, box = 38, iconSize = 22, radius = 12)
                        Column(Modifier.weight(1f)) {
                            JText("AI cursor control", 15, Ivory)
                            JText("Let ${Prefs.assistantName} move a cursor and tap the screen for you", 11, Muted)
                        }
                        JToggle(Prefs.aiCursor) { Prefs.aiCursor = it; Prefs.save() }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefRowV(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        GoldIconBox(icon, box = 38, iconSize = 22, radius = 12)
        JText(label, 15, Ivory, modifier = Modifier.weight(1f))
        JIcon(R.drawable.ic_chevron, Muted, 18.dp)
    }
}

@Composable
private fun PrefRow(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        GoldIconBoxRes(icon, box = 38, iconSize = 22, radius = 12)
        JText(label, 15, Ivory, modifier = Modifier.weight(1f))
        JIcon(R.drawable.ic_chevron, Muted, 18.dp)
    }
}
