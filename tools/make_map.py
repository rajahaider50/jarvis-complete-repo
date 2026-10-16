#!/usr/bin/env python3
"""Generates docs/PROJECT_MAP.md (one line per source file). Run: python3 tools/make_map.py"""
import os, re, json, hashlib, datetime, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "app/src/main/java/com/jarvis/app/"
STATE = os.path.join(ROOT, "docs", ".map_state.json")
OUT = os.path.join(ROOT, "docs", "PROJECT_MAP.md")
DECL = re.compile(r'^(?:@\w+[ \t]+)*(?:(?:private|internal|public|data|sealed|enum|abstract|open)[ \t]+)*(?:class|object|interface|fun|val|var)[ \t]+(?:<[^>]*>[ \t]*)?(?:\w+\.)?(\w+)', re.M)

def files():
    out = []
    for d, _, fs in os.walk(os.path.join(ROOT, "app", "src")):
        for f in fs:
            p = os.path.join(d, f)
            out.append(os.path.relpath(p, ROOT).replace(os.sep, "/"))
    return sorted(out)

def h(p):
    return hashlib.md5(open(os.path.join(ROOT, p), "rb").read()).hexdigest()

def summary(txt):
    m = re.search(r'/\*\*(.*?)\*/', txt, re.S)
    if not m:
        m = re.search(r'^//\s*(.+)$', txt, re.M)
        return (m.group(1).strip() if m else "-")[:90]
    s = " ".join(l.strip(" *") for l in m.group(1).strip().splitlines()).strip()
    return s[:90] or "-"

allf = files()
old = json.load(open(STATE)) if os.path.exists(STATE) else {}
new = {p: h(p) for p in allf}
changed = [p for p in new if old.get(p) != new[p]] if old else []
deleted = [p for p in old if p not in new]

kt = [p for p in allf if p.endswith(".kt")]
decls = {}
for p in kt:
    txt = open(os.path.join(ROOT, p), encoding="utf-8", errors="ignore").read()
    top = [m.group(1) for m in DECL.finditer(txt)]
    decls[p] = (txt, list(dict.fromkeys(top))[:6])

sym = {}
for p, (_, names) in decls.items():
    for n in names:
        if len(n) > 4 and n[0].isupper():
            sym.setdefault(n, p)

L = ["# PROJECT MAP (generated: do not edit by hand)", "",
     f"Updated {datetime.datetime.now(datetime.timezone.utc):%Y-%m-%d %H:%M} UTC · {len(allf)} files. Read docs/AI_GUIDE.md first, then this map, then ONLY the files you need.", "",
     "## Changed since the previous map"]
L += [f"- {p}" for p in changed[:30]] or ["- nothing"]
if deleted: L += [f"- deleted: {p}" for p in deleted]
L += ["", f"## Kotlin source (`{BASE}`)", "format: `path` · lines · what it is · declares · uses (other project files)", ""]
cur = None
for p in kt:
    rel = p[len(BASE):] if p.startswith(BASE) else p
    d = os.path.dirname(rel) or "(root)"
    if d != cur:
        L += ["", f"### {d}/"]; cur = d
    txt, names = decls[p]
    uses = sorted({sym[n] for n in set(re.findall(r'\b[A-Z]\w{4,}\b', txt)) if n in sym and sym[n] != p})
    uses = [os.path.basename(u) for u in uses][:8]
    L.append(f"- `{os.path.basename(p)}` · {txt.count(chr(10))+1} · {summary(txt)} · {', '.join(names) or '-'}" + (f" · uses: {', '.join(uses)}" if uses else ""))
L += ["", "## Other files"]
for p in allf:
    if not p.endswith(".kt"):
        L.append(f"- `{p[len('app/src/main/'):] if p.startswith('app/src/main/') else p}`")
other = [x for x in ("app/build.gradle.kts", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", ".github/workflows", "design/DESIGN_SPEC.md") if os.path.exists(os.path.join(ROOT, x))]
L += ["", "## Build / design files", *[f"- `{x}`" for x in other]]
open(OUT, "w", encoding="utf-8").write("\n".join(L) + "\n")
json.dump(new, open(STATE, "w"), indent=0)

arch = os.path.join(ROOT, "docs", "ARCHITECTURE.md")
if os.path.exists(arch):
    missing = []
    for n in set(re.findall(r'`([\w/]+\.kt)`', open(arch, encoding="utf-8").read())):
        if not (os.path.exists(os.path.join(ROOT, BASE, n)) or os.path.exists(os.path.join(ROOT, n))):
            missing.append(n)
    if missing: print("WARNING: ARCHITECTURE.md names files that do not exist:", ", ".join(sorted(missing)))
print(f"map written: {len(allf)} files, {len(changed)} changed, {len(deleted)} deleted")
