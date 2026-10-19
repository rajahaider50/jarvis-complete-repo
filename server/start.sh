#!/usr/bin/env bash
# ==============================================================================
#  JARVIS BRAIN SERVER - one command to install, test, start and keep it alive.
#
#  Every step lands in a table with PASS / FAIL / SKIP, its own log file and the
#  first error line, so nothing is guessed:
#     1 environment          5 write .env + token      9  watchdog (auto-restart)
#     2 system packages      6 dependencies           10  Termux wakelock
#     3 git + the code       7 self-test (real API)   11  boot autostart
#     4 python version       8 start + /health check  12  public HTTPS URL
#
#  USAGE
#    bash server/start.sh                 install + test + start + verify (inside a clone)
#    bash start.sh --status               is it running? real /health answer + pids + URL
#    bash start.sh --stop                 stop server, watchdog and tunnel
#    bash start.sh --restart              stop then start again
#    bash start.sh --logs                 follow the server log
#    bash start.sh --token                print the shared token + .env path
#    bash start.sh --tunnel-only          open (or refresh) just the public URL
#    bash start.sh --no-tunnel            do not open a public URL
#    bash start.sh --no-watch             do not run the watchdog
#    bash start.sh --port 9000            use another port
#    bash start.sh --dir ~/jarvis-src     run from another checkout
#
#  One-liner (clones the repo by itself when run outside it):
#    bash <(curl -fsSL https://raw.githubusercontent.com/rajahaider50/jarvis-complete-repo/main/server/start.sh)
#
#  No root needed anywhere. On Termux it also takes the wakelock and installs the
#  boot script; on a PC/VPS those two steps are reported as SKIP.
# ==============================================================================
set -uo pipefail

SCRIPT_VERSION="1.2.0"
REPO_URL="${JARVIS_REPO:-https://github.com/rajahaider50/jarvis-complete-repo.git}"
BRANCH="${JARVIS_BRANCH:-main}"
PORT="${JARVIS_PORT:-8787}"
RUNTIME_DIR="${JARVIS_HOME:-$HOME/.jarvis-server}"
LOG_DIR="$RUNTIME_DIR/logs"
EV="$RUNTIME_DIR/events.log"
PID_FILE="$RUNTIME_DIR/server.pid"
WATCH_PID_FILE="$RUNTIME_DIR/watchdog.pid"
TUNNEL_PID_FILE="$RUNTIME_DIR/tunnel.pid"
TUNNEL_URL_FILE="$RUNTIME_DIR/tunnel.url"
WATCHDOG="$RUNTIME_DIR/watchdog.sh"
WANT_TUNNEL="ask"
WANT_WATCH=1
PIP_TIMEOUT="${JARVIS_PIP_TIMEOUT:-240}"
# cloudflared: http2 survives networks that block UDP/QUIC (common on mobile data).
# Set JARVIS_TUNNEL_PROTOCOL=quic to go back to the default protocol.
JARVIS_TUNNEL_PROTOCOL="${JARVIS_TUNNEL_PROTOCOL:-http2}"
ACTION="start"
SRCDIR=""

# ------------------------------------------------------------------ pretty output
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  R=$'\033[0m'; D=$'\033[2m'; B=$'\033[1m'; OK=$'\033[32m'; BAD=$'\033[31m'
  SK=$'\033[33m'; IN=$'\033[36m'; GD=$'\033[38;5;179m'
else
  R=""; D=""; B=""; OK=""; BAD=""; SK=""; IN=""; GD=""
fi

say()  { printf '%s\n' "$*"; }
head1(){ printf '\n%s>> %s%s\n' "$D$GD" "$*" "$R"; }
hint() { printf '%s%s%s\n' "$D" "$*" "$R"; }

# ------------------------------------------------------------------ step engine
STEP_N=0; N_PASS=0; N_FAIL=0; N_SKIP=0; T_START=$(date +%s)
TABLE=""; FAILED_STEPS=""

# run_step "<name>" <function...>  -- runs IN THIS SHELL (so steps can set globals),
# captures the output into its own log, then prints one table row.
run_step() {
  local name="$1"; shift
  STEP_N=$((STEP_N + 1))
  local n log t0 t1 ms rc status detail color lastline ln
  n=$(printf '%02d' "$STEP_N")
  log="$LOG_DIR/step-$n.log"
  t0=$(date +%s%N)
  "$@" >"$log" 2>&1; rc=$?
  t1=$(date +%s%N); ms=$(( (t1 - t0) / 1000000 ))
  case $rc in
    0) status=PASS; color="$OK"; N_PASS=$((N_PASS + 1));;
    3) status=SKIP; color="$SK";  N_SKIP=$((N_SKIP + 1));;
    *) status=FAIL; color="$BAD"; N_FAIL=$((N_FAIL + 1)); FAILED_STEPS+="$n|$name|$rc|$log"$'\n';;
  esac
  lastline=""
  while IFS= read -r ln; do [ -n "${ln// /}" ] && lastline="$ln"; done < "$log" 2>/dev/null
  detail="${lastline:0:70}"
  [ -z "$detail" ] && detail="-"
  detail="${detail//|/ }"
  TABLE+="$n|$name|$status|$ms|$detail"$'\n'
  printf '  %s%s%s %s%-4s%s %s%-36s%s %s%6s ms%s  %s\n' \
    "$GD" "$n" "$R" "$color" "$status" "$R" "$B" "$name" "$R" "$D" "$ms" "$R" "$detail"
}

