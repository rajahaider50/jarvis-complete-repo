"""Server-Driven UI: pages the server describes with a strict component whitelist.

The phone renders only what is listed in COMPONENTS, so a page from the server can never run code,
open a file or pretend to be a native screen. Anything sensitive (calls, camera, accessibility) stays
native in the app; this is for simple dashboards, lists, notes and toggles.

Later steps (S7) wire these pages into the app; for now the API already serves them, tested.
"""
from __future__ import annotations

COMPONENTS = {
    "title",     # {text}
    "text",      # {text}
    "status",    # {label, value, tone: ok|warn|bad}
    "list",      # {title, items: [text]}
    "button",    # {label, action, confirm}
    "toggle",    # {label, key, value}
    "number",    # {label, key, value, min, max, step}
    "memory",    # {query, limit}   -> the app shows real recall results
    "affect",    # {}               -> the app shows the real affect state
}

ACTIONS = {"recall", "learn", "sync", "open_page", "none"}


def _page(pid: str, title: str, icon: str, blocks: list[dict]) -> dict:
    bad = [b for b in blocks if b.get("type") not in COMPONENTS]
    if bad:
        raise ValueError(f"page {pid} uses unknown component(s): {[b.get('type') for b in bad]}")
    act = [b.get("action") for b in blocks if b.get("type") == "button"]
    unknown = [a for a in act if a not in ACTIONS]
    if unknown:
        raise ValueError(f"page {pid} uses unknown action(s): {unknown}")
    return {"id": pid, "title": title, "icon": icon, "blocks": blocks}


def default_pages() -> list[dict]:
    return [
        _page("brain", "Brain server", "hub", [
            {"type": "text", "text": "Live state of your own server. Numbers come from the server's database."},
            {"type": "status", "label": "Connection", "value": "checking", "tone": "warn"},
            {"type": "status", "label": "Memories", "value": "?", "tone": "ok"},
            {"type": "status", "label": "Skills", "value": "?", "tone": "ok"},
            {"type": "status", "label": "Learning jobs", "value": "?", "tone": "ok"},
            {"type": "affect"},
            {"type": "button", "label": "Sync now", "action": "sync"},
        ]),
        _page("learn", "Teach JARVIS", "school", [
            {"type": "text", "text": "Write what to learn. The server distils it into lessons and a skill while you are busy."},
            {"type": "memory", "query": "", "limit": 5},
            {"type": "button", "label": "Queue a learning job", "action": "learn"},
        ]),
        _page("recall", "Recall", "search", [
            {"type": "text", "text": "Ask the server what it remembers."},
            {"type": "memory", "query": "", "limit": 8},
        ]),
    ]
