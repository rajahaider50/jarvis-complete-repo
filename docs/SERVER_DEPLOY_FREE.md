# Host the JARVIS brain server for free — honest comparison + step-by-step

The server is a plain Flask app + one SQLite file, so almost any free host works. Two things decide
which one is right for you:

1. **Does it sleep?** Free tiers that sleep lose the learner thread until the next request wakes it.
2. **Does the disk survive?** SQLite needs a *persistent* disk, otherwise your memory is wiped on deploy.

| Host | Free? | Sleeps? | Persistent disk on free? | Verdict |
| --- | --- | --- | --- | --- |
| **Termux on the phone** | yes | no (with wakelock) | yes (the phone) | **Best default.** Zero cost, zero sleep, data is local. Use `SERVER_SETUP_TERMUX.md`. |
| **Cloudflare quick tunnel** | yes | no | n/a | Only exposes the Termux server to the internet (no domain, URL changes each run). |
| **Render** | yes | yes, ~15 min idle → cold start ~30 s | no on free | Good for testing; memory can be lost on redeploy. |
| **Koyeb** | yes (hobby) | no for one small service | no free volume | Good free always-on; use a redeploy-safe data path or accept resets. |
| **Hugging Face Spaces (Docker)** | yes | no (sleeps after long inactivity on some tiers) | no on free | Easy Docker deploy from this repo, public URL, `/health` works. |
| **Fly.io** | small free allowance | no | yes (small volume) | Best free *cloud* option if you want persistence. Needs a card. |
| **PythonAnywhere** | yes | no (for 3 months) | yes (small) | Very easy for Flask, but HTTP is one worker and outbound API calls are whitelisted by plan. |
| **Deta Space / Railway** | changed/discontinued tiers | – | – | Not recommended in 2026 (verify before relying on them). |
| **Vercel / Netlify functions** | yes | n/a | no | Wrong shape: serverless has no long-running learner thread and no SQLite. **Do not use.** |

**Recommendation:** run it on the phone with Termux (free forever, always on, data local). Add a free
cloud host only if you want it reachable when the phone is off, and prefer **Fly.io** (small volume) or
**Koyeb** for that.

---

## A. Render (easiest to try)

1. Push this repo to GitHub (already done).
2. render.com → **New → Web Service** → connect the repo.
3. Settings:
   - **Root directory:** `server`
   - **Build command:** `pip install -r requirements.txt`
   - **Start command:** `gunicorn wsgi:app --bind 0.0.0.0:$PORT --workers 1 --threads 4 --timeout 120`
   - **Instance type:** Free
4. Environment variables:
   - `JARVIS_TOKEN` = a long random string
   - `JARVIS_DATA` = `/opt/render/project/src/server/data` (a writable path inside the service)
   - optional `JARVIS_LLM_KEY`, `JARVIS_LLM_MODEL`
5. Deploy, then open `https://<name>.onrender.com/health`. `{"ok": true, ...}` means it is live.
6. Reality check: after ~15 minutes with no traffic the free instance sleeps; the first request takes
   ~30 s. The learner only runs while the instance is awake. On redeploy the free disk can be reset.
   For **understanding and testing** it is perfect; for a permanently learning brain use Termux.

## B. Koyeb (free always-on)

1. koyeb.com → **Create Service → GitHub** → pick this repo, root `server`.
2. **Build:** `pip install -r requirements.txt` — **Run:** `gunicorn wsgi:app --bind 0.0.0.0:8000 --workers 1 --threads 4`
3. Port **8000**, instance type **Free (hobby)**, then add the same env vars as Render
   (`JARVIS_DATA=/data` will not persist on free — treat memory as temporary).
4. Open `https://<app>.koyeb.app/health`.

## C. Hugging Face Spaces (Docker, public URL)

1. huggingface.co → **New Space** → SDK **Docker** → *Blank*.
2. Upload the contents of `server/` (or `git push` the Space repo with `server/` as the root).
   The included `Dockerfile` already listens on **7860** and sets `JARVIS_DATA=/data`.
3. Space → **Settings → Variables and secrets**:
   `JARVIS_TOKEN` (secret), and optionally `JARVIS_LLM_KEY` (secret), `JARVIS_LLM_MODEL` (variable).
4. Wait for the build, then open `https://<user>-<space>.hf.space/health`.
   Note: free Spaces have **no persistent disk** — memory lives until the Space restarts. Use it as a
   remote brain for testing, not as your long-term memory.

## D. Fly.io (free-ish, with a real persistent disk)

```sh
# on a PC, after installing flyctl and logging in
cd server
fly launch --no-deploy          # accept the Dockerfile; choose the smallest machine
fly volumes create jarvis_data --size 1
```
`fly.toml` — set the mount and the env:
```toml
[[mounts]]
  source = "jarvis_data"
  destination = "/data"

[env]
  JARVIS_DATA = "/data"
```
```sh
fly secrets set JARVIS_TOKEN=... JARVIS_LLM_KEY=...
fly deploy
curl https://<app>.fly.dev/health
```
This is the only free-ish cloud option here where the SQLite file (your memory) really survives.

---

## E. Point the app at it

Profile → **Brain server** → paste the URL (`https://...`) and the same `JARVIS_TOKEN` → **Test
connection**. The page must show `ok` plus the real counts. If it shows "unreachable", the app is
working correctly — the URL/token or the host is wrong (see the troubleshooting table in
`SERVER_SETUP_TERMUX.md`).

## F. Hard rules, whatever you choose

- `JARVIS_TOKEN` is **mandatory** on anything reachable from the internet.
- Keep **one worker** (`--workers 1`): SQLite plus the in-process learner thread are designed for that.
- If you want the learner to keep working, the process must stay awake — that is why the phone (Termux)
  is the recommended home for the brain, and a free cloud host is the backup.
