# HANDOFF STATE (newest entry on top; each AI adds one entry when it finishes)

## Current state
- App version: workspace + documents update (zip `figma-screen-v8-workspace-documents`) plus a new Custom-provider JSON import. Never compiled by the authoring AIs: expect small compile errors; the owner pastes the GitHub Actions log and the AI fixes only those errors.
- AI workflow v9 + tracking files (Task, Planning, Roadmap, Pending, Complete, Goal) + docs/skills + docs/START_PROMPT.md are in the repo.
- Done: 5 interface styles, Gemini chat/live with model fallback, custom providers (chat + voice calls), console, accessibility tools, floating call bubble, wake word (vosk/porcupine/basic), device admin lock, code & documents workspace with skills, real phone facts, chat actions + live thinking, import a provider .json to add all its models at once.
- Open ideas (not started): Figma frames for the new screens (Figma MCP quota was exhausted), chat/live layouts per interface style, Urdu wake word.

## Next step
- Phone-test the new import (Custom providers > "Import models from .json"), then continue Phase 1: first clean GitHub Actions build.

## Log
- 2026-10-17: Custom providers: added "Import models from .json" (file picker) that reads a provider .json and adds one provider per model (baseURL/apiKey/name + per-model vision/tools). Files: `ui/screens/ProvidersScreen.kt`, `data/CustomProviders.kt`. Untested (not compiled).
- 2026-10-16: merged AI workflow kit (v9) with tracking files, skills and start prompts. No app code changed. Next: first clean GitHub Actions build.