skip() { printf '%s\n' "$*"; return 3; }

# ------------------------------------------------------------------ helpers
is_termux() { case "${PREFIX:-}" in *com.termux*) return 0;; esac; return 1; }

py() {                            # python3, using the vendored libs when they exist
  if [ -n "${SERVER_DIR:-}" ] && [ -d "$SERVER_DIR/.pylibs" ]; then
    PYTHONPATH="$SERVER_DIR/.pylibs${PYTHONPATH:+:$PYTHONPATH}" python3 "$@"
  else
    python3 "$@"
  fi
}

health_ok() {
  python3 - "$PORT" <<'PY' >/dev/null 2>&1
import json, sys, urllib.request
try:
    with urllib.request.urlopen(f"http://127.0.0.1:{sys.argv[1]}/health", timeout=5) as r:
        sys.exit(0 if json.loads(r.read().decode()).get("ok") else 1)
except Exception:
    sys.exit(1)
PY
}

health_json() {
  python3 - "$PORT" <<'PY'
import json, sys, urllib.request
try:
    with urllib.request.urlopen(f"http://127.0.0.1:{sys.argv[1]}/health", timeout=5) as r:
        d = json.loads(r.read().decode())
    print(f"v{d['version']} memories={d['memories']} skills={d['skills']} jobs={d['jobs_queued']} mood={d['mood']}")
except Exception as e:
    print(f"not reachable ({type(e).__name__})")
PY
}

load_env() {
  if [ -f "$SERVER_DIR/.env" ]; then set -a; . "$SERVER_DIR/.env"; set +a; fi
  export JARVIS_PORT="$PORT"
}

py_path_for_child() {
  if [ -d "$SERVER_DIR/.pylibs" ]; then
    export PYTHONPATH="$SERVER_DIR/.pylibs${PYTHONPATH:+:$PYTHONPATH}"
  fi
}

pid_alive() { [ -n "${1:-}" ] && kill -0 "$1" 2>/dev/null; }
read_pid()  { [ -f "$1" ] && head -1 "$1" 2>/dev/null; }

# Start a fully detached background process: new session (setsid) where available,
# nohup otherwise, stdin closed, output appended to the given log. Returns its pid.
spawn() {
  local log="$1"; shift
  if command -v setsid >/dev/null 2>&1; then
    setsid "$@" >>"$log" 2>&1 </dev/null &
  else
    nohup "$@" >>"$log" 2>&1 </dev/null &
  fi
  echo $!
}

stop_thing() {
  local pf="$1" name="$2" pid
  pid="$(read_pid "$pf")"
  if pid_alive "$pid"; then
    kill "$pid" 2>/dev/null
    local i; for i in 1 2 3 4 5 6 7 8 9 10; do pid_alive "$pid" || break; sleep 0.3; done
    pid_alive "$pid" && kill -9 "$pid" 2>/dev/null
    say "stopped $name (pid $pid)"
  else
    say "$name was not running"
  fi
  rm -f "$pf"
}

event() { printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >> "$EV"; }

# =========================================================================== STEPS
s_environment() {
  mkdir -p "$LOG_DIR" "$RUNTIME_DIR" || return 1
  say "host        $(uname -srm)"
  say "user        ${USER:-$(id -un 2>/dev/null || echo unknown)}"
  say "mode        $MODE   port $PORT"
  say "runtime dir $RUNTIME_DIR"
  say "logs        $LOG_DIR"
  if is_termux; then say "termux      yes ($PREFIX)"; else say "termux      no (PC/VPS: wakelock + boot steps are skipped)"; fi
  event "start v$SCRIPT_VERSION mode=$MODE port=$PORT"
  return 0
}

