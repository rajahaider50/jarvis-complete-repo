> **Yeh kya hai:** ek hi command jo JARVIS ka brain server install karta hai, khud test karta hai,
> background me start karta hai, mar jaaye to wapas uthata hai, public HTTPS URL deta hai, aur
> aakhir me **PASS / FAIL / SKIP table** dikhata hai — har step ka apna log.

# JARVIS brain server — one command

`server/start.sh` is the only thing you need on a phone (Termux), a PC, or a VPS.
It never fakes a step: every row is a real command with a real exit code.

---

## 1. The command (public repo — bas yeh paste karo)

Naya Termux tab kholo, `cd` ki zarurat **nahi** — jis folder me bhi ho, chalega:

```sh
curl -fsSL https://raw.githubusercontent.com/rajahaider50/jarvis-complete-repo/main/server/start.sh -o ~/jarvis-start.sh && bash ~/jarvis-start.sh
```

Pehli baar sirf itna extra (agar `curl`/`python` pehle se nahi hain):

```sh
pkg install -y curl python
```

* Script apni copy **`~/jarvis-start.sh`** me bacha leta hai, is liye aage yeh sab kahin se bhi:
  `bash ~/jarvis-start.sh --status` · `--console` · `--stop` · `--restart` · `--logs` · `--token`
* Script khud ko executable bana leti hai, is liye `~/jarvis-start.sh` bhi chal jati hai.
  (`curl -o` file ko +x ke bina banata hai — pehle `Permission denied` isi wajah se aata tha;
  ab dono chalte hain, aur safe tareeqa `bash ~/jarvis-start.sh` hai.)
* Code `~/.jarvis-server/repo` me jata hai — aapki **current directory se koi farq nahi padta**
  (na `cd`, na path ka jhanjhat).
* `git` optional hai: agar git hai to clone/pull, warna GitHub ka archive download karke
  `.env` (token) aur `.pylibs` bacha kar rakh deta hai.
* `bash <(curl ...)` bhi chalta hai, lekin kuch restricted shells (proot) me `/dev/fd` nahi hota —
  is liye `-o` wala version upar rakha hai (warna `bash: /dev/fd/63: No such file or directory`).
* `pkg install` fail ho to pehle `pkg update -y` chala kar dobara try karo.

**Private fork ho to** (token chahiye):

```sh
bash -c 'R=$HOME/jarvis-src; git clone -q https://<TOKEN>@github.com/rajahaider50/jarvis-complete-repo.git $R 2>/dev/null || git -C $R pull -q --ff-only; bash $R/server/start.sh'
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

## 3. The command center (khud khul jata hai)

Jab install green ho jata hai, script khud **command center** khol deti hai — ek live
terminal panel jisme sab kuch asli server se aata hai (koi mock nahi):

```
  ╭──────────────────────────────────────────────────╮
  │  J A R V I S  ·  brain server                    │
  │  command center                                  │
  │  v1.3.0 · q = quit · 1-9 0 = commands            │
  ╰──────────────────────────────────────────────────╯

  ● SERVER   UP      pid 20167   up 3h 12m
  ● BRAIN    v0.1.0   memories 4   skills 2   jobs 1
  ● STATE    mood calm   learner on/15s   queued 0  done 3  failed 0
  ● TUNNEL   running   url opened 12s ago

  PUBLIC URL
    https://xxxx.trycloudflare.com
  LOCAL
    http://127.0.0.1:8787   dashboard /   control /control
  TOKEN  <aapka token>
  DATA   ~/jarvis-data (24 KB)   LOGS ~/.jarvis-server/logs
