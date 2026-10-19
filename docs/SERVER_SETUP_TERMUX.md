# Run the JARVIS brain server on the phone (Termux) — step by step

Everything here is done **on the phone**. No PC, no root. Termux is a terminal app; the server then
runs as a normal Termux process and the JARVIS app talks to it over `127.0.0.1`.

> If Termux is not installed: install it from **F-Droid** (the Play Store build is outdated). Then open it
> once and let it finish setting up.

## 0. One-time: give Termux access to the phone's storage (for the git clone)

```sh
termux-setup-storage      # tap "Allow"
pkg update -y && pkg upgrade -y
pkg install -y git python
```

## 1. Get the server code

```sh
cd ~
git clone https://github.com/<your-user>/jarvis-complete-repo.git
cd jarvis-complete-repo/server
```

If the repo is **private**, `git clone` asks for a username and a **token** (create one in GitHub →
Settings → Developer settings → Personal access tokens → *Fine-grained*, read-only on this repo is
enough for cloning). Paste the token as the password. Never paste a token into a chat you will keep.

## 2. Install the server's Python packages

```sh
pip install --upgrade pip
pip install -r requirements.txt
```
That installs `flask`, `gunicorn` and `pytest` — all pure Python, nothing to compile.

## 3. Configure it

```sh
cp .env.example .env
nano .env       # or:  termux-open .env   (edits in another app)
```
Set at least these two lines:

```
JARVIS_DATA=$HOME/jarvis-data
JARVIS_TOKEN=<paste-a-long-random-string-here>
```

- `JARVIS_TOKEN` is the shared secret. The app sends it as `X-Jarvis-Token`. Make it long: e.g. generate
  one with `python -c "import secrets;print(secrets.token_urlsafe(32))"`.
- Optional but recommended for real learning: `JARVIS_LLM_KEY` (your Gemini key) and
  `JARVIS_LLM_MODEL=gemini-2.0-flash`. Without it the server still works — it saves raw notes and says so.

## 4. Run it

```sh
bash run_termux.sh
```
Expected output:

```
JARVIS server -> http://0.0.0.0:8787   (data: /data/data/com.termux/files/home/jarvis-data)
JARVIS server on http://0.0.0.0:8787  (token: set)
```

Keep this Termux session open (or use step 7 to keep it alive). Now check it — **in the phone browser**
open:

```
http://127.0.0.1:8787/
```

You should see the dashboard with real numbers (memories / skills / jobs / mood). If the page loads, the
server is alive. The same data is available as JSON at `http://127.0.0.1:8787/health`.

From another device on the same Wi-Fi, use the phone's IP: `http://192.168.x.x:8787/`
(phone: `ifconfig | grep inet` or Settings → About → Status).

## 5. Prove it really works (3 real checks)

```sh
# 1) add a memory
curl -s -X POST http://127.0.0.1:8787/api/memories \
  -H "Content-Type: application/json" -H "X-Jarvis-Token: $JARVIS_TOKEN" \
  -d '{"text":"Owner builds dynamic 3D websites","kind":"fact"}'

# 2) queue a learning job, then read its real result
JOB=$(curl -s -X POST http://127.0.0.1:8787/api/learn \
  -H "Content-Type: application/json" -H "X-Jarvis-Token: $JARVIS_TOKEN" \
  -d '{"task":"learn how to build a dynamic 3D website","observation":"uses requestAnimationFrame"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['job'])")
sleep 6
curl -s http://127.0.0.1:8787/api/jobs/$JOB -H "X-Jarvis-Token: $JARVIS_TOKEN"

# 3) see what it learned
curl -s "http://127.0.0.1:8787/api/memories" -H "X-Jarvis-Token: $JARVIS_TOKEN"
```

Job `status` must become `done` with a `result` like `memories=2; skill=learn-how-to-...`. If it says
`failed`, the `error` field tells you why (usually the model key or the provider URL).

## 6. Keep it running in the background

```sh
# start detached, log to a file
nohup bash run_termux.sh > ~/jarvis-server.log 2>&1 &
tail -f ~/jarvis-server.log        # watch it; Ctrl-C stops only the tail
```

Also do these once, or Android will freeze the server when the screen is off:
- Termux notification → **Acquire wakelock**.
- Android Settings → Apps → Termux → **Battery → Unrestricted**.
- Settings → Apps → Termux → Battery → *Don't optimise* / *Allow background activity*.

## 7. Start it automatically after a reboot (optional, no root)

```sh
pkg install -y termux-services
mkdir -p ~/.termux/boot
cat > ~/.termux/boot/jarvis-server.sh <<'EOF'
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock
cd ~/jarvis-complete-repo/server && nohup bash run_termux.sh >> ~/jarvis-server.log 2>&1 &
EOF
chmod +x ~/.termux/boot/jarvis-server.sh
```
The **Termux:Boot** app (F-Droid) runs everything in `~/.termux/boot/` after a reboot.

## 8. Make it reachable from outside the phone (optional, no root)

Only needed if the app runs on another device or on mobile data:

```sh
pkg install -y cloudflared
cloudflared tunnel --url http://127.0.0.1:8787
```
It prints a temporary public URL like `https://random-words.trycloudflare.com`. Put that URL + the token
into the app (Profile → Brain server). The URL changes each run; for a stable one you need a real domain
(see `SERVER_DEPLOY_FREE.md`).

⚠️ Anything reachable from the internet **must** have `JARVIS_TOKEN` set. Without a token, anyone who
finds the URL can read and write your memory.

## 9. Updating the server later

```sh
cd ~/jarvis-complete-repo && git pull && cd server && pip install -r requirements.txt
# then restart it (kill the old one first)
pkill -f jarvis_server ; nohup bash run_termux.sh >> ~/jarvis-server.log 2>&1 &
```

## Common problems

| Symptom | Cause / fix |
| --- | --- |
| `ModuleNotFoundError: No module named 'flask'` | you are in the wrong folder or forgot `pip install -r requirements.txt` |
| dashboard loads but the app says "unreachable" | app URL must include the port (`...:8787`), and on another device use the phone's IP, not `127.0.0.1` |
| `401 bad or missing X-Jarvis-Token` | the token in `.env` and the token in the app must match exactly |
| jobs stay `queued` | the learner is off (`JARVIS_LEARNER=1` to enable) or the process was restarted; it is one thread by design |
| `status: failed` + `HTTP 400` on a job | wrong `JARVIS_LLM_BASE`/model name, or the key has no quota |
| server dies when the screen locks | wakelock + battery "Unrestricted" (step 6) |
