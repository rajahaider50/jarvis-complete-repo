#!/usr/bin/env python3
"""Applies small edits from a JSON file, so an AI can change a BIG file by returning only a few lines.
Usage: python tools/apply_edits.py docs/edits/001.json
JSON: [{"file":"app/src/main/java/com/jarvis/app/ui/screens/ChatScreen.kt","old":"exact old text","new":"new text"}, ...]
Each "old" must appear EXACTLY once; otherwise nothing is changed and the problem is printed."""
import json, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
edits = json.load(open(sys.argv[1], encoding="utf-8"))
texts, problems = {}, []
for i, e in enumerate(edits, 1):
    p = os.path.join(ROOT, e["file"])
    if not os.path.exists(p):
        problems.append(f"#{i}: file not found: {e['file']}"); continue
    t = texts.get(p) or open(p, encoding="utf-8").read()
    n = t.count(e["old"])
    if n != 1:
        problems.append(f"#{i}: {e['file']}: the old text was found {n} times (needs exactly 1)"); continue
    texts[p] = t.replace(e["old"], e["new"], 1)
if problems:
    print("NOTHING CHANGED:\n" + "\n".join(problems)); sys.exit(1)
for p, t in texts.items():
    open(p, "w", encoding="utf-8").write(t)
print(f"applied {len(edits)} edits to {len(texts)} files")
