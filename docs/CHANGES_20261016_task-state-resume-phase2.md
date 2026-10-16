# Task state and exact resume — Phase 2

## What changed

Coding runs now create a durable local task state before the first model request. The state pins the original user goal and stores project identity, status, current file, partial editor content, checkpoint notes, and the current project file inventory with SHA-256 hashes. State is written to the app's private `code_task_state.json` file; no API keys or full model secrets are stored.

`CodeAgent` checkpoints after backend turns and every tool result, preserves state on cancellation or errors, and clears it only after a successful finish. A resumed run sends a dedicated resume prompt built from the saved state instead of relying on the previous model's memory. `MainActivity` loads unfinished state on app startup, and the Code workspace shows a Resume previous task card for the matching project using the existing shared UI style.

## Changed files

- `Task.md`
- `Planning.md`
- `Complete.md`
- `Pending.md`
- `FEATURES.md`
- `app/src/main/java/com/jarvis/app/MainActivity.kt`
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `app/src/main/java/com/jarvis/app/code/TaskStateStore.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/CodeScreen.kt`
- `docs/HANDOFF_STATE.md`
- `docs/CHANGES_20261016_task-state-resume-phase2.md`

## How to test on the phone

1. Open Code workspace, start a small coding task and wait until at least one file is written.
2. Stop the run or force-close the app before `finish`; reopen the app and open the same project.
3. Confirm the **UNFINISHED TASK** card appears, then tap **Resume previous task**. The model should receive the original goal plus saved file/checkpoint context and continue in the same project.
4. Finish the task and confirm the Resume card disappears. A successful finish removes the local pending state; stop/error keeps it.

## Verification

GitHub Actions Build APK run `38934731149` passed after the two compiler corrections from the first attempt (`TaskStateStore` mutable plan and `CodeScreen` import). The local diff passed `git diff --check`.

## Untested / limitations

- A real Android force-stop/reopen and duplicate-content test still needs to be performed on the owner's phone.
- This phase stores a compact conversation/checkpoint note tail and file hashes; it does not yet persist the full provider message transcript or a structured per-step plan/decision editor.
- The hash scan runs off the main thread, but the small JSON state write is intentionally kept simple and local.
- Network loss auto-resume, foreground service/WorkManager execution, scored cross-provider routing, quota/cooldown tracking, and skill import are later phases.

## Noticed, not changed

Existing Workspace file operations, provider routing, Gemini voice/call flows and all screen styles were left intact. The existing `Pending.md` open items remain except for the newly added Phase 2 phone validation item.
