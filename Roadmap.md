# ROADMAP (ordered; follow the order, do not jump ahead; owner may reorder)
Proposed starting order. Owner edits freely; AI only appends new ideas at the end.

## Phase 1 - Stabilise v8 (first)
- Get a clean GitHub Actions build; fix compile errors from the log only.
- Test on a phone: chat, live call (both engines), wake word, overlay, workspace agent, DocGen outputs.
- Fix real bugs found in Profile > Console.

## Phase 2 - Match the design
- Figma frames for updates 3-8 are still pending; bring code and design spec in line, screen by screen (UI-only tasks, no logic changes).

## Phase 3 - New features
(owner adds features here, one per line, in the order wanted)

## Phase 4 - Autonomous companion server (owner's request, 2026-10-19)
Order matters: each step must be green (app build + server CI) before the next starts.
- [ ] **S1 Foundation** — repo layout `server/`, real SQLite store, `GET /health`, token auth, pytest suite, `Server CI` workflow, Termux + free-host guides. (no app change)
- [x] **S2 App connection** — `Prefs.serverUrl/serverToken`, `net/JarvisServer.kt` (real HTTP over `HttpURLConnection`), Profile > "Brain server" page with a real Test-connection button and live state. Offline = works exactly as before.
- [x] **S2.5 One-command server + control site** — `bash server/start.sh` (14-step PASS/FAIL table, watchdog that restarts the server, Termux wakelock + boot, free public HTTPS URL that self-heals, `--status/--stop/--restart/--token`), `/control` website, `selftest.py` (33 checks) and a wheel fallback for broken pip. Docs: `docs/SERVER_ONE_COMMAND.md`.
- [ ] **S3 Memory sync** — app pushes new memories/lessons, pulls server memories; chat + live call read the merged memory. Diagnostics shows last sync + counts.
- [ ] **S4 Background learning** — `POST /api/learn` job queue + server-side learner loop (LLM distil -> memories + skills). App gets a "Learn this while I am busy" action (long-press a chat message / voice command) and a job list.
- [ ] **S5 Skills** — server-side skill store (SKILL.md bodies) synced into the app's existing Skills screen; skills counted when actually used.
- [ ] **S6 Affect (honest emotions)** — documented valence/arousal/energy/trust model from real events; shown on the orb/call screen; app posts real events (call ok/fail, tool ok/fail, praise/anger, idle).
- [ ] **S7 Server-Driven UI** — `/api/pages` component whitelist + app renderer; owner can add/change simple screens without a new APK. Everything else (camera, calls, accessibility) stays native.
- [ ] **S8 Step-by-step Profile** — Profile hub + separate pages per system; old rows keep working; each interface style still renders.
- [ ] **S9 Always-on phone server (optional)** — Termux boot script + Cloudflare quick tunnel so the phone itself can host the brain; documented, no root needed.

## Ideas parking lot
(AI adds ideas here; they move to Phase 3 only when the owner says so)
- Whisper/Vosk local STT on the server for fully offline calls.
- Per-day "diary" summarised by the server from real events.
- Skill marketplace: import/export skill packs as JSON between phones.
- Multi-device sync (phone + tablet) with the same brain server.
