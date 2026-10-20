# TASK (current task; ONE at a time)
Status: in progress — S1 + S2 + S2.5 + S2.6 done (green), S3 is next.

- **Step S2.6 — DONE (v1.3.0, commit `9d74b17d`)** — the first real phone run exposed two fatal bugs and
  a slow-tunnel false negative; all three are fixed and proven:
  1. `logs/step-14.log` on the phone said `boot_dir: unbound variable` — `local boot_dir=… f="$boot_dir/…"`
     under `set -u` reads `$boot_dir` before it exists, so the shell exited after step 14 (no step 15,
     no table, no URL). Fixed by splitting the assignments (with a comment so it cannot come back).
  2. `watchdog.sh` was 0 bytes: the generated script had `PYTHONPATH="$PYTHONPATH"`, which errors when
     `PYTHONPATH` is unset (fresh Termux + flask already installed) and leaves the file empty while the
     step still said PASS. Fixed to `${PYTHONPATH:-}` **and** the step now FAILs on an empty script or a
     watchdog that dies within 1 s.
  3. cloudflared needed ~2m45s on that phone (precheck 08:25:14 → registered 08:28:01) but the script
     gave up after 40 s. Now: 240 s wait with a heartbeat, **recovery** of a tunnel that is already
     running (same pid/URL), then the ssh fallback.
  Also new: **`server/console.sh` — the command center** (live panel: server/health/uptime/memories/
  skills/jobs/mood/learner/local URL/public URL/token/data/logs; 14 commands incl. teach, add memory,
  jobs, memories, live logs, restart, new URL, stop, update, tutorial; `q` closes only the panel).
  `start.sh` auto-opens it after a green run (needs a real TTY) and `--console` opens it any time;
  it also chmods its own saved copy (fixes `Permission denied` on `~/jarvis-start.sh`).
  Tested: Termux-like run (PREFIX + PYTHONPATH unset + fake HOME) 14/14 PASS; public install from the
  pushed repo 13 PASS/0 FAIL/2 SKIP with a verified URL; 9/9 public checks; console driven in a pty
  (teach → job done → memory + skill, jobs, memories, tutorial, stop, quit); tunnel recovery; ssh
  fallback with a broken cloudflared stub; watchdog restart after `kill -9`.

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
