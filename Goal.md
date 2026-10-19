# GOAL (what the app must become). Tick/remove only when really reached.
- [ ] Luxury AI assistant that speaks and acts like a real phone assistant (voice-to-voice calls, wake word, overlay).
- [ ] Works with the owner's own Gemini key and any OpenAI-compatible provider; no fake models or fake data.
- [ ] Real phone control through the tools Android allows.
- [ ] One consistent premium design across all screens and all 5 interface styles.
- [ ] Stable APK built by GitHub Actions from a phone, with no manual PC steps.
- [ ] Any AI can add a feature using only the docs + a few files without breaking existing code.
- [ ] **Autonomous companion server (own backend, no third-party lock-in):** a real HTTP server the owner runs on Termux / a free host; it stores long-term memory, skills and jobs in its own database, can learn by itself while the owner is busy, and never returns fake data.
- [ ] **App ↔ server connection is real and offline-safe:** the app keeps working with no server; when the server is reachable it syncs memory/skills/state and can render server-defined screens (Server-Driven UI). Every screen must show the true connection state.
- [ ] **Background skill learning:** tasks the owner gives ("is tarah ki website banane ka tareeqa seekho") are queued, distilled into lessons + skills by the server on its own, and land in the app's memory/skills — verifiable, not simulated.
- [ ] **Human-like memory:** episodes, facts, preferences and lessons with timestamps, sources and recall counts, searchable from chat and from live calls.
- [ ] **Honest affect model:** a documented, deterministic emotion/state engine (valence, arousal, energy, trust) driven by real events (call outcome, tool success/failure, praise/anger, idle time) and shown in the UI. No pretending it is biology.
- [ ] **Step-by-step UI:** Profile becomes a small hub with separate pages per system (voice, brain, server, memory, skills, cursor, privacy, appearance) instead of one long list; existing screens and styles must not break.

