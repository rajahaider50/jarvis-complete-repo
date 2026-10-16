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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

private class StyleInfo(val name: String, val about: String, val bg: Long, val card: Long, val accent: Long)

private val STYLES = listOf(
    StyleInfo("Classic Gold", "The original: serif titles, gold outlines, tab bar with labels, all settings in one Profile page.", 0xFF0B0B0D, 0xFF151518, 0xFFC9A84C),
    StyleInfo("Aurora", "Glass and gradients: floating pill navigation, one big hero card, ring gauges, settings as separate tiles.", 0xFF0A0A1A, 0xFF14142B, 0xFF8B7CFF),
    StyleInfo("Nova", "Light and clean like Google's apps: search bar, tonal cards, list-style controls and grouped settings.", 0xFFFFFFFF, 0xFFF1F3F9, 0xFF1A73E8),
    StyleInfo("Pulse", "Pure black HUD: monospace text, sharp corners, a raised talk button in the middle, live waveform, tabbed settings.", 0xFF000000, 0xFF0D0D0D, 0xFFC6FF3D),
    StyleInfo("Folio", "A warm paper magazine: serif headlines, navigation as text tabs at the top, key figures, accordion settings.", 0xFFFAF6EF, 0xFFF1EADD, 0xFFC2410C)
)

/** Pick the whole design of the app. Everything changes at once: layout, navigation, icons, colors, settings structure. */
@Composable
fun UiStyleScreen(onBack: () -> Unit) {
    ScreenScaffold(null, {}) {
        SubHeader("Interface style", onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JText("Tap one to switch the whole app. More interface styles will be added here.", 12, Muted)
            STYLES.forEachIndexed { i, st ->
                val sel = Prefs.uiStyle == i
                val shape = RoundedCornerShape(24.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(Surface1).border(if (sel) 2.dp else 1.dp, Gold.copy(alpha = if (sel) 0.9f else 0.18f), shape)
                        .clickable { Prefs.uiStyle = i; Prefs.theme = 0; Prefs.applyTheme(); Prefs.save() }.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Mini(i, st)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            JText(st.name, 17, Ivory, FontWeight.SemiBold)
                            if (sel) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Gold.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) { JText("Active", 10, Gold, FontWeight.Medium) }
                        }
                        JText(st.about, 12, Muted, lh = 17)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** A tiny phone drawing of each style (fixed colors, so it always shows the real look). */
@Composable
private fun Mini(index: Int, st: StyleInfo) {
    val accent = Color(st.accent)
    val card = Color(st.card)
    Box(Modifier.size(64.dp, 112.dp).clip(RoundedCornerShape(12.dp)).background(Color(st.bg)).border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(6.dp)) {
        when (index) {
            0 -> Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(34.dp).background(accent.copy(alpha = 0.35f), CircleShape).border(1.dp, accent, CircleShape))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(2) { Box(Modifier.size(22.dp, 16.dp).clip(RoundedCornerShape(4.dp)).background(card).border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) } }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(2) { Box(Modifier.size(22.dp, 16.dp).clip(RoundedCornerShape(4.dp)).background(card).border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) } }
            }
            1 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) { Box(Modifier.size(24.dp).background(accent, CircleShape)) }
                Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(accent))
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(card).border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(7.dp)))
            }
            3 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).border(2.dp, accent, CircleShape))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(12.dp)) { repeat(7) { Box(Modifier.width(3.dp).fillMaxHeight(if (it % 2 == 0) 1f else 0.5f).background(accent)) } }
                repeat(2) { Box(Modifier.fillMaxWidth().height(14.dp).border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp))) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(22.dp).background(accent, CircleShape))
            }
            4 -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(3) { Box(Modifier.width(12.dp).height(3.dp).background(if (it == 0) accent else Color(0xFFCFC3AD))) } }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFCFC3AD)))
                Box(Modifier.fillMaxWidth(0.9f).height(10.dp).background(Color(0xFF2B2118)))
                Box(Modifier.fillMaxWidth(0.6f).height(10.dp).background(Color(0xFF2B2118)))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCFC3AD)))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { repeat(3) { Box(Modifier.size(10.dp, 14.dp).background(Color(0xFFE6DCCA))) } }
                repeat(2) { Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCFC3AD))) }
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE3E8F3)))
                Box(Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.15f)))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { repeat(3) { Box(Modifier.size(12.dp).background(accent.copy(alpha = 0.3f), CircleShape)) } }
                repeat(2) { Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(card)) }
            }
        }
    }
}