s_packages() {
  if is_termux; then
    local need=""
    command -v git     >/dev/null 2>&1 || need="$need git"
    command -v python3 >/dev/null 2>&1 || need="$need python"
    command -v cloudflared >/dev/null 2>&1 || say "cloudflared not installed (optional: 'pkg install cloudflared' for a clean public URL)"
    if [ -n "$need" ]; then
      say "installing Termux packages:$need"
      pkg install -y $need || return 1
    fi
    say "python: $(python3 -V 2>&1)"
    say "git: $(git --version 2>/dev/null)"
    say "ready"
  else
    command -v python3 >/dev/null 2>&1 || { say "python3 is required and is not in PATH"; return 1; }
    say "python3: $(python3 -V 2>&1)"
    command -v git >/dev/null 2>&1 && say "git: $(git --version)" || say "git: missing (only needed to fetch the code)"
    command -v cloudflared >/dev/null 2>&1 && say "cloudflared: present" || say "cloudflared: missing (optional)"
    say "ready"
  fi
  return 0
}

s_git() {
  if ! command -v git >/dev/null 2>&1; then skip "git not installed"; return 3; fi
  say "$(git --version)"
  return 0
}

s_use_local() {
  [ -d "$SERVER_DIR/jarvis_server" ] || { say "$SERVER_DIR has no jarvis_server directory"; return 1; }
  say "using the checkout this script lives in:"
  say "$SERVER_DIR"
  return 0
}

s_fetch() {
  mkdir -p "$RUNTIME_DIR" || return 1
  local dest="$RUNTIME_DIR/repo"
  if [ -d "$dest/.git" ]; then
    say "updating $dest"
    ( cd "$dest" && git fetch --depth 1 origin "$BRANCH" -q && git reset --hard "origin/$BRANCH" -q ) || {
      say "git update failed (network, or a private repo needs a token)"; return 1; }
  else
    say "cloning $REPO_URL (branch $BRANCH)"
    git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$dest" -q || {
      say "clone failed - if the repo is private use:"
      say "  JARVIS_REPO=https://<user>:<token>@github.com/rajahaider50/jarvis-complete-repo.git bash start.sh"
      return 1; }
  fi
  SERVER_DIR="$dest/server"
  REPO_ROOT="$dest"
  [ -d "$SERVER_DIR/jarvis_server" ] || { say "the clone has no server/jarvis_server - wrong repo?"; return 1; }
  say "code ready at $SERVER_DIR"
  return 0
}

s_python() {
  python3 -c 'import sys;print("python %d.%d.%d"%sys.version_info[:3])'
  python3 -c 'import sys; sys.exit(0 if sys.version_info[:2] >= (3, 9) else 1)' || { say "python 3.9+ is required"; return 1; }
  return 0
}

s_dotenv() {
  local env_file="$SERVER_DIR/.env" tok=""
  if [ -f "$env_file" ]; then tok="$(sed -n 's/^JARVIS_TOKEN=//p' "$env_file")"; tok="${tok%%$'\n'*}"; fi
  if [ -z "$tok" ]; then
    tok="$(python3 -c 'import secrets;print(secrets.token_urlsafe(32))')" || return 1
    say "new token generated (searchable again any time with: bash start.sh --token)"
  else
    say "keeping the existing token from $env_file"
  fi
  {
    echo "# JARVIS brain server - written by start.sh v$SCRIPT_VERSION"
    echo "JARVIS_DATA=${JARVIS_DATA:-$HOME/jarvis-data}"
    echo "JARVIS_TOKEN=$tok"
    echo "JARVIS_LEARNER=${JARVIS_LEARNER:-1}"
    echo "JARVIS_LEARNER_INTERVAL=${JARVIS_LEARNER_INTERVAL:-15}"
    echo "JARVIS_LLM_BASE=${JARVIS_LLM_BASE:-https://generativelanguage.googleapis.com/v1beta/openai}"
    echo "JARVIS_LLM_KEY=${JARVIS_LLM_KEY:-}"
    echo "JARVIS_LLM_MODEL=${JARVIS_LLM_MODEL:-gemini-2.0-flash}"
  } > "$env_file" || return 1
  chmod 600 "$env_file" 2>/dev/null
  mkdir -p "${JARVIS_DATA:-$HOME/jarvis-data}"
  say "wrote $env_file (mode 600) - data dir ${JARVIS_DATA:-$HOME/jarvis-data}"
  if [ -n "${JARVIS_LLM_KEY:-}" ]; then say "learning model key: set"; else say "no JARVIS_LLM_KEY: learning keeps honest raw notes instead of inventing lessons"; fi
  return 0
}

