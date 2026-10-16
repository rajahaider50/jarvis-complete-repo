# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** Coding mode میں network/power-loss resilience اور auto-resume شامل کرنا: connectivity loss detect ہو، active request cleanly pause ہو، task state `waiting_network` محفوظ کرے، network واپس آنے پر same project/task سے resume ہو، اور app restart پر Continue/Resume دستیاب رہے۔
- **Scope:** Phase 2 TaskStateStore/CodeAgent/CodeScreen؛ Android ConnectivityManager callback، bounded request cancellation، foreground/background survival foundation، اور existing UI style میں status/resume action۔
- **Rules:** Original goal، project identity، files اور partial state نہیں بدلیں گے؛ network loss کو normal failure نہ دکھایا جائے؛ API keys/logs/private content محفوظ رہیں؛ voice/call/Gemini startup flow untouched رہے گا۔
- **Do not touch:** Provider scoring logic except integration hooks، `docs/PROJECT_MAP.md`، Phase 5 Skills/Memory import، existing screen styling۔
- **Test:** Offline transition pauses with `waiting_network`; reconnect or Continue resumes once without duplicate run; app restart shows Resume; ordinary provider errors remain Phase 3 behavior; build passes.
- **Status:** in progress
