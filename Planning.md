# PLANNING (active task only)
Task: JARVIS BUGFIX GUIDE — 3 masle (evidence-first, in the user's order).

## Order
1. Diagnostics (logger + Diag screen + Cursor Test). Build clean.
2. Masla 3 (call stalling) measurements → Root Cause Report → fixes.
3. Masla 1 (mic + recording): MicCaptureHub, in-app recorder mix, warning + auto-recover.
4. Masla 2 (cursor): chain log, Cursor Test, fix.
5. Soak test + checklist + final report.

## Phase 1 — Diagnostics (in progress)
- `live/Diag.kt`: logcat tag `JARVIS_DIAG` (timestamp + thread) + in-memory ring + counters
  (mic open/release/zero/recovery, ws state/underruns/interrupted/reconnects/lastClose,
   toolCalls/lastTool, framesSent/Dropped, cursorAdds/Fails, foreignRecorder).
- `ui/screens/DiagnosticsScreen.kt`: live status cards + Cursor Test (centre then 4 corners, then clear) +
  Copy / Save-to-file / Clear logs + recent events. Route `diagnostics` + Profile row.
- Chain logging added at: LiveClient (mic open/release/zero, setup tools, setupComplete, sessionResumptionUpdate,
  goAway, interrupted, toolCall/toolResponse + thread name, reconnect, close code, playback underruns),
  CursorController (add/remove/update/grid + thread + failure reason), JarvisAccessibilityService
  (onServiceConnected/onUnbind), ScreenShareService (frames sent/dropped).

## Phase 2 — Masla 3 (in progress)
Root Cause Report: docs/REPORT_20261018_masla3_call_stalling.md. Fixes F3-1..F3-7 in
docs/CHANGES_20261018_bugfix-masla3-call-stability.md. Next: build, then Masla 1.

## Notes
- No local JDK/Android SDK → verify with the Build APK workflow after each phase.
- Evidence that needs the phone (logcat) will be written as "confirm nahi hua" + the exact command.