s_deps() {
  cd "$SERVER_DIR" || return 1
  if [ -f "$SERVER_DIR/.pylibs/flask/__init__.py" ]; then
    say "using the vendor libraries already in $SERVER_DIR/.pylibs (no download needed)"
    return 0
  fi
  if python3 -c 'import flask' 2>/dev/null; then
    say "flask already available ($(python3 -c 'import flask;print(flask.__version__)'))"
    return 0
  fi
  if command -v pip >/dev/null 2>&1; then
    say "trying pip install -r requirements.txt (limit ${PIP_TIMEOUT}s)"
    if command -v timeout >/dev/null 2>&1; then
      timeout "$PIP_TIMEOUT" pip install -q --disable-pip-version-check -r requirements.txt >>"$LOG_DIR/pip.log" 2>&1 \
        || say "pip did not finish in ${PIP_TIMEOUT}s (or failed) - see $LOG_DIR/pip.log"
    else
      pip install -q --disable-pip-version-check -r requirements.txt >>"$LOG_DIR/pip.log" 2>&1 || true
    fi
    if python3 -c 'import flask' 2>/dev/null; then say "installed with pip"; return 0; fi
    say "pip did not provide flask - using the bundled wheel downloader instead"
  else
    say "pip not found - using the bundled wheel downloader"
  fi
  py vendor_wheels.py --dir "$SERVER_DIR/.pylibs" || return 1
  py -c 'import flask' || return 1
  say "flask ready from .pylibs"
  return 0
}

s_selftest() {
  cd "$SERVER_DIR" || return 1
  py selftest.py || return 1
  return 0
}

s_free_port() {
  [ -f "$WATCH_PID_FILE" ] && stop_thing "$WATCH_PID_FILE" "old watchdog"
  if health_ok; then
    say "an instance already answers on port $PORT - stopping it"
    stop_thing "$PID_FILE" "old server"
    sleep 1
  fi
  if [ -f "$TUNNEL_PID_FILE" ] && ! pid_alive "$(read_pid "$TUNNEL_PID_FILE")"; then
    rm -f "$TUNNEL_PID_FILE" "$TUNNEL_URL_FILE"
  else
    [ -s "$TUNNEL_URL_FILE" ] && say "keeping the open tunnel: $(cat "$TUNNEL_URL_FILE")"
  fi
  say "port $PORT ready"
  return 0
}

s_start() {
  cd "$SERVER_DIR" || return 1
  load_env; py_path_for_child
  local pid
  pid="$(spawn "$LOG_DIR/server.log" python3 -u -m jarvis_server "$PORT")"
  echo "$pid" > "$PID_FILE"
  say "pid $pid -> $LOG_DIR/server.log"
  event "server start pid $pid"
  return 0
}

s_health() {
  local i
  for i in $(seq 1 40); do
    health_ok && { say "healthy - $(health_json)"; return 0; }
    sleep 0.5
  done
  say "no answer on http://127.0.0.1:$PORT/health after 20s"
  say "--- last lines of $LOG_DIR/server.log ---"
  tail -n 12 "$LOG_DIR/server.log" 2>/dev/null
  event "health FAIL"
  return 1
}

s_watchdog() {
  if [ "$WANT_WATCH" != 1 ]; then skip "disabled with --no-watch"; return 3; fi
  cat > "$WATCHDOG" <<EOF
#!/usr/bin/env bash
# auto-generated by start.sh - keeps the server (and the public tunnel) alive
export JARVIS_HOME="$RUNTIME_DIR"
PORT=$PORT
SERVER_DIR=$SERVER_DIR
PYTHONPATH="$PYTHONPATH"
export PYTHONPATH
LOG="$LOG_DIR/watchdog.log"
SERVER_LOG="$LOG_DIR/server.log"
PID_FILE="$PID_FILE"
TUNNEL_PID_FILE="$TUNNEL_PID_FILE"
TUNNEL_URL_FILE="$TUNNEL_URL_FILE"
N=0
TUNNEL_FAILS=0
[ -f "\$SERVER_DIR/.env" ] && { set -a; . "\$SERVER_DIR/.env"; set +a; }
cd "\$SERVER_DIR" || exit 1
while true; do
  if ! python3 - "\$PORT" <<'PY' >/dev/null 2>&1
import json, sys, urllib.request
try:
    with urllib.request.urlopen(f"http://127.0.0.1:{sys.argv[1]}/health", timeout=6) as r:
        sys.exit(0 if json.loads(r.read().decode()).get("ok") else 1)
except Exception:
    sys.exit(1)
PY
  then
    echo "\$(date '+%Y-%m-%d %H:%M:%S') server down - restarting" >> "\$LOG"
    nohup python3 -u -m jarvis_server "\$PORT" >> "\$SERVER_LOG" 2>&1 &
    echo \$! > "\$PID_FILE"
    sleep 4
  fi
  N=\$((N + 1))
  if [ "\$N" -ge 3 ] && [ -s "\$TUNNEL_URL_FILE" ]; then
    N=0
    if python3 - "\$(cat "\$TUNNEL_URL_FILE")" <<'PY' >/dev/null 2>&1
import json, sys, urllib.request
try:
    with urllib.request.urlopen(sys.argv[1].rstrip("/") + "/health", timeout=15) as r:
        sys.exit(0 if json.loads(r.read().decode()).get("ok") else 1)
except Exception:
    sys.exit(1)
PY
    then
      TUNNEL_FAILS=0
    else
      TUNNEL_FAILS=\$((TUNNEL_FAILS + 1))
      echo "\$(date '+%Y-%m-%d %H:%M:%S') public URL check failed (\$TUNNEL_FAILS/2)" >> "\$LOG"
      if [ "\$TUNNEL_FAILS" -ge 2 ]; then
        TUNNEL_FAILS=0
        echo "\$(date '+%Y-%m-%d %H:%M:%S') reopening the tunnel (the URL may change)" >> "\$LOG"
        bash "\$SERVER_DIR/start.sh" --tunnel-only --port "\$PORT" >> "\$LOG" 2>&1
      fi
    fi
  fi
  sleep 20
done
EOF
  chmod +x "$WATCHDOG"
  local wpid
  wpid="$(spawn "$LOG_DIR/watchdog.log" bash "$WATCHDOG")"
  echo "$wpid" > "$WATCH_PID_FILE"
  say "pid $wpid - restarts the server within ~20s if it dies"
  event "watchdog pid $wpid"
  return 0
}

