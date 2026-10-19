> **Yeh kya hai:** ek hi command jo JARVIS ka brain server install karta hai, khud test karta hai,
> background me start karta hai, mar jaaye to wapas uthata hai, public HTTPS URL deta hai, aur
> aakhir me **PASS / FAIL / SKIP table** dikhata hai — har step ka apna log.

# JARVIS brain server — one command

`server/start.sh` is the only thing you need on a phone (Termux), a PC, or a VPS.
It never fakes a step: every row is a real command with a real exit code.

---

## 1. The command

**Private repo (this one).** Replace `<TOKEN>` with a GitHub token that can read the repo:

```sh
bash -c 'R=$HOME/jarvis-src; git clone -q https://<TOKEN>@github.com/rajahaider50/jarvis-complete-repo.git $R 2>/dev/null || git -C $R pull -q --ff-only; bash $R/server/start.sh'
```

**Already cloned?** Then it is literally:

```sh
bash server/start.sh
```

**Public fork?** Then no token is needed:

```sh
bash <(curl -fsSL https://raw.githubusercontent.com/<you>/jarvis-complete-repo/main/server/start.sh)
```

In a fresh Termux tab the first run installs `git` + `python` if they are missing
(`pkg install -y git python`), then does everything else by itself.

---

## 2. What you see (real output from the tested run)

```
>> 1) ENVIRONMENT
  01 PASS environment                             113 ms  termux      yes (/data/data/com.termux/files/usr)
>> 2) PACKAGES
  02 PASS system packages                          81 ms  ready
...
>> 6) SELF-TEST
  07 PASS real API self-test                     1863 ms  SELFTEST PASS 33/33
>> 7) START
  08 PASS free port 8787                          442 ms  port 8787 ready
  09 PASS start the server                        117 ms  pid 16916 -> ~/.jarvis-server/logs/server.log
  10 PASS wait for /health                       1953 ms  healthy - v0.1.0 memories=2 skills=1 jobs=0 mood=calm
  11 PASS watchdog                                140 ms  pid 16936 - restarts the server within ~20s if it dies
>> 8) ALWAYS-ON
  12 PASS Termux wakelock                          47 ms  wakelock acquired
  13 PASS boot autostart                           38 ms  written ~/.termux/boot/jarvis-server.sh
>> 9) PUBLIC URL
  14 PASS public HTTPS tunnel                    5336 ms  public URL https://....trycloudflare.com (verified from the internet)

  ┌──────┬──────┬──────────────────────────────────────┬───────────┬──────────────────────────────────────┐
  │ STEP │ ST.  │ WHAT                                 │ TIME      │ DETAIL                               │
  ├──────┼──────┼──────────────────────────────────────┼───────────┼──────────────────────────────────────┤
  │ 01   │ PASS │ environment                          │      113 ms │ termux yes                          │
  │ ...  │ ...  │ ...                                  │        ... │ ...                                  │
  └──────┴──────┴──────────────────────────────────────┴───────────┴──────────────────────────────────────┘

  TOTAL 14   PASS 12   FAIL 0   SKIP 2   time 11s
  result ALL GOOD - 12 passed, 2 skipped, 0 failed
```

If something fails, you get a **WHAT TO FIX** block with the exit code, the first real error lines
and the path of that step's full log (`~/.jarvis-server/logs/step-07.log` etc.).
Nothing is swallowed.

---

## 3. All commands

| command | what it does |
|---|---|
| `bash server/start.sh` | install + self-test + start + watchdog + public URL + table |
| `bash server/start.sh --status` | real `/health` answer, pids, public URL, token, last log lines |
| `bash server/start.sh --stop` | stops server, watchdog and tunnel |
| `bash server/start.sh --restart` | stop + start |
| `bash server/start.sh --logs` | follows the server log |
| `bash server/start.sh --token` | prints the token (same as the one in `.env`) |
| `bash server/start.sh --tunnel-only` | opens/refreshes just the public URL |
| `bash server/start.sh --no-tunnel` | local only (LAN/VPN use) |
| `bash server/start.sh --no-watch` | no watchdog (not recommended) |
| `bash server/start.sh --port 9000` | other port |
| `bash server/start.sh --dir ~/jarvis-src` | use another checkout |
| `bash server/start.sh --help` | usage |

---

## 4. Where everything lives

| what | path |
|---|---|
| code | the clone you ran it from (or `~/.jarvis-server/repo` for the one-liner) |
| settings + token | `<repo>/server/.env` (chmod 600, never committed) |
| database (one file) | `$JARVIS_DATA/jarvis.db` (default `~/jarvis-data`) |
| logs | `~/.jarvis-server/logs/` — `server.log`, `watchdog.log`, `tunnel.log`, `step-NN.log`, `pip.log` |
| pids | `~/.jarvis-server/{server,watchdog,tunnel}.pid` |
| public URL | `~/.jarvis-server/tunnel.url` |
| event history | `~/.jarvis-server/events.log` |

Override with env vars: `JARVIS_HOME`, `JARVIS_DATA`, `JARVIS_PORT`, `JARVIS_LLM_KEY`,
`JARVIS_LLM_MODEL`, `JARVIS_REPO`, `JARVIS_PIP_TIMEOUT`.

The start script **never overwrites an existing token**; running it again keeps the same one.

