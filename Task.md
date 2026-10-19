# TASK (current task; ONE at a time)
Status: in progress — S1 done (green), S2 is next.

- **Task:** Phase 4 — Autonomous companion server (see Roadmap.md). Owner's request: a real backend that
  learns in the background, keeps human-like memory/skills, has an honest affect model, syncs with the app,
  runs on Termux now and on a free host later, plus a step-by-step UI. No fake data, no fake connections.
- **Step S1 — DONE (Server CI run 39286186755, commit `40d536a9`)** — `server/` foundation: SQLite store
  (memories / skills / jobs / events / kv), Flask HTTP API with token auth, `GET /health` + dashboard,
  background learner thread, honest affect model, SDUI whitelist, 11 pytest tests, `Server CI` workflow,
  Termux + free-hosting + app-connection guides. App untouched, `Build APK` green.
  One fix during the step: the bad-job test had to run the learner `JARVIS_LEARNER_TRIES` times
  (the retry policy is real, not a single-shot failure).
- **Step S2 (next)** — app side: `Prefs.serverUrl` / `serverToken` / `serverEnabled`,
  `net/JarvisServer.kt` (real HTTP over `HttpURLConnection`, same style as `data/CustomProviders.kt`),
  Profile > **Brain server** page (Test connection shows real version/counts/mood, real error text),
  Diagnostics "Brain server" card. Must stay offline-safe: no server = app behaves exactly as today.
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
