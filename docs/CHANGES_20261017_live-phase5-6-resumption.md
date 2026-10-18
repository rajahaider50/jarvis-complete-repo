# Live Screen Stream Phase B5-B6: keep the call alive (session resumption, compression, goAway)

## What changed
- **`live/LiveClient.kt`**
  - **Session resumption:** the setup message now includes `sessionResumption` (with the saved `handle` when it is
    fresh). The server's `sessionResumptionUpdate.newHandle` is saved to `Prefs` (with a timestamp, reused only
    within 30 min), so a dropped connection continues the SAME conversation instead of starting over.
  - **Context window compression:** setup includes `contextWindowCompression.slidingWindow.targetTokens = 12000`
    and `triggerTokens = 24000`, so a long call does not run out of context.
  - **goAway:** when the server says it will disconnect soon (`goAway.timeLeft`), the client proactively opens a
    fresh socket and resumes immediately (no awkward silence).
  - **Auto-reconnect:** an unexpected close AFTER setup now reconnects (up to `MAX_RECONNECT = 5`) with a 0.7 s
    pause and the info line shows "Reconnecting…", instead of ending the call. `stop()` (the user hangs up) clears
    the handle so a fresh call always starts a fresh conversation.
- **`data/Prefs.kt`** — `liveResume` (default on), `liveCompress` (default on), `resumeHandle`, `resumeAt`.
- **`ui/screens/ProfileScreen.kt`** — two toggles: **Keep the call alive** (session resumption) and
  **Compress long calls** (context compression).

## How to test
1. Start a live call, then turn Wi-Fi/mobile data off for a few seconds and back on -> the call should come back by
   itself ("Reconnecting…") and remember what you were talking about.
2. Have a very long conversation -> it keeps going (compression) instead of erroring out.
3. Hang up and start a new call -> a fresh conversation (no old context).

## Protocol
Field names verified against the Gemini Live API reference: `sessionResumption` / `sessionResumptionUpdate`
(`newHandle`, `resumable`), `contextWindowCompression` (`slidingWindow.targetTokens`, `triggerTokens`), and
`goAway` (`timeLeft`).

## Notes
- This completes Guide B (screen stream / share / record + Live robustness). `look_at_screen` (fresh look) was
  added in Guide A Phase A5.
