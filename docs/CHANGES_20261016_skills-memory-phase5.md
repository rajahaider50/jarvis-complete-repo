# Skills import and file memory — Phase 5

## What changed

Skills can now be previewed and imported from a raw `SKILL.md` URL, a GitHub repository or folder URL, or an HTTPS zip containing `SKILL.md`. GitHub blob/tree links are normalized to the GitHub API/raw endpoints, repository folders are searched for a nested `SKILL.md`, and zip extraction reads only the skill document. The UI shows the parsed name, description and source before the user confirms saving. Imported skills retain their source URL and use the existing enable, edit and delete lifecycle.

Downloaded content is treated as untrusted instructions. No downloaded script, hook, binary or extra file is executed. The importer has HTTP/size limits and rejects missing or empty `SKILL.md` content. Existing built-in skills and the existing `skills.json` format remain backward compatible; source metadata is additive.

The coding agent now has app-private topic memory stored as relative `.md` files under the app's private files directory. It can list, read, write, append and exact-replace memory through five new tools. Paths are restricted to relative markdown topics, traversal and unsupported characters are rejected, file/content sizes are bounded, and obvious API keys, bearer tokens, passwords and secret assignments are refused. The agent prompt explicitly says to save only durable user-stated facts and never guesses, keys or temporary task chatter.

The Memory screen now explains and lists these agent memory files while preserving the existing name, facts, instructions and chat-memory controls.

## Changed files

- `Task.md`
- `Planning.md`
- `FEATURES.md`
- `Complete.md`
- `Pending.md`
- `app/src/main/java/com/jarvis/app/MainActivity.kt`
- `app/src/main/java/com/jarvis/app/code/AgentTools.kt`
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `app/src/main/java/com/jarvis/app/code/MemoryStore.kt`
- `app/src/main/java/com/jarvis/app/code/Skills.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/MemoryScreen.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/SkillsScreen.kt`
- `docs/HANDOFF_STATE.md`
- `docs/CHANGES_20261016_skills-memory-phase5.md`

## Verification

`git diff --check` passed before commit. The repository has no Gradle wrapper, so local `./gradlew` compilation was unavailable. The repository's configured Gradle 8.7 GitHub Actions workflow compiled and packaged the debug APK successfully on run `38939373696`.

## Manual tests still required

On a real phone, import a trusted raw skill, GitHub folder and zip; confirm the preview and source; save, disable, edit and delete the imported skill; and try malformed/no-`SKILL.md` sources. Then ask the Code agent to write a durable non-sensitive fact, restart the app and confirm the `.md` topic remains available. Verify `../escape.md`, absolute paths, API-key-like text, bearer tokens and password/secret assignments are rejected.

Network/provider availability, GitHub rate limits and authentication are external to the importer. Imported instructions can influence the model when enabled, so users should review the preview/source and only enable trusted skills.

## Next step

Phase 4 and Phase 5 phone validation remain pending. No Phase 6 is assumed until the next feature is specified.
