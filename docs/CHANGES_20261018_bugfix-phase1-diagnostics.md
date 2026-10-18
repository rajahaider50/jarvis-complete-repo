# CHANGES 2026-10-18 — Bugfix Phase 1: Diagnostics (logger + screen + Cursor Test)

Guide: JARVIS BUGFIX GUIDE — 3 masle. Phase 1 builds the evidence layer before any fix.

## What changed

### New: `live/Diag.kt`
An evidence logger, separate from the error console (`AppLog`). Every line goes to logcat with tag
`JARVIS_DIAG` (with timestamp + thread name, via the `HH:mm:ss.SSS` stamp) AND into a 400-item in-memory ring
the Diagnostics screen shows. Counters kept as Compose state:
- mic: `micState`, `micSource`, `micLevel` (peak 0..1), `micZeroRuns`, `micRecoveries`, `micOpens`, `micReleases`,
  `foreignRecorder`.
- connection: `wsState`, `underruns`, `interrupted`, `reconnects`, `lastClose`.
- tools/frames/cursor: `toolCalls`, `lastTool`, `framesSent`, `framesDropped`, `cursorAdds`, `cursorAddFails`.
`dump()` returns the whole snapshot + events (for clipboard / file).

### New: `ui/screens/DiagnosticsScreen.kt`
Live status cards (Microphone · Connection · Tools & frames · Cursor & accessibility) + **Cursor Test** button
(clears, then draws a cursor at the centre and each of the 4 corners ~700 ms apart, then clears) + Copy logs /
Save logs (`filesDir/diag/diag_<ts>.txt`) / Clear log + the recent-events list.

### Route + entry
- `MainActivity.kt`: `composable("diagnostics") { DiagnosticsScreen(back) }`.
- `ProfileScreen.kt`: row `Icons.Outlined.BugReport "Diagnostics (mic · call · cursor)"`.

### Chain logging (evidence for masla 1/2/3)
- `LiveClient.kt`: AudioRecord open (source) / release, capture-thread start/end, exact-zero buffer runs
  (`Diag.micZero()`) and return-to-audio, `buildSetup` line (tools present? cursor tools? resumption? compression?),
  `setupComplete`, `sessionResumptionUpdate`, `goAway`, `interrupted` (+ thread), `toolCall`/`toolResponse`
  with the **thread name** and how long the reader thread was blocked, reconnect count, close code/reason,
  playback start + `AudioTrack.getUnderrunCount()` sampling (1 s) with queue size.
- `CursorController.kt`: `add` (thread, accessibility present, success/fail reason, window count), `remove`,
  `updateViewLayout` failures, grid shown.
- `JarvisAccessibilityService.kt`: `onServiceConnected` / `onUnbind` (MIUI tends to kill the service).
- `ScreenShareService.kt`: frames sent / dropped counters.

## Why
The guide requires evidence before fixing. These logs make the whole chain visible in one `adb logcat`
and on-screen: setup tools → toolCall → dispatch → overlay add → toolResponse for the cursor;
mic open/zero/release + foreign recorder for audio; underruns/interrupted/reconnects for the call.

## Verify
- Build APK workflow green (Phase 1).
- Phone: Profile > Diagnostics shows moving counters; Cursor Test draws 5 positions;
  `adb logcat -v time JARVIS_DIAG:V *:S` captures the full trace.

## Not changed
Existing UI style/colours/fonts untouched (new screen reuses `ScreenScaffold`/`SubHeader`/`JText`/`GoldButton`/
`OutlineButton`). No existing behaviour changed.
