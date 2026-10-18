# CHANGES 2026-10-18 — Bugfix Masla 2: AI cursor main-thread fix

Root Cause Report: `docs/REPORT_20261018_masla2_cursor.md`.

## Root cause
AI cursor tools ran on a worker thread (OkHttp reader, now the `live-tools` thread), but `WindowManager.addView` /
`updateViewLayout` and `Choreographer` require the **main** thread. The exception was swallowed (`AppLog.w`) and the
tool returned `-1`, so no cursor ever appeared — while the Cursor Lab / panel (main thread) worked.

## Fixes
- **`live/CursorController.kt`** — `onMain(fallback)` (blocking, 900 ms) and `postMain` helpers; all overlay work
  (`add`, `move`, `moveAnimated`, `remove`, `clear`, `showGrid`, `setTouchable`, `panic`) is marshalled to the main
  thread; `ensure()`/`place()` therefore run on main; every failure is logged (never swallowed). Failure/timeout
  reasons are recorded in `Diag`.
- **`live/CursorController.kt`** — `snapshotForRestore()` / `restoreIfAny()` so cursors survive an accessibility
  service restart (MIUI).
- **`live/CursorPanel.kt`** — `refresh()` marshalled to main.
- **`live/JarvisAccessibilityService.kt`** — `onServiceConnected` logs + restores cursors; `onUnbind` logs + snapshots
  + clears.
- **`live/AppControl.kt`** — unknown tool names logged (`Diag`) and returned as an error; clearer, actionable errors
  when AI cursor / accessibility is off; `cursor_create` failure now explains why.

## Files
`live/CursorController.kt`, `live/CursorPanel.kt`, `live/JarvisAccessibilityService.kt`, `live/AppControl.kt`, docs.

## Verify
- Build APK workflow green.
- Phone: Diagnostics > Cursor Test → 5 positions; by voice "screen ke center me cursor lagao" → cursor appears and
  the AI gets `cursor id=..`; 3 cursors add/move/remove; service off → clean error (no crash).
- Logcat shows the whole chain with `thread=main` for the overlay add.

## Not changed
Cursor geometry, colours, gestures, click memory, panel layout/UI style.
