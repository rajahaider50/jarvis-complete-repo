# HANDOFF STATE (newest entry on top; each AI adds one entry when it finishes)

## Current state
- Upgrade Guide Phase 1 is DONE and builds green (`Build APK` runs 48/49, commit 222f1aa). Provider model discovery now reads real price/context/tools, free-only filter with an Unverified group, manual model entry, an offline model catalog (bundled + optional remote URL update) and real per-model probes. Files: data/CustomProviders.kt (ProviderModel+freeOnly+fetchModelDetails), data/ModelCatalogFile.kt (new), data/ProbeService.kt (new), app/src/main/assets/model_catalog.json (new), MainActivity.kt, Prefs.kt, ui/screens/ProvidersScreen.kt. Doc: docs/CHANGES_20261017_phase1-providers-catalog.md.
- IMPORTANT: `ModelRouter`, `TaskStateStore`, `NetworkMonitor`, `CodeTaskService`, `MemoryStore` exist as classes but are NOT referenced anywhere (dead code) — the earlier phase docs over-claimed. Real wiring for Phase 2 (task state) / Phase 3 (router) / Phase 4 (network) / Phase 5 (skills+memory) is still to do.
- App version: v8 workspace + documents, Custom-provider JSON import, green `Build APK`.
- AI workflow v9 + tracking files + docs/skills are in the repo.
- Done: 50 interface styles (5 layouts engines), 100 orb styles, 50 themes, Gemini chat/live fallback, custom providers, console, accessibility tools, call bubble, wake word, code/documents workspace, provider JSON import.

## Next step
- Install the debug APK on a phone and test Phase 1 (provider discovery + free filter + probes); then start Phase 2: wire `TaskStateStore` into `CodeAgent`/`CodeScreen` (checkpoints + resume).

## Log
- 2026-10-17: Upgrade Guide Phase 1 done (providers + catalog + probes). `ProviderModel` extended with real fields; `CustomProvider.freeOnly`; `OpenAiCompat.fetchModelDetails`; new `ModelCatalogFile` + `assets/model_catalog.json` + `ProbeService`; ProvidersScreen groups Free/Paid/Unverified, manual add, catalog panel. Build green (runs 48/49).
- 2026-10-17: Premium interface styles now have unique LAYOUTS. New engine 5 in ui/styles/IfaceScreens.kt with 4 home / 3 control / 3 settings archetypes, 5 nav bars and 4 corner radii; recipe per style via IfaceCatalog.cfgFor. StyleKit + Components route engine 5. Build green (runs 38976576705 / 38976561352).
- 2026-10-17: Interface styles 5 -> 50 (25 native-Android premium + 25 iOS premium). Added `ui/styles/IfaceCatalog.kt` (45 styles each with palette + layout engine) and decoupled routing via new `Palettes.engine`/`Palettes.serif`.
- 2026-10-17: Voice orb styles 50 -> 100 (+50 iOS/premium, indexes 50..99) and colour themes 15 -> 50 (+35 dark premium palettes). Files: `ui/components/SiriOrb.kt`, `ui/theme/Color.kt`. ThemeScreen is dynamic so it shows the new counts automatically. `Build APK` green (run 38972312028). Phone test still pending.
- 2026-10-17: Voice orb styles expanded 16 -> 50 (added 34 professional/premium styles). `ORB_STYLES` now 50 names; `when(style.coerceIn(0,49))` with 34 new draw fns + `polygon()` helper. ThemeScreen auto-shows them. File: `ui/components/SiriOrb.kt`. First CI run caught one error (`Path.lineTo` needs x,y floats, not an `Offset`, in `prism`); fixed. `Build APK` now green (run 38970254753). Phone test still pending.
- 2026-10-17: Fixed all compile errors so `Build APK` passes. Added the smart-router pieces that were missing (`ProviderModel`, `CustomProvider.models`/`dailyLimitTokens`, `CustomProviders.eligibleModels`), `override` on `LiveClient.switchLanguage`, explicit `ColumnScope` receiver for the Home `AnimatedVisibility`, and renamed `WakeService.paused`/`Overlay.setExpanded` to fix JVM signature clashes. Files: `data/CustomProviders.kt`, `live/LiveClient.kt`, `ui/screens/HomeScreen.kt`, `live/WakeService.kt`, `overlay/Overlay.kt`.
- 2026-10-17: Custom providers: added "Import models from .json" (file picker) that reads a provider .json and adds one provider per model (baseURL/apiKey/name + per-model vision/tools). Files: `ui/screens/ProvidersScreen.kt`, `data/CustomProviders.kt`. Untested (not compiled).
- 2026-10-16: merged AI workflow kit (v9) with tracking files, skills and start prompts. No app code changed. Next: first clean GitHub Actions build.