```

Menu (har number asli kaam karta hai — 14 commands):

| key | command | kya hota hai |
|---|---|---|
| `1` | Status refresh | `/health` + `/api/state` dobara padhta hai (panel 10s me khud bhi refresh hota hai) |
| `2` | Links + token | app me paste karne wali URL + token, aur `/` `/control` `/health` links |
| `3` | Teach JARVIS | learning job queue karta hai; learner ~15s me memory/skill likhta hai |
| `4` | Add a memory | seedha brain me note (turant recall hota hai) |
| `5` | Jobs + results | last jobs ka status/result/error — proof learner ne kaam kiya |
| `0` | Memories | jo abhi tak sikhaya gaya hai (khali = honest empty brain) |
| `6` | Live logs | `server.log` live tail (`q` se rukta hai, server chalta rehta hai) |
| `7` | Restart server | poora `--restart` (stop → self-test → start → health → watchdog → tunnel) |
| `8` | New public URL | `--tunnel-only` (chal raha URL ho to wahi reuse hota hai) |
| `9` | Stop everything | server + watchdog + tunnel band (pehle confirm poochta hai) |
| `u` | Update + restart | naya code pull karke poora check + restart |
| `t` | Tutorial | har command ka detail — kya karta hai, kab use karna hai |
| `h` | Help | keys + shortcuts |
| `q` | Quit console | sirf panel band hota hai, **server chalta rehta hai** |

`q` dabane par server nahi rukta: server, watchdog aur tunnel apni alag session
(`setsid`/`nohup`) me chalte hain. Wapas kholne ke liye: `bash ~/jarvis-start.sh --console`.

---

## 4. All commands

| command | what it does |
|---|---|
| `bash server/start.sh` | install + self-test + start + watchdog + public URL + table + command center |
| `bash server/start.sh --console` | seedha command center (live panel + menu + tutorial) |
| `bash server/start.sh --no-console` | sirf install/table, panel nahi |
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

## 5. Where everything lives

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
`JARVIS_LLM_MODEL`, `JARVIS_REPO`, `JARVIS_PIP_TIMEOUT`, `JARVIS_TUNNEL_PROTOCOL`
(default `http2`; use `quic` for Cloudflare's default protocol).

The start script **never overwrites an existing token**; running it again keeps the same one.

---

## 6. 24/7 — what actually keeps it alive

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

## 7. Free public URL

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

## 8. Connect the app (real, no fake state)

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

## 9. Troubleshooting

| symptom | real cause | fix |
|---|---|---|
| `FAIL install dependencies` | no pip for this python, or pip hung | the script falls back to `vendor_wheels.py` (downloads the official wheels into `server/.pylibs`). If that also fails, read `logs/step-06.log` |
| `FAIL wait for /health` | port taken, or python error | `--status`, then `logs/server.log`; `--port 9000` if 8787 is busy |
| `FAIL public HTTPS tunnel` | no cloudflared **and** no ssh, or no internet | `pkg install cloudflared`, or use `--no-tunnel` and connect over LAN/VPN |
| `Permission denied` on `~/jarvis-start.sh` | `curl -o` does not set +x | use `bash ~/jarvis-start.sh ...` (v1.3.0 also chmods itself on the first run) |
| cloudflared says `printed no working URL` | on mobile data the Cloudflare precheck + edge connect can take minutes | v1.3.0 waits up to 240 s and prints `still connecting ... Ns`; if it dies it automatically falls back to the ssh tunnel |
| a public URL exists but the script "does not know it" | an older run opened the tunnel after the script gave up | v1.3.0 **recovers** it: `--tunnel-only` reads the running tunnel's log, verifies the URL and saves it (same pid, no new tunnel) |
| app: `401` | wrong token | `bash server/start.sh --token` |
| app: timeout | old tunnel address | `bash server/start.sh --status` → paste the new URL |
| the public URL dies every few minutes | that network blocks UDP/QUIC (common on mobile data) | the script already runs cloudflared with `--protocol http2`; try `JARVIS_TUNNEL_PROTOCOL=quic` if http2 is slow on your network |
| server dies when phone sleeps | battery optimisation | wakelock + Termux battery = Unrestricted |
| `git clone` asks for a password | private repo | use the token URL from section 1 |

---

## 10. What I actually tested (2026-10-19/20)

* `bash -n server/start.sh` — syntax clean.
* Full run in a phone-like sandbox: **14–15 steps → 0 FAIL**, total 11–86 s
  (wakelock + boot skipped because the sandbox is not a real Termux UI; 15 steps when git is present).
* **The public command itself**, run from `/tmp` (nowhere near the repo, fresh `$HOME`):
  `curl ... -o ~/jarvis-start.sh && bash ~/jarvis-start.sh` →
  clone + `.env` + dependencies + **33/33 self-test** + server + `/health` + watchdog + verified public
  URL, **13 PASS / 0 FAIL / 2 SKIP**, brain `memories=0 skills=0` (honest empty start).
* **9/9 public checks** on that fresh install: `/health` open, `/api/state` 401 without token and 200
  with it, memory written + deleted, recall, `/control`, `/`.
  (`/control` and the dashboard answer 200; the write test used the new token from `.env`.)
* `python3 selftest.py` inside the script: **33/33 checks** (health, memory, dedupe, recall, a real
  learning job, skills CRUD, affect deltas, SDUI pages, sync, 401/200 auth, dashboard, control page).
* Watchdog: killed the server pid → back up within 20 s (new pid, same data).
* Tunnel reuse: running `start` again kept the same public URL instead of churning it.
* Tunnel self-heal: the URL watchdog noticed a dead free tunnel and opened a working one
  (`watchdog.log`: `reopening the tunnel`), which then passed the 9 public checks.
* cloudflared path tested with the real `cloudflared-linux-arm64` binary (quick tunnel, `--protocol http2`).
* **Command center** (v1.3.0), driven in a real pty: links screen, teach → `job #2 queued`, learner
  finished it (`done 1`, memory `raw` + skill `pakistan-ka-capital-is`), jobs screen, memories screen,
  tutorial, `q` → `console closed - server, watchdog and tunnel are still running`.
