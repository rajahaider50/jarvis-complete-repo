package com.jarvis.app.live

import android.content.Context
import com.jarvis.app.data.AppLog
import com.jarvis.app.data.DeviceControl
import com.jarvis.app.data.DeviceStats
import com.jarvis.app.data.PhoneActions
import com.jarvis.app.data.Weather
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything the assistant can do on the phone, described once and used by BOTH call engines
 * (Gemini Live function calling and OpenAI-style tool calling for custom providers).
 */
object PhoneTools {
    class Tool(val name: String, val desc: String, val props: List<Pair<String, String>> = emptyList())

    val all = listOf(
        Tool("set_flashlight", "Turn the phone flashlight on or off", listOf("on" to "BOOLEAN")),
        Tool("set_volume", "Set the phone media volume, 0 to 100 percent", listOf("percent" to "INTEGER")),
        Tool("device_status", "Read the REAL current status of this phone: time, battery, temperature, storage, memory, Wi-Fi, Bluetooth, volume, brightness and weather"),
        Tool("get_time", "Read the real current date and time"),
        Tool("get_weather", "Current weather for a city, or for the phone's location if the city is empty", listOf("city" to "STRING")),
        Tool("read_screen", "Read all text currently visible on the phone screen"),
        Tool("click_on_screen", "Tap a button or item on the screen by its visible text", listOf("text" to "STRING")),
        Tool("type_text", "Type text into the focused input field on the screen", listOf("text" to "STRING")),
        Tool("scroll_screen", "Scroll the current screen. direction is down or up", listOf("direction" to "STRING")),
        Tool("press_button", "Press a system button: back, home, recents, notifications, quick_settings, power_menu, lock_screen or screenshot", listOf("button" to "STRING")),
        Tool("end_phone_call", "Hang up the current phone or app call (the red End / Decline button). Only when the user clearly asks"),
        Tool("answer_phone_call", "Answer the phone call that is ringing. Only when the user clearly asks"),
        Tool("end_assistant_call", "End this conversation with the assistant (hang up the live call with you). Say goodbye first"),
        Tool("lock_screen", "Lock the phone screen now. Only when the user clearly asks"),
        Tool("power_menu", "Show the power-off menu. The user taps the option; you cannot shut the phone down yourself"),
        Tool("take_screenshot", "Take a screenshot of the phone screen (saved to the gallery)"),
        Tool("share_screen", "Start sharing the screen with this call so you can see it. Android shows a question the user must confirm"),
        Tool("stop_screen_share", "Stop sharing the screen"),
        Tool("open_settings", "Open a settings page: wifi, bluetooth, display, sound, battery, apps, location, accessibility, date, language, storage, security, privacy, airplane, nfc, network, about", listOf("page" to "STRING")),
        Tool("set_timer", "Start a countdown timer", listOf("seconds" to "INTEGER")),
        Tool("set_alarm", "Set an alarm clock. hour 0-23, minute 0-59", listOf("hour" to "INTEGER", "minute" to "INTEGER")),
        Tool("dial_number", "Open the phone dialer with a number typed in (the user presses call)", listOf("number" to "STRING")),
        Tool("swipe_screen", "Swipe the screen. direction: up (next video / scroll down), down, left or right", listOf("direction" to "STRING")),
        Tool("tap_at", "Tap a spot on the screen by percentage: x and y from 0 to 100", listOf("x" to "NUMBER", "y" to "NUMBER")),
        Tool("double_tap", "Double tap a spot on the screen (for example to like a video): x and y from 0 to 100", listOf("x" to "NUMBER", "y" to "NUMBER")),
        Tool("long_press_text", "Long press an item on the screen by its visible text (for example an image, to save it)", listOf("text" to "STRING")),
        Tool("submit_text", "Press the Enter / Search / Send key after typing"),
        Tool("paste_text", "Paste the clipboard into the focused field"),
        Tool("media_control", "Control playing video or music. action: play_pause, next, previous, mute, unmute, volume_up, volume_down", listOf("action" to "STRING")),
        Tool("wait_seconds", "Wait for an app or page to finish loading (1 to 6 seconds)", listOf("seconds" to "NUMBER")),
        Tool("open_app", "Open an installed app by its name", listOf("name" to "STRING")),
        Tool("open_url", "Open a web address in the browser", listOf("url" to "STRING")),
        Tool("get_app_state", "Read the current state of the JARVIS app: which screen is open, the selected orb style, assistant theme, interface style, colour theme, mute, camera and whether screen recording or screen sharing is on"),
        Tool("show_options", "Show a page of five options as popups on the screen and read them out. category is orb, theme, interface or color. page starts at 0 (5 options per page). Use this whenever the user wants to change how the assistant looks", listOf("category" to "STRING", "page" to "INTEGER")),
        Tool("select_option", "Apply an option that was shown with show_options. category and id must match the shown options", listOf("category" to "STRING", "id" to "STRING")),
        Tool("close_options", "Close the option popups on the screen"),
        Tool("set_screen_recording", "Start or stop recording this call (screen video plus your voice and the user's voice). Android shows a question the user must confirm", listOf("enabled" to "BOOLEAN")),
        Tool("set_screen_share", "Start or stop sharing the phone screen with this call so you can see it. Android shows a question the user must confirm", listOf("enabled" to "BOOLEAN")),
        Tool("set_camera", "Turn the call camera on or off. mode is off, front or back", listOf("mode" to "STRING")),
        Tool("navigate", "Open a screen of the JARVIS app: home, chat, control, profile, api, providers, voices, themes, themelab, memory, assistant, code, skills, uistyle, access, console, cursorlab", listOf("screen" to "STRING")),
        Tool("look_at_screen", "Take a fresh look at the screen right now and send the image to you"),
        Tool("get_screen_info", "Read the cursor coordinate system, the screen size, the foreground app and the current cursors"),
        Tool("cursor_create", "Place a new AI cursor on the screen. Coordinates are -100..+100 (centre 0). X right is +, Y up is +. Up to 10 cursors with their own number and colour", listOf("x" to "NUMBER", "y" to "NUMBER", "label" to "STRING")),
        Tool("cursor_move", "Move a cursor to a coordinate. mode is absolute (x,y are the target) or relative (x,y are added to the current spot). It slides smoothly so the user can follow", listOf("id" to "INTEGER", "x" to "NUMBER", "y" to "NUMBER", "mode" to "STRING")),
        Tool("cursor_click", "Click where a cursor is. type is tap, double or long. The result is the TRUTH: outcome (completed/cancelled/rejected/timeout/rate_limited/no_service), the screen pixel, what is under_cursor and effect (changed/no_change). If outcome is not completed, or under_cursor is not the thing you wanted, move the cursor and try again — never tell the user you clicked when you did not. Optional target = a name so this spot is remembered", listOf("id" to "INTEGER", "type" to "STRING", "target" to "STRING")),
        Tool("cursor_swipe", "Swipe from a cursor to another coordinate (scroll / next)", listOf("id" to "INTEGER", "to_x" to "NUMBER", "to_y" to "NUMBER", "duration_ms" to "INTEGER")),
        Tool("click_all", "Click every cursor on the screen at the same time"),
        Tool("cursor_remove", "Remove one cursor", listOf("id" to "INTEGER")),
        Tool("cursor_clear", "Remove all cursors"),
        Tool("cursor_list", "List the current cursors and their positions"),
        Tool("grid", "Show or hide the coordinate grid overlay (-100..+100 with labels)", listOf("show" to "BOOLEAN")),
        Tool("find_element", "Find a button or text on the screen by its visible text or description and get its centre in cursor coordinates", listOf("query" to "STRING")),
        Tool("recall_clicks", "Look up spots you remembered earlier for a target in the app the user is now using, and get their cursor coordinates", listOf("query" to "STRING")),
        Tool("save_click", "Remember where the selected cursor is for a target name (so next time you go straight there). Set correct=true when the user moved the cursor to fix your mistake", listOf("target" to "STRING", "correct" to "BOOLEAN"))
    )

