# CHANGES 2026-10-18 — Bugfix Masla 3: call stalling / dropping (stability)

Root Cause Report: `docs/REPORT_20261018_masla3_call_stalling.md`.

## Fixes (each maps to an RC in the report)
- **F3-1 (RC-1) tools off the reader thread.** `LiveClient` now has a single-thread executor `live-tools`;
  `handle()` posts `toolCall` work to it instead of running `PhoneTools.execute(...)` on the OkHttp reader thread.
  So while `read_screen` / `look_at_screen` / `wait_seconds` / `open_app` runs, Gemini's audio keeps being read.
  `toolResponse` is sent from the worker.
- **F3-2 (RC-2) video never blocks the audio uplink.** `sendJpeg` has a latest-wins in-flight guard and skips a frame
  when `ws.queueSize() > 96 KB`; payload + queue sizes are logged and drops counted.
- **F3-3 (RC-3) VAD + interruption handling.** Setup now sends `realtimeInputConfig`:
  `automaticActivityDetection` with `START_SENSITIVITY_LOW`, `END_SENSITIVITY_LOW`, `prefixPaddingMs=40`,
  `silenceDurationMs` 500 ms (barge-in on) / 900 ms (off); `activityHandling` = `START_OF_ACTIVITY_INTERRUPTS`
  or `NO_INTERRUPTION`. New Profile toggle **"Let me interrupt"** (`Prefs.liveBargeIn`, default on) so the user can
  stop the assistant's echo from cutting it off on speaker. (Field names verified against https://ai.google.dev/api/live.)
- **F3-4 (RC-4) smooth playback.** Mic + playback threads get `THREAD_PRIORITY_URGENT_AUDIO`; playback pre-buffers
  ~150 ms (250 ms max) before the first write; `playQBytes` tracked; underrun + queue logged; interrupt flushes and
  resets the byte counter.
- **F3-5 (RC-5) keep the radio alive.** New `live/CallLocks.kt`: `PARTIAL_WAKE_LOCK` + `WIFI_MODE_FULL_LOW_LATENCY`
  (high-perf on API < 29) held for the whole call by `CallHost.begin`, released in `CallHost.end`. `WAKE_LOCK`
  permission already present.
- **F3-6 (RC-6) survive a network outage.** `reconnect()` uses exponential backoff + jitter (500 ms → 8 s) instead of
  a fixed 700 ms; `MAX_RECONNECT` 5 → 20; `ConnectivityManager.registerDefaultNetworkCallback` restarts the call the
  moment the network returns (registered in `start()`, removed in `stop()`).
- **F3-7 (RC-7 / guide FIX 5) thermal-aware sharing.** `ScreenShareService` reads `PowerManager.currentThermalStatus`
  (API 29+); when the device is hot it drops to 480 px / quality 45 and ≥ 2.5 s between frames, and logs the change,
  so recording + share + shader + call together don't cook the phone.

## Files
- `live/LiveClient.kt` (tool executor, video guard, VAD config, priorities/pre-buffer, backoff + net callback)
- `live/CallLocks.kt` (new), `live/CallHost.kt` (acquire/release locks)
- `live/ScreenShareService.kt` (thermal-aware)
- `data/Prefs.kt` (`liveBargeIn`), `ui/screens/ProfileScreen.kt` ("Let me interrupt" toggle)
- `docs/REPORT_20261018_masla3_call_stalling.md` (new)

## Verify
- Build APK workflow green.
- Phone soak test (30/60 min, network switch, barge-in, long answer): call must not drop; Diagnostics shows
  `underruns` roughly flat, `interrupted` only when the user really talks over the assistant, `reconnects` on a
  network blip, and `tool ... ->/< -` lines no longer coincide with audio gaps.
- Logcat: `adb logcat -v time JARVIS_DIAG:V AudioRecord:V AudioTrack:V *:S`.

## Not changed
UI style/colours/fonts untouched (one new settings row in the existing style). No behaviour removed.