* **Auto-open**: a full local install ended with `opening the command center`, showed the live URL +
  token, and `q` returned to the shell with the server still up.
* **Tunnel recovery**: deleted `tunnel.url` while a tunnel was running → `--tunnel-only` printed
  `recovered the tunnel that was already running: https://... (verified from the internet)`,
  same pid, same URL, instant.
* **ssh fallback**: with a cloudflared stub that exits immediately, the script printed its last lines,
  said `trying the free ssh tunnel instead` and produced a verified `https://....lhr.life` URL.
* **Watchdog after the fix**: `kill -9` on the server pid → new pid within 20 s, same database
  (`memories=1 skills=1` survived).

### Two real bugs found by the first phone run (2026-10-20) — both fixed in v1.3.0

1. **The run stopped right after step 14 and never printed the table.**
   `set -u` + `local boot_dir="$HOME/.termux/boot" f="$boot_dir/jarvis-server.sh"` reads `$boot_dir`
   before it exists → `boot_dir: unbound variable` → the shell exits. Proven from the phone's own
   `logs/step-14.log`. Fixed by assigning the two variables separately (and the cause is commented
   in the code so it cannot come back).
2. **The watchdog script was 0 bytes, so nothing was watching the server.**
   The generated script contains `PYTHONPATH="$PYTHONPATH"`; when `PYTHONPATH` is unset (fresh
   Termux, flask already installed) that expansion fails under `set -u`, leaving a truncated file
   while the step still reported PASS. Fixed with `PYTHONPATH="${PYTHONPATH:-}"` **and** a guard:
   an empty watchdog file or a watchdog that dies within 1 s is now a hard FAIL, never a silent pass.
3. **The tunnel looked "failed" but was actually working.** On that phone cloudflared needed
   ~2m45s (precheck 08:25:14 → connection registered 08:28:01) while the old script gave up after
   40 s. v1.3.0 waits 240 s with a heartbeat, recovers a URL that is already live, and only then
   falls back to ssh.

Those runs are the reason every step in the table has a real log behind it.
