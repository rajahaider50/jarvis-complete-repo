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
        "set_screen_recording", "set_screen_share", "set_camera", "navigate", "look_at_screen",
        "get_screen_info", "cursor_create", "cursor_move", "cursor_click", "cursor_swipe", "click_all",
        "cursor_remove", "cursor_clear", "cursor_list", "grid", "find_element", "recall_clicks", "save_click"
    )

    private val cursorNames = setOf(
        "get_screen_info", "cursor_create", "cursor_move", "cursor_click", "cursor_swipe", "click_all",
        "cursor_remove", "cursor_clear", "cursor_list", "grid", "find_element", "recall_clicks", "save_click"
    )

    private val routes = setOf(
        "home", "chat", "control", "profile", "api", "providers", "voices", "themes", "themelab",
        "memory", "assistant", "code", "skills", "uistyle", "access", "console", "cursorlab", "cursormemory"
    )

    fun execute(ctx: Context, name: String, args: JSONObject): String {
        if (name in cursorNames) return cursor(ctx, name, args)
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
            "look_at_screen" -> {
                if (!JarvisAccessibilityService.canScreenshot) "Looking needs Android 11+ and the accessibility service on."
                else { JarvisAccessibilityService.screenshot { b -> if (b != null) { CallHost.client?.sendJpeg(com.jarvis.app.data.ImageUtil.toJpeg(b, 1024, 70)); b.recycle() } }; "Sent you a fresh look at the screen." }
            }
            "navigate" -> {
                val r = args.optString("screen").lowercase()
                if (r in routes) { Launch.route.value = r; "Opening $r." }
                else "Unknown screen '$r'. Use one of: ${routes.joinToString(", ")}."
            }
            else -> "Unknown command."
        }
    }

    private fun cursor(ctx: Context, name: String, args: JSONObject): String {
        if (!Prefs.aiCursor) return "AI cursor control is off. Turn it on in Settings."
        if (!JarvisAccessibilityService.enabled) return "The accessibility service is off, so I cannot control the screen."
        AppLog.i("AICursor", "$name $args")
        val x = args.optDouble("x", 0.0).toFloat()
        val y = args.optDouble("y", 0.0).toFloat()
        return when (name) {
            "get_screen_info" -> {
                val s = CursorGeometry.screenSize(ctx)
                "Screen ${s.w}x${s.h}px. Centre is (0,0); X right +, Y up +; range -100..+100. " +
                    "Foreground app: ${JarvisAccessibilityService.foregroundPackage() ?: "unknown"}. " +
                    "Cursors: ${CursorController.list()}. Grid: ${CursorController.gridOn}."
            }
            "cursor_create" -> {
                val id = CursorController.add(x, y, args.optString("label").ifBlank { null })
                if (id > 0) { CursorController.select(id); CursorPanel.refresh(); "cursor id=$id at ${CursorGeometry.fmt(x)}, ${CursorGeometry.fmt(y)}" }
                else "could not add a cursor (max 10 or service off)"
            }
            "cursor_move" -> {
                val id = args.optInt("id", CursorController.selectedId ?: 1)
                val mode = args.optString("mode", "absolute").lowercase()
                val cur = CursorController.positions().firstOrNull { it.first == id }
                val tx: Float; val ty: Float
                if (mode == "relative" && cur != null) { tx = cur.second + x; ty = cur.third + y } else { tx = x; ty = y }
                CursorController.moveAnimated(id, tx, ty, args.optLong("duration_ms", 450))
                CursorController.select(id); CursorPanel.refresh()
                "cursor #$id -> ${CursorGeometry.fmt(tx)}, ${CursorGeometry.fmt(ty)}"
            }
            "cursor_click" -> {
                val id = args.optInt("id", CursorController.selectedId ?: 1)
                val explicit = args.optString("target").ifBlank { null }
                val type = when (args.optString("type", "tap").lowercase()) {
                    "double", "two" -> CursorController.ClickType.DOUBLE
                    "long", "hold", "press" -> CursorController.ClickType.LONG
                    else -> CursorController.ClickType.TAP
                }
                var result = "clicking"
                var okFlag = false
                CursorController.select(id)
                CursorController.click(id, type) { ok -> okFlag = ok; result = if (ok) "clicked cursor #$id" else "click was blocked (rate limit or busy)" }
                Thread.sleep(150)
                val pkg = JarvisAccessibilityService.foregroundPackage()
                val p = CursorController.pointOf(id)
                if (okFlag && p != null) {
                    val label = explicit ?: CursorController.labelOf(id)
                    if (!label.isNullOrBlank() && label != id.toString()) {
                        ClickMemory.remember(pkg, label, p.first, p.second, source = "ai_success")
                        CursorController.lastAiTarget = label; CursorController.lastAiPoint = p
                    }
                } else if (!okFlag && p != null) {
                    ClickMemory.fail(pkg, p.first, p.second)
                }
                result
            }
            "cursor_swipe" -> {
                val id = args.optInt("id", CursorController.selectedId ?: 1)
                val tx = args.optDouble("to_x", 0.0).toFloat()
                val ty = args.optDouble("to_y", 0.0).toFloat()
                CursorController.swipe(id, tx, ty, args.optLong("duration_ms", 300))
                "swiped cursor #$id to ${CursorGeometry.fmt(tx)}, ${CursorGeometry.fmt(ty)}"
            }
            "click_all" -> { CursorController.clickAll(); "clicked all cursors" }
            "cursor_remove" -> { CursorController.remove(args.optInt("id", CursorController.selectedId ?: -1)); CursorPanel.refresh(); "cursor removed" }
            "cursor_clear" -> { CursorController.clear(); "all cursors removed" }
            "cursor_list" -> CursorController.list()
            "grid" -> { CursorController.showGrid(args.optBoolean("show", true)); if (CursorController.gridOn) "grid on" else "grid off" }
            "find_element" -> JarvisAccessibilityService.findElementNorm(args.optString("query").ifBlank { args.optString("text_or_desc") })
            "recall_clicks" -> ClickMemory.recall(args.optString("query"), JarvisAccessibilityService.foregroundPackage())
            "save_click" -> {
                val t = args.optString("target").ifBlank { args.optString("label") }.trim()
                val pkg = JarvisAccessibilityService.foregroundPackage()
                val p = CursorController.pointOf(CursorController.selectedId ?: -1)
                if (p == null) "no cursor to save - move a cursor first"
                else {
                    val old = CursorController.lastAiPoint
                    val res = if (args.optBoolean("correct", false) && old != null) ClickMemory.correct(pkg, t, old.first, old.second, p.first, p.second)
                    else ClickMemory.remember(pkg, t, p.first, p.second, source = "user_marked")
                    CursorController.lastAiTarget = t; CursorController.lastAiPoint = p
                    res
                }
            }
            else -> "Unknown cursor command."
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
