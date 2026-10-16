#!/usr/bin/env python3
"""Builds docs/PROJECT_MAP.md: a small map of the whole project (one line per file) so an AI never has to read everything.
Run:  python tools/make_map.py        (also runs by itself on GitHub after every push, see .github/workflows/map.yml)"""
import hashlib, json, os, re, sys, datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "app/src/main/java/com/jarvis/app/"
SKIP_DIRS = {".git", "build", ".gradle", ".idea", "node_modules", "__pycache__"}
SKIP_FILES = {"docs/PROJECT_MAP.md", "docs/.map_state.json"}
BIN_EXT = {".png", ".jpg", ".jpeg", ".webp", ".zip", ".jar", ".ttf", ".otf", ".woff", ".woff2", ".apk", ".ico", ".pdf"}
DECL = re.compile(r"^(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:private|internal|public|abstract|open|data|sealed|enum|inline|suspend|const)\s+)*(class|object|interface|fun|val|var)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)")


def files():
    for d, dirs, fs in os.walk(ROOT):
        dirs[:] = sorted(x for x in dirs if x not in SKIP_DIRS)
        for f in sorted(fs):
            p = os.path.relpath(os.path.join(d, f), ROOT).replace("\\", "/")
            if p in SKIP_FILES or os.path.splitext(f)[1].lower() in BIN_EXT:
                continue
            yield p


def read(p):
    try:
        return open(os.path.join(ROOT, p), encoding="utf-8").read()
    except Exception:
        return ""


def kt_info(text):
    decls, summary, pending = [], "", ""
    for line in text.split("\n"):
        s = line.strip()
        if s.startswith("/**") or s.startswith("/*"):
            pending = s.strip("/* ").rstrip("*/ ").strip()
        elif s.startswith("*") and not pending:
            pending = s.lstrip("* ").strip()
        elif s.startswith("//") and not pending:
            pending = s.lstrip("/ ").strip()
        m = DECL.match(line) if line and not line[0].isspace() else None
        if m:
            if not summary and pending:
                summary = pending
            if m.group(1) in ("class", "object", "interface", "fun") or len(decls) < 3:
                decls.append(m.group(2))
            pending = ""
        elif s and not s.startswith(("*", "/", "@", "import", "package")) and not line[0].isspace():
            pending = ""
    return decls, summary


def main():
    prev = {}
    state = os.path.join(ROOT, "docs/.map_state.json")
    if os.path.exists(state):
        try: prev = json.load(open(state))
        except Exception: prev = {}
    cur, rows, owner = {}, [], {}
    for p in files():
        t = read(p)
        cur[p] = hashlib.sha1(t.encode("utf-8", "ignore")).hexdigest()
        n = t.count("\n") + 1 if t else 0
        if p.endswith(".kt"):
            decls, summ = kt_info(t)
            rel = p[len(BASE):] if p.startswith(BASE) else p
            for d in decls:
                owner.setdefault(d, rel)
            rows.append((p, n, summ, decls, t))
        else:
            rows.append((p, n, "", [], t))
    out = []
    changed = [p for p in cur if prev and prev.get(p) != cur[p]]
    gone = [p for p in prev if p not in cur]
    out.append("# PROJECT MAP (generated: do not edit by hand)\n")
    out.append(f"Updated {datetime.datetime.now().strftime('%Y-%m-%d %H:%M')} · {len(cur)} files. Read docs/AI_GUIDE.md first, then this map, then ONLY the files you need.\n")
    if prev:
        out.append("## Changed since the previous map\n" + ("\n".join(f"- {p}" for p in (changed + [g + ' (deleted)' for g in gone])[:60]) or "- nothing") + "\n")
    out.append("## Kotlin source (`" + BASE + "`)\nformat: `path` · lines · what it is · declares · uses (other project files)\n")
    last = None
    for p, n, summ, decls, t in rows:
        if not p.startswith(BASE):
            continue
        rel = p[len(BASE):]
        folder = rel.rsplit("/", 1)[0] if "/" in rel else "(root)"
        if folder != last:
            out.append(f"\n### {folder}/")
            last = folder
        uses = []
        for m in re.finditer(r"^import com\.jarvis\.app\.([\w.]+)", t, re.M):
            name = m.group(1).split(".")[-1]
            f = owner.get(name)
            if f and f != rel and f.rsplit("/", 1)[-1] not in uses:
                uses.append(f.rsplit("/", 1)[-1])
        out.append(f"- `{rel.rsplit('/', 1)[-1]}` · {n} · {summ[:90] or '-'} · {', '.join(decls[:7])}" + (f" · uses: {', '.join(uses[:6])}" if uses else ""))
    out.append("\n## Resources, manifest, build, CI, docs\n")
    groups = {}
    for p, n, _, _, _ in rows:
        if p.startswith(BASE):
            continue
        key = "app/src/main/res/" + p.split("res/")[1].split("/")[0] if "/res/" in p else None
        if key and p.split("/")[-1].startswith("ic_"):
            groups.setdefault(key + "/ic_*", []).append(p)
        else:
            out.append(f"- `{p}` · {n} lines")
    for k, v in groups.items():
        out.append(f"- `{k}` · {len(v)} icon files")
    open(os.path.join(ROOT, "docs/PROJECT_MAP.md"), "w", encoding="utf-8").write("\n".join(out) + "\n")
    json.dump(cur, open(state, "w"))
    # check that every path named in ARCHITECTURE.md exists
    arch = read("docs/ARCHITECTURE.md")
    bad = [m for m in re.findall(r"`([\w/]+\.kt)`", arch) if not os.path.exists(os.path.join(ROOT, BASE, m))]
    print(f"map written: {len(cur)} files, {len(changed)} changed, {len(gone)} deleted")
    if bad:
        print("WARNING: ARCHITECTURE.md names files that do not exist:", ", ".join(bad))


if __name__ == "__main__":
    main()
