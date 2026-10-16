#!/usr/bin/env python3
"""Apply AI edits: python3 tools/apply_edits.py docs/edits/001.json
JSON: [{"file":"path","old":"exact old text (must occur once)","new":"new text"}]
All-or-nothing per file; prints a clear error and changes nothing if any edit fails."""
import json, sys, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
edits = json.load(open(sys.argv[1], encoding="utf-8"))
by = {}
for e in edits: by.setdefault(e["file"], []).append(e)
plan, errors = {}, []
for f, es in by.items():
    p = os.path.join(ROOT, f)
    if not os.path.exists(p): errors.append(f"missing file: {f}"); continue
    t = open(p, encoding="utf-8").read()
    for i, e in enumerate(es, 1):
        n = t.count(e["old"])
        if n != 1: errors.append(f"{f} edit {i}: old text found {n} times (need exactly 1)"); break
        t = t.replace(e["old"], e["new"], 1)
    else: plan[f] = t
if errors:
    print("NOTHING CHANGED. Errors:"); [print(" -", x) for x in errors]; sys.exit(1)
for f, t in plan.items():
    open(os.path.join(ROOT, f), "w", encoding="utf-8").write(t); print("edited", f, f"({len(by[f])} edits)")