    /** Gemini Live format. */
    fun gemini(): JSONObject {
        val arr = JSONArray()
        all.forEach { t ->
            val o = JSONObject().put("name", t.name).put("description", t.desc)
            if (t.props.isNotEmpty()) {
                val p = JSONObject()
                t.props.forEach { (k, ty) -> p.put(k, JSONObject().put("type", ty)) }
                o.put("parameters", JSONObject().put("type", "OBJECT").put("properties", p).put("required", JSONArray(t.props.map { it.first })))
            }
            arr.put(o)
        }
        return JSONObject().put("functionDeclarations", arr)
    }

    /** OpenAI-compatible format. */
    fun openAi(): JSONArray {
        val arr = JSONArray()
        all.forEach { t ->
            val p = JSONObject()
            t.props.forEach { (k, ty) -> p.put(k, JSONObject().put("type", ty.lowercase())) }
            val params = JSONObject().put("type", "object").put("properties", p)
            if (t.props.isNotEmpty()) params.put("required", JSONArray(t.props.map { it.first }))
            arr.put(JSONObject().put("type", "function").put("function", JSONObject().put("name", t.name).put("description", t.desc).put("parameters", params)))
        }
        return arr
    }

    fun execute(ctx: Context, name: String, args: JSONObject): String = try {
        val a11y = PhoneActions.needsAccessibility()
        val x = args.optDouble("x", 50.0).toFloat()
        val y = args.optDouble("y", 50.0).toFloat()
        when (name) {
            in AppControl.names -> AppControl.execute(ctx, name, args)
            "set_flashlight" -> if (DeviceControl.setTorch(ctx, args.optBoolean("on"))) "ok" else "this phone has no flashlight"
            "set_volume" -> { DeviceControl.setMusicVolumePercent(ctx, args.optInt("percent", 50)); "ok" }
            "device_status" -> com.jarvis.app.data.RealFacts.phoneFacts()
            "get_time" -> com.jarvis.app.data.RealFacts.timeLine()
            "get_weather" -> kotlinx.coroutines.runBlocking {
                val city = args.optString("city")
                (if (city.isBlank()) Weather.fetch(ctx) else Weather.fetchCity(city)) ?: "The weather could not be read right now (no internet answer). Do not guess it."
            }
            "read_screen" -> a11y ?: JarvisAccessibilityService.readScreen()
            "click_on_screen" -> a11y ?: if (JarvisAccessibilityService.clickByText(args.optString("text"))) "tapped" else "could not find that on the screen"
            "type_text" -> a11y ?: if (JarvisAccessibilityService.typeText(args.optString("text"))) "typed" else "no text field is focused"
            "scroll_screen" -> a11y ?: if (JarvisAccessibilityService.scroll(args.optString("direction") != "up")) "scrolled" else "nothing to scroll"
            "press_button" -> a11y ?: if (JarvisAccessibilityService.press(args.optString("button"))) "done" else "unknown button"
            "swipe_screen" -> a11y ?: if (JarvisAccessibilityService.swipe(args.optString("direction"))) "swiped" else "swipe failed"
            "tap_at" -> a11y ?: if (JarvisAccessibilityService.tapAt(x, y)) "tapped" else "tap failed"
            "double_tap" -> a11y ?: if (JarvisAccessibilityService.doubleTapAt(x, y)) "double tapped" else "double tap failed"
            "long_press_text" -> a11y ?: if (JarvisAccessibilityService.longClickByText(args.optString("text"))) "long pressed" else "could not find that on the screen"
            "submit_text" -> a11y ?: if (JarvisAccessibilityService.submit()) "submitted" else "no field to submit"
            "paste_text" -> a11y ?: if (JarvisAccessibilityService.paste()) "pasted" else "no field to paste into"
            "end_phone_call" -> PhoneActions.endPhoneCall(ctx)
            "answer_phone_call" -> PhoneActions.answerPhoneCall(ctx)
            "end_assistant_call" -> {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ CallHost.end(ctx, showPopup = false) }, 2500)
                "The call will end in a moment. Say goodbye now."
            }
            "lock_screen" -> PhoneActions.lockScreen(ctx)
            "power_menu" -> a11y ?: if (JarvisAccessibilityService.press("power_dialog")) "The power menu is showing. The user has to tap an option." else "could not show the power menu"
            "take_screenshot" -> a11y ?: if (JarvisAccessibilityService.press("screenshot")) "Screenshot taken." else "Screenshots need Android 9 or newer."
            "share_screen" -> { ScreenShareService.start(ctx); "Sharing the screen with the call now. You can see what is on the screen." }
            "stop_screen_share" -> { ScreenShareService.stop(ctx); "Screen sharing stopped." }
            "open_settings" -> PhoneActions.openSettings(ctx, args.optString("page"))
            "set_timer" -> PhoneActions.setTimer(ctx, args.optInt("seconds", 60), "Jarvis")
            "set_alarm" -> PhoneActions.setAlarm(ctx, args.optInt("hour", 7), args.optInt("minute", 0), "Jarvis")
            "dial_number" -> PhoneActions.dial(ctx, args.optString("number"))
            "media_control" -> PhoneActions.media(ctx, args.optString("action"))
            "wait_seconds" -> { Thread.sleep((args.optDouble("seconds", 2.0).coerceIn(1.0, 6.0) * 1000).toLong()); "waited" }
            "open_app" -> PhoneActions.openApp(ctx, args.optString("name"))
            "open_url" -> PhoneActions.openUrl(ctx, args.optString("url"))
            else -> "unknown tool"
        }
    } catch (e: Exception) { AppLog.w(e); "error: ${e.message}" }
}
