# CHANGES — 2026-10-17 · Phase 1: provider model discovery, free filter, manual models, catalog, probes

Upgrade Guide Phase 1 (Features A + B). All changes are additive; existing behaviour is kept.

## What changed
- **`data/CustomProviders.kt`**
  - `ProviderModel` now carries only real values: `source` (`api|catalog|probe|manual`), `contextWindow`, `supportsTools`, `qualityTier`, **`isFree` (`true`/`false`/`null = unknown`)**, `pricePrompt`, `priceCompletion`, `notes`, `lastVerifiedAt`.
  - `CustomProvider` gained **`freeOnly: Boolean = true`** (default on) and now persists its discovered `models` list.
  - Saved/loaded `models` + `freeOnly` in `providers.json` (backward compatible).
  - `OpenAiCompat.fetchModelDetails(p)` parses `/models` for price, context size and tool support without guessing; fills known gaps from the local catalog.
  - `CustomProviders.eligibleModels(p)` honours `freeOnly` (only `isFree == true`); `modelsOf(p)` and `isFreeKnown(p)` helpers added.
- **`data/ModelCatalogFile.kt` (new)** — offline catalog loader. Reads bundled `assets/model_catalog.json`, can be replaced by a downloaded copy. `lookup(id)`, `update(ctx,url)`, `count()`, `source`, `version`.
- **`app/src/main/assets/model_catalog.json` (new)** — small fallback catalog (only confirmed values).
- **`data/ProbeService.kt` (new)** — real probe: one tiny chat call (latency + answer) and an optional tool-calling check. Returns real results; failures are reported, never faked.
- **`data/Prefs.kt`** — new `catalogUrl` setting (remote catalog URL), saved/loaded.
- **`MainActivity.kt`** — `ModelCatalogFile.init(this)` on start.
- **`ui/screens/ProvidersScreen.kt`**
  - Provider editor: "Free models only" switch, discovered models grouped **Free / Paid / Unverified**, each row shows context size + tool support + verified state, tap to use, ⓘ to probe.
  - "Add a model by hand" (id, context, tier, tools, free) for servers that hide price info.
  - Model catalog panel: shows source/version and updates from a URL.
  - Provider list card shows model count + free-only state.

## How to verify
1. Profile > Custom providers > Add provider (e.g. OpenRouter preset) > paste key > **Fetch the model list** → models appear grouped by Free/Paid/Unverified.
2. Leave **Free models only** on and Save → tap **Use in chat**; only free models are used.
3. Tap the ⓘ on a model → it answers; the row shows "Verified in … ms".
4. Add a model by hand for a server that hides prices; it is stored with `unknown` where data is missing.
5. Paste a `model_catalog.json` URL and press **Save URL and update now**; the catalog version/count updates.

## Files
- `app/src/main/java/com/jarvis/app/data/CustomProviders.kt`
- `app/src/main/java/com/jarvis/app/data/ModelCatalogFile.kt` (new)
- `app/src/main/java/com/jarvis/app/data/ProbeService.kt` (new)
- `app/src/main/java/com/jarvis/app/data/Prefs.kt`
- `app/src/main/java/com/jarvis/app/MainActivity.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt`
- `app/src/main/assets/model_catalog.json` (new)
