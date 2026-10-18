# CHANGES 2026-10-18 — Bugfix Masla 1: one mic owner + self-healing recording

Root Cause Report: `docs/REPORT_20261018_masla1_mic_recording.md`.

## Fixes
- **New `live/MicCaptureHub.kt`** — the single owner of the microphone for a live call:
  - fan-out: `addListener`/`removeListener`; Gemini sender + in-app recorder (`CallAudioTap`) both get the same PCM;
    level/peak + exact-zero detection computed inside.
  - source list `VOICE_COMMUNICATION → MIC → DEFAULT`, AEC + NoiseSuppressor on every open, thread priority
    `URGENT_AUDIO`.
  - **self-healing**: after ~1.5 s of continuous exact-zero audio it releases + re-creates the AudioRecord itself,
    tries the next source with backoff (3 attempts), probes ~600 ms and logs which source recovered.
  - `foreignRecorder()` (best effort, `getActiveRecordingConfigurations`) names the app holding the mic.
- **`live/LiveClient.kt`** — `startAudio()` now attaches `micListener` to `MicCaptureHub` (no AudioRecord of its own);
  `stopAudio()` removes it and stops the hub. Removed the local AudioRecord + zero-run logic.
- **`ui/screens/LiveVoiceScreen.kt`** — mic warning now appears only on real silence, names the holding app, and tells
  the user Jarvis is retrying by itself.
- **`ui/screens/DiagnosticsScreen.kt`** — Microphone card shows hub source / silent / zero-runs / recoveries and a
  "Check who holds the mic" button.
- **`live/WakeService.kt`** — logs every wake-listener mic open/close (Vosk, Porcupine, basic SpeechRecognizer) so
  "only one mic at a time" is provable.

## Files
`live/MicCaptureHub.kt` (new), `live/LiveClient.kt`, `live/WakeService.kt`,
`ui/screens/LiveVoiceScreen.kt`, `ui/screens/DiagnosticsScreen.kt`, docs.

## Verify
- Build APK workflow green.
- Phone: Jarvis recorder on → user's voice reaches the AI and BOTH voices are in `Movies/Jarvis`; mic never drops to
  silent on record on/off (RMS flat in Diagnostics); red warning only on real silence; no two AudioRecords at once.
- Logcat: `adb logcat -v time JARVIS_DIAG:V AudioRecord:V AudioFlinger:V *:S`.

## Not changed
Recorder video/audio pipeline (already the guide's design), UI style, FGS types.
