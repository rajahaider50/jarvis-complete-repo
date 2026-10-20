#!/usr/bin/env bash
# ==============================================================================
#  JARVIS BRAIN SERVER - COMMAND CENTER (terminal UI, no extra packages needed)
#
#  Opens with:  bash ~/jarvis-start.sh --console
#  It shows the live truth every few seconds: health, pids, uptime, memories,
#  skills, jobs, mood, learner, local URL, public URL, token, data + log paths.
#  The menu runs real API calls and the real start.sh actions - nothing is faked.
#
#  Closing the console (q) does NOT stop the server: the server, the watchdog
#  and the tunnel live in their own session (setsid/nohup), not in this terminal.
# ==============================================================================
set -uo pipefail

RUNTIME="${JARVIS_HOME:-$HOME/.jarvis-server}"
PORT="${JARVIS_PORT:-8787}"
SERVER_DIR="${JARVIS_SERVER_DIR:-$RUNTIME/repo/server}"
START_SH="${JARVIS_START_SH:-$HOME/jarvis-start.sh}"
LOG_DIR="$RUNTIME/logs"
PID_FILE="$RUNTIME/server.pid"
WATCH_PID_FILE="$RUNTIME/watchdog.pid"
TUNNEL_PID_FILE="$RUNTIME/tunnel.pid"
TUNNEL_URL_FILE="$RUNTIME/tunnel.url"
TOK=""
DATA=""
[ -f "$SERVER_DIR/.env" ] && {
  TOK="$(sed -n 's/^JARVIS_TOKEN=//p' "$SERVER_DIR/.env" 2>/dev/null)"; TOK="${TOK%%$'\n'*}"
  DATA="$(sed -n 's/^JARVIS_DATA=//p' "$SERVER_DIR/.env" 2>/dev/null)"; DATA="${DATA%%$'\n'*}"
}
[ -z "$DATA" ] && DATA="$HOME/jarvis-data"
[ -f "$START_SH" ] || START_SH="$SERVER_DIR/start.sh"

# ------------------------------------------------------------------ colours
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  R=$'\033[0m'; B=$'\033[1m'; D=$'\033[38;5;244m'
  GOLD=$'\033[38;5;179m'; TEAL=$'\033[38;5;73m'; GREEN=$'\033[38;5;35m'
  RED=$'\033[38;5;203m'; AMBER=$'\033[38;5;214m'; WHITE=$'\033[38;5;253m'
  RULE=$'\033[38;5;240m'; CLEAR=$'\033[H\033[2J'
else
  R=""; B=""; D=""; GOLD=""; TEAL=""; GREEN=""; RED=""; AMBER=""; WHITE=""; RULE=""; CLEAR=""
fi

# ------------------------------------------------------------------ helpers
pid_alive() { [ -n "${1:-}" ] && kill -0 "$1" 2>/dev/null; }
read_pid()  { local p=""; [ -f "$1" ] && p="$(cat "$1" 2>/dev/null)"; printf '%s' "${p//[^0-9]/}"; }
rule()      { printf '  %s%s%s\n' "$RULE" "$(printf '─%.0s' $(seq 1 "${1:-52}"))" "$R"; }

fmt_dur() {                       # seconds -> 3h 12m / 12m 04s
  local s="${1:-0}"
  if   [ "$s" -ge 86400 ]; then printf '%dd %dh' $((s/86400)) $((s%86400/3600))
  elif [ "$s" -ge 3600 ];  then printf '%dh %02dm' $((s/3600)) $((s%3600/60))
  elif [ "$s" -ge 60 ];    then printf '%dm %02ds' $((s/60)) $((s%60))
  else printf '%ds' "$s"; fi
}

age_of() {                        # seconds since a file was written
  [ -f "$1" ] || { printf '?'; return; }
  local m; m="$(stat -c %Y "$1" 2>/dev/null || echo 0)"
  [ "$m" = 0 ] && { printf '?'; return; }
  printf '%s ago' "$(fmt_dur $(( $(date +%s) - m )))"
}

