package com.jarvis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import com.jarvis.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.ui.theme.*

fun goldBrush(): Brush = Brush.horizontalGradient(listOf(GoldDark, GoldLight, Gold))

@Composable
fun JText(
    text: String, size: Int, color: Color = Ivory, weight: FontWeight = FontWeight.Normal,
    serif: Boolean = false, ls: Float = 0f, lh: Int = 0, align: TextAlign? = null,
    modifier: Modifier = Modifier
) = Text(
    text = text, modifier = modifier, color = color, fontSize = size.sp, fontWeight = weight,
    fontFamily = if (serif && Palettes.serif) JarvisSerif else JarvisSans, letterSpacing = ls.sp,
    lineHeight = if (lh > 0) lh.sp else TextUnit.Unspecified, textAlign = align
)

@Composable
fun Hairline(alpha: Float = 0.16f) =
    Box(Modifier.fillMaxWidth().height(1.dp).background(Gold.copy(alpha = alpha)))

@Composable
fun JarvisOrb(size: Dp) {
    Box(Modifier.size(size).border(1.dp, Gold.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size * 0.74f).clip(CircleShape).background(Gold.copy(alpha = 0.06f))
                .border(1.dp, Gold.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(size * 0.46f)
                    .shadow(24.dp, CircleShape, ambientColor = Gold, spotColor = Gold)
                    .background(Brush.linearGradient(listOf(GoldLight, GoldDark)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                JText("J", (size.value * 0.24f).toInt(), Bg, FontWeight.Medium, serif = true)
            }
        }
    }
}

@Composable
fun GoldButton(label: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.shadow(16.dp, RoundedCornerShape(30.dp), ambientColor = Gold, spotColor = Gold)
            .clip(RoundedCornerShape(30.dp)).background(goldBrush()).clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JText(label, 15, Bg, FontWeight.SemiBold, ls = 0.5f)
        if (icon != null) Icon(icon, contentDescription = null, tint = Bg, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun GoldIconBox(icon: ImageVector, box: Int = 42, iconSize: Int = 22, radius: Int = 13) {
    Box(
        Modifier.size(box.dp).clip(RoundedCornerShape(radius.dp)).background(Gold.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(iconSize.dp)) }
}

@Composable
fun ScreenScaffold(current: String?, onNav: (String) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        if (Palettes.engine == 4 && current != null) com.jarvis.app.ui.styles.FolioTabs(current, onNav)
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        if (current != null) BottomNav(current, onNav)
    }
}

/** The bottom bar follows the chosen interface style. */
@Composable
fun BottomNav(current: String, onNav: (String) -> Unit) = when (Palettes.engine) {
    1 -> com.jarvis.app.ui.styles.AuroraNav(current, onNav)
    2 -> com.jarvis.app.ui.styles.NovaNav(current, onNav)
    3 -> com.jarvis.app.ui.styles.PulseNav(current, onNav)
    4 -> Unit
    5 -> com.jarvis.app.ui.styles.IfaceNav(current, onNav)
    else -> ClassicBottomNav(current, onNav)
}

@Composable
private fun ClassicBottomNav(current: String, onNav: (String) -> Unit) {
    val items = listOf(
        Triple("home", "Home", R.drawable.ic_nav_home),
        Triple("chat", "Assistant", R.drawable.ic_nav_assistant),
        Triple("control", "Control", R.drawable.ic_nav_control),
        Triple("profile", "Profile", R.drawable.ic_nav_profile)
    )
    Column(Modifier.fillMaxWidth().background(Surface1)) {
        Hairline()
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)) {
            items.forEach { (route, label, icon) ->
                val active = route == current
                val tint = if (active) Gold else Muted
                Column(
                    Modifier.weight(1f).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { if (!active) onNav(route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.size(56.dp, 30.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (active) Gold.copy(alpha = 0.16f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { JIcon(icon, tint, 24.dp) }
                    JText(label, 11, tint, if (active) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun SubHeader(title: String, onBack: () -> Unit) = when (Palettes.engine) {
    1 -> com.jarvis.app.ui.styles.AuroraHeader(title, onBack)
    2 -> com.jarvis.app.ui.styles.NovaHeader(title, onBack)
    3 -> com.jarvis.app.ui.styles.PulseHeader(title, onBack)
    4 -> com.jarvis.app.ui.styles.FolioHeader(title, onBack)
    5 -> com.jarvis.app.ui.styles.IfaceHeader(title, onBack)
    else -> ClassicSubHeader(title, onBack)
}

@Composable
private fun ClassicSubHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Ivory,
            modifier = Modifier.size(24.dp).clickable { onBack() })
        JText(title, 22, Ivory, FontWeight.Medium, serif = true)
    }
}

@Composable
fun JField(
    value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, secret: Boolean = false, minHeight: Dp = 0.dp,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.clip(shape).background(Surface1).border(1.dp, Gold.copy(alpha = 0.25f), shape)
            .heightIn(min = minHeight).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicTextField(
            value = value, onValueChange = onChange, modifier = Modifier.weight(1f), singleLine = singleLine,
            textStyle = TextStyle(color = Ivory, fontSize = 14.sp, lineHeight = 22.sp),
            cursorBrush = SolidColor(Gold),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { inner ->
                Box { if (value.isEmpty()) JText(hint, 14, Muted); inner() }
            }
        )
        trailing?.invoke()
    }
}

@Composable
fun JToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val bg: Brush = if (on) goldBrush() else SolidColor(Surface2)
    Box(
        Modifier.size(42.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .clickable { onChange(!on) }.padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) { Box(Modifier.size(18.dp).background(if (on) Bg else Muted, CircleShape)) }
}

@Composable
fun JRadio(on: Boolean) {
    Box(Modifier.size(22.dp).border(1.6.dp, if (on) Gold else Muted, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Box(Modifier.size(10.dp).background(goldBrush(), CircleShape))
    }
}

@Composable
fun OutlineButton(label: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(28.dp)).border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            .clickable { onClick() }.padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JText(label, 14, Gold, FontWeight.SemiBold, ls = 0.4f)
        if (icon != null) Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
    }
}


@Composable
fun JIcon(@DrawableRes id: Int, tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(id), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

@Composable
fun GoldIconBoxRes(@DrawableRes id: Int, box: Int = 42, iconSize: Int = 22, radius: Int = 13) {
    Box(
        Modifier.size(box.dp).clip(RoundedCornerShape(radius.dp)).background(Gold.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) { JIcon(id, Gold, iconSize.dp) }
}

/** Premium loading panel: orb + real percentage + stage name. */
@Composable
fun LoadingPanel(percent: Int, label: String, title: String = "Activating your system") {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        SiriOrb(220.dp, 0.25f, 2)
        JText(title.uppercase(), 11, Gold, FontWeight.Medium, ls = 3f)
        JText("${percent.coerceIn(0, 100)}%", 56, Ivory, FontWeight.Medium, serif = true)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Surface2)) {
            Box(Modifier.fillMaxWidth(percent.coerceIn(0, 100) / 100f).fillMaxHeight().background(goldBrush()))
        }
        JText(label, 13, Muted)
    }
}
