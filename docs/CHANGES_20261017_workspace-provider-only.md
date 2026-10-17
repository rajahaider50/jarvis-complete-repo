# Changes — 2026-10-17 — Workspace uses provider models only (Auto/Manual) + live memory

## What changed
The coding Workspace no longer uses Gemini at all. It now routes every run through the
user's **custom providers** only, with a selectable provider model and automatic failover.

### 1. Workspace provider selection (Manual vs Auto) — no Gemini
- New setting `Prefs.workspaceModel`: `""` = **Auto**, `"custom:<id>"` = **Manual** (one provider).
- New `ModelRouter.rankWorkspace(onlyProviderId, task, est, needsTools)`:
  - **Auto** (`onlyProviderId == null`): every custom provider's usable models, best score
    first — when one model's limit/health runs out the router moves to the next suitable
    model and the next provider, step by step.
  - **Manual** (`onlyProviderId != null`): exactly that provider's chosen model, nothing else.
  - Only `CustomProviders.list` is used — Gemini is never a candidate here.
- `CodeAgent.makeBackend` now always returns a provider backend built from `rankWorkspace`;
  the Gemini coding backend was removed. If no provider is ready the run stops with a clear
  message ("Gemini is not used in the Workspace").
- Workspace UI (`CodeScreen.ProjectBar`): a new model chip shows the current route
  ("Auto · all providers, step by step" or "<provider> · <model>") and opens a menu to pick
  Auto or any provider (Manual).

### 2. Skills used per task
- `CodeAgent.systemPrompt` now explicitly instructs the agent to open every skill whose
  description matches the task with `use_skill(name)` before working.

### 3. Live memory, reviewable by the user
- `MemoryStore` exposes `revision` (Compose state, bumped on every write) and `entries()`
  (topic file + size).
- `MemoryScreen` gained an **AGENT MEMORY (LIVE)** section: it lists every memory topic the
  agent has written, shows byte size, a live update counter, and expands to show content.
  New memory appears in real time as the agent works.

## Files
- `app/src/main/java/com/jarvis/app/data/Prefs.kt`
- `app/src/main/java/com/jarvis/app/data/ModelRouter.kt`
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `app/src/main/java/com/jarvis/app/code/MemoryStore.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/CodeScreen.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/MemoryScreen.kt`

## How to verify
1. Settings > Providers: add at least two providers (or import a provider .json).
2. Open Workspace > the new model chip > choose **Auto**. Start a coding task; the header
   shows the model in use. When a model returns a quota/limit error the router switches to
   the next model/provider automatically.
3. Choose a single provider (Manual): only that provider's model is used.
4. With no provider added, a coding task stops with the "Add a provider" message (Gemini is
   never used in the Workspace).
5. Memory screen: run a coding task that writes memory; the AGENT MEMORY section updates live.
