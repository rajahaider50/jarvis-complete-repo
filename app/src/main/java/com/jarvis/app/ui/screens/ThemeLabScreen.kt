package com.jarvis.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.audio.AudioLevel
import com.jarvis.app.ui.components.ScreenScaffold
import com.jarvis.app.ui.components.SubHeader
import com.jarvis.app.ui.theme.AssistantState
import com.jarvis.app.ui.theme.AssistantTheme
import com.jarvis.app.ui.theme.Fx
import com.jarvis.app.ui.theme.ThemeRegistry
import com.jarvis.app.ui.theme.ThemeRepository
import com.jarvis.app.ui.theme.anim.rememberPhase
import com.jarvis.app.ui.theme.fx.FxEvent
import kotlin.math.abs
import kotlin.math.sin

/** Debug screen: shows every theme at once with live state/level/sound controls. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeLabScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(AssistantState.IDLE) }
    var level by remember { mutableStateOf(0f) }
    var simulate by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<AssistantTheme?>(null) }
    var fps by remember { mutableIntStateOf(0) }
    val t by rememberPhase(1f)
    val shown = if (simulate) AudioLevel.simulated(t) else level

    LaunchedEffect(Unit) {
        var frames = 0; var last = 0L
        while (true) {
            withFrameNanos { now ->
                frames++
                if (last == 0L) last = now
                if (now - last >= 1_000_000_000L) { fps = frames; frames = 0; last = now }
            }
        }
    }

    val pv = preview
    if (pv != null) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable { preview = null }) {
            pv.Render(state, shown, Modifier.fillMaxSize())
            Text("${pv.displayName} · tap to close", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
        return
    }

    ScreenScaffold(null, {}) {
        SubHeader("Theme Lab", onBack)
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistantState.values().forEach { s ->
                    FilterChip(selected = state == s, onClick = { state = s }, label = { Text(s.name, fontSize = 11.sp) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Level", color = Color.White, fontSize = 11.sp)
                Slider(value = level, onValueChange = { level = it; simulate = false }, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Text(if (simulate) "sim ${"%.2f".format(shown)}" else "%.2f".format(level), color = Color.White, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = simulate, onClick = { simulate = !simulate }, label = { Text("Simulate speech", fontSize = 10.sp) })
                Text("FPS $fps", color = Color(0xFF7FD4A0), fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FxEvent.values().forEach { e ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1E)).border(1.dp, Color(0xFF3A3A42), RoundedCornerShape(8.dp)).clickable { Fx.play(e) }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(e.name, color = Color(0xFFC9C4B8), fontSize = 9.sp)
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemeRegistry.all, key = { it.id }) { theme ->
                    Column(Modifier.padding(2.dp)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.Black)
                                .combinedClickable(onClick = { preview = theme }, onLongClick = { ThemeRepository.select(theme.id) })
                        ) { theme.Render(state, shown, Modifier.fillMaxSize()) }
                        Text(theme.displayName, color = Color.White, fontSize = 11.sp,
                            fontWeight = if (ThemeRepository.selectedId() == theme.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}
