# TASK (current task; ONE at a time)
Status: done.

- **Task:** JARVIS LIVE CALL UPDATE (6 phases): smooth no-lag live call, fast (~1s) connect with saved Live Call Models, call keeps running in-app (down-arrow + mini bar), in-app screen recorder (screen + both voices) with mic-silent fix, and AI control by voice (5-5 option popups for orb/theme/interface/color + camera/recording/share/navigate).
- **Files:** ui/screens/LiveVoiceScreen.kt, live/{CallHost,LiveClient,CallService,CallEngine,CallAudioTap,AudioMixer,ScreenRecordService,ScreenRecordRequestActivity,PhoneTools,AiPanel,AppControl,CallCamera}.kt, ui/components/{MiniCallBar,AiOptionsOverlay}.kt, data/{Prefs,NavState,ModelCatalog}.kt, ui/screens/{ApiScreen,ProfileScreen}.kt, MainActivity.kt, AndroidManifest.xml.
- **Test:** Build APK green (runs 39049063340, 39049749611, 39050810331, 39051398001, 39052284373). Phone test pending (see Pending.md).
- **Status:** done.

At the end: mark Status done, add the item to Complete.md, then reset this file to "none active".
