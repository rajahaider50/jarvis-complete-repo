# AI Cursor Phase A6: click-lesson memory (JSON + ui_clicks.md + journal)

## What changed
- **`live/ClickMemory.kt`** (new) — file-based click memory (no Room):
  - `memory/ui_clicks.json` (machine), `memory/ui_clicks.md` (human-readable, editable), `memory/ui_clicks.journal`
    (append-only event log for recovery).
  - A lesson = package, screen, target label, X/Y (normalized), recorded screen size, density, anchor, ok/fail
    counts, last-used time, source (`ai_success` / `user_correction` / `user_marked`), note.
  - `remember` / `correct` / `fail`, `recall(query, pkg)` (ranked by ok − fail + text match), `all`, `delete`,
    `clearAll`, `consolidate` (prune to 400 newest-useful + rewrite the readable file + clear the journal).
  - **Privacy:** refuses to learn on banking / payment / wallet / password packages or when a password field is
    open; never stores screen text. Session lessons kept in memory for "this call".
- **`live/AppControl.kt`** — new cursor tools `recall_clicks` and `save_click`. `cursor_click` now auto-learns a
  successful tap (when the cursor has a real target label) and records a failure when it was blocked; remembers
  the last AI target so a later correction knows the old spot.
- **`live/PhoneTools.kt`** — declarations for `recall_clicks` and `save_click`.
- **`live/CursorPanel.kt`** — "Save spot" button: if the AI had a target here it becomes a **correction**
  (drops the wrong lesson, saves the right one), otherwise a user-marked spot.
- **`live/LiveClient.kt`** — prompt: call `recall_clicks` before tapping; on "asal jagah yahan hai" move the
  cursor and `save_click(correct=true)`; never save from a banking/payment/password screen.
- **`live/CallHost.kt`** — `ClickMemory.consolidate()` when the call ends.
- **`MainActivity.kt`** — `ClickMemory.init(this)`.
- **`ui/screens/CursorMemoryScreen.kt`** (new) + route `cursormemory` + Profile link "Cursor memory (click
  lessons)" + added to the AI `navigate` list.

## How to test
1. Live call, AI cursor on. Ask it to click something a few times -> open Profile > Cursor memory: lessons appear
   with ok counts and X/Y.
2. If the AI taps the wrong spot, drag the cursor to the right spot and tap "Save spot" on the panel -> the wrong
   lesson is replaced (source `user_correction`).
3. Next call, ask again -> it calls `recall_clicks` and goes straight there.
4. Open a banking / password screen and tap -> nothing is saved.

## Notes
- This completes Guide A (A1-A6). Next: Guide B Phase 5-6 (Live `sessionResumption` + `contextWindowCompression`
  + goAway reconnect, UI toggles).
