package com.jarvis.app

import com.jarvis.app.data.AppLog
import com.jarvis.app.data.CustomProviders
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.app.audio.Speaker
import com.jarvis.app.data.ChatStore
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.ModelCatalog
import com.jarvis.app.data.Prefs
import com.jarvis.app.live.CallHost
import com.jarvis.app.live.WakeService
import com.jarvis.app.overlay.Overlay
import com.jarvis.app.ui.screens.*
import com.jarvis.app.ui.theme.JarvisTheme

/** A screen requested from outside the UI (floating bubble, popup, notification). */
object Launch { val route = mutableStateOf<String?>(null) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Launch.route.value = intent?.getStringExtra("route")
        Prefs.init(this)
        AppLog.init(this)
        CustomProviders.init(this)
        ModelRouter.init(this)
        com.jarvis.app.data.RealFacts.init(this)
        com.jarvis.app.code.Workspace.init(this)
        com.jarvis.app.code.TaskStateStore.init(this)
        com.jarvis.app.code.Skills.init(this)
        Speaker.init(this)
        ChatStore.init(this)
        DeviceControl.init(this)
        if (Prefs.wakeEnabled && Prefs.wakePhrase.isNotBlank() && !WakeService.running) {
            try { WakeService.start(this) } catch (e: Exception) { AppLog.w(e); }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent { JarvisTheme { JarvisApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Launch.route.value = intent.getStringExtra("route")
    }

    // The app is on screen: floating windows are not needed.
    override fun onStart() { super.onStart(); Overlay.hideAll() }

    // Home pressed (or another app opened) while a call is running: keep it alive as a draggable bubble.
    override fun onStop() {
        super.onStop()
        if (CallHost.active && !isChangingConfigurations) Overlay.showBubble(this)
    }
}

private val bottomRoutes = setOf("home", "chat", "control", "profile")

@Composable
fun JarvisApp() {
    val nav = rememberNavController()
    LaunchedEffect(Unit) { if (Prefs.apiKey.isNotBlank()) ModelCatalog.refresh() }
    // Never re-open the screen you are already on (stops the "refresh/splash" flicker on repeated taps).
    val go: (String) -> Unit = { r ->
        if (nav.currentDestination?.route != r) {
            nav.navigate(r) {
                if (r in bottomRoutes) {
                    popUpTo("home") { saveState = true }
                    restoreState = true
                }
                launchSingleTop = true
            }
        }
    }
    val back: () -> Unit = { nav.popBackStack() }
    val pending = Launch.route.value
    LaunchedEffect(pending) {
        if (pending != null && Prefs.onboarded) { go(pending); Launch.route.value = null }
    }
    NavHost(
        navController = nav,
        startDestination = if (Prefs.onboarded) "home" else "welcome",
        enterTransition = { fadeIn(tween(140)) },
        exitTransition = { fadeOut(tween(100)) },
        popEnterTransition = { fadeIn(tween(140)) },
        popExitTransition = { fadeOut(tween(100)) }
    ) {
        composable("welcome") { WelcomeScreen(onStart = { nav.navigate("language") }) }
        composable("language") { LanguageScreen(onNext = { nav.navigate("permissions") }) }
        composable("permissions") { PermissionsScreen(onNext = { nav.navigate("apisetup") }) }
        composable("apisetup") { ApiSetupScreen(onNext = { nav.navigate("setup") }) }
        composable("setup") { SetupScreen(onDone = { nav.navigate("home") { popUpTo("welcome") { inclusive = true } } }) }
        composable("home") { HomeScreen(go) }
        composable("chat") { ChatScreen(go) }
        composable("control") { ControlScreen(go) }
        composable("profile") { ProfileScreen(go) }
        composable("live") { LiveVoiceScreen(onClose = back, onChat = { nav.popBackStack(); go("chat") }, onApi = { go("api") }) }
        composable("api") { ApiScreen(back) { go("providers") } }
        composable("providers") { ProvidersScreen(back) }
        composable("voices") { VoiceScreen(back) }
        composable("themes") { ThemeScreen(back) }
        composable("memory") { MemoryScreen(back) }
        composable("assistant") { AssistantScreen(back) }
        composable("code") { CodeScreen(back) }
        composable("skills") { SkillsScreen(back) }
        composable("uistyle") { UiStyleScreen(back) }
        composable("access") { AccessScreen(back) }
        composable("console") { ConsoleScreen(back) }
    }
}
