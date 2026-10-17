# CHANGES — 2026-10-17 · Upgrade Guide Phases 2–5 (task state, router, network, skills)

Completes the JARVIS UPGRADE GUIDE after Phase 1. All additive; existing UI style untouched.

## Phase 2 — Task state, checkpoints and resume (Feature D)
- `code/CodeAgent.kt`: every run now creates a `TaskStateStore.State`; after each tool round it writes a **checkpoint** (completed file list + current file + partial content). On finish it clears the state; on stop/error/step-limit it pauses and keeps the state.
- New `resumeTask()` restarts the unfinished task from the checkpoint, keeping the **immutable original goal**, and feeds `TaskStateStore.resumePrompt`.
- `ui/screens/CodeScreen.kt`: shows an "UNFINISHED TASK" card with Resume / Discard when a checkpoint exists.
- `MainActivity.kt`: `TaskStateStore.init`.

## Phase 3 — Smart model router + failover + log (Feature C)
- `data/ChatStore.kt`: provider chat now goes through `ModelRouter.rank` (best first, includes fallbacks), records success/failure, latency and daily token usage.
- `code/CodeAgent.kt`: the provider backend is a router backend that fails over to the next model on retryable errors while keeping the message history.
- `data/ModelRouter.kt`: added `snapshot()` + `clearAll()` for the log.
- `ui/screens/RouterLogScreen.kt` (new) + a "Router log" button in `ui/screens/ProvidersScreen.kt`.

## Phase 4 — Network / power-loss resilience (Feature E)
- `code/CodeAgent.kt`: `init(ctx)` registers a `NetworkMonitor` listener; a run pauses to `waiting_network` when the network drops and **auto-resumes** when it returns. A foreground `CodeTaskService` keeps the process alive during a run.
- `AndroidManifest.xml`: registered `.code.CodeTaskService` (`foregroundServiceType="dataSync"`) + `FOREGROUND_SERVICE_DATA_SYNC` permission.
- `MainActivity.kt`: `CodeAgent.init`.

## Phase 5 — Skills + memory tools (Feature F)
- `code/Skills.kt`: `Skill` gained `category`, `phase`, `source`; added `importMarkdown` (SKILL.md frontmatter parser), `importZip`, `importFromUrl`, `installPack`, `phases`, `setPhaseEnabled`.
- `app/src/main/assets/skills_pack.json` (new): the **95-skill pack** (voice, phone control, productivity, research, vision, files, coding, memory, …) in 3 phases. Installed at start-up; pack skills start **disabled** so you enable only the groups you need (progressive loading: only name + one line go to the model).
- `ui/screens/SkillsScreen.kt`: pack install/refresh, per-group enable chips, collapsible pack list, import from link (.zip / raw GitHub SKILL.md).
- `code/AgentTools.kt`: new agent tools `memory_list`, `memory_read`, `memory_write`, `memory_append`, `memory_replace` (backed by `code/MemoryStore.kt`, which refuses probable secrets).
- `MainActivity.kt`: `MemoryStore.init`, `Skills.installPack`.

## How to verify
- Coding: start a task, kill the app mid-run, reopen Workspace > Agent → "Resume task".
- Router: Profile > Custom providers > Router log → real tokens/cooldowns per model.
- Network: start a task, turn off Wi-Fi/mobile data → "will resume"; turn it back on → it resumes.
- Skills: Settings > Skills → install the pack, enable a group, tap a skill; or import a SKILL.md from a link.

## Files
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `app/src/main/java/com/jarvis/app/code/Skills.kt`
- `app/src/main/java/com/jarvis/app/code/AgentTools.kt`
- `app/src/main/java/com/jarvis/app/data/ModelRouter.kt`
- `app/src/main/java/com/jarvis/app/data/ChatStore.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/CodeScreen.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/SkillsScreen.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/RouterLogScreen.kt` (new)
- `app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/skills_pack.json` (new)
- `app/src/main/java/com/jarvis/app/MainActivity.kt`
