# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** Coding workspace agent کے لیے durable task state، file checkpoints، crash-safe progress اور exact resume foundation بنانا۔
- **Screen/feature:** Code workspace/run flow، `Workspace` projects، `CodeAgent` execution state؛ موجودہ Gemini/custom-provider selection برقرار رہے گی۔
- **Do not touch:** Existing UI design، provider discovery/free filtering، Gemini startup/voice/call flows، `docs/PROJECT_MAP.md`، اور Phase 3 scored router/usage limits یا Phase 4 network service۔
- **Test:** Coding run شروع ہو، task state disk پر محفوظ ہو، ہر completed file checkpoint بنے، process restart کے بعد unfinished task detect ہو اور resume prompt/state دستیاب ہو، cancellation پر state ضائع نہ ہو۔
- **Status:** in progress
