# PLANNING (active task only)
Task: AI Cursor system + live screen stream/share/record (interleaved, per the two guides).

## Order
1. Guide B P1-2: one MediaProjection session -> recording (ScreenRecordService). onStop handling, state.
2. Guide B P3: FrameSink via AccessibilityService.takeScreenshot (API 30+), no second projection.
3. Guide B P4: frame policy — change detection (downscale gray diff), base ~1fps, slow to ~0.3fps when idle,
   action burst after clicks, latest-wins, privacy pause (isPassword / sensitive app).
4. Guide B wiring: LiveVoiceScreen share button starts share directly (no consent dialog); AppControl set_screen_share.
5. Guide A P1: coordinate engine toPx/toNorm (center=0, right/up +) + Cursor Lab screen (5 targets).
6. Guide A P2-3: cursor overlay (TYPE_ACCESSIBILITY_OVERLAY, per-cursor window, FLAG_NOT_TOUCHABLE during gesture),
   gestures tap/double/long/swipe/click_all with callbacks + rate limit.
7. Guide A P4: manual panel + joystick (speed 1-15) + panic + master switch.
8. Guide A P5: AI cursor tools + system prompt rules + node-first click strategy.
9. Guide A P6: click-lesson memory (JSON + ui_clicks.md), journal, session lessons, recall_clicks, privacy screen.
10. Guide B P5-6: Live session resumption + context compression + goAway reconnect; UI toggles; AI tools.

## Key facts
- Only ONE MediaProjection (recording). Share uses accessibility takeScreenshot -> no conflict.
- Accessibility config needs canTakeScreenshot="true" (API 30+).
- Mic 16k (CallAudioTap.mic), assistant out 24k (CallAudioTap.out) -> AudioMixer 44.1k (already exists).
- Memory is file-based (no Room): lessons JSON + ui_clicks.md under filesDir/memory.
- Coordinates normalized -100..+100, 2 decimals, center (0,0), up/right positive; clamp inside screen.

## Risk / check
- Old screens/UI unchanged; new code additive; keep builds green after each push.
