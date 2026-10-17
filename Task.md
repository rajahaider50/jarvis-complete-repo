# TASK (current task; ONE at a time)
Status: in progress.

- **Task:** JARVIS AI CURSOR SYSTEM + LIVE SCREEN STREAM/SHARE/RECORD (interleaved). Order: (B1-4) one MediaProjection session for recording + share via accessibility takeScreenshot + frame policy; (A1-2) coordinate engine + Cursor Lab + cursor overlay; (A3-6) gestures, manual panel/joystick, AI cursor tools, click-lesson memory (file-based JSON + ui_clicks.md); (B5-6) Live session resumption/compression + UI toggles/AI tools.
- **Files:** live/{JarvisAccessibilityService,ScreenRecordService,ScreenShareService,PhoneTools,AppControl,LiveClient}.kt, res/xml/accessibility_service_config.xml, AndroidManifest.xml, ui/screens/CursorLabScreen.kt, ui/screens/LiveVoiceScreen.kt.
- **Test:** Build APK green after each push. Phone test pending (see Pending.md).
- **Status:** in progress.

At the end: mark Status done, add the item to Complete.md, then reset this file to "none active".
