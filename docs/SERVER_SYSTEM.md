# JARVIS companion server — system design (real, no fake data)

This is the backend the owner asked for: a **real** HTTP server that keeps long-term memory, skills and
a job queue, learns in the background, holds an honest affect (mood) model, and can describe simple
screens for the phone. It lives in `server/` in this same repository and needs nothing but Python.

> Honest limits first: the server cannot "rewrite JARVIS by itself". Android does not allow an app to
> replace its own code in the background (no self-installing APKs outside the Play Store). What a real
> system *can* do — and what is built here — is: **learn** (memory + skills), **adapt** (settings,
> prompts, memory used by the AI), and **redraw simple screens** (Server-Driven UI). Anything that needs
> new native code still goes through a normal build (the app's GitHub Actions workflow).

## 1. Shape of the system

```
   Phone app (Kotlin/Compose)                Your own server (Python/Flask)
   ┌──────────────────────────┐              ┌───────────────────────────────┐
   │ chat, live call, tools   │   HTTP/JSON  │ /health  /api/state           │
   │ net/JarvisServer.kt  ────┼─────────────►│ /api/memories /api/recall     │
   │ offline-first: no server │  X-Jarvis-   │ /api/skills     /api/learn    │
   │ = works exactly as now   │  Token       │ /api/jobs /api/affect         │
   └──────────────────────────┘              │ /api/pages (SDUI) /api/sync   │
                                             ├───────────────────────────────┤
                                             │ Learner thread (job queue)    │
                                             │  learn → distil → memories    │
                                             │        → SKILL.md skills      │
                                             ├───────────────────────────────┤
                                             │ SQLite file: memories, skills,│
                                             │ jobs, events, kv (settings)   │
                                             └───────────────────────────────┘
                                                        │ HTTPS (optional)
                                                        ▼
                                             any OpenAI-compatible model
                                             (Gemini compat API, Groq, …)
```

## 2. Why these choices (and not the usual stack)

| Usual suggestion | Why not here | What is used instead |
| --- | --- | --- |
| FastAPI + Redis + Celery | needs compiling (Termux pain) and extra services on free hosts | **Flask + a plain thread** (pure Python) |
| Pinecone / ChromaDB | paid / heavy on a phone | **SQLite + IDF keyword ranking**, plus optional embeddings if a key is set |
| Flutter/React Native for dynamic UI | rewriting the whole app; the app is already Kotlin/Compose | **Server-Driven UI**: the server sends a page of *whitelisted* components, the app renders it |
| Self-installing new APK in the background | blocked by Android/Play policy | skills + memory + SDUI change behaviour without a new APK; real code changes still go through CI |

## 3. Data model (one SQLite file, `JARVIS_DATA/jarvis.db`)

| table | columns | meaning |
| --- | --- | --- |
| `memories` | id, kind, text, tags, source, embedding, uses, created, updated | one durable fact/lesson/episode. `uses` = how often recall returned it (real usage counter) |
| `skills` | id, name, body, source, uses, created, updated | named SKILL.md bodies |
| `jobs` | id, kind, payload, status, result, error, tries, created/started/finished | the queue the learner drains (`queued → running → done/failed`) |
| `events` | id, kind, weight, note, created | real happenings used by the affect model |
| `kv` | key, value | settings and cached state (e.g. the affect snapshot) |

## 4. Components

- **`store.py`** — SQLite (WAL) with one lock. Single file ⇒ works on Termux, on a free host, or on a PC.
- **`memory.py`** — `remember`, `recall` (IDF ranking + usage + recency), `distil(raw → lessons)`.
- **`skills.py`** — `save/get/list/delete`, and `learn(task, observation)` which writes a SKILL.md.
- **`affect.py`** — documented affect model: `valence, arousal, energy, trust, focus`, fixed per-event
  deltas (`EVENTS`), exponential decay to baseline with a configurable half-life, derived `mood` label.
  This is the "real emotions" part — deterministic and inspectable, not fake biology.
- **`learner.py`** — daemon thread that drains jobs (`learn`, `summarise`, `recall`). Each job's real
  result/error is written back to its row, so the phone can show exactly what happened.
- **`sdui.py`** — page definitions built only from `COMPONENTS` (`title, text, status, list, button,
  toggle, number, memory, affect`) and `ACTIONS`. Unknown parts are rejected at build time (tested).
- **`api.py`** — Flask routes, token auth, and a small HTML dashboard at `/`.

## 5. Security / privacy rules

- One shared secret: when `JARVIS_TOKEN` is set, every `/api` route needs `X-Jarvis-Token` (compared with
  `hmac.compare_digest`). `/health` stays open so a host can health-check it.
- Keys live **only** in the server's environment (`.env`), never in the app.
- The server never returns the keys; `config.mask()` shows only whether they are set.
- The app stays the owner of the phone: no phone permissions exist on the server side, and the server
  cannot act on the phone by itself — it only answers requests.

## 6. Roadmap mapping (see `Roadmap.md` Phase 4)

| Step | What lands | State |
| --- | --- | --- |
| S1 | `server/` foundation + tests + `Server CI` + guides | **this step** |
| S2 | `net/JarvisServer.kt`, `Prefs.serverUrl/serverToken`, Profile > Brain server page | next |
| S3 | memory sync both ways (chat + live call use the merged memory) | next |
| S4 | "learn this while I'm busy" from chat/voice → `/api/learn` + job list | next |
| S5 | skills sync into the app's Skills screen; `uses` counter grows when used | next |
| S6 | affect wired to real app events (call ok/fail, tool ok/fail, praise/anger, idle) | next |
| S7 | SDUI renderer in the app for `/api/pages` | next |
| S8 | Profile split into step-by-step pages | next |
| S9 | always-on: Termux boot script + Cloudflare quick tunnel (no root) | optional |

## 7. What is deliberately NOT in this system

- No fake "AI is thinking" placeholders: if no model key is set, `distil` stores the raw note marked
  `kind=raw` and `learn` writes a plain record — and says so.
- No hidden data collection: only what the app sends, plus what the learner is explicitly asked to learn.
- No background reading of the owner's phone from the server (impossible without the app asking).