s_wakelock() {
  if ! is_termux; then skip "not Termux - nothing to hold awake here"; return 3; fi
  if ! command -v termux-wake-lock >/dev/null 2>&1; then skip "termux-wake-lock missing (pkg install termux-api)"; return 3; fi
  termux-wake-lock && { say "wakelock acquired - Android will not freeze the server"; return 0; }
  return 1
}

s_boot() {
  if ! is_termux; then skip "not Termux - on a PC/VPS use systemd or pm2 (see docs/SERVER_DEPLOY_FREE.md)"; return 3; fi
  local boot_dir="$HOME/.termux/boot" f="$boot_dir/jarvis-server.sh"
  mkdir -p "$boot_dir" || return 1
  cat > "$RUNTIME_DIR/run-now.sh" <<EOF
#!/usr/bin/env bash
export PYTHONPATH="$SERVER_DIR/.pylibs"
cd "$SERVER_DIR" || exit 1
[ -f .env ] && { set -a; . ./.env; set +a; }
nohup python3 -u -m jarvis_server "$PORT" >> "$LOG_DIR/server.log" 2>&1 &
echo \$! > "$PID_FILE"
nohup bash "$WATCHDOG" >> "$LOG_DIR/watchdog.log" 2>&1 &
echo \$! > "$WATCH_PID_FILE"
EOF
  cat > "$f" <<EOF
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock 2>/dev/null
bash "$RUNTIME_DIR/run-now.sh" >> "$LOG_DIR/boot.log" 2>&1
EOF
  chmod +x "$f" "$RUNTIME_DIR/run-now.sh"
  say "written $f - install the Termux:Boot app once and the server returns after every reboot"
  return 0
}

public_health_ok() {               # GET <url>/health through the internet - real proof the tunnel works
  python3 - "$1" <<'PY' >/dev/null 2>&1
import json, sys, urllib.request
url = sys.argv[1].rstrip("/")
try:
    req = urllib.request.Request(url + "/health", headers={"User-Agent": "jarvis-start/1.1"})
    with urllib.request.urlopen(req, timeout=15) as r:
        sys.exit(0 if json.loads(r.read().decode()).get("ok") else 1)
except Exception:
    sys.exit(1)
PY
}

