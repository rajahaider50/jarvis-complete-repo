# ROOT CAUSE REPORT — Masla 3: call atakti hai aur beech me band ho jati hai
Date: 2026-10-18 · Evidence layer: Phase 1 (`Diag`, tag `JARVIS_DIAG`).

## Khulasa (asan zuban)
Call ke teen alag masle ek doosre par charh rahe hain:
1. **Jab AI tool chalata hai**, tool ka kaam **wahi thread** par hota hai jo Gemini se audio padhta hai — jab tak
   tool khatam na ho, nayi audio padhi nahi jati → "jumla, 2-3 second ruk, phir bol, phir atak".
2. **Speaker par AI ki apni awaz** mic me jaa kar server ko lagta hai ke user bol raha hai → server `interrupted`
   bhej deta hai → playback flush → phir dobara bolna. (Echo = false interrupt.)
3. **2 minute ka session limit** (audio+video) aur **~10 minute ka connection limit** — in par `goAway`/resume ka
   intezam to hai, lekin network 20 second chala jaye to 5 retries jaldi khatam ho jate hain aur call "Call lost"
   ho jati hai. Wake-lock/Wi-Fi-lock nahi tha, is liye screen off par radio so sakti hai.

## Evidence (code se)
| # | What happens | Where (file:line) |
|---|---|---|
| RC-1 | Tool calls run **on the OkHttp reader thread**: `WebSocketListener.onMessage` → `onFrame` → `handle` → `onTool` → `PhoneTools.execute(...)`. While a tool runs the reader can't read the next frames, so Gemini's audio/text stop being consumed. Heavy tools: `read_screen`, `look_at_screen` (screenshot+base64), `wait_seconds` (sleep), `open_app`, `click_on_screen` (sleep 150 ms), shell/`DeviceControl`. | `LiveClient.kt:165-166`, `:177-184`, `:318`, `:343`, `:366-382` |
| RC-2 | `sendJpeg` Base64-encodes a 720p frame (~60-200 KB → ~80-270 KB) and puts it on the **same WebSocket** as mic audio; OkHttp's writer is FIFO, so one big frame in front of the audio adds head-of-line delay to the mic uplink. No queue-size / in-flight guard. | `LiveClient.kt:385-389`, `ScreenShareService.kt:92-97` |
| RC-3 | No VAD tuning and no `activityHandling`: default is `START_OF_ACTIVITY_INTERRUPTS` with `START_SENSITIVITY_HIGH`. Speakerphone is on with voice-call volume at max (`AudioRouter.apply`), so the assistant's own voice (echo) trips server VAD → `interrupted` → `playQ.clear()` + `pause/flush/play`. | `LiveClient.kt:287-315` (no `realtimeInputConfig`), `:344-351`; `AudioRouter.kt` |
| RC-4 | Playback thread has **default priority** (not `THREAD_PRIORITY_URGENT_AUDIO`) and starts on the **very first chunk** (no pre-buffer); the mic thread likewise. Combined with RC-1/RC-2 this gives the tuk-tuk / underruns. Underrun counter existed nowhere. | `LiveClient.kt:437-471`, `:410` |
| RC-5 | No `PARTIAL_WAKE_LOCK` / `WifiLock` during the call → screen-off / Doze can suspend the radio and the app. (FGS type `microphone|camera` IS declared, that part is fine.) | `CallHost.kt`, `CallService.kt` (none), manifest |
| RC-6 | Reconnect uses a **fixed 700 ms** delay, no backoff/jitter, and `MAX_RECONNECT = 5`. A 20 s network outage burns all 5 tries (5 × ~(0.7 s + 2.5 s connect timeout) ≈ 16 s) → "Call lost". No `ConnectivityManager` callback to retry the moment the network returns. | `LiveClient.kt:88-130`, `:225-257` |
| RC-7 | Session lifetime limits confirmed by the current docs: **audio-only 15 min, audio+video 2 min, connection ~10 min**; compression + resumption extend them. We already set `contextWindowCompression` (`slidingWindow.targetTokens=12000`, `triggerTokens=24000`) and `sessionResumption`; `goAway` → `reconnect` exists. So the *intent* is right; the failure is RC-6 (network) plus the fact that `resumable=false` windows exist. | `LiveClient.kt:296-305`, `:336-342` |

Docs used: https://ai.google.dev/api/live (fields `realtimeInputConfig.automaticActivityDetection`,
`activityHandling`, `sessionResumption`, `contextWindowCompression`, `goAway`) and
https://ai.google.dev/gemini-api/docs/live-session (session lifetime table: 15 min audio / 2 min audio+video /
~10 min connection; resumption tokens valid 2 h).

## What is NOT the cause (checked)
- FGS type: `CallService` already declares `microphone|camera` (manifest line 75). OK.
- Keepalive: `OkHttpClient.pingInterval(20 s)` is set. OK.
- `AudioTrack` buffer `maxOf(min, rate)` = 500 ms at 24 kHz — acceptable (not the main cause).
- AEC + NS are attached to the record session. OK (but not enough alone — see RC-3).
- `Prefs.save()` uses `apply()` (async) → not blocking the reader. OK.

## Fixes (in this phase)
- **F3-1 (RC-1):** tool execution moved to a dedicated `live-tools` single-thread executor; the reader thread only
  parses. `toolResponse` is sent from the worker when done.
- **F3-2 (RC-2):** `sendJpeg` gets a latest-wins guard: skip if a frame is in flight or `ws.queueSize()` is large;
  log payload size + queue; frames dropped are counted in Diagnostics.
- **F3-3 (RC-3):** `realtimeInputConfig.automaticActivityDetection` tuned (`START_SENSITIVITY_LOW`,
  `END_SENSITIVITY_LOW`, `prefixPaddingMs=40`, `silenceDurationMs` 500/900) + `activityHandling`
  = `START_OF_ACTIVITY_INTERRUPTS` when barge-in is ON (default) else `NO_INTERRUPTION`. New Profile toggle
  "Let me interrupt (barge-in)".
- **F3-4 (RC-4):** `THREAD_PRIORITY_URGENT_AUDIO` on both the mic and the playback thread; ~150-200 ms jitter
  pre-buffer before the first write; underrun + queue logged.
- **F3-5 (RC-5):** `CallLocks` (PARTIAL_WAKE_LOCK + WIFI_MODE_FULL_HIGH_PERF) held for the whole call.
- **F3-6 (RC-6):** exponential backoff + jitter (`500 ms … 8 s`), `MAX_RECONNECT` raised, and a
  `ConnectivityManager.NetworkCallback` that restarts **immediately** when the network comes back.
- **F3-7 (RC-7, FIX 5):** thermal-aware sharing: when the device is hot the share loop slows down and drops
  quality (logged), so recording + share + shader + call together don't cook the phone.

## Confirm on the phone (the parts that are device/OEM specific)
Not confirmed from code alone: whether the *echo* (RC-3) is what the user sees, and whether the drop is a
`goAway` resume failure vs a network outage. Exact command the user should run while reproducing:

```
adb logcat -c
adb logcat -v time JARVIS_DIAG:V AudioRecord:V AudioTrack:V AndroidRuntime:E *:S
```

Then in Diagnostics: `underruns`, `interrupted`, `reconnects`, `lastClose` and the event list show the chain
(`tool ... ->`/`<-`, `interrupted -> flush`, `reconnect #n`, `CLOSED code=...`, `underrun +n`).
