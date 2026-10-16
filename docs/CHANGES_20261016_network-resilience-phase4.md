# Network and power-loss resilience — Phase 4

## What changed

Coding mode now has a process-level `ConnectivityManager.NetworkCallback`. When the network disappears during an active request, CodeAgent cancels the request cleanly, keeps the current project/task checkpoint, sets the durable state to `waiting_network`, and shows a non-error waiting status. The original goal, project identity, completed file hashes, current file and partial content remain in the existing TaskStateStore.

When connectivity returns, the active waiting run auto-resumes once from the saved task state. The Code screen also exposes a Continue task action, and an offline start is queued as `waiting_network` rather than silently discarded. App restart still loads the saved state and offers Continue/Resume for the matching project.

A small foreground data-sync service is started while a coding task is active and stopped on completion/pause. This gives Android a foreground-priority process foundation for longer coding tasks without changing the voice/call services. The service does not store secrets or run an independent duplicate agent.

## Changed files

- `Task.md`
- `Planning.md`
- `FEATURES.md`
- `Complete.md`
- `Pending.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/jarvis/app/MainActivity.kt`
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `app/src/main/java/com/jarvis/app/code/CodeTaskService.kt`
- `app/src/main/java/com/jarvis/app/code/NetworkMonitor.kt`
- `app/src/main/java/com/jarvis/app/code/TaskStateStore.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/CodeScreen.kt`
- `docs/HANDOFF_STATE.md`
- `docs/CHANGES_20261016_network-resilience-phase4.md`

## How to test on the phone

1. Start a small Code task and wait until a file/checkpoint appears.
2. Disable Wi-Fi and mobile data, or enable airplane mode during a request. The run should show **Waiting for network**, not an ordinary red failure; the saved state should say `waiting_network`.
3. Restore connectivity. The same run should auto-resume once from the saved checkpoint. The Continue task card remains available if the app was backgrounded or the automatic resume could not start.
4. Force-close the app during a task, reopen the same project, and confirm Continue/Resume is offered without duplicated file content.
5. Complete the task and confirm the foreground notification/service stops.

## Verification

The first Phase 4 build exposed and fixed one duplicate `Context` import in CodeAgent. Corrected GitHub Actions Build APK run `38938269693` passed. `git diff --check` passed before the fix commit.

## Untested / limitations

- Real network toggling, Android force-stop/reopen behavior, notification permission behavior, and duplicate-content checks still require a physical Android device.
- The foreground service keeps the process at foreground priority but the agent remains owned by CodeAgent; it is not yet a separate WorkManager job and cannot guarantee survival after an OS force-stop.
- Connectivity callbacks indicate transport availability; provider authentication, captive portals and provider-side outages still go through Phase 3 router/error handling.
- If Android kills the process, the service is `START_NOT_STICKY`; the durable state is offered on next app launch rather than starting hidden background work.
- Phase 5 GitHub skill import and file-based memory tools are not included.

## Not changed

Existing provider scoring, Gemini startup, voice/call services, UI theme and Phase 2/3 state formats were preserved except for the additive `waiting_network` status and network-aware lifecycle hooks.
