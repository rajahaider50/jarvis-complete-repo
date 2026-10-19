# JARVIS companion server ("the brain")

Your own backend. It keeps long-term **memory**, **skills** and a **job queue**, learns in the
background while you are busy, holds an honest **affect** (mood) model, and serves **Server-Driven UI**
pages. The phone talks to it over plain HTTP with one shared token. No third-party service is required:
everything is a single SQLite file.

Full architecture + roadmap: [`../docs/SERVER_SYSTEM.md`](../docs/SERVER_SYSTEM.md)
Run it on the phone: [`../docs/SERVER_SETUP_TERMUX.md`](../docs/SERVER_SETUP_TERMUX.md)
Free hosting: [`../docs/SERVER_DEPLOY_FREE.md`](../docs/SERVER_DEPLOY_FREE.md)
How the app connects: [`../docs/APP_SERVER_CONNECTION.md`](../docs/APP_SERVER_CONNECTION.md)

## 60-second start (Termux)

```sh
cd server
bash run_termux.sh              # installs flask/gunicorn and starts on port 8787
```
Then open `http://127.0.0.1:8787/` in the phone browser — you get the live dashboard.

## Start on a PC / any Linux

```sh
cd server
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env            # optional: add JARVIS_LLM_KEY for real learning
python -m jarvis_server 8787
```

## Tests (no network needed)

```sh
cd server
python -m pytest -q
```
CI runs the same command in the `Server CI` workflow.

## API at a glance

| Method | Path | What it does |
| --- | --- | --- |
| GET | `/health` | version, counts, mood, config (never keys) — always open |
| GET | `/api/state` | counts + affect + learner status |
| GET/POST | `/api/memories` | list / add a memory (`?q=` searches) |
| DELETE | `/api/memories/<id>` | forget one |
| POST | `/api/recall` | real recall: ranked memories + the current mood line |
| GET/PUT/DELETE | `/api/skills[/<name>]` | skill store (SKILL.md bodies) |
| POST | `/api/learn` | queue a learning job (distil memory + write a skill) |
| POST | `/api/summarise` | queue a summarise job |
| GET | `/api/jobs[/<id>]` | queue + per-job result/error |
| GET/POST | `/api/affect` | mood state / record a real event |
| GET | `/api/pages[/<id>]` | Server-Driven UI pages (whitelisted components) |
| POST | `/api/sync` | app pushes memory/skills/events, gets the brain back |

Auth: when `JARVIS_TOKEN` is set, every `/api` call needs the header `X-Jarvis-Token: <token>`.
