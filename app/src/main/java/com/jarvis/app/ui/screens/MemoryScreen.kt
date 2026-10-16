package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.ChatStore
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf(Prefs.userName) }
    var instr by remember { mutableStateOf(Prefs.instructions) }
    var newMem by remember { mutableStateOf("") }
    var keepChats by remember { mutableStateOf(Prefs.rememberChats) }
    var saved by remember { mutableStateOf(false) }
    ScreenScaffold(null, {}) {
        SubHeader("Memory", onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("YOUR NAME", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JField(name, { name = it }, "How should Jarvis call you?", Modifier.fillMaxWidth())
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("THINGS JARVIS REMEMBERS", 11, Gold, FontWeight.Medium, ls = 2.2f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Prefs.memories.toList().forEach { m ->
                        Row(
                            Modifier.clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = 0.1f))
                                .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable { Prefs.memories.remove(m) }.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            JText(m, 12, GoldLight)
                            Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = Gold, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                JField(newMem, { newMem = it }, "Add a fact, e.g. I am vegetarian", Modifier.fillMaxWidth(), trailing = {
                    Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Gold,
                        modifier = Modifier.size(22.dp).clickable {
                            val t = newMem.trim()
                            if (t.isNotEmpty()) { Prefs.memories.add(t); newMem = "" }
                        })
                })
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JText("CUSTOM INSTRUCTIONS", 11, Gold, FontWeight.Medium, ls = 2.2f)
                JField(instr, { instr = it }, "e.g. Always be concise. Reply in Urdu unless I speak English.",
                    Modifier.fillMaxWidth(), singleLine = false, minHeight = 110.dp)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1)
                    .border(1.dp, Gold.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    JText("Remember past chats", 14, Ivory, FontWeight.Medium)
                    JText("Use earlier messages as context", 11, Muted)
                }
                JToggle(keepChats) { keepChats = it }
            }
            Row(
                Modifier.fillMaxWidth().clickable {
                    Prefs.memories.clear(); Prefs.instructions = ""; instr = ""; ChatStore.messages.clear(); Prefs.save()
                }.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFC4483F), modifier = Modifier.size(16.dp))
                JText("Clear all memory", 13, Color(0xFFC4483F), FontWeight.Medium)
            }
            GoldButton(if (saved) "Saved ✓" else "Save", null, {
                Prefs.userName = name.trim(); Prefs.instructions = instr.trim(); Prefs.rememberChats = keepChats
                Prefs.save(); saved = true
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}
