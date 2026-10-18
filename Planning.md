# PLANNING (active task only)
Task: DONE — AI Cursor system + live screen stream/share/record (interleaved, per the two guides).
See Complete.md (2026-10-18) and docs/HANDOFF_STATE.md for the phase runs.

## Result (all phases green)
1. Guide B P1-4: ONE MediaProjection session -> recording (ScreenRecordService); share via AccessibilityService
   takeScreenshot (no second projection); frame policy (gray change detection, base 1fps / 0.3fps idle / action
   burst / latest-wins / privacy pause). Build run 39055393526.
2. Guide A P1: CursorGeometry (center=0, right/up +, 2 decimals, real display size) + Cursor Lab (grid, 5 targets,
   real tap test). Run 39107227663.
3. Guide A P2-3: cursor overlay (TYPE_ACCESSIBILITY_OVERLAY, per-cursor window 56dp, FLAG_NOT_TOUCHABLE during a
   gesture, up to 10, colours/numbers, drag) + gestures tap/double/long/swipe/click_all with callbacks + rate
   limit. Run 39107861727.
4. Guide A P4: manual panel + joystick (speed 1-15) + nudge + Save spot + grid/lock + PANIC. Run 39108238142.
5. Guide A P5: AI cursor tools + system prompt rules + node-first strategy (find_element -> cursor_move ->
   cursor_click -> look_at_screen). Run 39108619174.
6. Guide A P6: click-lesson memory (JSON + ui_clicks.md + journal), session lessons, recall_clicks / save_click,
   user corrections, privacy blocklist, Cursor memory screen. Run 39109634068.
7. Guide B P5-6: Live session resumption + context compression + goAway reconnect; Profile toggles. Run 39110018456.

## Key facts (implemented)
- Only ONE MediaProjection (recording). Share uses accessibility takeScreenshot -> no conflict.
- Accessibility config has canTakeScreenshot="true" (API 30+).
- Mic 16k (CallAudioTap.mic), assistant out 24k (CallAudioTap.out) -> AudioMixer 44.1k.
- Memory is file-based (no Room): ui_clicks.json + ui_clicks.md + ui_clicks.journal under filesDir/memory.
- Coordinates normalized -100..+100, 2 decimals, center (0,0), up/right positive; clamped inside the screen.
- Live resumption handle gated by Prefs.liveResume; cleared on a real hang-up.

## Next (not started)
- Phone testing of all of the above (see Pending.md).
- Optional: AI "show the panel", cursor size slider, per-app lesson screen scoping.
