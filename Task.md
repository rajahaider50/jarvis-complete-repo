# TASK (current task; ONE at a time)
Status: in progress — S1 + S2 done (green), S3 is next.

- **Task:** Phase 4 — Autonomous companion server (see Roadmap.md). Owner's request: a real backend that
  learns in the background, keeps human-like memory/skills, has an honest affect model, syncs with the app,
  runs on Termux now and on a free host later, plus a step-by-step UI. No fake data, no fake connections.
- **Step S1 — DONE (Server CI run 39286186755, commit `40d536a9`)** — `server/` foundation: SQLite store
  (memories / skills / jobs / events / kv), Flask HTTP API with token auth, `GET /health` + dashboard,
  background learner thread, honest affect model, SDUI whitelist, 11 pytest tests, `Server CI` workflow,
  Termux + free-hosting + app-connection guides. App untouched, `Build APK` green.
  One fix during the step: the bad-job test had to run the learner `JARVIS_LEARNER_TRIES` times
  (the retry policy is real, not a single-shot failure).
- **Step S2 — DONE (`Build APK` run 39289817001, commit `31d8a574`)** — app side: `net/JarvisServer.kt`
  (real HTTP, no fake replies), `Prefs.serverUrl/serverToken/serverEnabled/serverSyncMemory`,
  Profile > **Brain server** page (Test connection = real `/health` + `/api/state`, live state, Teach
  JARVIS → real `/api/learn` job, Read the queue), Diagnostics "Brain server" card + `srv` dump line.
  Offline-safe: no URL set = zero server calls. See docs/CHANGES_20261019_server-s2-app-connection.md.
- **Step S2.5 — DONE (2026-10-20)** — one-command server: `server/start.sh` (14-step table with
  PASS/FAIL/SKIP + time + detail + per-step log, `--status/--stop/--restart/--logs/--token/--tunnel-only`,
  watchdog in its own session, Termux wakelock + `~/.termux/boot`, cloudflared quick tunnel with a real
  internet `/health` proof, localhost.run fallback, tunnel self-heal after 2 failed checks, token kept
  across runs), `server/selftest.py` (33 real checks, no pytest), `server/vendor_wheels.py` (stdlib wheel
  downloader for phones/containers where pip is broken), `/control` website (live state, add memory,
  queue learning, jobs, mood events, recall — token in localStorage), docs/SERVER_ONE_COMMAND.md.
  Tested in the sandbox: 12 PASS / 0 FAIL / 2 SKIP, selftest 33/33, public 9/9, watchdog restart,
  URL reuse, tunnel self-heal, cloudflared binary path.
- **Step S3 (next)** — memory sync both ways: push local memories (chat memory + click lessons) with
  `POST /api/sync`, pull the server's memories, merge into the AI context for chat + live calls, and show
  real pushed/pulled/last-sync counters in Diagnostics.
- **Later steps** — S3 memory sync, S4 background learning ("learn this while I'm busy"), S5 skills sync,
  S6 affect wired to real app events, S7 Server-Driven UI renderer, S8 step-by-step Profile pages,
  S9 Termux always-on. One step per task, each ends green (`Build APK` + `Server CI`) and moves lines
  between Pending.md / Complete.md.
- **Files so far (S1):** server/** , .github/workflows/server-ci.yml, docs/SERVER_SYSTEM.md,
  docs/SERVER_SETUP_TERMUX.md, docs/SERVER_DEPLOY_FREE.md, docs/APP_SERVER_CONNECTION.md,
  Goal.md, Roadmap.md, Task.md, Pending.md, Complete.md, docs/HANDOFF_STATE.md.
- **Test:** `Server CI` (`cd server && python -m pytest -q`) + app `Build APK` must both stay green.
- **User side (pending, not code):** run `server/run_termux.sh` on the phone, open the dashboard, complete
  one real learning job (`status=done`), and decide phone-hosting vs a free host.

Next: S2 — the first app change (Brain server page + real HTTP client).
