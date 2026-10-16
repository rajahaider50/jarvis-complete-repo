# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** GitHub سے Skills import/management اور file-based Memory tools شامل کرنا: repository/folder/raw URL سے untrusted skill files fetch، `SKILL.md` validate/preview/save، enable/disable/update/delete lifecycle، اور coding agent کے لیے local memory list/read/write/append/replace tools۔
- **Scope:** Existing `Skills.kt`, `AgentTools.kt`, `CodeAgent` prompt/tool execution، Workspace-local app storage، اور existing Profile/Code UI patterns۔
- **Rules:** Downloaded skill content untrusted رہے گا؛ scripts auto-run نہیں ہوں گے؛ secrets/guesses memory میں save نہیں ہوں گے؛ existing original task/state, provider/voice flows اور UI style برقرار رہیں گے۔
- **Do not touch:** `docs/PROJECT_MAP.md`، provider router logic، network resilience lifecycle، Gemini startup/voice/call services، existing screen theme.
- **Test:** GitHub raw/folder/zip URLs validate ہوں؛ preview metadata آئے؛ disabled skills prompt سے خارج ہوں؛ memory tools persist across restart and model switch؛ malformed/untrusted content safely reject ہو؛ build passes.
- **Status:** in progress
