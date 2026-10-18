# AI Cursor Phase A2: on-screen cursors (multiple, drag, grid)

## What changed
- **`live/CursorController.kt`** (new) — draws the AI cursor(s) with `TYPE_ACCESSIBILITY_OVERLAY` (no extra
  permission). Each cursor is its own 56dp window so the rest of the screen stays touchable.
  - Up to 10 cursors, each with its own colour + number label.
  - Drag any cursor with your finger; position is clamped inside the screen (normalized -100..+100).
  - `add`, `move`, `remove`, `clear`, `list`, `positions`.
  - `gesture { }` makes all cursors `FLAG_NOT_TOUCHABLE` during an AI tap so the tap goes to the app underneath,
    then restores them (the "cursor must not swallow its own click" trick).
  - Grid overlay (`showGrid`) — lines every 10 units with -100/0/+100 labels.
  - `panic()` clears cursors + grid.
- **`live/JarvisAccessibilityService.kt`** — `onUnbind` now calls `CursorController.panic()` so cursors disappear
  when the service is turned off.
- **`ui/screens/CursorLabScreen.kt`** — buttons: "Cursor at target" (add/move a cursor), "Click with cursor"
  (NOT_TOUCHABLE + real tap), "Add cursor", "Clear cursors"; the Grid toggle now shows the overlay grid; the
  cursor list is shown live.

## How to test
1. Accessibility on, Profile > Cursor Lab. Turn Grid on -> a grid appears over the whole screen.
2. Tap "Add cursor" -> a numbered ring appears at the centre; drag it anywhere with your finger.
3. Tap "Cursor at target" -> the cursor jumps to the highlighted target; "Click with cursor" -> the real tap
   lands on it (the cursor does not block its own tap).
4. Add a second cursor -> different colour + number.

## Notes
- A3 will add `click_all` wiring + verify; A4 the manual panel + joystick; A5 the AI tools; A6 the memory.
