# HANDOFF STATE (every AI adds a line when it finishes; the next account starts here)

## Current version
v8: workspace agent + documents + skills, real phone facts, chat actions + streaming thinking, 5 interface styles, custom providers + provider voice calls, system control tools, console, offline wake word. AI-workflow docs, tracking files and tools added (v8.1).

## Done (newest first)
- v8.1 AI workflow: AI_GUIDE, ARCHITECTURE, skills, tracking files (Task/Planning/Roadmap/Pending/Complete/Goal), tools (make_map, make_pack, apply_edits) + workflows (map, pack, apply-edits).
- v8 Workspace (code agent, Files/Output, DocGen), Skills, RealFacts + weather, chat message actions + thinking card.
- v7 System control: end/answer call, lock screen, power menu, screen share by voice, settings intents.
- v6 Gemini error diagnosis + model fallback, provider voice calls.
- v5 Custom providers, Pulse + Folio styles, Vosk offline wake phrase.

## Known open points
- Check the first GitHub Actions build log; fix compile errors only.
- PPTX output should be opened once in PowerPoint/Google Slides.
- Vosk / Porcupine / jna dependency versions may need a bump if Gradle cannot resolve them.

## Next step
- Done: custom provider save now fetches and persists the complete `/models` catalog; Chat and Code use provider Auto/model selection with retries for transient HTTP errors.
- Done: provider discovery phase 1 adds API pricing/context/tools metadata, Free only filtering, Unverified grouping, manual model metadata, cached provider catalogs, and Chat eligibility; GitHub Actions build passed on run 38932098213.
- Done: Phase 2 adds durable JSON task state, original-goal pinning, SHA-256 file checkpoints, stop/error preservation, startup loading and Code screen Resume action; build passed on run 38934731149.
- Done: Phase 3 adds scored custom-provider routing for Chat and Code, context/tools eligibility, per-model cooldown/backoff, latency/success health, daily token usage limits, and Router console logging; build passed on run 38936841893.
- Next step: phone-test Phase 2/3 behavior, then implement Phase 4 network/power-loss resilience and auto-resume.