human_size() {
  [ -f "$1" ] || { printf '-'; return; }
  local b; b="$(stat -c %s "$1" 2>/dev/null || echo 0)"
  if [ "$b" -ge 1048576 ]; then printf '%d MB' $((b/1048576))
  elif [ "$b" -ge 1024 ]; then printf '%d KB' $((b/1024))
  else printf '%d B' "$b"; fi
}

# ------------------------------------------------------------------ probe (real HTTP)
# one python call -> one line:  UP|version|mem|skills|jobs|mood|learner|interval|done|failed|queued
probe() {
  python3 - "$PORT" "$TOK" <<'PY' 2>/dev/null
import json, sys, urllib.request
port, tok = sys.argv[1], sys.argv[2]
def get(path, headers):
    req = urllib.request.Request(f"http://127.0.0.1:{port}{path}", headers=headers)
    with urllib.request.urlopen(req, timeout=4) as r:
        return json.loads(r.read().decode())
try:
    h = get("/health", {"User-Agent": "jarvis-console"})
except Exception:
    print("DOWN|-|-|-|-|-|-|-|-|-|-"); sys.exit(0)
mem, sk, jobs = h.get("memories", "-"), h.get("skills", "-"), h.get("jobs_queued", "-")
mood, ver = h.get("mood", "-"), h.get("version", "-")
learner, interval, done, failed, queued = "-", "-", "-", "-", "-"
try:
    s = get("/api/state", {"X-Jarvis-Token": tok, "User-Agent": "jarvis-console"})
    l = s.get("learner") or {}
    learner = "on" if l.get("running") else "off"
    interval, done, failed, queued = l.get("interval_s", "-"), l.get("done", "-"), l.get("failed", "-"), l.get("queued", "-")
    c = s.get("counts") or {}
    jobs = c.get("jobs", jobs)
except Exception:
    learner = "?"          # no token: /api/state is refused, health still tells the rest
print(f"UP|{ver}|{mem}|{sk}|{jobs}|{mood}|{learner}|{interval}|{done}|{failed}|{queued}")
PY
}

api() {                          # api METHOD PATH [JSON] -> prints "HTTPCODE\nBODY"
  python3 - "$PORT" "$TOK" "$1" "$2" "${3:-}" <<'PY' 2>/dev/null
import json, sys, urllib.request, urllib.error
port, tok, method, path, body = sys.argv[1:6]
data = body.encode() if body else None
req = urllib.request.Request(f"http://127.0.0.1:{port}{path}", data=data, method=method,
      headers={"X-Jarvis-Token": tok, "Content-Type": "application/json", "User-Agent": "jarvis-console"})
try:
    with urllib.request.urlopen(req, timeout=15) as r:
        print(r.status); print(r.read().decode()[:6000])
except urllib.error.HTTPError as e:
    print(e.code); print(e.read().decode()[:2000])
except Exception as e:
    print(0); print(json.dumps({"ok": False, "error": str(e)}))
PY
}

http_code() { printf '%s' "${1%%$'\n'*}"; }
http_body() { printf '%s' "${1#*$'\n'}"; }
http_body_short() { local b; b="$(http_body "$1")"; printf '%s' "${b:0:300}"; }

