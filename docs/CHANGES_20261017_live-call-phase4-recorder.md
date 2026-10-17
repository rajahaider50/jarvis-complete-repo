# Change · 2026-10-17 · Live Call Phase 4 — in-app screen recorder + mic fix

Part of the **JARVIS LIVE CALL UPDATE**. This is **Phase 4**.

## Goal
Record a live call inside the app: the screen (video) **and both voices** (the user's mic + the assistant's
voice) into one `.mp4` in `Movies/Jarvis`. Also warn the user when the microphone goes silent (usually
because another app / the system screen recorder is holding it).

## Changes
- `live/CallAudioTap.kt` (new) — a tap into the live call's raw PCM: `mic` (16 kHz) and `out` (24 kHz).
- `live/LiveClient.kt`
  - Feeds the mic PCM and the assistant PCM to `CallAudioTap`.
  - Detects **all-zero mic buffers for >= 1 second** and sets `CallEngine.micSilent`.
- `live/AudioMixer.kt` (new) — resamples both sides to 44.1 kHz mono and joins them into one stream for AAC.
- `live/ScreenRecordService.kt` (new) — foreground service (`mediaProjection`):
  - MediaProjection → VirtualDisplay → MediaCodec **H.264** (720p, 6 Mbps).
  - AAC-LC 128 kbps audio fed by `AudioMixer` (mic + assistant).
  - `MediaMuxer` writes `Movies/Jarvis/Jarvis_<time>.mp4` (MediaStore on Android 10+).
- `live/CallService.kt` / `CallHost.kt` — recorder is stopped when the call ends.
- `ui/screens/LiveVoiceScreen.kt`
  - New **record** button (red when active), a “Recording call” badge, and a **mic-silent warning**.
  - Bottom control row is horizontally scrollable (more buttons now).
- `AndroidManifest.xml` — `ScreenRecordService` (`mediaProjection`), `WRITE_EXTERNAL_STORAGE` (<= API 28).

## Notes
- Screen recording always uses the Android system dialog; the app cannot start it without the user’s tap.
- One microphone at a time: the recorder re-uses the call's mic (no second `AudioRecord`), so it does not
  fight the call for the mic.

## How to test
1. Start a call, tap the record button → allow the system dialog → red “Recording call” badge appears.
2. Talk (both sides) for ~20s, tap record again → check `Movies/Jarvis/Jarvis_*.mp4` plays video + both voices.
3. Play back: you hear yourself and Jarvis.
4. If the mic is held by another recorder, after ~1s the call shows the mic-silent warning.
