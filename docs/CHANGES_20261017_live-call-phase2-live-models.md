# Change · 2026-10-17 · Live Call Phase 2 — Live Call Models + fast connect

Part of the **JARVIS LIVE CALL UPDATE**. This is **Phase 2**.

## Goal
1. Let the user choose and order the Gemini **Live Call models** in the Gemini settings page.
2. Connect fast (~1s): race the top models in parallel and take the first one that finishes setup.

## Changes
- `data/Prefs.kt`
  - New persisted ordered list `Prefs.liveModels` (`livemodels` key). First entry = tried first.
- `data/ModelCatalog.kt`
  - `liveCandidates()` now puts the user's saved order first, then every other Live model of the key.
- `ui/screens/ApiScreen.kt`
  - New **LIVE CALL MODELS** section: every Live model of the key, tap to select, `#n` priority with
    up/down arrows, plus a manual “add a model id” field. Model names come only from `models.list`
    (the API) — nothing is hard-coded.
- `live/LiveClient.kt` — fast parallel connect (rewritten connection):
  - `PARALLEL = 2`: the next two candidate models open WebSockets **at the same time**; the first that
    sends `setupComplete` wins, the loser is closed.
  - `CONNECT_TIMEOUT_MS = 2500`: if a batch does not connect in 2.5s, close it and try the next models.
  - Sequential fallback across all models; if everything fails, one more round with a **minimal setup**
    (no tools / no nested `generationConfig`) for keys/models that reject those fields, then a clear error.
  - Timing logs: `live_connect_start`, `live_connect_batch`, `live_connected <ms>`.
  - Removed the old model-by-model retry cycle.

## How to test
1. Profile → Gemini API → **LIVE CALL MODELS**: select 2–3 models, reorder with arrows.
2. Start a live call. Logcat `Timing`: `live_connect_batch` should list 2 models,
   `live_connected <ms>` should be ~1s on a good network.
3. Unplug / force a bad first model (put it first) → call should still connect via the second.
