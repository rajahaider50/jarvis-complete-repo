# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** JARVIS BUGFIX GUIDE — 3 MASLE (evidence-first: logging → root cause report → fix → proof).
  - (Phase 1) Diagnostics: `Diag` logger (tag `JARVIS_DIAG`, timestamp + thread), hidden Diagnostics screen
    (mic state/source/level, foreign recorder, websocket, underruns, interrupts, reconnects, tool calls,
    frames, cursor windows), **Cursor Test** button (centre + 4 corners), log copy/save. Chain logging at
    every link: mic open/release/zero, ws connect/live/close/reconnect, interrupted, underruns, toolCalls,
    frame sent/dropped, cursor add/remove/update + a11y connect/disconnect — IN PROGRESS (first build).
  - (Masla 3) Call stalling / dropping — Root Cause Report + fixes (tools off the reader thread, video guard,
    VAD/interruption config + "Let me interrupt" toggle, urgent-priority audio threads + pre-buffer, wake/wifi
    locks, exponential backoff + network callback, thermal-aware sharing) — IN PROGRESS (build).
  - (Masla 1) Mic + recording (MicCaptureHub, in-app recorder mix, accurate warning + auto-recover). NOT STARTED.
  - (Masla 2) AI cursor not appearing — chain diagnosis, Cursor Test, fix. NOT STARTED.
  - (Final) Soak test + checklist + final report. NOT STARTED.
- **Files:** live/Diag.kt, ui/screens/DiagnosticsScreen.kt, MainActivity.kt, ui/screens/ProfileScreen.kt,
  live/{LiveClient,CursorController,ScreenShareService,JarvisAccessibilityService}.kt, docs/CHANGES_20261018_*.md.
- **Test:** `./gradlew assembleDebug` via the Build APK workflow after every phase (no local JDK/SDK here).
- **Status:** in progress.

Next: build Phase 1, then Masla 3 measurements + Root Cause Report.
