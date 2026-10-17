# Change · 2026-10-17 · Live Call Phase 1 — Lag fix (first frame + fast setup)

Part of the **JARVIS LIVE CALL UPDATE**. This is **Phase 1** only.

## Goal
Opening the Live Call screen (or tapping the mic) must show the screen instantly and never hang
for 1–3 seconds. All heavy work must happen after the first frame.

## What was wrong (audit)
- `LiveVoiceScreen` waited a fixed `delay(150)` and then ran `CallHost.begin()` on the main thread.
- `CallHost.begin()` did the heavy setup synchronously (`AudioRouter.start`, `CallService.start`,
  `Overlay.hideAll`, `WakeService.setPaused`) before returning.
- `LiveClient` posted a UI level update on **every** mic/playback buffer (needless recomposition).
- The OkHttp client for the Live WebSocket was cold on the first call.

## Changes
- `ui/screens/LiveVoiceScreen.kt`
  - Removed the fixed `delay(150)`; uses `withFrameNanos { }` so the screen draws one frame first,
    then the call starts. Added `call_screen_first_frame` timing log.
  - `ThemeRegistry.byId(...)` is now `remember(...)`d (no re-lookup on each recomposition).
- `live/CallHost.kt`
  - `begin()` now sets `client` and returns immediately; the heavy setup
    (`Overlay`, `WakeService`, `AudioRouter`, `CallService`) runs in a coroutine **after** the return,
    so the first frame is not blocked. Added `call_setup_begin` / `call_setup_services` timing logs.
- `live/LiveClient.kt`
  - Mic and playback level UI updates are throttled to ~20/s (`>=50ms`), cutting recompositions.
  - Added `live_connect_start` and `live_connected <ms>` timing logs.
  - Added `LiveClient.prewarm()` (touches the shared OkHttp client).
- `MainActivity.kt`
  - Calls `LiveClient.prewarm()` at start-up so DNS/TLS/OkHttp warm-up is paid before the first call.

## Targets
- Call screen first frame: **immediate** (no fixed delay, no main-thread blocking).
- Connect time: mostly network; measured by `live_connected <ms>` in the log.

## How to test
1. Open Live Call → screen must appear at once (no frozen frame); orb/“Connecting…” shows immediately.
2. Logcat filter `Timing` → `call_screen_first_frame`, `live_connect_start`, `live_connected <ms>`.
3. Talk normally; move mic — orb should stay smooth.
