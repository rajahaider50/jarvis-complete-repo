# HANDOFF STATE (newest entry on top; each AI adds one entry when it finishes)

## Current state
- App version: v8 workspace + documents, Custom-provider JSON import, and a **green `Build APK`** on GitHub Actions (run 38962801453, artifact `jarvis-debug-apk`). The compiler now passes; still needs phone testing.
- AI workflow v9 + tracking files (Task, Planning, Roadmap, Pending, Complete, Goal) + docs/skills + docs/START_PROMPT.md are in the repo.
- Done: 5 interface styles, Gemini chat/live with model fallback, custom providers (chat + voice calls), console, accessibility tools, floating call bubble, wake word (vosk/porcupine/basic), device admin lock, code & documents workspace with skills, real phone facts, chat actions + live thinking, import a provider .json to add all its models at once.
- Open ideas (not started): Figma frames for the new screens (Figma MCP quota was exhausted), chat/live layouts per interface style, Urdu wake word.

## Next step
- Install the debug APK on a phone and test it (chat, both call engines, wake word, overlay, provider JSON import); then continue Phase 1 test pass.

## Log
- 2026-10-17: Voice orb styles expanded 16 -> 50 (added 34 professional/premium styles). `ORB_STYLES` now 50 names; `when(style.coerceIn(0,49))` with 34 new draw fns + `polygon()` helper. ThemeScreen auto-shows them. File: `ui/components/SiriOrb.kt`. First CI run caught one error (`Path.lineTo` needs x,y floats, not an `Offset`, in `prism`); fixed. `Build APK` now green (run 38970254753). Phone test still pending.
- 2026-10-17: Fixed all compile errors so `Build APK` passes. Added the smart-router pieces that were missing (`ProviderModel`, `CustomProvider.models`/`dailyLimitTokens`, `CustomProviders.eligibleModels`), `override` on `LiveClient.switchLanguage`, explicit `ColumnScope` receiver for the Home `AnimatedVisibility`, and renamed `WakeService.paused`/`Overlay.setExpanded` to fix JVM signature clashes. Files: `data/CustomProviders.kt`, `live/LiveClient.kt`, `ui/screens/HomeScreen.kt`, `live/WakeService.kt`, `overlay/Overlay.kt`.
- 2026-10-17: Custom providers: added "Import models from .json" (file picker) that reads a provider .json and adds one provider per model (baseURL/apiKey/name + per-model vision/tools). Files: `ui/screens/ProvidersScreen.kt`, `data/CustomProviders.kt`. Untested (not compiled).
- 2026-10-16: merged AI workflow kit (v9) with tracking files, skills and start prompts. No app code changed. Next: first clean GitHub Actions build.
