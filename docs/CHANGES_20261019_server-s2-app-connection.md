# Change — Phase 4 S2: the app now really talks to the owner's own brain server (2026-10-19)

Companion to `docs/SERVER_SYSTEM.md` (what the server is) and `docs/APP_SERVER_CONNECTION.md` (the full
S2→S8 plan). This file is only about the **app side** that landed in S2, and how to prove it on the phone.

## What was added

| File | Change |
| --- | --- |
| `net/JarvisServer.kt` *(new)* | The single door to the server: `health`, `state`, `addMemory`, `recall`, `learn`, `jobs`, `affectEvent`, `skills`. Real `HttpURLConnection`, 6 s connect / 12 s read, `X-Jarvis-Token` when set, JSON in/out. Every call returns `Result(ok, status, body, error)` — no exceptions escape, no fake replies. It logs `JARVIS_DIAG` lines tagged `srv:` and keeps the counters the UI shows. |
| `data/Prefs.kt` | New saved settings: `serverUrl`, `serverToken`, `serverEnabled` (default **on**), `serverSyncMemory` (default on, used by S3). |
| `ui/screens/ServerScreen.kt` *(new)* | Profile → **Brain server**: URL + token fields (token hidden), the two switches, **Test connection** (real `/health` then `/api/state`), a live-state card read straight from the server (version, memories, skills, jobs queued, mood, learner status, last success, last error), a **Teach JARVIS** field that queues a real learning job, and **Read the queue** showing the real job rows (`queued/running/done/failed` + each job's `result`/`error`). |
| `ui/screens/DiagnosticsScreen.kt` | New **Brain server** card: state, URL, version · memories · skills, jobs · mood, learner, calls · last success, the real last error text, and a *Test the brain server now* button. |
| `live/Diag.kt` | Evidence fields `srvUrl/srvState/srvVersion/srvMemories/srvSkills/srvJobs/srvMood/srvLearner/srvCalls/srvLastError/srvLastOk` + one dump line: `srv url=… state=ok ver=0.1.0 mem=12 skills=3 jobs=0 mood=content learner=on/15s … calls=9 lastOk=18:22:41` and `srv lastError=…`. |
| `MainActivity.kt`, `ui/screens/ProfileScreen.kt` | Route `server` + one Profile row "Brain server (memory · learning)". |

## Why it is safe for everything that already worked

- With `serverUrl` empty (the default) the app makes **zero** network calls; the page only shows
  "not set — local memory only".
- Nothing in chat, live calls, cursor, recording or wake reads the server yet — S2 is only the manual
  page + the client. Memory sync joins the AI context in S3, and only when `serverEnabled` is on.
- Failures are displayed, not swallowed: the real exception class/message (`ConnectException`, `HTTP 401`,
  `SocketTimeoutException`) is shown on the page and written to logcat.

## How to prove it on the phone (5 minutes)

1. Start the server in Termux (`cd server && bash run_termux.sh`) and open `http://127.0.0.1:8787/` in the
   phone browser. Copy the `JARVIS_TOKEN` you put in `.env`.
2. App → **Profile → Brain server**: URL `http://127.0.0.1:8787`, paste the token → **Test connection**.
   Expected: `Connected to v0.1.0: 0 memories, 0 skills, 0 jobs queued, mood calm, learner on/15s`.
3. Type a task in **Teach JARVIS** (e.g. `build a dynamic 3D website like this sample`) → **Queue a
   learning job** → wait ~10 s → **Read the queue**. Expected: row `#1 · learn` with status `done` and
   result `memories=…; skill=learn-…` (or `failed` + the real error if the model key is missing).
4. Stop the server (Ctrl-C in Termux) → **Test connection** again. Expected:
   `Not reachable — ConnectException: …`. The rest of the app keeps working (chat/call/cursor unchanged).
5. Diagnostics → **Brain server** card must show the same real values, and in a terminal:
   `adb logcat -v time JARVIS_DIAG:V *:S` must contain `srv: health ok v=0.1.0 memories=…` and
   `srv: ConnectException -> …` after step 4. Press *Copy*/*Save* on Diagnostics to keep the dump:
   the line `srv url=… state=… mem=… skills=… mood=… calls=… lastOk=…` is the evidence.

## Live test server (temporary, started by the AI on 2026-10-19)

Because the owner asked to connect right away, the server was also started for real and exposed over
HTTPS so the app can be tested before Termux is set up:

- What was run: `JARVIS_DATA=/tmp/opencode/jarvis-data JARVIS_TOKEN=<shared in chat> JARVIS_LEARNER=1
  JARVIS_LEARNER_INTERVAL=10 python3 -m jarvis_server 8787` plus a temporary tunnel to
  `https://b84cdbfec0abc8.lhr.life` (localhost.run).
- Verified through the public URL: `/health` 200, `/api/state` **401 without the token** and 200 with it,
  `POST /api/memories` stored a memory, `POST /api/learn` produced job #1 with status `done` and result
  `memories=1; skill=learn-how-to-build-a-dynamic-3d-website-like-the-sample ai=False`, and
  `GET /api/memories` listed both rows (`kind=fact` from the app plus the learner's `kind=raw` note).
  The same 32 checks were also run locally against the real Flask app (`app.test_client()`) — all passed.
- The `ai=False` is honest: no `JARVIS_LLM_KEY` was configured, so the learner stored the raw note instead
  of inventing distilled lessons. With a key set, `Brain.distil()` parses the model's JSON lessons.
- ⚠️ This instance lives in the AI's own sandbox and dies with it; the URL and token are disposable. The
  permanent homes are Termux on the phone (`docs/SERVER_SETUP_TERMUX.md`) or a free host
  (`docs/SERVER_DEPLOY_FREE.md`). Rotate the token afterwards — never keep it in a chat.

## Not done in this step (next)

- S3: memories actually flow between the phone and the server and into the AI context (chat + live call).
- S4: "learn this while I'm busy" from a chat message / voice, plus a notification when the job finishes.
- S5 skills sync, S6 affect events from real app events, S7 the SDUI renderer, S8 the step-by-step Profile.

Build: `Build APK` green — run **39289817001**, commit `31d8a574`.
