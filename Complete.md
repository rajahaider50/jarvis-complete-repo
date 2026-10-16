# COMPLETE (newest first: date - what - files)
- 2026-10-16 - Phase 2 coding task state: durable JSON state, file SHA-256 checkpoints, original-goal resume prompt, stop/error preservation, startup load and Code screen Resume action; Actions build passed - app/src/main/java/com/jarvis/app/code/TaskStateStore.kt, app/src/main/java/com/jarvis/app/code/CodeAgent.kt, app/src/main/java/com/jarvis/app/ui/screens/CodeScreen.kt, app/src/main/java/com/jarvis/app/MainActivity.kt
- 2026-10-16 - Provider discovery phase 1: API model metadata, free-only filtering, unverified grouping, manual model overrides, cached catalog, Chat eligibility, and clean Actions build - FEATURES.md, app/src/main/java/com/jarvis/app/data/CustomProviders.kt, app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt, app/src/main/java/com/jarvis/app/ui/screens/ChatScreen.kt
- 2026-10-16 - Custom provider model catalogs, provider Auto/model picker, and bounded retries for transient provider errors - app/src/main/java/com/jarvis/app/data/CustomProviders.kt, app/src/main/java/com/jarvis/app/data/ChatStore.kt, app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt, app/src/main/java/com/jarvis/app/ui/screens/ChatScreen.kt, app/src/main/java/com/jarvis/app/code/CodeAgent.kt
- 2026-10-16 - AI workflow system: AI_GUIDE, ARCHITECTURE, skills, tracking files, tools and workflows - docs/, tools/, .github/workflows/
- v8 - Workspace code agent + documents (PDF/PNG/DOCX/XLSX/PPTX), in-app Skills, real phone facts + weather, chat actions + thinking card
- v7 - System control tools: end/answer call, lock screen, power menu, screen share by voice, settings intents
- v6 - Gemini error diagnosis + model fallback, provider voice calls (STT -> chat -> TTS)
- v5 - Custom providers, Pulse + Folio interface styles, offline wake phrase
- Earlier - Home boot + briefing, Live call (Gemini Live), Chat with auto model, Control, Profile, 16 orb styles, 15 palettes, Aurora + Nova styles, Console, assistant overlay