s_tunnel() {
  if [ "$WANT_TUNNEL" = 0 ]; then skip "disabled with --no-tunnel"; return 3; fi
  if [ -s "$TUNNEL_URL_FILE" ] && public_health_ok "$(cat "$TUNNEL_URL_FILE")"; then
    say "the public URL is already answering: $(cat "$TUNNEL_URL_FILE")"; return 0
  fi
  if [ -f "$TUNNEL_PID_FILE" ]; then stop_thing "$TUNNEL_PID_FILE" "old tunnel" >/dev/null 2>&1; fi
  local i url tpid
  if ! command -v cloudflared >/dev/null 2>&1 && is_termux; then
    say "cloudflared is not installed - installing it once for a sturdier tunnel"
    pkg install -y cloudflared >/dev/null 2>&1 || say "install did not work - using the ssh tunnel instead"
  fi
  if command -v cloudflared >/dev/null 2>&1; then
    say "opening a cloudflared quick tunnel"
    : > "$LOG_DIR/tunnel.log"
    local -a cfargs
    cfargs=(tunnel --no-autoupdate)
    if [ -n "${JARVIS_TUNNEL_PROTOCOL:-}" ]; then
      cfargs+=(--protocol "$JARVIS_TUNNEL_PROTOCOL")
      say "cloudflared protocol: $JARVIS_TUNNEL_PROTOCOL"
    fi
    cfargs+=(--url "http://127.0.0.1:$PORT")
    tpid="$(spawn "$LOG_DIR/tunnel.log" cloudflared "${cfargs[@]}")"
    echo "$tpid" > "$TUNNEL_PID_FILE"
    for i in $(seq 1 40); do
      url="$(awk 'match($0,/https:\/\/[a-z0-9-]+\.trycloudflare\.com/){u=substr($0,RSTART,RLENGTH)} END{print u}' "$LOG_DIR/tunnel.log" 2>/dev/null)"
      if [ -n "$url" ] && public_health_ok "$url"; then
        printf '%s\n' "$url" > "$TUNNEL_URL_FILE"; say "public URL $url (verified from the internet)"; event "tunnel $url"; return 0
      fi
      sleep 1
    done
    say "cloudflared printed no working URL - see $LOG_DIR/tunnel.log"
    tail -n 6 "$LOG_DIR/tunnel.log" 2>/dev/null
    return 1
  fi
  if command -v ssh >/dev/null 2>&1; then
    say "using the free localhost.run tunnel (cloudflared is sturdier: pkg install cloudflared)"
    : > "$LOG_DIR/tunnel.log"
    tpid="$(spawn "$LOG_DIR/tunnel.log" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
      -o ServerAliveInterval=20 -o ExitOnForwardFailure=yes -R 80:localhost:"$PORT" nokey@localhost.run)"
    echo "$tpid" > "$TUNNEL_PID_FILE"
    for i in $(seq 1 60); do
      url="$(awk 'match($0,/https:\/\/[a-z0-9-]+\.lhr\.life/){u=substr($0,RSTART,RLENGTH)} END{print u}' "$LOG_DIR/tunnel.log" 2>/dev/null)"
      [ -z "$url" ] && url="$(awk 'match($0,/https:\/\/[a-z0-9-]+\.localhost\.run/){u=substr($0,RSTART,RLENGTH)} END{print u}' "$LOG_DIR/tunnel.log" 2>/dev/null)"
      case "$url" in *admin*) url="";; esac
      if [ -n "$url" ] && public_health_ok "$url"; then
        printf '%s\n' "$url" > "$TUNNEL_URL_FILE"; say "public URL $url (verified from the internet)"; event "tunnel $url"; return 0
      fi
      sleep 1
    done
    say "the tunnel started but no address answered /health - see $LOG_DIR/tunnel.log"
    return 1
  fi
  skip "no cloudflared and no ssh - install one for a public URL (Termux: pkg install cloudflared)"; return 3
}

# =========================================================================== OUTPUT
summary_table() {
  local total=$((N_PASS + N_FAIL + N_SKIP)) elapsed=$(( $(date +%s) - T_START ))
  head1 "RESULT TABLE"
  printf '  %s┌──────┬──────┬──────────────────────────────────────┬───────────┬──────────────────────────────────────┐%s\n' "$D" "$R"
  printf '  %s│ %-4s │ %-4s │ %-36s │ %-9s │ %-36s │%s\n' "$D" "STEP" "ST." "WHAT" "TIME" "DETAIL" "$R"
  printf '  %s├──────┼──────┼──────────────────────────────────────┼───────────┼──────────────────────────────────────┤%s\n' "$D" "$R"
  local n name status ms detail color
  while IFS='|' read -r n name status ms detail; do
    [ -z "${n:-}" ] && continue
    case "$status" in PASS) color="$OK";; FAIL) color="$BAD";; *) color="$SK";; esac
    printf '  %s│%s %s%-4s%s %s│%s %s%-4s%s %s│%s %-36.36s %s│%s %8s ms %s│%s %-36.36s %s│%s\n' \
      "$D" "$R" "$GD" "$n" "$R" "$D" "$R" "$color" "$status" "$R" "$D" "$R" "$name" "$D" "$R" "$ms" "$D" "$R" "$detail" "$D" "$R"
  done <<< "$TABLE"
  printf '  %s└──────┴──────┴──────────────────────────────────────┴───────────┴──────────────────────────────────────┘%s\n' "$D" "$R"
  printf '\n  %sTOTAL%s %d   %sPASS%s %d   %sFAIL%s %d   %sSKIP%s %d   %stime%s %ds\n' \
    "$B" "$R" "$total" "$OK" "$R" "$N_PASS" "$BAD" "$R" "$N_FAIL" "$SK" "$R" "$N_SKIP" "$D" "$R" "$elapsed"

  if [ "$N_FAIL" -gt 0 ]; then
    head1 "WHAT TO FIX"
    local rc log
    while IFS='|' read -r n name rc log; do
      [ -z "${n:-}" ] && continue
      printf '  %sFAIL%s step %s - %s (exit %s)\n' "$BAD" "$R" "$n" "$name" "$rc"
      grep -iEm3 'error|exception|traceback|no module|refused|denied|not found|failed' "$log" 2>/dev/null
      printf '       full log: %s\n' "$log"
    done <<< "$FAILED_STEPS"
  fi
}

