# AI GUIDE: how to work on the Jarvis app (read this first, every time)

You continue an existing Android app (Kotlin + Jetpack Compose, package `com.jarvis.app`, minSdk 26, compileSdk 34). The owner works ONLY from an Android phone, writes Urdu / Roman Urdu, and builds the APK with GitHub Actions. Reply in the owner's language. Keep chat replies short.

## 0. Start-up routine (always, in this order)
1. `docs/AI_GUIDE.md` (this) -> `REQUEST.md` (if present) -> `Task.md` -> `Planning.md` -> `Pending.md`.
2. `docs/PROJECT_MAP.md` + `docs/ARCHITECTURE.md` (find which files the task needs) + `docs/HANDOFF_STATE.md`.
3. Open the matching skill in `docs/skills/` (ui-design, add-feature, fix-bug, delivery-format).
4. `Roadmap.md` and `Goal.md` only when planning or when the task changes direction.
5. Then open ONLY the files you need. Never read the whole project.

## 1. Tracking files (keep them true; they are how the next AI continues)
| File | Meaning | You must |
|---|---|---|
| `Task.md` | the ONE current task | write/refresh it from the owner's request before starting; mark it done at the end and clear it |
| `Planning.md` | steps for the current task | write a short numbered plan first; tick steps `[x]` as you finish |
| `Roadmap.md` | ordered future phases | follow its order; do not jump ahead; add new ideas at the end |
| `Pending.md` | not done yet | add anything unfinished or newly found; delete a line when it is finished |
| `Complete.md` | done, newest first | add one line per finished item (date + what + files) |
| `Goal.md` | final goals of the app | tick or remove a goal only when it is really reached |
Rule: when something finishes it moves Pending -> Complete (delete from Pending, add to Complete). Never leave the same item in both. Also add one line to `docs/HANDOFF_STATE.md` ("Done" and "Next step").
Deliver these tracking files in the same zip as your code changes.

## 2. Token discipline
- Each file once. Do not re-read what is in the conversation. Do not print files in chat.
- Need a file that is not in the pack? Ask for its exact path from the map. Never guess its content.
- Finish the CURRENT task before suggesting new ones. One big feature per chat.

## 3. Scope: surgical changes only
- Do exactly what the request says. No refactor, rename, reformat, import reordering, "clean up", library upgrades, or touching files that do not need to change.
- Prefer a NEW file for a new feature + the smallest hook lines in existing files (route, menu row, Prefs key).
- If something the request touches is already broken, fix it and say so. Anything else you notice goes under "Noticed, not changed" (and into `Pending.md`), untouched.
- Never change the look of existing screens. New UI follows section 5.
- No fake model names, fake phone data or mock features presented as real. Models come from the user's own key. Never ask for secrets (API keys, tokens) in chat.

## 4. Delivery (phone friendly) — see `docs/skills/delivery-format.md`
ONE zip with ONLY new/changed files at their real paths + `docs/CHANGES_<yyyymmdd>_<slug>.md` + updated tracking files. Files under ~400 lines: deliver the COMPLETE file. Big file (400+ lines) with small changes: EDITS MODE `docs/edits/<nnn>.json` `[{"file":"...","old":"exact unique old text","new":"new text"}]`; the owner runs the GitHub workflow "Apply AI edits". Do not edit `docs/PROJECT_MAP.md` (generated).

## 5. Design rules (the app must look like one product)
- Source of truth: `design/DESIGN_SPEC.md`, `ui/components/Components.kt`, `ui/theme/Color.kt`, `ui/styles/StyleKit.kt`.
- Build UI ONLY from existing components: `JText`, `JField`, `GoldButton`, `OutlineButton`, `JToggle`, `JRadio`, `ScreenScaffold`, `SubHeader`, `GoldIconBox`, `Hairline`, `LoadingPanel`, `JarvisOrb`. Missing something? Add one small shared component next to them, do not style inline.
- Colours ONLY from the palette (`Gold`, `Ivory`, `Muted`, `Surface1`, `Surface2`, `Bg`, `Palettes.current`). No hard-coded colours, no new fonts.
- Shapes: cards 18-24dp, buttons 30dp, chips 18-20dp, bubbles 18dp. Spacing multiples of 4dp, screen side padding same as existing screens.
- No default Material widgets used raw (no plain `Button`, `TextField`, `Switch`).
- The app has 5 interface styles (Classic Gold, Aurora, Nova, Pulse, Folio). A new tab/settings screen must work in all of them (use shared components, check how `StyledHome/Control/Settings` are wired).
- Copy the structure of the closest existing screen (see `docs/skills/ui-design.md`).

## 6. Self-check before delivering (you cannot compile; re-read what you changed)
- Every symbol exists (grep the map/opened files; never invent functions or library APIs). Unsure about an API: say so in CHANGES.
- Imports present, no duplicate declarations, braces balanced, `@Composable` rules respected, no state writes during composition, no blocking work on main thread, API guards above 26.
- Checklists live in `docs/skills/add-feature.md` (Prefs, screen, phone tool, permission, resource, error logging).
- Be honest: state what is untested and what Android does not allow.

## 7. Project facts
- Sandbox has no internet and no compiler: GitHub Actions compiles later. If the owner pastes a build log, fix only the errors in it.
- Data lives in `Prefs` (SharedPreferences), `ChatStore` (files), `Workspace`, `AppLog`, `CustomProviders` (json in filesDir). No browser-storage tricks.
- Networking is plain `HttpURLConnection` / OkHttp (`data/GeminiClient.kt`, `data/CustomProviders.kt`); no new networking libraries without need.
- Two call engines behind `live/CallEngine.kt`: Gemini Live (`LiveClient`) and custom provider (`PipelineClient`).
- `code/Skills.kt` is the APP's own in-app skills feature, not these docs skills.

## 8. Final message (max ~12 lines, owner's language)
1. What you did (2-4 lines). 2. Files changed/added (names only). 3. How to test (2-3 lines). 4. Untested / not possible. 5. Noticed, not changed. 6. The zip.
