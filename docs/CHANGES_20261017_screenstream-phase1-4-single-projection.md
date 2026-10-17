# Screen stream Phase B1-4: one MediaProjection (recording) + share via accessibility screenshots

## Problem
Recording (`ScreenRecordService`) and sharing (`ScreenShareService`) each opened their OWN MediaProjection
session. On Android 14 one projection token allows a single capture and a new session can stop the previous one,
so turning on share stopped the recorder (and vice versa) and asked for consent twice.

## Fix (one projection session)
- Recording keeps the only `MediaProjection` (`ScreenRecordService`).
- Sharing no longer opens a projection at all: `ScreenShareService` now uses
  `AccessibilityService.takeScreenshot` (~1 fps). So share and record are independent toggles and the second one
  never opens a consent dialog or kills the first.

## Files
- `res/xml/accessibility_service_config.xml` — added `android:canTakeScreenshot="true"`.
- `live/JarvisAccessibilityService.kt` — new `screenshot(cb)` (API 30+), `canScreenshot`, `foregroundPackage()`,
  `isSensitiveScreen()`; gestures/`clickByText` now trigger `ScreenShareService.burst()`.
- `live/ScreenShareService.kt` — rewritten: no MediaProjection; foreground service (dataSync); frame policy with
  64x64 grayscale change detection, base 1 fps, slow to 0.3 fps when idle, action burst (~0.3 s), latest-wins,
  and **privacy pause** on password / banking / payment screens (`paused` state).
- `live/ScreenShareRequestActivity.kt` — now just starts sharing (no consent dialog).
- `live/PhoneTools.kt` / `live/AppControl.kt` — `share_screen` / `set_screen_share` start sharing directly.
- `ui/screens/LiveVoiceScreen.kt` — Share button starts sharing with no dialog.
- `AndroidManifest.xml` — `ScreenShareService` foreground type `mediaProjection` -> `dataSync`.

## How to test
1. Start a call, tap Record (allow once), then tap Share: recording keeps going and no second dialog appears.
2. Share off -> recording continues. Record off -> share continues. Both off -> resources released.
3. Open a banking app or a password field while sharing -> frames pause (add "Sharing paused" later in UI).
4. Saved clip in `Movies/Jarvis` has your voice and Jarvis's voice.

## Notes
- Sharing needs Android 11+ and the accessibility service on. Recording works on all versions.
- Guide B Phase 5-6 (Live session resumption + compression, goAway reconnect, UI toggles) still pending.