final_notes() {
  local tok="" url="" data
  if [ -f "$SERVER_DIR/.env" ]; then tok="$(sed -n 's/^JARVIS_TOKEN=//p' "$SERVER_DIR/.env")"; tok="${tok%%$'\n'*}"; fi
  [ -s "$TUNNEL_URL_FILE" ] && url="$(cat "$TUNNEL_URL_FILE")"
  if [ -f "$SERVER_DIR/.env" ]; then data="$(sed -n 's/^JARVIS_DATA=//p' "$SERVER_DIR/.env")"; data="${data%%$'\n'*}"; fi

  head1 "SERVER"
  printf '  %-12s %s\n' "local"  "http://127.0.0.1:$PORT    dashboard /    control /control"
  [ -n "$url" ] && printf '  %-12s %s%s%s\n' "public" "$IN" "$url" "$R"
  printf '  %-12s %s\n' "health" "$(health_json)"
  printf '  %-12s %s\n' "token" "${tok:-(none)}"
  printf '  %-12s %s\n' "data" "${data:-?}   (one SQLite file: jarvis.db)"
  printf '  %-12s %s\n' "code" "$SERVER_DIR"
  printf '  %-12s %s\n' "logs" "$LOG_DIR   (server.log, watchdog.log, tunnel.log, step-*.log)"

  head1 "IN THE APP  (Profile > Brain server)"
  printf '  URL    %s\n' "${url:-http://127.0.0.1:$PORT}"
  printf '  token  %s\n' "${tok:-(run: bash start.sh --token)}"
  hint "  press TEST CONNECTION - it shows the real version and counts from this server."

  head1 "KEEP IT RUNNING 24/7"
  say "  - watchdog is on: if the server ever stops it is back within ~20s (log: watchdog.log)"
  if is_termux; then
    say "  - wakelock is on: Android will not freeze Termux while the screen is off"
    say "  - boot script installed: install the Termux:Boot app once and it survives reboots"
    say "  - one manual step: Settings > Apps > Termux > Battery > Unrestricted"
    say "  - the public URL is re-opened automatically if the tunnel drops (the URL changes; see docs/SERVER_DEPLOY_FREE.md for a fixed one)"
  else
    say "  - this machine must stay on; for a real always-on host see docs/SERVER_DEPLOY_FREE.md"
  fi
  say "  - stop: --stop   status: --status   restart: --restart   logs: --logs"
  printf '\n  %sresult%s %s\n\n' "$B" "$R" \
    "$( [ "$N_FAIL" -eq 0 ] && printf '%sALL GOOD%s - %d passed, %d skipped, 0 failed' "$OK$B" "$R" "$N_PASS" "$N_SKIP" \
       || printf '%s%d STEP(S) FAILED%s - see WHAT TO FIX above' "$BAD$B" "$N_FAIL" "$R" )"
}

do_tunnel_only() {
  mkdir -p "$LOG_DIR"
  s_tunnel
  local rc=$?
  [ "$rc" = 0 ] && cat "$TUNNEL_URL_FILE"
  return $rc
}

do_status() {
  head1 "STATUS"
  if health_ok; then printf '  %-12s %sUP%s - %s\n' "health" "$OK" "$R" "$(health_json)"; else printf '  %-12s %sDOWN%s\n' "health" "$BAD" "$R"; fi
  printf '  %-12s %s (pid %s)\n' "server"   "$(pid_alive "$(read_pid "$PID_FILE")"        && echo running || echo stopped)" "$(read_pid "$PID_FILE"        || echo -)"
  printf '  %-12s %s (pid %s)\n' "watchdog" "$(pid_alive "$(read_pid "$WATCH_PID_FILE")"  && echo running || echo stopped)" "$(read_pid "$WATCH_PID_FILE"  || echo -)"
  printf '  %-12s %s (pid %s)\n' "tunnel"   "$(pid_alive "$(read_pid "$TUNNEL_PID_FILE")" && echo running || echo stopped)" "$(read_pid "$TUNNEL_PID_FILE" || echo -)"
  [ -s "$TUNNEL_URL_FILE" ] && printf '  %-12s %s\n' "public" "$(cat "$TUNNEL_URL_FILE")"
  printf '  %-12s %s\n' "local" "http://127.0.0.1:$PORT"
  local stok=""; stok="$(sed -n 's/^JARVIS_TOKEN=//p' "${SERVER_DIR:-$RUNTIME_DIR}/.env" 2>/dev/null)"; printf '  %-12s %s\n' "token" "${stok%%$'\n'*}"
  say ""
  say "last log lines:"
  tail -n 8 "$LOG_DIR/server.log" 2>/dev/null
}

