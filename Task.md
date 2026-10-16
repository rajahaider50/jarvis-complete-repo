# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** Custom providers کے لیے حقیقی model discovery کو upgrade کرنا: API metadata کے ساتھ free-only filtering، unknown/unverified group، model catalog fallback، manual model override، refresh اور local cache۔
- **Screen/feature:** Profile > Custom providers، provider editor/model list، Chat model picker؛ موجودہ Gemini/core flow اور voice features برقرار رہیں گے۔
- **Do not touch:** Existing interface styles/design language، `docs/PROJECT_MAP.md` (generated)، Gemini startup/voice APIs، اور Phase 2–5 کے task-state/router/network/skills بڑے refactors۔
- **Test:** Provider save/refresh پر `/models` سے model metadata آئے؛ free-only ON پر صرف verified free models default دکھیں؛ unknown models الگ Unverified میں ہوں؛ manual model محفوظ رہے؛ offline cache سے آخری catalog دکھے؛ Chat میں صرف eligible models selectable ہوں۔
- **Status:** in progress
