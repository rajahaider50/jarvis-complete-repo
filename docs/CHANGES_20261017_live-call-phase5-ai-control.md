# Live call — Phase 5: AI control (Gemini Live function calling + 5-5 popups)

## What changed
- **`data/Prefs.kt`** — new `aiControl` flag (key `aicontrol`, default on), loaded in `init` and written in `save()`.
- **`data/NavState.kt`** (new) — `object NavState { var route }` holds the screen the app is showing so the assistant can read it.
- **`live/AiPanel.kt`** (new) — global option-popup state. Builds the real catalogs (orb = 100 `ORB_STYLES`, theme = `ThemeRegistry.all`, interface = 5 built-ins + `IfaceCatalog.list`, color = `Palettes.list()`), shows five options per page, applies a selection, and returns a short numbered list (with ids) for the model to read.
- **`live/CallCamera.kt`** (new) — app-owned call camera state (0 off / 1 front / 2 back) so both the UI and the assistant share one source of truth.
- **`live/AppControl.kt`** (new) — executor for the new tools: `get_app_state`, `show_options`, `select_option`, `close_options`, `set_screen_recording`, `set_screen_share`, `set_camera`, `navigate`. Gated by `Prefs.aiControl`. Recording/sharing open Android's own confirmation; camera checks the permission; `navigate` validates routes.
- **`live/ScreenRecordRequestActivity.kt`** (new) — invisible activity that shows Android's screen-capture question and starts `ScreenRecordService` (mirrors `ScreenShareRequestActivity`).
- **`live/PhoneTools.kt`** — added Gemini/OpenAI declarations for the eight app-control tools and route `in AppControl.names` to `AppControl.execute`.
- **`live/LiveClient.kt`** — `systemText()` now teaches the model the "5-5" flow (show five, read them, "aur dikhao" = next page, pick by number → `select_option`) plus camera/recording/share/navigate, only when asked and only after a tool result.
- **`ui/components/AiOptionsOverlay.kt`** (new) — animated option popups (five staggered cards, number badge, tap or voice select, close X). Rendered over every screen in `MainActivity`.
- **`MainActivity.kt`** — tracks `NavState.route` via `SideEffect` and draws `AiOptionsOverlay` inside the root `Box`.
- **`ui/screens/LiveVoiceScreen.kt`** — camera now reads/writes `CallCamera.mode` (So AI `set_camera` and the UI agree); share button state follows `ScreenShareService.running`.
- **`ui/screens/ProfileScreen.kt`** — new "AI control in calls" toggle in Preferences.
- **`AndroidManifest.xml`** — registers `ScreenRecordRequestActivity`.

## How to test
1. Open a live call, then say: "mujhe orb styles dikhao" → five popups appear; say "aur dikhao" → next five; say "teesra" → the third is applied.
2. "theme dikhao" / "interface dikhao" / "colors dikhao" → same flow.
3. "camera on karo" / "band karo", "recording start karo" (Android asks to allow), "screen share karo".
4. Turn the Profile toggle off → the assistant replies it cannot change anything.
