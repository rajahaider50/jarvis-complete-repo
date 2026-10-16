package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.app.ui.components.*
import com.jarvis.app.ui.theme.*

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    ScreenScaffold(null, {}) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically)
        ) {
            JarvisOrb(230.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JText("JARVIS", 36, Ivory, FontWeight.Medium, serif = true, ls = 9f)
                Box(Modifier.width(44.dp).height(1.dp).background(Gold.copy(alpha = 0.6f)))
                JText("PRIVATE AI CONCIERGE", 11, Gold, FontWeight.Medium, ls = 3f)
                JText(
                    "Elegant intelligence, tailored to your life. Discreet, secure and always at your service.",
                    14, Muted, lh = 22, align = TextAlign.Center
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoldButton("Get Started", Icons.Outlined.ArrowForward, onStart, Modifier.fillMaxWidth())
            JText("I already have an account", 13, Muted)
        }
    }
}
