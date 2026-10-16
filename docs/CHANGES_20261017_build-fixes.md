# CHANGES 2026-10-17 · Fix GitHub Actions build (compile errors)

## What changed
- **`data/CustomProviders.kt`**: added the missing model-router pieces the smart-router phase referenced but never defined:
  - `data class ProviderModel(id, source, contextWindow, supportsTools, qualityTier)`
  - `CustomProvider.models: List<ProviderModel>` and `CustomProvider.dailyLimitTokens: Long` (both defaulted, so existing calls/saved JSON keep working)
  - `CustomProviders.eligibleModels(p)` (the provider's discovered models, or its single manual model)
- **`live/LiveClient.kt:288`**: `switchLanguage` now has the `override` modifier (it overrides `CallEngine.switchLanguage`).
- **`ui/screens/HomeScreen.kt:104`**: the boot `AnimatedVisibility` inside a `Box` now uses the explicit `ColumnScope` receiver (`val col = this` + `col.AnimatedVisibility(...)`) to fix the implicit-receiver error.

## Why
The GitHub `Build APK` job failed with these errors; none were in the new JSON-import code, they were pre-existing in the smart-router / live / home code.

## How to test
Push/re-run the `Build APK` workflow; the Kotlin compile step should pass.

## Untested
Nothing was compiled locally; verification is the GitHub Actions build.
