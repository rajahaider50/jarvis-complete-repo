# FINAL REPORT — JARVIS BUGFIX GUIDE (3 masle)
Date: 2026-10-18. Order followed: Diagnostics → Masla 3 → Masla 1 → Masla 2 → (this) final report.

## Method
Evidence-first: built the logging + Diagnostics layer first (`Diag`, tag `JARVIS_DIAG`, timestamp + thread),
then wrote a Root Cause Report per masla **before** changing code, then fixed, then verified with a green
`assembleDebug` (GitHub Actions build, workflow 386705151).

## Build runs (all green)
| Phase | Run |
|---|---|
| 1 Diagnostics (logger + screen + Cursor Test + chain logging) | 39112262099 |
| Masla 3 call stability | 39113131430 (after 39112856558 fix) |
| Masla 1 mic + recording | 39114125443 (after 39113875538 fix) |
| Masla 2 AI cursor | 39114550181 |
| Final confirmation | (this push) |

## Masla 3 — call atakti hai / band ho jati hai
Root causes (code-confirmed): tools ran on the WebSocket **reader** thread (blocking audio); big video frames
queued ahead of audio (head-of-line); no VAD/interruption tuning so echo tripped `interrupted`; playback had no
priority/pre-buffer; no wake/Wi-Fi lock; fixed 700 ms reconnect with only 5 tries so a 20 s outage killed the call.
Fixes: `live-tools` executor; latest-wins video guard (`ws.queueSize()`); `realtimeInputConfig` VAD +
`activityHandling` (+ "Let me interrupt" toggle); `THREAD_PRIORITY_URGENT_AUDIO` + 150 ms pre-buffer; `CallLocks`
(wake + Wi-Fi); exponential backoff + `ConnectivityManager` callback + `MAX_RECONNECT` 20; thermal-aware sharing.
Report: `docs/REPORT_20261018_masla3_call_stalling.md`.

## Masla 1 — recording on → meri awaz AI tak nahi pahunchti
Root cause: the red warning came from `live.micSilent` (exact-zero mic buffers ~1 s). The in-app recorder never
opened the mic (it uses `CallAudioTap`), and the wake listener is paused during a call — so the zeros meant an
**external** client held the mic (e.g. the phone's system/MIUI recorder) or the source went silent; the app had no
detection and no recovery.
Fixes: `MicCaptureHub` (single mic owner + fan-out to Gemini and the recorder), self-heal (release+recreate, source
fallback `VOICE_COMMUNICATION → MIC → DEFAULT`, backoff, probe), warning only on real 1.5 s silence + names the holder
+ says Jarvis is retrying, "Check who holds the mic" on Diagnostics, wake-mic open/close logging.
Report: `docs/REPORT_20261018_masla1_mic_recording.md`.

## Masla 2 — AI cursor screen par nazar nahi aata
Root cause: AI cursor tools ran on a **worker** thread, but `WindowManager.addView/updateViewLayout` and
`Choreographer` need the **main** thread; the exception was swallowed (`AppLog.w` → `return -1`), so nothing appeared
— while the Cursor Lab/panel (main thread) worked.
Fix: all overlay work marshalled to the main thread (`onMain` blocking / `postMain`), `CursorPanel.refresh()` too;
unknown-tool logging; clearer errors to the AI; service connect/disconnect logging + cursor restore on reconnect.
Report: `docs/REPORT_20261018_masla2_cursor.md`.

## Not confirmed from code alone (needs the phone)
Which app exactly took the mic (OEM behaviour), whether echo was the interrupt trigger on this device, and whether a
`goAway` resume vs a network outage caused a specific drop. Diagnostics + the logcat command answer these:

```
adb logcat -c
adb logcat -v time JARVIS_DIAG:V AudioRecord:V AudioTrack:V AudioFlinger:V AudioPolicyManager:V AccessibilityManager:V WindowManager:W AndroidRuntime:E *:S
```

## Phone checklist (soak test) — see Pending.md
30/60 min call, network switch, barge-in, long answer, mic never silent on record on/off, both voices in
Movies/Jarvis, Cursor Test 5 positions, voice-placed cursors, service-off clean error.