# ------------------------------------------------------------------ screens
draw() {
  local pr pid wpid tpid up line queue_age
  url=""                         # global: links_screen uses it too
  pr="$(probe)"
  IFS='|' read -r state ver mem sk jobs mood learner interval done failed queued <<< "$pr"
  pid="$(read_pid "$PID_FILE")"; wpid="$(read_pid "$WATCH_PID_FILE")"; tpid="$(read_pid "$TUNNEL_PID_FILE")"
  up=""
  if pid_alive "$pid"; then
    up="$(ps -o etime= -p "$pid" 2>/dev/null | tr -d ' ')"
    [ -z "$up" ] && up="$(fmt_dur $(( $(date +%s) - $(stat -c %Y "$PID_FILE" 2>/dev/null || date +%s) )))"
  fi
  url=""; [ -s "$TUNNEL_URL_FILE" ] && url="$(cat "$TUNNEL_URL_FILE")"
  queue_age="$(age_of "$TUNNEL_URL_FILE")"

  printf '%s' "$CLEAR"
  printf '  %s╭──────────────────────────────────────────────────╮%s\n' "$RULE" "$R"
  printf '  %s│%s%s%s%s│%s\n' "$RULE" "$R" "$B$GOLD" "$(printf '%-50s' '  J A R V I S  ·  brain server')" "$RULE" "$R"
  printf '  %s│%s%s%s%s│%s\n' "$RULE" "$R" "$D" "$(printf '%-50s' '  command center')" "$RULE" "$R"
  printf '  %s│%s%s%s%s│%s\n' "$RULE" "$R" "$D" "$(printf '%-50s' '  v1.3.1 · q = quit · 1-9 0 = commands')" "$RULE" "$R"
  printf '  %s╰──────────────────────────────────────────────────╯%s\n\n' "$RULE" "$R"

  # server
  if [ "$state" = UP ]; then
    printf '  %s●%s SERVER   %sUP%s      pid %-7s up %s\n' "$GREEN$B" "$R" "$GREEN$B" "$R" "${pid:--}" "${up:-?}"
  else
    printf '  %s●%s SERVER   %sDOWN%s    watchdog %s   (press 7 to restart)\n' \
      "$RED$B" "$R" "$RED$B" "$R" "$(pid_alive "$wpid" && echo running || echo stopped)"
  fi
  if [ "$state" = UP ]; then
    printf '  %s●%s BRAIN    v%s   memories %s   skills %s   jobs %s\n' \
      "$TEAL" "$R" "$ver" "$mem" "$sk" "$jobs"
  else
    printf '  %s●%s BRAIN    %soffline - no counts to show (press 7 to start it)%s\n' \
      "$TEAL" "$R" "$D" "$R"
  fi
  printf '  %s●%s STATE    mood %s   learner %s/%ss   queued %s  done %s  failed %s\n' \
    "$TEAL" "$R" "$mood" "$learner" "$interval" "$queued" "$done" "$failed"
  printf '  %s●%s TUNNEL   %s   url opened %s\n' \
    "$TEAL" "$R" "$(pid_alive "$tpid" && printf '%srunning%s' "$GREEN" "$R" || printf '%sstopped%s' "$AMBER" "$R")" "$queue_age"

  printf '\n'
  printf '  %sPUBLIC URL%s\n' "$D" "$R"
  if [ -n "$url" ]; then printf '    %s%s%s\n' "$TEAL" "$url" "$R"
  else printf '    %s(none yet - press 8 to open one)%s\n' "$AMBER" "$R"; fi
  printf '  %sLOCAL%s\n    %shttp://127.0.0.1:%s%s   %sdashboard /   control /control%s\n' \
    "$D" "$R" "$WHITE" "$PORT" "$R" "$D" "$R"
  printf '  %sTOKEN%s  %s%s%s\n' "$D" "$R" "$WHITE" "${TOK:-(missing - press 7)}" "$R"
  printf '  %sDATA%s   %s (%s)   %sLOGS%s %s\n' \
    "$D" "$R" "$DATA" "$(human_size "$DATA/jarvis.db")" "$D" "$R" "$LOG_DIR"

  rule 56
  printf '  %sCOMMANDS%s %s(14)%s\n' "$B$GOLD" "$R" "$D" "$R"
  printf '   %s1%s  Status refresh          %s2%s  Links + token\n' "$GOLD" "$R" "$GOLD" "$R"
  printf '   %s3%s  Teach JARVIS            %s4%s  Add a memory\n' "$GOLD" "$R" "$GOLD" "$R"
  printf '   %s5%s  Jobs + results          %s0%s  Memories (what it knows)\n' "$GOLD" "$R" "$GOLD" "$R"
  printf '   %s6%s  Live logs               %s7%s  Restart server\n' "$GOLD" "$R" "$GOLD" "$R"
  printf '   %s8%s  New public URL          %s9%s  Stop everything\n' "$GOLD" "$R" "$GOLD" "$R"
  printf '   %st%s  Tutorial (detail)       %su%s  Update code + restart\n' "$GOLD" "$R" "$GOLD" "$R"
  printf '   %sh%s  Help / keys             %sq%s  Quit console (server stays up)\n' "$GOLD" "$R" "$GOLD" "$R"
  rule 56
  printf '  %schoice (number + Enter) >%s ' "$B$GOLD" "$R"
}

