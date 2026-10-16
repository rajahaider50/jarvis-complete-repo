package com.jarvis.app.ui.styles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.data.Stats
import com.jarvis.app.ui.components.JText
import com.jarvis.app.ui.components.goldBrush
import com.jarvis.app.ui.theme.*

/** What the Home screen controller hands to every interface style. */
class HomeUi(
    val stats: Stats, val stage: Int, val talking: Boolean,
    val greet: () -> Unit, val onNav: (String) -> Unit
)

val Warn = Color(0xFFE0665C)

fun gemStatus(): String =
    if (ModelCatalog.ok == true) "Gemini link · connected"
    else if (Prefs.apiKey.isBlank()) "Add your Gemini API key in Settings"
    else "Gemini link · ${ModelCatalog.status.ifBlank { "connecting…" }}"

// ------------------------------------------------------------------------------------------------
// Navigation bars
// ------------------------------------------------------------------------------------------------

/** Aurora: a floating glass pill; only the active tab shows its name. Rounded icons. */
@Composable
fun AuroraNav(current: String, onNav: (String) -> Unit) {
    val items = listOf(
        Triple("home", "Home", Icons.Rounded.Home), Triple("chat", "Assistant", Icons.Rounded.AutoAwesome),
        Triple("control", "Control", Icons.Rounded.Tune), Triple("profile", "Settings", Icons.Rounded.Settings)
    )
    val shape = RoundedCornerShape(34.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().shadow(14.dp, shape, ambientColor = Gold, spotColor = Gold).clip(shape)
                .background(Surface2.copy(alpha = 0.96f)).border(1.dp, Gold.copy(alpha = 0.22f), shape).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, label, icon) ->
                val active = route == current
                Row(
                    Modifier.clip(RoundedCornerShape(26.dp)).then(if (active) Modifier.background(goldBrush()) else Modifier)
                        .clickable { if (!active) onNav(route) }.padding(horizontal = if (active) 16.dp else 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, label, tint = if (active) Bg else Muted, modifier = Modifier.size(22.dp))
                    if (active) JText(label, 13, Bg, FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Nova: a flat bar with a tonal indicator and always-visible labels, like Google's own apps. Filled icons. */
@Composable
fun NovaNav(current: String, onNav: (String) -> Unit) {
    val items = listOf(
        Triple("home", "Home", Icons.Filled.Home), Triple("chat", "Assistant", Icons.Filled.AutoAwesome),
        Triple("control", "Control", Icons.Filled.Dashboard), Triple("profile", "Account", Icons.Filled.AccountCircle)
    )
    Row(Modifier.fillMaxWidth().background(Surface1).padding(top = 10.dp, bottom = 12.dp)) {
        items.forEach { (route, label, icon) ->
            val active = route == current
            Column(
                Modifier.weight(1f).clickable { if (!active) onNav(route) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(64.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (active) Gold.copy(alpha = 0.20f) else Color.Transparent), contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = if (active) Gold else Muted, modifier = Modifier.size(24.dp))
                }
                JText(label, 12, if (active) Ivory else Muted, if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Headers for inner pages
// ------------------------------------------------------------------------------------------------

/** Aurora header: round back button, centered title. */
@Composable
fun AuroraHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Surface2, CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(22.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { JText(title, 18, Ivory, FontWeight.SemiBold) }
        Spacer(Modifier.size(42.dp))
    }
}

/** Nova header: back arrow and a large left-aligned title. */
@Composable
fun NovaHeader(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Ivory, modifier = Modifier.size(24.dp))
        }
        JText(title, 28, Ivory, FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp))
    }
}

// ------------------------------------------------------------------------------------------------
// Small shared pieces
// ------------------------------------------------------------------------------------------------

@Composable
fun SectionLabel(text: String) = JText(text, 13, Muted, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 4.dp))

@Composable
fun GlassCircle(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).background(Surface2, CircleShape).border(1.dp, Gold.copy(alpha = 0.18f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(21.dp))
    }
}

/** A tile with a circular progress ring (Aurora). */
@Composable
fun RingTile(label: String, value: String, sub: String, frac: Float, warn: Boolean, icon: ImageVector, modifier: Modifier) {
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), tween(1200), label = "ring")
    val col = if (warn) Warn else Gold
    val shape = RoundedCornerShape(26.dp)
    val track = Surface2
    Row(
        modifier.clip(shape).background(Surface1).border(1.dp, col.copy(alpha = 0.22f), shape).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sw = 6.dp.toPx()
                val tl = Offset(sw / 2, sw / 2)
                val sz = Size(size.width - sw, size.height - sw)
                drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(sw))
                drawArc(col, -90f, 360f * f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Icon(icon, null, tint = col, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            JText(value, 18, Ivory, FontWeight.SemiBold)
            JText(label, 11, Muted)
            JText(sub, 10, Muted)
        }
    }
}

/** A list row with a thin progress bar (Nova). */
@Composable
fun BarRow(icon: ImageVector, label: String, value: String, frac: Float, warn: Boolean) {
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), tween(1200), label = "bar")
    val col = if (warn) Warn else Gold
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(col.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = col, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row { JText(label, 15, Ivory, FontWeight.Medium, modifier = Modifier.weight(1f)); JText(value, 13, Muted) }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Surface2)) { Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(col)) }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Which interface draws each tab
// ------------------------------------------------------------------------------------------------
@Composable
fun StyledHome(h: HomeUi) = when (Palettes.engine) { 1 -> AuroraHome(h); 2 -> NovaHome(h); 3 -> PulseHome(h); 5 -> IfaceHome(h); else -> FolioHome(h) }

@Composable
fun StyledControl(onNav: (String) -> Unit) = when (Palettes.engine) { 1 -> AuroraControl(onNav); 2 -> NovaControl(onNav); 3 -> PulseControl(onNav); 5 -> IfaceControl(onNav); else -> FolioControl(onNav) }

@Composable
fun StyledSettings(onNav: (String) -> Unit) = when (Palettes.engine) { 1 -> AuroraSettings(onNav); 2 -> NovaSettings(onNav); 3 -> PulseSettings(onNav); 5 -> IfaceSettings(onNav); else -> FolioSettings(onNav) }
