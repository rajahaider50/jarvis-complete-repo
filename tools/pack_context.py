#!/usr/bin/env python3
"""Makes a SMALL zip for an AI: the guide, the map and only the files a task needs (instead of the whole project).
Examples:
  python tools/pack_context.py --goal "add dark mode toggle to chat" --keywords chat bubble
  python tools/pack_context.py --files live/LiveClient.kt live/CallHost.kt --deps
  python tools/pack_context.py --module ui/styles --goal "restyle the Aurora home"
"""
import argparse, os, re, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "app/src/main/java/com/jarvis/app/"
ALWAYS = ["docs/AI_GUIDE.md", "docs/ARCHITECTURE.md", "docs/PROJECT_MAP.md", "docs/HANDOFF_STATE.md",
          "Task.md", "Planning.md", "Pending.md", "Roadmap.md", "Complete.md", "Goal.md", "design/DESIGN_SPEC.md"]
SKILLS_DIR = "docs/skills"


def split(v):
    out = []
    for x in v or []:
        out += [y for y in re.split(r"[,\s]+", x) if y]
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--goal", default="")
    ap.add_argument("--files", nargs="*", default=[])
    ap.add_argument("--keywords", nargs="*", default=[])
    ap.add_argument("--module", default="")
    ap.add_argument("--deps", action="store_true", help="also include the project files the chosen files import")
    ap.add_argument("--build", action="store_true", help="include manifest + gradle files")
    ap.add_argument("--max-files", type=int, default=10)
    ap.add_argument("--max-kb", type=int, default=120)
    ap.add_argument("--out", default="context-pack.zip")
    a = ap.parse_args()
    a.files, a.keywords = split(a.files), [k.lower() for k in split(a.keywords)]

    src = {}
    for d, _, fs in os.walk(os.path.join(ROOT, BASE)):
        for f in fs:
            if f.endswith(".kt"):
                p = os.path.relpath(os.path.join(d, f), ROOT).replace("\\", "/")
                src[p] = open(os.path.join(ROOT, p), encoding="utf-8").read()
    chosen = []

    def add(p):
        if p in src and p not in chosen:
            chosen.append(p)

    for f in a.files:                                   # explicit files: match by path ending
        for p in src:
            if p.endswith("/" + f) or p.endswith(f):
                add(p)
    if a.module:
        for p in sorted(src):
            if a.module in p[len(BASE):]:
                add(p)
    if a.keywords:                                      # keyword search: path > declared names > content
        scored = []
        for p, t in src.items():
            low, s = t.lower(), 0
            for k in a.keywords:
                s += 5 * (k in p.lower()) + 3 * len(re.findall(r"^(?:\w+\s+)*(?:class|object|fun)\s+\w*" + re.escape(k), low, re.M)) + min(low.count(k), 5)
            if s:
                scored.append((s, p))
        for s, p in sorted(scored, reverse=True)[: max(4, a.max_files // 2)]:
            add(p)
    if a.deps:
        names = {}
        for p, t in src.items():
            for m in re.finditer(r"^(?:\w+\s+)*(?:class|object|interface)\s+(\w+)", t, re.M):
                names.setdefault(m.group(1), p)
        for p in list(chosen):
            for m in re.finditer(r"^import com\.jarvis\.app\.[\w.]*?(\w+)$", src[p], re.M):
                if m.group(1) in names:
                    add(names[m.group(1)])
    chosen = chosen[: a.max_files]
    extra = []
    if a.build:
        extra = [p for p in ["app/src/main/AndroidManifest.xml", "app/build.gradle.kts"] if os.path.exists(os.path.join(ROOT, p))]
    total, keep = 0, []
    for p in chosen:
        sz = len(src[p].encode("utf-8")) / 1024
        if total + sz > a.max_kb and keep:
            continue
        keep.append(p); total += sz
    left = [p[len(BASE):] for p in src if p not in keep]
    req = ["# REQUEST\n", "## Goal\n" + (a.goal or "(write the goal here)") + "\n",
           "## Files included in this pack (open ONLY these; they are the likely place for the change)\n" + "\n".join(f"- {p} ({src[p].count(chr(10)) + 1} lines)" for p in keep) + "\n",
           "## Not included (exist in the project; ask for the exact path if you really need one)\n" + ", ".join(left[:150]) + (" ..." if len(left) > 150 else "") + "\n",
           "## Rules\nRead docs/AI_GUIDE.md and follow its start-up routine (Task, Planning, Pending, skills). Change only what the goal needs. Return ONLY new/changed files in a zip at their real paths, plus docs/CHANGES_<slug>.md and the updated tracking files (Task/Planning/Pending/Complete/Goal) and a HANDOFF_STATE entry.\n"]
    with zipfile.ZipFile(os.path.join(ROOT, a.out) if not os.path.isabs(a.out) else a.out, "w", zipfile.ZIP_DEFLATED) as z:
        skills = [SKILLS_DIR + "/" + f for f in sorted(os.listdir(os.path.join(ROOT, SKILLS_DIR)))] if os.path.isdir(os.path.join(ROOT, SKILLS_DIR)) else []
        for p in ALWAYS + skills + extra + keep:
            if os.path.exists(os.path.join(ROOT, p)):
                z.write(os.path.join(ROOT, p), p)
        z.writestr("REQUEST.md", "\n".join(req))
    approx = int(sum(len(src[p]) for p in keep) / 4) + 6000
    print(f"{a.out}: {len(keep)} source files, about {approx} tokens to read (a whole-project read is about {int(sum(len(t) for t in src.values()) / 4)}).")
    for p in keep:
        print("  +", p[len(BASE):])
    if approx > 30000:
        print("TIP: this pack is big. Use exact file names (--files A.kt B.kt) or more specific keywords to make it smaller.")


if __name__ == "__main__":
    main()
