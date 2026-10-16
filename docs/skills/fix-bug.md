# SKILL: fix-bug
1. Reproduce from the report: a build log (fix ONLY the listed errors, file+line given), or a Console log from Profile > Console, or a description.
2. Locate the file via `docs/PROJECT_MAP.md` / `docs/ARCHITECTURE.md`; open only that file and what it calls.
3. Find the root cause; state it in one sentence in CHANGES.
4. Smallest possible fix. Same style as the surrounding code. No refactor, no formatting changes.
5. Check the fix cannot break callers (grep the symbol in the map's "uses" column).
6. Fragile areas (see ARCHITECTURE) need extra care: describe the risk in CHANGES.
7. Other problems seen on the way: list under "Noticed, not changed" and add to `Pending.md`.
8. Prefer EDITS MODE (docs/edits/NNN.json) for big files; old text must be copied exactly and be unique.
