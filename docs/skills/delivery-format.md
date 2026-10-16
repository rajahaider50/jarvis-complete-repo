# SKILL: delivery-format (owner is on a phone)
Return ONE zip with only new/changed files at real paths, e.g. `app/src/main/java/com/jarvis/app/ui/screens/ChatScreen.kt`, plus:
- `docs/CHANGES_<yyyymmdd>_<slug>.md` (what, why, how to test, untested)
- updated `Task.md`, `Planning.md`, `Pending.md`, `Complete.md`, `Goal.md` (if they changed), and a line in `docs/HANDOFF_STATE.md`
Rules: file < ~400 lines or many edits -> COMPLETE file. File 400+ lines with small edits -> `docs/edits/<nnn>.json`:
`[{"file":"app/src/main/java/.../X.kt","old":"exact old text, unique in file","new":"new text"}]`
Owner then: put the json in `docs/edits/` on GitHub, run Actions > "Apply AI edits" with that path, then run "Build APK".
Never deliver the whole project. Never edit `docs/PROJECT_MAP.md`. A single requested document (md/txt/csv) is delivered alone, not zipped.
Final chat message: max ~12 lines in the owner's language (done / files / how to test / untested / noticed / zip).
