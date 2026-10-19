# Bugfix — call recording: BOTH voices in the saved video, and phone recorders can hear JARVIS

Request (user, 2026-10-19), two parts:

1. **In-app call recording** ("Live Call ke dauran recording call ka option … is video recording mein meri aur
   Jarvis ki aane-jaane wali awaaz record nahi hoti"): the video saved by JARVIS's own Record button had **no
   voices**.
2. **Android's own screen recorder** ("jab main apne khud mobile se manual screen recording on karun"): the user's
   voice must still reach JARVIS **and** the system screen recording must contain **both** voices.

Everything below is code that was read first, then changed. Every change is logged so the next dump can prove it.

---

## Root causes (evidence read from the code, not guessed)

### A. JARVIS's own recorder could be completely silent

1. **The provider-pipeline call engine never fed the recorder.**
   `CallAudioTap.mic` / `.out` were only ever written by `LiveClient` (Gemini Live). A call made with a
   **custom provider** (`PipelineClient`) uses its own `AudioRecord` in `listen()` and `PcmPlayer` for the voice,
   and neither touched the tap. Result: with a provider call the .mp4 had a video track and a *silent* audio track.
   Proven by `grep -rn "CallAudioTap"` → only `LiveClient.kt` (2 lines) and `ScreenRecordService.kt`.
2. **The tap hard-coded the sample rates.** `CallAudioTap.mic = { buf, len -> mixer.add(buf, len, 16000) }` and
   `out … 24000` meant any engine with a different rate would be resampled wrongly.
3. **The audio timeline only advanced while samples existed.** `feedAudio()` incremented the AAC presentation time
   only for frames the mixer returned. Every pause between turns therefore shifted the voices later against the
   video (progressive A/V desync — the bigger the call, the more the voices lag). Worse, if **nothing** ever reached
   the mixer, the audio encoder never emitted `INFO_OUTPUT_FORMAT_CHANGED`, `maybeStart()` never ran, and the muxer
   never started at all.
4. **The video track was stamped with the device uptime.** A surface-fed `MediaCodec` gets its presentation times
   from the producer (SurfaceFlinger = `SYSTEM_TIME_MONOTONIC` in ns). The audio started at `0`. In the muxed file
   the audio therefore sat "days before" the video — players that trust the video's first timestamp show the picture
   and drop/ignore the audio.

### B. Android's own screen recorder could never record JARVIS's voice

`LiveClient` played the assistant with `AudioAttributes.USAGE_VOICE_COMMUNICATION`. Android's **playback capture**
(what the built-in screen recorder uses for the "Media sounds" option) can only capture media-like usages;
voice-call audio is excluded by platform policy. JARVIS's voice was therefore invisible to the system recorder no
matter what the user selected.

Related side effects that had to be handled with it: the stream the voice plays on changes (media, not voice call),
so the "Call volume" setting and the echo guard's decision had to follow.

---

## Fixes

### 1. One tap, every engine, with the real sample rate — `live/CallAudioTap.kt`

- `mic` / `out` are now `(buf, len, rate) -> Unit`; callers pass their true rate.
- New single entry points `putMic` / `putOut` / `putMicShorts` / `putOutShorts` (+ `resetCounters()`).
- **Byte counters** `micBytes` / `outBytes` are the proof that each side really reached the recorder.
- Every tap failure is logged through `Diag.fail` instead of being swallowed.

Fed by:
- `live/LiveClient.kt` — mic 16 kHz, assistant 24 kHz (Gemini Live).
- `live/PipelineClient.kt` — microphone frames of the provider pipeline (16 kHz, only while not muted).
- `audio/PcmPlayer.kt` — the provider/TTS voice playback, per written chunk at its own rate.

### 2. The recorder always produces a complete, in-sync file — `live/ScreenRecordService.kt`

- Taps installed rate-aware; `Prefs.recBothVoices` decides whether JARVIS's side is mixed.
- `feedAudio()` now keeps the audio track on **real time**: it computes how many 44.1 kHz samples are due by the
  wall clock and pads the frame with silence when the mixer is empty (with a resync if the thread is ever more than
  3 s behind). The voices can no longer drift away from the picture, and the audio encoder always produces output,
  so the muxer always starts.
- **Video PTS is normalised**: the first video sample's timestamp is remembered and subtracted, and it is logged
  (`rec: video first pts=… -> normalised to 0`). Both tracks now start at zero.
- Evidence logs: `rec: started …`, `rec: video first pts=…`, `rec: audio format …`, `rec: audio first pts=…`,
  `rec: mic=…B out=…B mixed=…B muxStarted=…` every 2 s, a one-time `WARNING: YOUR voice has not reached the
  recorder` / `WARNING: JARVIS's voice has not reached the recorder`, and a closing `rec: stopping …` +
  `rec: saved …`.
- `live/AudioMixer.kt`: `drain(dst, waitMs)` (so the feeder never blocks for 200 ms) + `samplesOut` counter.

### 3. Phone recorders can hear JARVIS — `live/LiveClient.kt`, `data/Prefs.kt`, `ui/screens/ProfileScreen.kt`

- New setting **"Phone recorders can hear JARVIS"** (`Prefs.shareToRecorders`, default **on**). When on, the live
  playback uses `USAGE_MEDIA` (+ `CONTENT_TYPE_SPEECH`) so Android's playback capture — the system screen recorder's
  "Media sounds" — records JARVIS's voice. When off it is the old voice-call behaviour (better earpiece/BT-call
  routing, but Android hides it from recorders). The chosen usage is logged on every call start.
