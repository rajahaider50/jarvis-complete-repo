# Change · 2026-10-17 · Live Call Phase 3 — call keeps running while you use the app

Part of the **JARVIS LIVE CALL UPDATE**. This is **Phase 3**.

## Goal
While a call is running you can leave the call screen and use any other screen of the app —
the call keeps going, and a small bar shows it and lets you return or end it.

## Changes
- `ui/screens/LiveVoiceScreen.kt`
  - Top-left button is now a **down arrow** = minimise (old X ended the call). The bottom red button still ends.
  - **Back** now minimises too (it no longer hangs up).
  - Camera toggle informs the foreground service (`CallService.setCamera`).
- `MainActivity.kt`
  - The `NavHost` now sits in a `Box`; on every screen except the call itself a **MiniCallBar** is shown
    while a call is active. Tapping it returns to the call; the red icon ends it.
- `ui/components/MiniCallBar.kt` (new)
  - Small pill: theme orb + “Jarvis · live call”, status label (Connecting / Listening / Speaking / Muted),
    tap to open, red end button.
- `live/CallService.kt`
  - Foreground service now manages its **types** dynamically: `microphone` always, `camera` when the
    call uses the camera (`setCamera`). `running` flag added.
- `AndroidManifest.xml`
  - Added `FOREGROUND_SERVICE_CAMERA` permission; `CallService` type is now `microphone|camera`.

## Notes
- The call object (`CallHost`) and the microphone are already owned by the app (`CallService`), so the
  call survives screen changes and leaving the app (floating bubble).
- One microphone at a time still holds: the wake service is paused for the call.

## How to test
1. Start a call, tap the down arrow → you are on Home with a “live call” bar; talk — it still works.
2. Move to Chat/Control/Profile → bar stays; tap it → back to the call.
3. Turn the camera on, minimise → notification/service keeps working; camera turns off (screen disposed).
4. Press Back on the call screen → it minimises (does not hang up). Bottom red button ends the call.