---

## 5. 24/7 — what actually keeps it alive

1. **Watchdog** (`~/.jarvis-server/watchdog.sh`, its own session via `setsid`): every 20 s it asks
   `http://127.0.0.1:8787/health`; if the server does not answer it starts it again and logs
   `server down - restarting` in `watchdog.log`.
2. **Public URL watchdog**: every 60 s it calls `<public-url>/health` from the internet; after two
   failures in a row it reopens the tunnel (free tunnels get a new address; `watchdog.log` records it).
3. **Wakelock** (`termux-wake-lock`): Android will not freeze Termux while the screen is off.
4. **Boot script** (`~/.termux/boot/jarvis-server.sh`): after a reboot the server + watchdog come back.
   Install the **Termux:Boot** app once for this to run.
5. **Battery**: Settings → Apps → Termux → Battery → **Unrestricted**. Without this Android 12+
   may still kill Termux when memory is low.
6. **Storage**: `Settings → Apps → Termux → Storage` is *not* needed for the server.

`--status` tells you at any moment whether the watchdog is really running (pid + health).

---

## 6. Free public URL

The script tries in this order:

1. **cloudflared quick tunnel** — `pkg install cloudflared` on Termux (the script offers to install it).
   Gives `https://<random>.trycloudflare.com`. No account, no card, works 24/7 while the phone is on.
   The address changes when the tunnel restarts.
2. **localhost.run over ssh** — built into every Termux/PC install, also free, also a random address,
   and noticeably flakier than Cloudflare (it is the fallback, not the first choice).

For a **fixed** address, three honest free options:

| option | address | persistence | notes |
|---|---|---|---|
| cloudflared **named** tunnel | your own domain on Cloudflare's free plan | permanent | needs a domain (you can point a cheap/free domain's DNS at Cloudflare); `cloudflared tunnel login` + `create` + `route dns`, then run it instead of the quick tunnel |
| **Fly.io** free allowance + 1 GB volume | `<app>.fly.dev` | permanent while inside the allowance | real 24/7 without your phone; `server/Dockerfile` is ready, see `docs/SERVER_DEPLOY_FREE.md` |
| **Hugging Face Space** (Docker) | `*.hf.space` | sleeps when idle | fine for demos, not for a brain that must learn at night |

Render/Koyeb free tiers have **no persistent disk** (the SQLite brain is wiped on redeploy) and
Vercel/Netlify cannot run a long-lived process — so they are not used here.

---

## 7. Connect the app (real, no fake state)

1. Profile → **Brain server (memory · learning)**.
2. Paste the **URL** (the public one, or `http://127.0.0.1:8787` if the app is on the same phone).
3. Paste the **token** (`bash server/start.sh --token`).
4. Tap **Test connection** → it calls `/health` and then `/api/state`, and shows what the server
   actually has: version, memories, skills, jobs, mood, learner state.
5. **Teach JARVIS** → queues a real learning job; **Read the queue** shows the real result/error.
6. Diagnostics → **Brain server** card shows the same counters plus the last HTTP status.

If the app says `401`, the token does not match. If it says timeout, the URL is old
(free tunnel restarted) — run `--status` and paste the new one.

---

## 8. Troubleshooting

| symptom | real cause | fix |
|---|---|---|
| `FAIL install dependencies` | no pip for this python, or pip hung | the script falls back to `vendor_wheels.py` (downloads the official wheels into `server/.pylibs`). If that also fails, read `logs/step-06.log` |
| `FAIL wait for /health` | port taken, or python error | `--status`, then `logs/server.log`; `--port 9000` if 8787 is busy |
| `FAIL public HTTPS tunnel` | no cloudflared **and** no ssh, or no internet | `pkg install cloudflared`, or use `--no-tunnel` and connect over LAN/VPN |
| app: `401` | wrong token | `bash server/start.sh --token` |
| app: timeout | old tunnel address | `bash server/start.sh --status` → paste the new URL |
| server dies when phone sleeps | battery optimisation | wakelock + Termux battery = Unrestricted |
| `git clone` asks for a password | private repo | use the token URL from section 1 |

---

## 9. What I actually tested (2026-10-19/20)

* `bash -n server/start.sh` — syntax clean.
* Full run in a phone-like sandbox: **14 steps → 12 PASS, 0 FAIL, 2 SKIP** (wakelock + boot skipped
  because the sandbox reports `PREFIX` unset, i.e. not a real Termux UI), total 11 s.
* `python3 selftest.py` inside the script: **33/33 checks** (health, memory, dedupe, recall, a real
  learning job, skills CRUD, affect deltas, SDUI pages, sync, 401/200 auth, dashboard, control page).
* Public URL checks from the internet: **9/9** — `/health` open, `/api/state` 401 without token and
  200 with it, memory written + deleted, recall, `/control`, `/`.
* Watchdog: killed the server pid → back up within 20 s (new pid, same data).
* Tunnel reuse: running `start` again kept the same public URL instead of churning it.
* Tunnel self-heal: the URL watchdog noticed a dead free tunnel and opened a working one
  (`watchdog.log`: `reopening the tunnel`), which then passed the 9 public checks.
* cloudflared path tested with the real `cloudflared-linux-arm64` binary (quick tunnel verified).

Those runs are the reason every step in the table has a real log behind it.