links_screen() {
  printf '\n  %sLINKS + TOKEN%s\n\n' "$B$GOLD" "$R"
  printf '  app URL      %s\n' "${url:-http://127.0.0.1:$PORT (no public URL - press 8)}"
  printf '  token        %s\n' "${TOK:-missing}"
  printf '  control      %s/control\n' "${url:-http://127.0.0.1:$PORT}"
  printf '  dashboard    %s/\n' "${url:-http://127.0.0.1:$PORT}"
  printf '  health       %s/health\n\n' "${url:-http://127.0.0.1:$PORT}"
  printf '  %sIn the app: Profile > Brain server > paste URL + token > Test connection%s\n' "$D" "$R"
  printf '  %sBoth fields are the two lines above. Nothing else is needed.%s\n' "$D" "$R"
}

tutorial_screen() {
  printf '\n  %sTUTORIAL - what every command does%s\n' "$B$GOLD" "$R"
  printf '  %severy item below is real: it calls this server or start.sh, no mock data%s\n\n' "$D" "$R"
  t_one() { printf '  %s%s%s %s\n' "$GOLD$B" "$1" "$R" "$2"
            printf '     %s%s%s\n' "$D" "$3" "$R"; }
  t_one 1 "Status refresh"        "Re-reads /health and /api/state. The panel redraws by itself every 5s anyway."
  t_one 2 "Links + token"         "Shows the exact URL + token you paste into the app, plus the web pages."
  t_one 3 "Teach JARVIS"          "Ask for a fact/lesson; it queues a real learning job. The learner picks it up within ~15s and writes a memory/skill."
  t_one 4 "Add a memory"          "Writes one note straight to the brain (kind=note). It is immediately recallable."
  t_one 5 "Jobs + results"        "Last jobs with status/result/error - proof the background learner actually did the work."
  t_one 0 "Memories"              "What the brain currently knows, newest first. Empty list = honest empty brain."
  t_one 6 "Live logs"             "tails server.log live. Press q to stop tailing (does not stop the server)."
  t_one 7 "Restart server"        "Runs start.sh --restart: stop, re-check, self-test, start, /health, watchdog, tunnel."
  t_one 8 "New public URL"        "Runs start.sh --tunnel-only. Reuses a working URL, otherwise opens a fresh one."
  t_one 9 "Stop everything"       "Stops server + watchdog + tunnel. Confirmation is asked first. The console stays open."
  t_one u "Update code"           "Same as restart but pulls the newest code first (git, or GitHub archive)."
  t_one q "Quit console"          "Only this screen closes. Server + watchdog + tunnel keep running."
  printf '\n  %sAlso available outside the console:%s\n' "$B" "$R"
  printf '     %sbash ~/jarvis-start.sh --status | --token | --logs | --restart | --stop | --tunnel-only%s\n' "$D" "$R"
}

help_screen() {
  printf '\n  %sKEYS%s\n\n' "$B$GOLD" "$R"
  printf '  1-9 0   run that command      t  tutorial      u  update + restart\n'
  printf '  r       redraw now            h  this help     q  quit console\n'
  printf '  %sCtrl+C only closes the console, the server is untouched.%s\n' "$D" "$R"
}

