# FEATURES — Jarvis capability registry

Updated 2026-10-16.

| Feature | Status | Source of truth |
|---|---|---|
| Gemini startup, chat and voice core | existing | `data/GeminiClient.kt`, `data/ModelCatalog.kt`, `data/Startup.kt` |
| OpenAI-compatible custom providers | active | `data/CustomProviders.kt`, `ui/screens/ProvidersScreen.kt` |
| Provider model discovery with cached metadata | active in v8.3 phase 1 | `/models` response; `providers.json` cache |
| Free-only provider filtering | active in v8.3 phase 1 | API pricing and `:free` IDs; unknown models stay unverified |
| Manual provider model override | active in v8.3 phase 1 | Provider editor; source is `manual` |
| Provider Auto model selection and bounded retry | active | `data/ChatStore.kt`, `ui/screens/ChatScreen.kt` |
| Coding task checkpoints and exact resume foundation | active in v8.4 phase 2 | `code/TaskStateStore.kt`, `code/CodeAgent.kt`, `ui/screens/CodeScreen.kt` |
| Workspace coding agent and skills | existing | `code/CodeAgent.kt`, `code/Skills.kt`, `code/Workspace.kt` |
| Durable task checkpoints/resume | planned | JARVIS update guide Phase 2 |
| Cross-provider scored router and usage limits | planned | JARVIS update guide Phase 3 |
| Network/power-loss auto-resume | planned | JARVIS update guide Phase 4 |
| GitHub skill import and file-based memory tools | planned | JARVIS update guide Phase 5 |
