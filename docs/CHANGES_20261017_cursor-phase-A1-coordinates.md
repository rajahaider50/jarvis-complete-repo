# AI Cursor Phase A1: coordinate engine + normalized gestures + Cursor Lab

## What changed
- **`live/CursorGeometry.kt`** (new) — the normalized system: centre `(0,0)`, X right `+100` / left `-100`,
  Y up `+100` / down `-100`, clamp `-100..+100`, 2-decimal `fmt`. `toPx` / `toNorm` use the REAL current
  display size (`WindowManager.defaultDisplay.getRealSize`), so rotation/density are handled automatically.
- **`live/JarvisAccessibilityService.kt`** — new normalized gestures with callbacks + rate limit:
  `tapNorm`, `doubleTapNorm`, `longPressNorm`, `swipeNorm`, `clickAll` (one stroke per point, capped to
  `GestureDescription.getMaxStrokeCount()`), `allowClick()` (max `Prefs.cursorClickRate` per second). Fixed a
  broken line in the old `gesture()` helper. Gestures still trigger `ScreenShareService.burst()`.
- **`data/Prefs.kt`** — `aiCursor` (master switch, default off) and `cursorClickRate` (default 5) with load/save.
- **`ui/screens/CursorLabScreen.kt`** (new) — a grid (labels -100..+100) with 5 targets (4 corners + centre).
  Tapping the grid shows the normalized position; the button sends a REAL tap to the next target so you can see
  the tap lands where the target ring is (720x1612 @360dpi reference).
- **`MainActivity.kt` / `ProfileScreen.kt` / `AppControl.kt`** — new route `cursorlab` (Profile > Cursor Lab) and
  it is in the AI `navigate` list.

## How to test
1. Turn the accessibility service on, open Profile > Cursor Lab.
2. Tap inside the grid: the readout shows `X +.. / Y -..`; right/up are `+`, left/down are `-`, centre is `0.00`.
3. Tap "Real tap at Top-Left" etc.: the tap should land on the highlighted target ring (checks the mapping).
4. Rotate the phone: the readout screen size and targets follow the new orientation.

## Notes
- A1 only does taps (no visible cursor yet). Overlay cursors (multiple, drag, grid, NOT_TOUCHABLE trick) are A2.