do_stop() {
  head1 "STOPPING"
  stop_thing "$WATCH_PID_FILE" "watchdog"
  stop_thing "$TUNNEL_PID_FILE" "tunnel"
  stop_thing "$PID_FILE" "server"
  rm -f "$TUNNEL_URL_FILE"
  event "stopped"
  say "done"
}

# =========================================================================== MAIN
while [ $# -gt 0 ]; do
  case "$1" in
    --status)    ACTION=status;;
    --stop)      ACTION=stop;;
    --restart)   ACTION=restart;;
    --logs)      ACTION=logs;;
    --token)     ACTION=token;;
    --tunnel)    WANT_TUNNEL=1;;
    --tunnel-only) ACTION=tunnelonly;;
    --no-tunnel) WANT_TUNNEL=0;;
    --no-watch)  WANT_WATCH=0;;
    --port)      shift; PORT="${1:-8787}";;
    --dir)       shift; SRCDIR="${1:-}";;
    --help|-h)   ACTION=help;;
    *) : ;;
  esac
  shift || break
done

# where does this script live, and which checkout do we serve?
SELF="${BASH_SOURCE[0]:-}"
SERVER_DIR=""; REPO_ROOT=""; MODE=fetch
if [ -n "$SRCDIR" ]; then
  if [ -d "$SRCDIR/jarvis_server" ]; then SERVER_DIR="$SRCDIR"
  elif [ -d "$SRCDIR/server/jarvis_server" ]; then SERVER_DIR="$SRCDIR/server"
  fi
  [ -n "$SERVER_DIR" ] && { REPO_ROOT="$(cd "$SERVER_DIR/.." && pwd)"; MODE=local; }
else
  if [ -n "$SELF" ] && [ -f "$SELF" ]; then
    HERE="$(cd "$(dirname "$SELF")" 2>/dev/null && pwd)"
    if [ -n "$HERE" ] && [ -d "$HERE/jarvis_server" ]; then
      SERVER_DIR="$HERE"; REPO_ROOT="$(cd "$HERE/.." && pwd)"; MODE=local
    fi
  fi
fi
if [ "$MODE" = fetch ]; then
  SERVER_DIR="$RUNTIME_DIR/repo/server"
  REPO_ROOT="$RUNTIME_DIR/repo"
  [ -d "$SERVER_DIR/jarvis_server" ] && MODE=local
fi

mkdir -p "$LOG_DIR"

case "$ACTION" in
  help)  awk 'NR>=2 && NR<=32 {sub(/^# ?/,""); print}' "$SELF" 2>/dev/null; exit 0;;
  status) do_status; exit 0;;
  stop)   do_stop;   exit 0;;
  tunnelonly) do_tunnel_only; exit $?;;
  logs)   tail -n 200 -f "$LOG_DIR/server.log"; exit 0;;
  token)  t="$(sed -n 's/^JARVIS_TOKEN=//p' "$SERVER_DIR/.env" 2>/dev/null)"; printf '%s\n' "${t%%$'\n'*}"; exit 0;;
esac

printf '\n%s  ────────────────────────────────────────────────────────────────────────%s\n' "$D" "$R"
printf '  %s%sJ A R V I S%s   ·   brain server   ·   install + run + keep alive    %sv%s%s\n' "$GD" "$B" "$R" "$D" "$SCRIPT_VERSION" "$R"
printf '%s  ────────────────────────────────────────────────────────────────────────%s\n' "$D" "$R"
hint "  install -> self-test -> start -> watchdog -> public URL -> result table"

[ "$ACTION" = restart ] && do_stop

head1 "1) ENVIRONMENT";     run_step "environment"             s_environment
head1 "2) PACKAGES";        run_step "system packages"         s_packages
head1 "3) CODE"
if [ "$MODE" = fetch ]; then
  run_step "git available"    s_git
  run_step "fetch the code"   s_fetch
else
  run_step "use this checkout" s_use_local
fi
head1 "4) SETTINGS";        run_step "python version"          s_python
run_step "write .env + token" s_dotenv
head1 "5) DEPENDENCIES";    run_step "install dependencies"    s_deps
head1 "6) SELF-TEST";       run_step "real API self-test"      s_selftest
head1 "7) START";           run_step "free port $PORT"         s_free_port
run_step "start the server"   s_start
run_step "wait for /health"   s_health
run_step "watchdog"           s_watchdog
head1 "8) ALWAYS-ON";       run_step "Termux wakelock"         s_wakelock
run_step "boot autostart"     s_boot
head1 "9) PUBLIC URL";      run_step "public HTTPS tunnel"     s_tunnel

summary_table
final_notes
[ "$N_FAIL" -eq 0 ] || exit 1
exit 0
