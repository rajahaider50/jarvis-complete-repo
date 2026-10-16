#!/usr/bin/env python3
"""Builds a small context pack zip for an AI.
python3 tools/make_pack.py "goal text" "keyword1 keyword2" -> context-pack.zip
Picks files whose path/summary/declarations match the keywords, plus fixed core docs."""
import sys, os, re, zipfile
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "app/src/main/java/com/jarvis/app/"
goal = sys.argv[1] if len(sys.argv) > 1 else ""
kws = [k.lower() for k in re.split(r'[\s,]+', sys.argv[2] if len(sys.argv) > 2 else "") if k]
MAX_FILES, MAX_BYTES = 12, 220_000
core = ["docs/AI_GUIDE.md", "docs/PROJECT_MAP.md", "docs/ARCHITECTURE.md", "docs/HANDOFF_STATE.md",
        "Task.md", "Planning.md", "Roadmap.md", "Pending.md", "Complete.md", "Goal.md", "design/DESIGN_SPEC.md"]
skills = sorted(f"docs/skills/{f}" for f in os.listdir(os.path.join(ROOT, "docs/skills"))) if os.path.isdir(os.path.join(ROOT, "docs/skills")) else []
scores = {}
for line in open(os.path.join(ROOT, "docs/PROJECT_MAP.md"), encoding="utf-8"):
    m = re.match(r'- `([\w]+\.kt)` ·', line)
    if not m: continue
    name = m.group(1); low = line.lower()
    s = sum(low.count(k) * (3 if k in name.lower() else 1) for k in kws)
    if s: scores[name] = s
paths = {}
for d, _, fs in os.walk(os.path.join(ROOT, BASE)):
    for f in fs: paths[f] = os.path.relpath(os.path.join(d, f), ROOT).replace(os.sep, "/")
pick = [paths[n] for n, _ in sorted(scores.items(), key=lambda x: -x[1]) if n in paths][:MAX_FILES]
for always in ("data/Prefs.kt",):
    pass
extra = [BASE + "ui/components/Components.kt", BASE + "MainActivity.kt"] if any(("screen" in k or "ui" in k or "design" in k) for k in kws) else []
files = core + skills + [p for p in extra + pick if os.path.exists(os.path.join(ROOT, p))]
seen, total = set(), 0
with zipfile.ZipFile(os.path.join(ROOT, "context-pack.zip"), "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("REQUEST.md", f"# REQUEST\n\n**Goal:** {goal}\n\nKeywords: {' '.join(kws)}\n\nRules: read docs/AI_GUIDE.md first. Open only files in this pack. Return only changed files in a zip + docs/CHANGES_<slug>.md. Update Pending/Complete/HANDOFF_STATE.\n")
    for f in files:
        if f in seen or not os.path.exists(os.path.join(ROOT, f)): continue
        sz = os.path.getsize(os.path.join(ROOT, f))
        if total + sz > MAX_BYTES and f in pick: continue
        seen.add(f); total += sz; z.write(os.path.join(ROOT, f), f)
print("pack files:", len(seen)); [print(" ", f) for f in sorted(seen)]
