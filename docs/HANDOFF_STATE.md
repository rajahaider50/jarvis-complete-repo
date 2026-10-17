# HANDOFF STATE (newest entry on top; each AI adds one entry when it finishes)

## Current state
- **JARVIS LIVE CALL UPDATE: all phases implemented and building green.** Phase 1 instant open/lag (run 39049063340), Phase 2 saved Live Call Models + parallel fast connect (39049749611), Phase 3 down-arrow minimise + mini bar (39050810331), Phase 4 in-app recorder (screen + both voices) + mic-silent fix (39051398001), Phase 5 AI control: 5-5 option popups + app tools (39052284373).
- **Two new features complete and building green.** (1) Workspace coding is provider-only (no Gemini) with Auto / Manual selection and a live agent-memory review section — run 62 green. (2) Full visual themes + sound FX system per JARVIS_VISUAL_THEMES_GUIDE: 10 themes + classic, Theme Lab, SynthFx/FxPlayer, AudioLevel, PerformanceTier — run 66 green.
- **JARVIS UPGRADE GUIDE: all 5 phases implemented and building green.** Phase 1 providers/catalog/probes (runs 48/49), Phase 2 task state + resume (51/52), Phase 3 smart router + failover + router log (53/54), Phase 4 network/power auto-resume + foreground service (55/56), Phase 5 SKILL.md skills + 95-skill pack + memory tools (59/60).
- No longer dead code: `ModelRouter` (chat + coding + router log), `TaskStateStore` (checkpoints + resume card), `NetworkMonitor` + `CodeTaskService` (manifest + auto-resume), `MemoryStore` (agent memory tools).
- App version: v8 workspace + documents, Custom-provider JSON import, green `Build APK`.
- Done: 50 interface styles (5 layout engines), 100 orb styles, 50 themes, Gemini chat/live fallback, custom providers with discovery/free filter/catalog/probes, console, accessibility tools, call bubble, wake word, code/documents workspace, provider JSON import.
- Phone testing of all phases is still pending.

## Next step
- Install the debug APK and test end to end: provider discovery + free filter + probes; a coding task killed mid-run and resumed; router log; network drop/resume; skill pack install + a group enable + import a SKILL.md.

## Log
- 2026-10-17: Live call phase 5 (AI control). Assistant shows 5-5 option popups via show_options(select_option); new tools get_app_state, close_options, set_camera, set_screen_recording, set_screen_share, navigate; `AppControl`, `AiPanel`, `AiOptionsOverlay`, `NavState`, `CallCamera`, `ScreenRecordRequestActivity`, `Prefs.aiControl` + Profile toggle. Build green (run 39052284373). See docs/CHANGES_20261017_live-call-phase5-ai-control.md.
- 2026-10-17: Live call phase 4 (in-app recorder + mic fix). Screen video + both voices to Movies/Jarvis; mic-silent detection. Build green (run 39051398001). See docs/CHANGES_20261017_live-call-phase4-recorder.md.
- 2026-10-17: Live call phase 3 (background). Down-arrow minimise + MiniCallBar on other screens; camera FGS type. Build green (run 39050810331; fix: `nav.currentBackStackEntryAsState()` receiver). See docs/CHANGES_20261017_live-call-phase3-minimize.md.
- 2026-10-17: Live call phase 2 (fast connect). Saved Live Call Models + parallel 2-trial race (2.5s). Build green (run 39049749611). See docs/CHANGES_20261017_live-call-phase2-live-models.md.
- 2026-10-17: Live call phase 1 (lag). Removed fixed delay, off-main-thread setup, throttled level UI, prewarm. Build green (run 39049063340). See docs/CHANGES_20261017_live-call-phase1-lag.md.
- 2026-10-17: Visual themes + sound FX system added (JARVIS_VISUAL_THEMES_GUIDE). 10 themes + classic, Theme Lab, SynthFx/FxPlayer, AudioLevel, PerformanceTier. Build green (run 66; one fix: HUD `label` needed @Composable). See docs/CHANGES_20261017_visual-themes-soundfx.md.
- 2026-10-17: Workspace now provider-only (no Gemini) with Auto/Manual selection + step-by-step failover; Memory screen shows live agent memory. Build green (run 62). See docs/CHANGES_20261017_workspace-provider-only.md.
- 2026-10-17: Phases 2-5 done (see docs/CHANGES_20261017_phases2-5-router-resume-network-skills.md). Build green (runs 59/60 after adding two missing imports in SkillsScreen).
- 2026-10-17: Upgrade Guide Phase 1 done (providers + catalog + probes). Build green (runs 48/49).
- 2026-10-17: Premium interface styles now have unique LAYOUTS (engine 5). Build green.
- 2026-10-17: Interface styles 5 -> 50 (25 native-Android premium + 25 iOS premium).
- 2026-10-17: Voice orb styles 50 -> 100 (+50 iOS/premium, indexes 50..99) and colour themes 15 -> 50 (+35 dark premium palettes). Files: `ui/components/SiriOrb.kt`, `ui/theme/Color.kt`. ThemeScreen is dynamic so it shows the new counts automatically. `Build APK` green (run 38972312028). Phone test still pending.
- 2026-10-17: Voice orb styles expanded 16 -> 50 (added 34 professional/premium styles). `ORB_STYLES` now 50 names; `when(style.coerceIn(0,49))` with 34 new draw fns + `polygon()` helper. ThemeScreen auto-shows them. File: `ui/components/SiriOrb.kt`. First CI run caught one error (`Path.lineTo` needs x,y floats, not an `Offset`, in `prism`); fixed. `Build APK` now green (run 38970254753). Phone test still pending.
- 2026-10-17: Fixed all compile errors so `Build APK` passes. Added the smart-router pieces that were missing (`ProviderModel`, `CustomProvider.models`/`dailyLimitTokens`, `CustomProviders.eligibleModels`), `override` on `LiveClient.switchLanguage`, explicit `ColumnScope` receiver for the Home `AnimatedVisibility`, and renamed `WakeService.paused`/`Overlay.setExpanded` to fix JVM signature clashes. Files: `data/CustomProviders.kt`, `live/LiveClient.kt`, `ui/screens/HomeScreen.kt`, `live/WakeService.kt`, `overlay/Overlay.kt`.
- 2026-10-17: Custom providers: added "Import models from .json" (file picker) that reads a provider .json and adds one provider per model (baseURL/apiKey/name + per-model vision/tools). Files: `ui/screens/ProvidersScreen.kt`, `data/CustomProviders.kt`. Untested (not compiled).
- 2026-10-16: merged AI workflow kit (v9) with tracking files, skills and start prompts. No app code changed. Next: first clean GitHub Actions build.
