# FEATURES — Jarvis capability registry

Updated 2026-10-18.

| Feature | Status | Source of truth |
|---|---|---|
| Gemini startup, chat and voice core | existing | `data/GeminiClient.kt`, `data/ModelCatalog.kt`, `data/Startup.kt` |
| OpenAI-compatible custom providers | active | `data/CustomProviders.kt`, `ui/screens/ProvidersScreen.kt` |
| Provider model discovery with cached metadata | active — Phase 1 | `OpenAiCompat.fetchModelDetails`, `providers.json` cache |
| Free-only provider filtering | active — Phase 1 | API pricing + `:free` ids; unknown models held back as Unverified |
| Manual provider model override | active — Phase 1 | Provider editor; `source = manual` |
| Offline model catalog (bundled + remote update) | active — Phase 1 | `data/ModelCatalogFile.kt`, `assets/model_catalog.json` |
| Real provider model probes (latency + tools) | active — Phase 1 | `data/ProbeService.kt` |
| Provider Auto model selection and bounded retry | active | `data/ChatStore.kt`, `ui/screens/ChatScreen.kt` |
| Coding task checkpoints and exact resume | active — Phase 2 | `code/TaskStateStore.kt`, `code/CodeAgent.kt`, `ui/screens/CodeScreen.kt` |
| Smart model router, cooldown, failover and daily usage | active — Phase 3 | `data/ModelRouter.kt`, `data/ChatStore.kt`, `code/CodeAgent.kt` |
| Router log (local health/usage) | active — Phase 3 | `data/ModelRouter.kt`, `ui/screens/RouterLogScreen.kt` |
| Network/power-loss auto-resume + foreground service | active — Phase 4 | `code/NetworkMonitor.kt`, `code/CodeTaskService.kt`, `code/CodeAgent.kt`, AndroidManifest |
| Workspace coding agent and skills | existing | `code/CodeAgent.kt`, `code/Skills.kt`, `code/Workspace.kt` |
| SKILL.md skills + bundled 95-skill pack + zip/URL import | active — Phase 5 | `code/Skills.kt`, `assets/skills_pack.json`, `ui/screens/SkillsScreen.kt` |
| File-based non-sensitive agent memory tools | active — Phase 5 | `code/MemoryStore.kt`, `code/AgentTools.kt`, `ui/screens/MemoryScreen.kt` |
| Workspace coding uses custom providers only (no Gemini) | active | `data/ModelRouter.rankWorkspace`, `code/CodeAgent.makeBackend`, `ui/screens/CodeScreen.kt` |
| Workspace Auto/Manual provider model selection + step-by-step failover | active | `data/Prefs.workspaceModel`, `data/ModelRouter.rankWorkspace` |
| Live agent memory review in the Memory screen | active | `code/MemoryStore.kt` (`revision`, `entries`), `ui/screens/MemoryScreen.kt` |
| Visual assistant themes (10) + classic, Theme Lab, sound FX, audio level | active | `ui/theme/**`, `ui/screens/ThemeLabScreen.kt`, `audio/AudioLevel.kt` |
| Live call: instant screen open, prefetch, throttled UI | active — LC Phase 1 | `ui/screens/LiveVoiceScreen.kt`, `live/CallHost.kt`, `live/LiveClient.kt`, `MainActivity.kt` |
| Live call: saved Live Call Models + parallel fast connect | active — LC Phase 2 | `data/Prefs.liveModels`, `data/ModelCatalog.liveCandidates`, `ui/screens/ApiScreen.kt`, `live/LiveClient.kt` |
| Live call: keeps running in-app (down-arrow + mini bar) | active — LC Phase 3 | `live/CallService.kt`, `ui/components/MiniCallBar.kt`, `MainActivity.kt`, `ui/screens/LiveVoiceScreen.kt` |
| Live call: in-app recorder (screen + both voices) + mic-silent fix | active — LC Phase 4 | `live/ScreenRecordService.kt`, `live/CallAudioTap.kt`, `live/AudioMixer.kt`, `live/CallEngine.kt`, `ui/screens/LiveVoiceScreen.kt` |
| Live call: AI control — 5-5 option popups + app tools | active — LC Phase 5 | `live/AiPanel.kt`, `live/AppControl.kt`, `live/PhoneTools.kt`, `live/LiveClient.kt`, `ui/components/AiOptionsOverlay.kt`, `data/NavState.kt`, `data/Prefs.aiControl` |
| Screen stream: single MediaProjection + share via accessibility takeScreenshot | active — Stream P1-4 | `live/ScreenRecordService.kt`, `live/ScreenShareService.kt`, `live/JarvisAccessibilityService.kt`, `res/xml/accessibility_service_config.xml` |
| Screen-stream frame policy (change detection, idle slow-down, action burst, privacy pause) | active — Stream P4 | `live/ScreenShareService.kt` |
| AI cursor coordinate engine (centre 0, right/up +, 2 decimals, real display size) | active — Cursor A1 | `live/CursorGeometry.kt`, `ui/screens/CursorLabScreen.kt` |
| On-screen AI cursors (up to 10, drag, colours/numbers, grid, NOT_TOUCHABLE during tap) | active — Cursor A2 | `live/CursorController.kt`, `ui/screens/CursorLabScreen.kt` |
| Cursor gestures + floating control panel + joystick + PANIC | active — Cursor A3-A4 | `live/CursorController.kt`, `live/CursorPanel.kt`, `live/JarvisAccessibilityService.kt` |
| AI cursor tools (create/move/click/swipe/click_all/grid/find_element/look_at_screen) | active — Cursor A5 | `live/AppControl.kt`, `live/PhoneTools.kt`, `data/Prefs.aiCursor` |
| Click-lesson memory (ui_clicks.json + ui_clicks.md + journal, recall/save, corrections, privacy) | active — Cursor A6 | `live/ClickMemory.kt`, `live/CallHost.kt`, `ui/screens/CursorMemoryScreen.kt` |
| Live call keep-alive: session resumption + context compression + goAway reconnect | active — Stream P5-6 | `live/LiveClient.kt`, `data/Prefs.liveResume/liveCompress`, `ui/screens/ProfileScreen.kt` |
| Diagnostics screen + `JARVIS_DIAG` logger + Cursor Test | active — Bugfix P1 | `live/Diag.kt`, `ui/screens/DiagnosticsScreen.kt`, `MainActivity.kt`, `ui/screens/ProfileScreen.kt` |
| Call stability: tools off reader thread, video guard, VAD/activityHandling + barge-in toggle, audio priority + pre-buffer, wake/Wi-Fi locks, backoff + network callback, thermal-aware share | active — Bugfix M3 | `live/LiveClient.kt`, `live/CallLocks.kt`, `live/ScreenShareService.kt`, `data/Prefs.liveBargeIn` |
| Single-owner microphone with fan-out + self-heal (source fallback, release/recreate) | active — Bugfix M1 | `live/MicCaptureHub.kt`, `live/LiveClient.kt`, `live/WakeService.kt` |
| AI cursor overlay marshalled to the main thread (+ service restore) | active — Bugfix M2 | `live/CursorController.kt`, `live/CursorPanel.kt`, `live/AppControl.kt` |
| Mic recovery keeps the source (AEC held + logged) + half-duplex echo guard + no-max volume + auto-earpiece hysteresis + make-before-break reconnect + per-answer pre-buffer + no-audio model list | active — Round 3 Step A | `live/MicCaptureHub.kt`, `live/LiveClient.kt`, `audio/AudioRouter.kt`, `data/Prefs.kt`, `data/ModelCatalog.kt`, `live/CallService.kt` |
| Cursor never eats its own tap (always NOT_TOUCHABLE in AI mode + "Move" switch + truthful `touchableAtClick`) | active — Round 3 Step B | `live/CursorController.kt`, `live/CursorPanel.kt`, `ui/screens/TapTestScreen.kt` |
| Truthful percent gestures (real display size + real `GestureOutcome`) | active — Round 3 Step B | `live/JarvisAccessibilityService.kt`, `live/PhoneTools.kt` |
| Element tools: `get_clickable_elements` / `click_element` / `cursor_move_to_element` + multi-match `find_element` | active — Round 3 Step B | `live/JarvisAccessibilityService.kt`, `live/PhoneTools.kt`, `live/AppControl.kt` |
| `click_on_screen` verifies the screen actually changed (800 ms) | active — Round 3 Step B | `live/JarvisAccessibilityService.kt`, `live/PhoneTools.kt` |
| Screen-share detail (1280 px q70 / 800 q55 hot / Fast) + save-one-frame debug PNG | active — Round 3 Step B | `live/ScreenShareService.kt`, `data/Prefs.shareQuality`, `ui/screens/DiagnosticsScreen.kt` |
| Only necessary tool parameters marked `required` | active — Round 3 Step B | `live/PhoneTools.kt` |
| Call setup off the main thread (`withContext(Dispatchers.Main)` only for the overlay) | active — Round 3 Step C | `live/CallHost.kt` |
