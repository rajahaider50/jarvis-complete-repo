# SKILL: add-feature (checklists)
Plan first (Planning.md), then: NEW file for the feature + smallest hooks in existing files.
- New Prefs value: property in `data/Prefs.kt` + load in `init` + save in `save()`. Never rename existing keys.
- New screen: composable file in `ui/screens/` + `composable("route")` in `MainActivity.kt` + entry in settings lists (`ProfileScreen.kt` and the four `ui/styles/*Screens.kt`) if reachable by the user.
- New phone tool for calls: `live/PhoneTools.kt` — add to the list (`all`) AND to `execute`; both call engines use it.
- New permission / service / activity: `AndroidManifest.xml` entry (correct foregroundServiceType) + row in `ui/screens/AccessScreen.kt` if the user must grant it.
- New resource used from code/manifest: the file must really exist under `res/`.
- Dependencies: avoid; if needed add to `app/build.gradle.kts` with a pinned version and say so in CHANGES.
- Errors: catch and log with `AppLog` (`AppLog.w(e)` handled, `AppLog.e(tag, e)` real failures) so they show in Profile > Console.
- No network on main thread; use coroutines like the neighbouring code.
- Finish: update Planning/Pending/Complete/HANDOFF_STATE, write CHANGES file, say what is untested.