- `audio/AudioRouter.kt`: because the voice can now play on the media stream, `applyVolume()` applies the user's
  "Call volume" percentage to `STREAM_MUSIC` as well, and the original media volume is saved/restored around the call
  so the phone is left exactly as it was.
- `live/LiveClient.kt`: the half-duplex echo guard follows the routing. With "Phone recorders can hear JARVIS" the
  platform echo canceller (attached to the voice-call mic session) does not use the media stream as its reference, so
  the **auto** guard now holds the mic back while the assistant speaks on a speaker (a clearly loud voice still gets
  through, so barge-in works). The `interrupted` evidence line now also prints `mediaPlayback=…`.

### 4. The user's voice keeps reaching JARVIS

Unchanged by design and already in place from Round 3 Step D: JARVIS keeps a `microphone` foreground service and the
call screen in front, `MicCaptureHub` detects a foreign recorder (`mic: ZERO buffers -> cause: …` +
`Diag.foreignRecorder`), and it takes the mic back by itself once the other recorder stops. The Profile note added
with this change tells the user to select **"Media sounds"** in Android's own recorder dialog.

### 5. Evidence in the app — `live/Diag.kt`, `ui/screens/DiagnosticsScreen.kt`

- New `Diag.recMicBytes` / `recOutBytes` / `recSamples` and a dump line
  `rec mic=…B out=…B mixed=…B active=…`.
- New Diagnostics card **"Call recording"**: recorder active, "YOUR voice fed in" (0B is shown in gold and says
  *NOT reaching the recorder*), "JARVIS's voice fed in", bytes mixed into the video, whether phone recorders hear
  JARVIS, and the last file name.

---

## How to prove it on the phone

1. Start a live call (Gemini Live **or** a custom provider), tap Record, talk for ~30 s, let JARVIS answer, stop.
   Open Movies/Jarvis: **both** voices must be audible and lip-synced.
2. During that recording, open Profile → Diagnostics → **Call recording**: `YOUR voice fed in` and
   `JARVIS's voice fed in` must both be non-zero; if one is 0B, the on-screen/dump WARNING names that side.
3. `adb logcat -v time JARVIS_DIAG:V *:S` should show `rec: video first pts=… -> normalised to 0`,
   `rec: audio first pts=…`, and the 2 s `rec: mic=…B out=…B mixed=…B` line.
4. Android's own recorder: start it with **"Media sounds" ON** (Microphone optional). Both voices must be in the
   system recording. If the recorder also takes the microphone, JARVIS shows the red "microphone is silent" warning
   and takes the mic back by itself after the recorder stops.
