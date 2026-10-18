# ROOT CAUSE REPORT — Masla 1: recording on → meri awaz AI tak nahi pahunchti
Date: 2026-10-18 · Evidence layer: Phase 1 (`Diag`, tag `JARVIS_DIAG`).

## Khulasa (asan zuban)
- **Red warning kahan se aata hai:** `ui/screens/LiveVoiceScreen.kt:192` — `live.micSilent` true hone par. `micSilent`
  `LiveClient` me set hota tha jab mic ke **lagataar ~1 second ke sab samples exactly 0** aa jayen (`silentRun >= 10`).
  Ye "quiet room" nahi — exactly-zero ka matlab hai mic ko zeros mil rahe hain (koi doosra client mic le chuka hai,
  ya source silence de raha hai).
- **Jarvis ka apna recorder mic nahi leta.** `ScreenRecordService` sirf `CallAudioTap` se PCM leta hai (jo LiveClient
  mic se bhejta hai) aur `AudioMixer` se AAC banata hai; mic wahan dobara nahi khulta. Yaani **Jarvis ke andar call ke
  dauran sirf ek hi AudioRecord** tha. To `micSilent` ka matlab hai **bahar ka koi client** (phone ka system/MIUI
  screen recorder, ya koi app) ne mic le liya, ya `VOICE_COMMUNICATION` source silent ho gaya.
- **Wake word listener** call ke dauran band ho jata hai (`CallHost.begin` → `WakeService.setPaused(true)` →
  SpeechRecognizer destroy, Vosk stop, Porcupine pause). Ab ye har step log hota hai (pehle nahi hota tha, is liye
  sabit karna mushkil tha).
- **Confirm nahi hua (device par):** user ne *Jarvis ka* record button dabaya ya *phone ka* recorder chalaya. Iske
  liye logcat chahiye (neeche command).

## Evidence (code se)
| # | Fact | Where |
|---|---|---|
| E-1 | Red warning = `live.micSilent` | `LiveVoiceScreen.kt:192-193` |
| E-2 | `micSilent` = 10 × ~100 ms buffers with `sum == 0.0` (exact digital zero) | old `LiveClient.startAudio` mic loop |
| E-3 | In-app recorder takes no mic: `MediaCodec` video + AAC from `AudioMixer`, fed by `CallAudioTap.mic/out` | `ScreenRecordService.kt:108-127`, `:121-122`, `AudioMixer.kt` |
| E-4 | Mic source `VOICE_COMMUNICATION`; AEC + NS attached | old `LiveClient.startAudio` |
| E-5 | Call pauses the wake listener (`setPaused(true)` → destroy SpeechRecognizer / stop Vosk / pause Porcupine) | `CallHost.kt:49`, `WakeService.kt:52-55` |
| E-6 | FGS types: call = `microphone|camera`, recorder = `mediaProjection`, share = `dataSync` | `AndroidManifest.xml:73-75`, `:120-127` |
| E-7 | Assistant PCM is 24 kHz (matches the guide's assumption); mic 16 kHz; mixer 44.1 kHz AAC | `LiveClient.startPlayback`, `AudioMixer` |

## Fixes
- **F1-1 (guide FIX 1) single mic owner `MicCaptureHub`.** New `live/MicCaptureHub.kt` is the only place an AudioRecord
  is opened. Consumers attach as listeners (Gemini sender; the recorder gets the same buffers through `CallAudioTap`)
  and the level meter is computed inside. `LiveClient.startAudio()` now just attaches + `MicCaptureHub.start()`.
- **F1-2 (guide FIX 4) self-healing warning.** After ~1.5 s of exact-zero audio the hub **releases and re-creates
  the AudioRecord itself**, walking the source list `VOICE_COMMUNICATION → MIC → DEFAULT` with backoff (3 tries),
  probing ~600 ms each time, and logs which source came back. So it recovers without the user doing anything; if it
  cannot, the user sees the warning.
- **F1-3 (guide FIX 4) accurate, actionable warning.** The warning now appears only on real (1.5 s) silence, names the
  app holding the mic (`getActiveRecordingConfigurations()`), and says Jarvis is retrying by itself.
- **F1-4 (guide FIX "pehchano")** "Check who holds the mic" button on Diagnostics; `Diag.foreignRecorder` shows the
  holder package + audio source.
- **F1-5 (evidence) wake-mic logging.** `WakeService` logs when Vosk / Porcupine / basic SpeechRecognizer opens and
  closes the mic, so "only one mic at a time" is provable from the log.
- **F1-6 (source kept constant)** The hub keeps `VOICE_COMMUNICATION` as the first choice and AEC + NS on every
  (re)open; it never changes source on recording on/off.

## Already correct (verified, unchanged)
- The recorder never uses `MediaRecorder` mic audio; user + assistant voices are mixed by `AudioMixer` into one AAC
  track (guide's target design). Video = `MediaCodec` + `MediaMuxer` from the single MediaProjection.
- FGS types are declared as the guide requires.

## Confirm on the phone
Not confirmed from code alone: which app took the mic, and whether recovery works on this OEM. Run while reproducing:

```
adb logcat -c
adb logcat -v time JARVIS_DIAG:V AudioRecord:V AudioFlinger:V AudioPolicyManager:V AndroidRuntime:E *:S
```

Look for: `MicCaptureHub START source=...`, `mic: silent: recovery attempt n/3`, `mic: RECOVERED with source=...`,
`wake: vosk/SpeechRecognizer/porcupine ... mic OPEN/CLOSED`. In Diagnostics: `silent now`, `zero-runs / recoveries`,
`another app on mic`.