pause_() { printf '\n  %spress Enter to go back (q + Enter = quit console)%s ' "$D" "$R"; local k=""; read -r k || true; [ "$k" = q ] && { restore; exit 0; }; }

restore() { printf '\033[?25h\n'; }

# ------------------------------------------------------------------ actions
do_teach() {
  local text resp body
  printf '\n  %sTEACH JARVIS%s  %s(jo cheez sikhani hai, likho)%s\n  > ' "$B$GOLD" "$R" "$D" "$R"
  read -r text || true
  [ -z "${text// /}" ] && { printf '  %sempty - cancelled%s\n' "$AMBER" "$R"; return; }
  resp="$(api POST /api/learn "$(python3 -c 'import json,sys; print(json.dumps({"text": sys.argv[1]}))' "$text")")"
  if [ "$(http_code "$resp")" = 202 ]; then
    printf '  %squeued%s - %s\n' "$GREEN" "$R" "$(http_body "$resp" | python3 -c 'import json,sys; d=json.load(sys.stdin); print("job #{} - {} waiting; the learner runs it within ~15s".format(d["job"], d["queued"]))' 2>/dev/null)"
    printf '  %ssee it finish: press 5%s\n' "$D" "$R"
  else
    printf '  %sfailed%s HTTP %s %s\n' "$RED" "$R" "$(http_code "$resp")" "$(http_body_short "$resp")"
  fi
}

do_memory() {
  local text resp
  printf '\n  %sADD A MEMORY%s  %s(ek line: fact / preference / note)%s\n  > ' "$B$GOLD" "$R" "$D" "$R"
  read -r text || true
  [ -z "${text// /}" ] && { printf '  %sempty - cancelled%s\n' "$AMBER" "$R"; return; }
  resp="$(api POST /api/memories "$(python3 -c 'import json,sys; print(json.dumps({"text": sys.argv[1], "kind": "note", "source": "console"}))' "$text")")"
  case "$(http_code "$resp")" in
    201) printf '  %ssaved%s - %s\n' "$GREEN" "$R" "$(http_body "$resp")";;
    *)   printf '  %sfailed%s HTTP %s %s\n' "$RED" "$R" "$(http_code "$resp")" "$(http_body_short "$resp")";;
  esac
}

do_jobs() {
  local resp
  resp="$(api GET '/api/jobs?limit=8')"
  if [ "$(http_code "$resp")" != 200 ]; then
    printf '\n  %sfailed%s HTTP %s %s\n' "$RED" "$R" "$(http_code "$resp")" "$(http_body_short "$resp")"; return
  fi
  printf '\n  %sJOBS (newest first)%s\n\n' "$B$GOLD" "$R"
  printf '%s' "$(http_body "$resp")" | python3 -c '
import json, sys
d = json.load(sys.stdin)
items = d.get("items") or []
if not items:
    print("  (no jobs yet - teach it with 3)")
for j in items:
    p = j.get("payload") or {}
    what = (p.get("text") or p.get("task") or p.get("title") or "")[:44]
    res = (j.get("result") or j.get("error") or "").replace("\n", " ")[:40]
    print("  #{:<4} {:<9} {:<9} {}".format(j["id"], j["kind"], j["status"], what))
    if res:
        print("        -> " + res)
'
}

do_memories() {
  local resp
  resp="$(api GET '/api/memories?limit=12')"
  if [ "$(http_code "$resp")" != 200 ]; then
    printf '\n  %sfailed%s HTTP %s %s\n' "$RED" "$R" "$(http_code "$resp")" "$(http_body_short "$resp")"; return
  fi
  printf '\n  %sMEMORIES - what JARVIS knows%s\n\n' "$B$GOLD" "$R"
  printf '%s' "$(http_body "$resp")" | python3 -c '
import json, sys
d = json.load(sys.stdin)
items = d.get("items") or []
if not items:
    print("  (empty brain - honest start; add with 4 or teach with 3)")
for m in items:
    t = (m.get("text") or "").replace("\n", " ")[:58]
    print("  #{:<4} {:<8} {}".format(m.get("id", "?"), m.get("kind", "note"), t))
'
}

