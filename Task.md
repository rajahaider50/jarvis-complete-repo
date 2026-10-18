# TASK (current task; ONE at a time)
Status: done.

- **Task:** JARVIS BUGFIX GUIDE — 3 MASLE (evidence-first: logging → root cause report → fix → proof).
  - (Phase 1) Diagnostics: `Diag` logger (tag `JARVIS_DIAG`, timestamp + thread), Diagnostics screen
    (mic state/source/level, foreign recorder, websocket, underruns, interrupts, reconnects, tool calls,
    frames, cursor windows), **Cursor Test** button (centre + 4 corners), log copy/save. Chain logging at
    every link — DONE (run 39112262099).
  - (Masla 3) Call stalling / dropping — Root Cause Report + fixes (tools off the reader thread, video guard,
    VAD/interruption config + "Let me interrupt" toggle, urgent-priority audio threads + pre-buffer, wake/wifi
    locks, exponential backoff + network callback, thermal-aware sharing) — DONE (run 39113131430).
  - (Masla 1) Mic + recording — Root Cause Report + fixes (single-owner MicCaptureHub with fan-out + self-heal
    release/recreate + source fallback, accurate actionable warning, wake-mic logging, foreign-recorder detect) — DONE (run 39114125443).
  - (Masla 2) AI cursor not appearing — Root Cause Report + fix (main-thread marshalling of all overlay work; the
    AI tools ran on a worker thread so addView/Choreographer threw and the error was swallowed) — DONE (run 39114550181).
  - (Final) Soak test checklist + final report written (docs/REPORT_20261018_bugfix-final-summary.md). Phone soak test
    is user-side (Pending.md).
- **Files:** live/Diag.kt, ui/screens/DiagnosticsScreen.kt, MainActivity.kt, ui/screens/ProfileScreen.kt,
  live/{LiveClient,CursorController,ScreenShareService,JarvisAccessibilityService}.kt, docs/CHANGES_20261018_*.md.
- **Test:** `./gradlew assembleDebug` via the Build APK workflow after every phase (no local JDK/SDK here).
- **Status:** done.

Next: user phone soak test (Pending.md), then next task.
