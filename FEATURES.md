# FEATURES — Jarvis capability registry

Updated 2026-10-17.

| Feature | Status | Source of truth |
|---|---|---|
| Gemini startup, chat and voice core | existing | `data/GeminiClient.kt`, `data/ModelCatalog.kt`, `data/Startup.kt` |
| OpenAI-compatible custom providers | active | `data/CustomProviders.kt`, `ui/screens/ProvidersScreen.kt` |
| Provider model discovery with cached metadata | **active — Phase 1** | `OpenAiCompat.fetchModelDetails`, `providers.json` cache |
| Free-only provider filtering | **active — Phase 1** | API pricing + `:free` ids; unknown models held back as Unverified |
| Manual provider model override | **active — Phase 1** | Provider editor; `source = manual` |
| Offline model catalog (bundled + remote update) | **active — Phase 1** | `data/ModelCatalogFile.kt`, `assets/model_catalog.json` |
| Real provider model probes (latency + tools) | **active — Phase 1** | `data/ProbeService.kt` |
| Provider Auto model selection and bounded retry | active | `data/ChatStore.kt`, `ui/screens/ChatScreen.kt` |
| Coding task checkpoints and exact resume | class exists — **wiring pending (Phase 2)** | `code/TaskStateStore.kt` (not yet referenced by `code/CodeAgent.kt`) |
| Smart model router, cooldown, failover, daily usage | class exists — **wiring pending (Phase 3)** | `data/ModelRouter.kt` (not yet referenced by `data/ChatStore.kt` / `code/CodeAgent.kt`) |
| Network/power-loss auto-resume | class exists — **wiring pending (Phase 4)** | `code/NetworkMonitor.kt`, `code/CodeTaskService.kt` (service not in AndroidManifest) |
| Workspace coding agent and skills | existing | `code/CodeAgent.kt`, `code/Skills.kt`, `code/Workspace.kt` |
| GitHub/raw/zip skill import + 95-skill pack | **planned (Phase 5)** | `code/Skills.kt` (import not implemented) |
| File-based non-sensitive agent memory tools | class exists — **wiring pending (Phase 5)** | `code/MemoryStore.kt` (not yet exposed to `code/AgentTools.kt`) |
