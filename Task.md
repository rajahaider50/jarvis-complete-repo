# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** Chat اور coding requests کے لیے smart model router بنانا: تمام eligible provider models score کرنا، quality/context/tools کے مطابق بہترین model منتخب کرنا، retryable failure پر cooldown اور failover، اور local daily usage tracking۔
- **Scope:** Custom provider Chat/Code routing؛ Gemini fallback اور voice/call flows برقرار رہیں گے۔ Existing provider free-only eligibility اور Phase 2 task state برقرار رہیں گے۔
- **Rules:** Same task/message state کے ساتھ next model پر switch؛ 429/402/5xx/timeout/connection errors retryable؛ unknown metadata کو optimistic نہ سمجھنا؛ API keys/logs محفوظ رہیں۔
- **Do not touch:** UI design، `docs/PROJECT_MAP.md`، task checkpoint schema، Gemini startup/voice/call implementation، اور Phase 4 network/power-resume service۔
- **Test:** Eligible models score ہوں؛ failed model cooldown میں جائے؛ next provider/model پر same request continue ہو؛ usage local day کے حساب سے track ہو؛ all models exhausted ہونے پر clear pause/error آئے۔
- **Status:** in progress
