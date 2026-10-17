package com.jarvis.app.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.jarvis.app.Launch
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.NavState
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.ORB_STYLES
import org.json.JSONObject

/**
 * App-control tools the Gemini Live model can call during a call: show option popups ("5-5"), pick one,
 * read the app state, switch camera / recording / screen share, and open a screen. All gated by Prefs.aiControl.
 */
object AppControl {
    val names = setOf(
        "get_app_state", "show_options", "select_option", "close_options",
        "set_screen_recording", "set_screen_share", "set_camera", "navigate"
    )

    private val routes = setOf(
        "home", "chat", "control", "profile", "api", "providers", "voices", "themes", "themelab",
        "memory", "assistant", "code", "skills", "uistyle", "access", "console"
    )

    fun execute(ctx: Context, name: String, args: JSONObject): String {
        if (!Prefs.aiControl) return "AI control is turned off in Settings, so I cannot do that."
        AppLog.i("AIControl", "$name $args")
        return when (name) {
            "get_app_state" -> state()
            "show_options" -> {
                val cat = args.optString("category").lowercase()
                AiPanel.show(cat, args.optInt("page", 0))
                    ?: "No options for '$cat'. Use one of: ${AiPanel.categories.joinToString(", ")}."
            }
            "select_option" -> {
                val cat = args.optString("category").lowercase()
                val id = args.optString("id")
                AiPanel.select(cat, id) ?: "Unknown $cat option '$id'. Call show_options first."
            }
            "close_options" -> { AiPanel.close(); "Options closed." }
            "set_screen_recording" -> {
                if (args.optBoolean("enabled")) {
                    ScreenRecordRequestActivity.open(ctx)
                    "Android is asking the user to allow recording. It starts when they press Start."
                } else { ScreenRecordService.stop(ctx); "Recording stopped." }
            }
            "set_screen_share" -> {
                if (args.optBoolean("enabled")) {
                    if (!JarvisAccessibilityService.canScreenshot) "Screen sharing needs Android 11+ and the accessibility service on."
                    else { ScreenShareService.start(ctx); "Sharing the screen with the call now." }
                } else { ScreenShareService.stop(ctx); "Screen sharing stopped." }
            }
            "set_camera" -> setCamera(ctx, args.optString("mode", "off"))
            "navigate" -> {
                val r = args.optString("screen").lowercase()
                if (r in routes) { Launch.route.value = r; "Opening $r." }
                else "Unknown screen '$r'. Use one of: ${routes.joinToString(", ")}."
            }
            else -> "Unknown command."
        }
    }

    private fun setCamera(ctx: Context, mode: String): String {
        val m = when (mode.lowercase()) {
            "front", "1" -> 1
            "back", "2" -> 2
            "off", "0", "" -> 0
            else -> return "mode must be off, front or back."
        }
        if (m != 0 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return "Camera permission is not granted. Open the call screen once and allow the camera."
        }
        CallCamera.mode = m
        CallService.setCamera(ctx, m != 0)
        return when (m) {
            0 -> "Camera off."
            1 -> "Camera on (front)."
            else -> "Camera on (back)."
        }
    }

    private fun state(): String {
        val orb = ORB_STYLES.getOrNull(Prefs.orbStyle) ?: "custom"
        return buildString {
            append("App state:\n")
            append("- screen: ${NavState.route}\n")
            append("- orb style: ${Prefs.orbStyle} ($orb)\n")
            append("- assistant theme: ${Prefs.assistantTheme}\n")
            append("- interface style: ${Prefs.uiStyle}\n")
            append("- colour theme: ${Prefs.theme}\n")
            append("- muted: ${CallHost.client?.muted == true}\n")
            append("- camera: ${CallCamera.mode}\n")
            append("- screen recording: ${ScreenRecordService.active}\n")
            append("- screen sharing: ${ScreenShareService.running}\n")
        }
    }
}
