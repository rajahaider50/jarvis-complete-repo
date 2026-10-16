package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

@Composable
fun ThemeScreen(onBack: () -> Unit) {
    ScreenScaffold(null, {}) {
        SubHeader("Appearance", onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.25f), shape).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(GoldLight, GoldDark)), CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    JText(Palettes.current.name, 16, Ivory, FontWeight.Medium, serif = true)
                    JText("Applies to the whole app instantly", 11, Muted)
                }
                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(goldBrush()).padding(horizontal = 16.dp, vertical = 9.dp)) {
                    JText("Aa", 13, Bg, FontWeight.SemiBold)
                }
            }
            JText("VOICE ORB STYLE · ${ORB_STYLES.size}", 11, Gold, FontWeight.Medium, ls = 2.2f)
            ORB_STYLES.chunked(4).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, nm ->
                        val idx = r * 4 + c
                        val sel = Prefs.orbStyle == idx
                        val cs = RoundedCornerShape(14.dp)
                        Column(
                            Modifier.weight(1f).clip(cs).background(Surface1)
                                .border(if (sel) 1.6.dp else 1.dp, Gold.copy(alpha = if (sel) 0.9f else 0.16f), cs)
                                .clickable { Prefs.orbStyle = idx; Prefs.save() }.padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            SiriOrb(70.dp, 0.35f, 1, idx)
                            JText(nm, 9, if (sel) Gold else Muted, align = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
            JText("COLOR STYLE · ${Palettes.list().size} THEMES", 11, Gold, FontWeight.Medium, ls = 2.2f)
            Palettes.list().chunked(3).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, p ->
                        val idx = r * 3 + c
                        val sel = Prefs.theme == idx
                        val cs = RoundedCornerShape(16.dp)
                        Column(
                            Modifier.weight(1f).clip(cs).background(Color(p.s1))
                                .border(if (sel) 1.6.dp else 1.dp, Color(p.accent).copy(alpha = if (sel) 0.9f else 0.18f), cs)
                                .clickable { Prefs.theme = idx; Prefs.applyTheme(); Prefs.save() }
                                .padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.size(40.dp).background(Brush.linearGradient(listOf(Color(p.light), Color(p.dark))), CircleShape))
                            JText(p.name, 11, if (sel) Color(p.accent) else Ivory, if (sel) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
