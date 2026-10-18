# AI Cursor Phase A3-A4: cursor clicks + manual panel + joystick + PANIC

## What changed
- **`live/CursorController.kt`** — gesture helpers: `click(id, TAP/DOUBLE/LONG)`, `swipe(id, toX, toY, ms)`,
  `clickAll()`, `clickAt(x,y,type)`; all run with cursors made NOT_TOUCHABLE and restore them after (via
  `gesture { }`). `selectedId` tracks the cursor the panel/joystick controls.
- **`live/CursorPanel.kt`** (new) — the floating manual control panel (plain Views, accessibility overlay):
  cursor list (◀ / + / del / ▶), live `X +..  Y ..` readout (2 decimals), nudge pad with a ±0.1/±0.5/±1 step
  button, Tap / Dbl / Long / All, Grid + Lock toggles, a **PANIC** button (clears cursors + grid and hides the
  panel), a speed slider **1-15**, and a **joystick** pad (deflection = speed, quadratic, releases to centre).
- **`ui/screens/CursorLabScreen.kt`** — "Show control panel" and "Panic / stop" buttons.

## How to test
1. Accessibility on, Profile > Cursor Lab > "Show control panel": the panel appears (drag it by its title bar).
2. + cursor, then nudge with ▲▼◀▶ and the step button; the readout shows 2-decimal X/Y.
3. Tap / Dbl / Long / All send real taps wherever the cursors are; Grid shows the overlay grid; PANIC stops all.
4. Joystick: open it, drag the stick — the cursor speeds up with deflection (speed 1-15); release to stop.

## Notes
- A5 (AI tools) and A6 (click memory) are next; the `aiCursor` master switch will gate the AI tools only.
