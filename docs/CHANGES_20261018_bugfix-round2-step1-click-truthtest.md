# Bugfix Round 2 — Step 1: truthful click result + Tap Test

**Scope (only this, as asked):** section 2.1 (the click tool must report what really happened) and section 2.6
(an in-app Tap Test that measures the coordinate mapping with real gestures). Nothing else from round 2 is touched
yet — the interrupt loop, the mic source regression, the background-mic issue, the bad model and the extra counters
are still open on purpose, so the next dump can point at the real cause.

## Why this step first

The round-2 dump showed:

```
cursor_click {"id":11,"type":"tap"}  ->  "clicked cursor #11"   (188 ms)
```

The old code fired the gesture asynchronously, slept 150 ms, read a boolean and then said "clicked cursor #11"
whatever happened. There was no pixel, no `dispatchGesture` return, no `onCompleted`/`onCancelled`, no touchability
state, no hit-test and no check whether the screen reacted. So the tool *guessed*. That is the reason we cannot yet
say where the tap went.

## What was added (files changed)

### 1. `live/JarvisAccessibilityService.kt` — real gesture results
- `enum GestureOutcome { COMPLETED, CANCELLED, REJECTED, TIMEOUT, RATE_LIMITED, NO_SERVICE }`.
- `dispatchAwait(...)` — builds the gesture, calls `dispatchGesture(...)` with a `GestureResultCallback` on the main
  looper, keeps the `Boolean` the call returns, waits on a latch (default 2 s) and returns the real outcome.
  Blocking; must be called off the main thread.
- `tapPxAwait(px,py)`, `doubleTapPxAwait(px,py)`, `longPressPxAwait(px,py)` — screen-pixel gestures with a result.
- `gestureCapabilities()` — pure evidence: `CAPABILITY_CAN_PERFORM_GESTURES` flag, raw `caps` hex, real display size,
  `getMaxStrokeCount()`.
- `eventsNow()` + a counter incremented in `onAccessibilityEvent` — the "did the screen react" signal.
- `nodeAt(px,py)` — hit-test that ignores our overlay windows (`windows` filtered to `TYPE_APPLICATION`), returns
  package/class/text/viewId/clickable, the hit bounds and the nearest clickable ancestor.

### 2. `live/CursorController.kt` — truthful `clickResult(...)`
- `enum ClickOutcome { COMPLETED, CANCELLED, REJECTED, TIMEOUT, RATE_LIMITED, NO_SERVICE, NO_CURSOR }` and
  `data class ClickResult(id, normX, normY, px, py, outcome, touchableBefore, cursorScreen, underCursor, effect)`.
- `clickResult(id, type, waitForEffectMs)`:
  - moves the cursor to its own point, reads whether the cursor window was touchable **at that moment**,
  - captures the cursor window's real `getLocationOnScreen()` vs the expected pixel (`windowLocOf`),
  - makes the cursors not touchable, dispatches the awaited gesture, restores them,
  - hit-tests the element under the point (`under_cursor`),
  - waits up to 800 ms for accessibility events and reports `effect: changed / no_change`,
  - logs one `JARVIS_DIAG` line with every field and bumps the gesture counters.
- `windowReport()` — used by the Diagnostics screen: every cursor window's **real** on-screen position vs the
  expected pixel (this is the "overlay vs `dispatchGesture`" calibration evidence).

### 3. `live/AppControl.kt` — `cursor_click` no longer lies
The tool result is now e.g.
```
outcome=completed | cursor #11 norm=-69.20,+25.80 px=(110,586) | cursorTouchableAtClick=true |
window[gotTopLeft=(54,530) gotCentre=(110,586) expectedCentre=(110,586) ...] |
under_cursor: pkg=com.jarvis.app class=... text='...' clickable=true hit=[...] clickTarget=[...] | effect=changed
```
Memory is only written when `outcome=completed`.

### 4. `live/PhoneTools.kt` — the AI is told to trust the result
`cursor_click` description now says the result is the truth (outcome / under_cursor / effect) and that the AI must
retry instead of claiming a click. `target` is now declared.

### 5. `live/Diag.kt` — gesture counters
`gestures`, `gesturesCompleted`, `gesturesFailed`, `lastGesture`, written by `gestureOutcome(...)` and printed in
`dump()`.

### 6. `ui/screens/TapTestScreen.kt` (new) + `DiagnosticsScreen` + `MainActivity`
- Tap Test: 5 targets (centre + 4 corners at ±65). For each target it places a real cursor on the target centre and
  fires a **real** gesture, then compares the pixel the tap was sent to with the raw pixel that actually received it
  (`MotionEvent.rawX/rawY` from a plain `View`). Table: sent px · received px · diff px · outcome · touchable ·
  effect · cursor-window position · what is under the point.
- If the cursor window swallowed the tap (`received == null`), it re-measures once with a clear screen and marks the
  row "cursor swallowed the tap", so that becomes provable evidence instead of a guess.
- The screen auto-runs once on entry; "Run tap test" re-runs it.
- Diagnostics has a new "Pointer & gestures (Masla A)" card: gestures ok/bad, last outcome, capabilities, cursor
  window report, and the Tap Test button.
- Route `taptest` added; `DiagnosticsScreen(onBack, onNav)`.

## How to verify (device)

1. Open Profile → **Diagnostics (mic · call · cursor)** → **Tap Test**. Read the table.
2. Then in a live call ask JARVIS to click something and read the `JARVIS_DIAG` line:
   `adb logcat -v time JARVIS_DIAG:V *:S`

Expected in the next dump:
- Tap Test rows: `sent(px)` vs `recv(px)` with `diff` in pixels (and whether the cursor swallowed the tap).
- `gesture: click ... outcome=... touchableAtClick=... effect=... window[...] under[...]`.
- `cursor_click` tool result containing `outcome=`, `under_cursor:` and `effect=`.

## Not touched in this step (deliberately)
§2.2 (always `FLAG_NOT_TOUCHABLE` / move mode), §2.3/2.4 are only *measured* here, §2.5 (element tools),
§3 (mic source regression + echo), §4 (background mic / foreground service), §5 (bad model 1007), §6 (frame-drop
reason counters). These come after the next dump.
