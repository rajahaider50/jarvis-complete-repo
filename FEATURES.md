# FEATURES — Jarvis capability registry

Updated 2026-10-17.

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
