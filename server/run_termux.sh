#!/data/data/com.termux/files/usr/bin/bash
# Run the JARVIS brain server on the phone itself (Termux). See docs/SERVER_SETUP_TERMUX.md.
set -e
cd "$(dirname "$0")"

pkg install -y python >/dev/null 2>&1 || true
pip install --upgrade pip >/dev/null
pip install -r requirements.txt

export JARVIS_DATA="${JARVIS_DATA:-$HOME/jarvis-data}"
export PORT="${PORT:-8787}"
[ -f .env ] && set -a && . ./.env && set +a

echo "JARVIS server -> http://0.0.0.0:$PORT   (data: $JARVIS_DATA)"
python -m jarvis_server "$PORT"
