# AI GUIDE: read this first, follow it exactly

You are helping the owner of the **Jarvis** Android app. The owner works **only from an Android phone**, speaks Urdu (Urdu script or Roman Urdu), and has limited AI usage per account. Reply in the language the owner writes in, in short mobile-friendly messages. **Never ask for secrets** (API keys, tokens, passwords). Be honest: you cannot compile the code, so say what is untested and never invent API/library names you are not sure of.

## 0. Start-up routine (always, in this order)
1. `docs/AI_GUIDE.md` (this) -> `REQUEST.md` (if present) -> `Task.md` -> `Planning.md` -> `Pending.md`.
2. `docs/PROJECT_MAP.md` + `docs/ARCHITECTURE.md` + `docs/HANDOFF_STATE.md`.
3. The matching skill in `docs/skills/`: `ui-design`, `add-feature`, `fix-bug`, `delivery-format` (`light-model-boost` if you are a fast/light model).
4. `Roadmap.md` and `Goal.md` only when planning or when direction changes.
5. Then section 1 (reading protocol) applies: open ONLY the files you need.

## 0b. Tracking files (keep them true; the next AI continues from them)
| File | Meaning | You must |
|---|---|---|
| `Task.md` | the ONE current task | write it from the owner's request before starting; mark done at the end, then reset |
| `Planning.md` | steps of the current task | write a short numbered plan first; tick `[x]` as steps finish |
| `Roadmap.md` | ordered future phases | follow its order; append new ideas at the end only |
| `Pending.md` | not done yet | add unfinished/newly found items; DELETE a line when finished |
| `Complete.md` | done, newest first | add one line per finished item (date, what, files) |
| `Goal.md` | final goals | tick/remove a goal only when really reached |
Finished item = removed from Pending + added to Complete (never in both). Also add one entry to `docs/HANDOFF_STATE.md`. Deliver the updated tracking files in the same zip as the code.

## 1. Reading protocol (this saves most of the usage limit)
1. Read this file, then `REQUEST.md` (the task), then `docs/ARCHITECTURE.md`, then `docs/PROJECT_MAP.md` (one line per file) and `docs/HANDOFF_STATE.md`.
2. Open **only** the files listed in `REQUEST.md` plus files the map says they depend on. **Do not unzip-and-read the whole project.** Do not run "review everything".
3. If you need a file that is not in the pack, ask for **its exact path** (from the map). Do not guess its contents.
4. Search before reading: use grep/search for a symbol instead of opening many files.
5. Never re-read a file you already have, and never paste large files back into the chat.

## 2. Scope: be surgical
- Do **only** what `REQUEST.md` asks. Do not refactor, rename, reformat, reorder imports, "clean up" or restyle anything else.
- Prefer **adding new files** and only small hook lines in existing files (route, setting, one call). This keeps old features safe.
- If the task touches an area that already has a bug (listed in `REQUEST.md` under *Known problems*, or a bug that blocks your change), fix it with the smallest edit. Any other problem you notice: list it under **Noticed, not changed** in your final message and leave the code alone.
- Keep the existing design: same style, same components (`JText`, `JField`, `GoldButton`, theme colors, spacing), same wording style. The 5 interface styles (`Palettes.style` 0-4) all must keep working. Design details: `design/DESIGN_SPEC.md`.
- Never change public function names/signatures that other files use unless the task requires it; if you do, update every caller (search for them) and say so.

## 3. How to deliver (phone friendly)
1. **Plan first, 6 lines at most**: which files you will add/change and why.
2. Return **one zip** containing **only the new and changed files at their real paths** (the owner unzips it over the project), plus `docs/CHANGES_<short-name>.md` (what changed, how to test, what is untested) and the updated tracking files from section 0b.
3. For a **very large file where only a few lines change** (more than ~300 lines), you may instead return `docs/edits/<number>.json` for `tools/apply_edits.py`: `[{"file":"<real path>","old":"<exact old text, unique>","new":"<new text>"}]`. Each `old` must occur exactly once. Use it only when it saves a lot of output.
4. Do not print code in the chat unless the owner asks. Final message: at most ~12 lines: what you did, the files, how to test, **Noticed, not changed**, what is untested.
5. If the work is too big for one go, split it into steps, finish step 1 completely (it must compile on its own), and write the next step in `docs/HANDOFF_STATE.md`.

## 4. Quality checklist (run it on every file you touch before delivering)
- Every import exists; no unused-name clashes; no duplicate declarations.
- New screen: route added + five settings hubs updated. New setting: Prefs property + load + save. New tool: `PhoneTools.all` + `execute`. New service/activity/permission: manifest. New resource: exists.
- API level: minSdk 26, compileSdk 34. Guard newer APIs with `Build.VERSION.SDK_INT`.
- Threads: network and file work on `Dispatchers.IO`; windows/views on the main thread; `suspend` functions only called from coroutines.
- Errors: no empty `catch`; call `AppLog.w(e)` (handled) or `AppLog.e(tag, e)` (real failure). Show a clear message to the user, not a crash.
- Text shown to the user is real: never fake data (phone status, weather, model names, results). Read it from the phone/API or say it is unavailable.
- Brackets, quotes and `$` in strings are balanced and escaped. Re-read each changed file once, top to bottom.

## 5. After the work
- Add one entry to `docs/HANDOFF_STATE.md` (date, what was done, files, next step). Update `docs/ARCHITECTURE.md` only if you added a feature/module or changed a convention.
- `docs/PROJECT_MAP.md` is generated automatically by GitHub (`tools/make_map.py`); do not edit it.

## 5b. Design rules (one product, one look)
- Source of truth: `design/DESIGN_SPEC.md`, `ui/components/Components.kt`, `ui/theme/Color.kt`, `ui/styles/StyleKit.kt`. Follow `docs/skills/ui-design.md`.
- Build UI only from existing components and palette colours. No hard-coded colours, no new fonts, no raw Material `Button`/`TextField`/`Switch`. Copy the structure of the nearest existing screen.
- A new tab/settings screen must work in all 5 interface styles.
- Design polish is a separate task; never restyle old screens while adding a feature.

## 6. Hard rules from the owner
- Do not redesign. Only add what is asked, in the same style.
- No fake/random model names: models always come from the owner's own API key.
- Keep the app's real behaviour: real Gemini API, real device data, real permissions. No mock-ups.
- If something is impossible on Android (for example shutting the phone down silently), say so and offer the closest honest alternative.
