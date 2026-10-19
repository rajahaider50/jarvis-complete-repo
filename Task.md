# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** Phase 4 — Autonomous companion server (see Roadmap.md). Owner's request: a real backend that
  learns in the background, keeps human-like memory/skills, has an honest affect model, syncs with the app,
  runs on Termux now and on a free host later, plus a step-by-step UI. No fake data, no fake connections.
- **Step S1 (this task, first)** — `server/` foundation: SQLite store (memories / skills / jobs / events / kv),
  Flask HTTP API with token auth, `GET /health`, real pytest suite, `Server CI` GitHub workflow,
  Termux + free-hosting guides. Server only — the app must not change in this step.
- **Step S2** — app side: `Prefs.serverUrl` / `serverToken`, `net/JarvisServer.kt` (real HTTP),
  Profile > **Brain server** page (live status, Test connection, sync). Offline-safe.
- **Later steps** — S3 memory sync, S4 background learning, S5 skills, S6 affect, S7 server-driven UI,
  S8 step-by-step Profile pages, S9 Termux always-on. One step per task, each ends green
  (app `Build APK` + `Server CI`) and moves lines between Pending.md / Complete.md.
- **Files (S1):** server/** , .github/workflows/server-ci.yml, docs/SERVER_SYSTEM.md,
  docs/SERVER_SETUP_TERMUX.md, docs/SERVER_DEPLOY_FREE.md, docs/APP_SERVER_CONNECTION.md,
  Goal.md, Roadmap.md, Task.md, Pending.md, Complete.md.
- **Test:** `Server CI` workflow (`python -m pytest server/tests -q`); app `Build APK` must stay green
  (no app file changes expected in S1).
- **Status:** in progress.

Next: S1 code, push, watch `Server CI` + `Build APK`, then S2.
