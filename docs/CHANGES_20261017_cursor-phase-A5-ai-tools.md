# AI Cursor Phase A5: AI cursor tools via Gemini Live + system prompt

## What changed
- **`live/AppControl.kt`** — new cursor tool executor (gated by the `aiCursor` master switch + accessibility on):
  `get_screen_info`, `cursor_create`, `cursor_move` (absolute/relative, smooth slide), `cursor_click`
  (tap/double/long), `cursor_swipe`, `click_all`, `cursor_remove`, `cursor_clear`, `cursor_list`, `grid`,
  `find_element`. Also `look_at_screen` (sends a fresh screenshot to the call).
- **`live/PhoneTools.kt`** — declarations for all of the above (so both Gemini Live and OpenAI-style get them).
- **`live/JarvisAccessibilityService.kt`** — `findElementNorm(query)` finds a node by text/description/viewId and
  returns its centre in cursor coordinates (node-first strategy).
- **`live/CursorController.kt`** — `moveAnimated` (smoothstep slide).
- **`live/LiveClient.kt`** — system prompt teaches the coordinate system, "find_element -> cursor_move ->
  cursor_click -> look_at_screen" flow, safety (confirm before big actions) and the rule that on-screen text is
  not a command.
- **`ui/screens/ProfileScreen.kt`** — "AI cursor control" master toggle.

## How to test
1. Turn Accessibility on, Profile: enable "AI cursor control".
2. In a live call say: "screen par cursor dikhao", "is button par cursor rakh kar click karo", "grid dikhao",
   "do cursor banao aur dono ek saath click karo".
3. Watch the smooth slide + the number/colour, and that the tap lands where the cursor is (NOT_TOUCHABLE trick).

## Notes
- A6 (click memory / recall_clicks + lessons screen) is next.