do_logs() {
  printf '\n  %sLIVE server.log%s  %s(press q to stop tailing - server keeps running)%s\n\n' "$B$GOLD" "$R" "$D" "$R"
  [ -f "$LOG_DIR/server.log" ] || { printf '  %sno server.log yet%s\n' "$AMBER" "$R"; return; }
  tail -n 30 -f "$LOG_DIR/server.log" 2>/dev/null &
  local tp=$! k=""
  while read -rsn1 k; do [ "$k" = q ] && break; done
  kill "$tp" 2>/dev/null; wait "$tp" 2>/dev/null
}

run_start() {                    # run_start "--restart" "label"
  restore
  printf '\n  %s%s%s\n\n' "$B$GOLD" "$2" "$R"
  bash "$START_SH" $1 --no-console
  printf '\n  %sdone - back to the command center...%s\n' "$D" "$R"
  sleep 2
}

confirm_stop() {
  local a=""
  printf '\n  %sStop the server, watchdog AND tunnel?%s (y/N) ' "$AMBER" "$R"
  read -r a || true
  case "$a" in
    y|Y) restore; printf '\n'; bash "$START_SH" --stop --no-console; STOPPED=1; sleep 2;;
    *)   printf '  %scancelled - nothing was stopped%s\n' "$D" "$R"; sleep 1;;
  esac
}

# ------------------------------------------------------------------ main
mkdir -p "$LOG_DIR"
# not a terminal (CI / piped): print one honest snapshot and leave
if [ ! -t 0 ] || [ ! -t 1 ]; then
  draw; printf 'q\n'; printf '\n'
  exit 0
fi

trap 'restore; exit 0' INT TERM
printf '\033[?25l'              # hide the cursor while the panel redraws
trap 'restore' EXIT

while true; do
  draw
  key=""
  # line based on purpose: on a phone keyboard Enter is natural, and a whole line keeps
  # the menu and the text prompts perfectly in sync (no half-consumed characters).
  read -t 10 -r key; rc=$?
  if [ "$rc" -eq 0 ] || [ -n "$key" ]; then
    key="${key#"${key%%[![:space:]]*}"}"
    key="${key:0:1}"
    case "$key" in
      1|r|R) : ;;
      2)     links_screen; pause_ ;;
      3)     do_teach;   pause_ ;;
      4)     do_memory;  pause_ ;;
      5)     do_jobs;    pause_ ;;
      0)     do_memories; pause_ ;;
      6)     do_logs ;;
      7)     run_start "--restart" "RESTART - stop, self-test, start, health, watchdog, tunnel" ;;
      8)     run_start "--tunnel-only" "NEW PUBLIC URL - reuse a working one or open a fresh one" ;;
      9)     confirm_stop ;;
      u|U)   run_start "--restart" "UPDATE + RESTART - newest code, then the full check" ;;
      t|T)   tutorial_screen; pause_ ;;
      h|H)   help_screen; pause_ ;;
      q|Q)   restore
             if [ "${STOPPED:-0}" = 1 ]; then
               printf '  %sconsole closed - everything was stopped with 9.%s\n' "$D" "$R"
               printf '  %sstart it again: bash ~/jarvis-start.sh%s\n\n' "$D" "$R"
             else
               printf '  %sconsole closed - server, watchdog and tunnel are still running.%s\n' "$D" "$R"
               printf '  %sback here any time: bash ~/jarvis-start.sh --console%s\n\n' "$D" "$R"
             fi
             exit 0;;
      *) : ;;
    esac
  elif [ "$rc" -le 128 ]; then
    restore
    printf '  %sinput closed - console closed, server keeps running.%s\n\n' "$D" "$R"
    exit 0
  fi
done
