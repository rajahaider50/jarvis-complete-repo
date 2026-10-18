# PROJECT MAP (generated: do not edit by hand)

Updated 2026-10-18 14:16 · 225 files. Read docs/AI_GUIDE.md first, then this map, then ONLY the files you need.

## Changed since the previous map
- FEATURES.md
- docs/CHANGES_20261018_bugfix-round3-mic-and-tap.md
- docs/HANDOFF_STATE.md

## Kotlin source (`app/src/main/java/com/jarvis/app/`)
format: `path` · lines · what it is · declares · uses (other project files)


### (root)/
- `AssistantActivity.kt` · 112 · Floating Siri-style assistant that opens over whatever is on screen (started by the wake p · AssistantActivity, Overlay, Circle · uses: AudioRouter.kt, ModelCatalog.kt, Prefs.kt, CallEngine.kt, LiveClient.kt, WakeService.kt
- `MainActivity.kt` · 166 · A screen requested from outside the UI (floating bubble, popup, notification). · Launch, MainActivity, bottomRoutes, JarvisApp · uses: AppLog.kt, CustomProviders.kt, Speaker.kt, ChatStore.kt, Device.kt, ModelCatalog.kt

### assistant/
- `Assistant.kt` · 42 · Registers the app as a real Android "digital assistant" (the same slot Google Assistant us · JarvisInteractionService, JarvisSessionService, JarvisSession, JarvisRecognitionService · uses: AssistantActivity.kt
- `DeviceAdmin.kt` · 32 · Lets the app lock the screen on your command. It can do nothing else (the policy list only · JarvisAdminReceiver, DeviceAdmin · uses: AppLog.kt

### audio/
- `AudioLevel.kt` · 19 · Turns 16-bit PCM into a 0..1 loudness value so themes can react to the voice. · AudioLevel
- `AudioRouter.kt` · 174 · Call audio routing. AUTO = wired headset > Bluetooth > earpiece when the phone is at your  · Route, AudioRouter · uses: AppLog.kt, Prefs.kt, Diag.kt
- `PcmPlayer.kt` · 47 · - · PcmPlayer · uses: AppLog.kt
- `Speaker.kt` · 79 · Speaks ONLY with Gemini's own voices. There is deliberately no phone / Google default-voic · Speaker · uses: AppLog.kt, GeminiClient.kt, ModelCatalog.kt, Prefs.kt
- `WavUtil.kt` · 60 · Small helpers to move audio between phone PCM and the WAV files that speech servers use. · WavUtil

### code/
- `AgentTools.kt` · 133 · The tools the code agent works with: a real project folder, an editor, document makers, sk · AgentTools · uses: AppLog.kt
- `CodeAgent.kt` · 260 · The coding / document agent. It uses the model you chose in Chat (a Gemini model or a cust · CodeAgent · uses: AppLog.kt, GeminiClient.kt, ModelRouter.kt, CustomProviders.kt, Prefs.kt
- `CodeTaskService.kt` · 34 · Keeps the app process at foreground priority while a coding task is running. · CodeTaskService
- `DocGen.kt` · 362 · Real file generators that run on the phone: PDF and PNG are DRAWN (shapes, gradients, icon · DocGen
- `MemoryStore.kt` · 76 · Topic-based markdown memory. It never runs content and rejects obvious secret material. · MemoryStore
- `NetworkMonitor.kt` · 57 · Process-level network state used by coding mode; callbacks are delivered on the main threa · NetworkMonitor · uses: AppLog.kt
- `Skills.kt` · 195 · Skills are short instruction packs. The agent sees only each skill's name and one line; wh · Skills · uses: AppLog.kt
- `TaskStateStore.kt` · 122 · Crash-safe local state for the one coding task that may need to resume. · TaskStateStore · uses: AppLog.kt
- `Workspace.kt` · 236 · Projects on the phone: folders of real files that you and the AI can create, edit, delete, · Workspace · uses: AppLog.kt

### data/
- `AppLog.kt` · 130 · The app-wide console. Every handled error (every catch block), every crash and the importa · AppLog
- `ChatStore.kt` · 245 · - · Msg, SessionMeta, ChatStore
- `CustomProviders.kt` · 474 · One model offered by a provider, with only real values. null = unknown (never guessed). · ProviderModel, CustomProvider, ImportSummary, CustomProviders, OpenAiCompat · uses: WavUtil.kt, PhoneTools.kt
- `Device.kt` · 142 · Real phone controls. Android does not let apps flip Wi-Fi/Bluetooth/Airplane directly, so  · Stats, DeviceStats, DeviceControl
- `GeminiClient.kt` · 255 · Minimal Gemini API client (REST, v1beta). No model names are hard-coded: · GeminiClient
- `ImageUtil.kt` · 31 · - · ImageUtil
- `Langs.kt` · 45 · - · Lang, Langs
- `ModelCatalog.kt` · 132 · Real models available for the user's key (from models.list) + automatic per-task selection · ModelCatalog
- `ModelCatalogFile.kt` · 102 · A small offline catalog of well-known models, so a provider that does not report · ModelCatalogFile
- `ModelRouter.kt` · 172 · Scores real provider models and remembers temporary health/quota state locally. · ModelRouter · uses: AppLog.kt
- `NavState.kt` · 11 · The screen the app is showing right now, so the assistant knows where the user is (get_app · NavState
- `PhoneActions.kt` · 132 · Things the assistant can do on the phone for you (browser, apps, screen). Used by the live · PhoneActions · uses: JarvisAccessibilityService.kt
- `Prefs.kt` · 180 · All user settings, stored privately on the device. · CustomVoice, Prefs · uses: Color.kt
- `ProbeService.kt` · 52 · Real, tiny requests that check whether a provider model actually answers, how fast, and · ProbeService
- `RealFacts.kt` · 58 · The real state of the phone, read from Android at the moment it is needed. It is given to  · RealFacts
- `Startup.kt` · 105 · The prepared "system active" briefing: text + voice audio, cached so Home can play it inst · Greeting, GreetingPlayer, Startup · uses: PcmPlayer.kt, Speaker.kt
- `Voices.kt` · 20 · Gender of each Gemini prebuilt voice + the language rule that keeps grammar matching the v · Voices
- `Weather.kt` · 72 · Today's weather from Open-Meteo (free, no key) using the phone's last known location. · Weather

### live/
- `AiPanel.kt` · 85 · The floating option popups the assistant shows during a call ("5-5"): orb styles, colour t · AiPanel · uses: Prefs.kt, SiriOrb.kt, IfaceCatalog.kt, Color.kt, ThemeRegistry.kt
- `AppControl.kt` · 225 · App-control tools the Gemini Live model can call during a call: show option popups ("5-5") · AppControl · uses: MainActivity.kt, AppLog.kt, NavState.kt, Prefs.kt, SiriOrb.kt
- `AudioMixer.kt` · 66 · Joins the two sides of a live call into one 44.1 kHz mono stream for the in-app screen rec · AudioMixer
- `CallAudioTap.kt` · 15 · A tap into the live call's raw audio, used by the in-app screen recorder to mix both sides · CallAudioTap
- `CallCamera.kt` · 14 · The live call camera, owned by the app (not the screen) so both the UI and the assistant c · CallCamera
- `CallEngine.kt` · 52 · What every kind of voice call looks like to the screens, the floating bubble and the notif · CallEngine · uses: Prefs.kt
- `CallHost.kt` · 100 · Owns the one live call of the whole process. · CallHost · uses: AudioRouter.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt, ModelCatalog.kt, Prefs.kt
- `CallLocks.kt` · 49 · Holds a partial wake lock (CPU) and a Wi-Fi high-performance lock for the whole live call, · CallLocks · uses: AppLog.kt
- `CallService.kt` · 87 · Keeps the live call (and its microphone) alive while the app is in the background, as Andr · CallService · uses: AppLog.kt, MainActivity.kt, Prefs.kt
- `ClickMemory.kt` · 269 · Click-lesson memory for the AI cursor. It is FILE based (no Room): a machine file `ui_clic · ClickMemory · uses: AppLog.kt
- `CursorController.kt` · 444 · Draws the on-screen AI cursor(s) with TYPE_ACCESSIBILITY_OVERLAY (no extra permission need · CursorController, CursorView, GridView · uses: AppLog.kt
- `CursorGeometry.kt` · 44 · The JARVIS cursor coordinate system. The screen CENTER is (0,0). X: left -100, right +100. · CursorGeometry
- `CursorPanel.kt` · 292 · The user's floating control panel for the AI cursor: cursor list, X/Y readout (2 decimals) · CursorPanel, JoystickView · uses: AppLog.kt
- `Diag.kt` · 108 · Evidence logger for the bugfix work. Everything goes to logcat with the tag [TAG] (timesta · Diag
- `JarvisAccessibilityService.kt` · 552 · The real result of a dispatched gesture (bugfix Masla A / §2.1). Top-level so CursorContro · GestureOutcome, JarvisAccessibilityService · uses: Prefs.kt
- `LiveClient.kt` · 636 · Real-time voice call with Gemini (Live API over WebSocket). · LiveClient, Http · uses: AppLog.kt, Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, Voices.kt
- `MicCaptureHub.kt` · 252 · The ONE owner of the microphone. Everything that needs mic PCM (the Gemini sender, the in- · MicCaptureHub · uses: AppLog.kt
- `PhoneTools.kt` · 203 · Everything the assistant can do on the phone, described once and used by BOTH call engines · PhoneTools · uses: AppLog.kt, Device.kt, PhoneActions.kt, Weather.kt
- `PipelineClient.kt` · 169 · A voice call with ANY provider: your speech is cut out of the microphone, turned into text · PipelineClient · uses: PcmPlayer.kt, Speaker.kt, WavUtil.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt
- `ScreenRecordRequestActivity.kt` · 39 · Invisible screen that shows Android's own "start recording?" question so the assistant can · ScreenRecordRequestActivity · uses: AppLog.kt
- `ScreenRecordService.kt` · 256 · In-app screen recorder: captures the screen (MediaProjection -> H.264) and mixes both side · ScreenRecordService · uses: AppLog.kt
- `ScreenShareRequestActivity.kt` · 26 · Kept for compatibility. Screen sharing no longer needs Android's MediaProjection question, · ScreenShareRequestActivity · uses: AppLog.kt
- `ScreenShareService.kt` · 177 · Shares the screen with the live call WITHOUT opening a second MediaProjection session (tha · ScreenShareService · uses: AppLog.kt, ImageUtil.kt
- `VoskModels.kt` · 61 · The small offline speech models (about 40 MB each) used by the keyless wake phrase engine. · VoskModels · uses: AppLog.kt
- `WakeService.kt` · 359 · Listens in the background for your wake phrase (any name / command you chose). When heard, · WakeService · uses: AppLog.kt, AssistantActivity.kt, MainActivity.kt, Prefs.kt

### overlay/
- `DragLayout.kt` · 62 · Root view of a floating window. Taps go to the Compose content (buttons); as soon as a fin · DragLayout
- `Overlay.kt` · 254 · Floating windows drawn over every app (needs "Display over other apps"): · Overlay · uses: AppLog.kt, MainActivity.kt, CallHost.kt, OverlayUi.kt, Theme.kt
- `OverlayLifecycleOwner.kt` · 35 · Compose needs a lifecycle, a saved-state registry and a view-model store to run. An overla · OverlayLifecycleOwner

### ui/components/
- `AiOptionsOverlay.kt` · 100 · The option popups the assistant shows during a call. Five animated cards; the user answers · AiOptionsOverlay · uses: AiPanel.kt, Color.kt
- `Components.kt` · 259 · The bottom bar follows the chosen interface style. · goldBrush, JText, Hairline, JarvisOrb, GoldButton, GoldIconBox, ScreenScaffold
- `MiniCallBar.kt` · 64 · A small "call is running" bar shown on every screen except the call itself. Tapping it ret · MiniCallBar · uses: Prefs.kt, CallEngine.kt, CallHost.kt
- `SiriOrb.kt` · 1340 · mode: 0 idle, 1 listening (reacts to level), 2 thinking, 3 speaking. style: 0..99 (see ORB · ORB_STYLES, SiriOrb, glow, ball, soft, warm, blobs · uses: Prefs.kt
- `VoiceWave.kt` · 47 · Siri-style layered voice waves that follow the voice level. · VoiceWave

### ui/overlay/
- `OverlayUi.kt` · 122 · The floating call bubble: your own orb, draggable anywhere. Tap it for mute / open / end. · Danger, BubbleUi, Controls, RoundIcon, CloseZoneUi, PopupUi · uses: ChatStore.kt, Prefs.kt, CallEngine.kt, CallHost.kt, LiveClient.kt

### ui/screens/
- `AccessScreen.kt` · 104 · One place for every permission the assistant needs. Tap a row that is not allowed to fix i · Access, AccessScreen · uses: Device.kt, JarvisAccessibilityService.kt
- `ApiScreen.kt` · 173 · - · ApiScreen, AutoRow · uses: ModelCatalog.kt, Prefs.kt
- `AssistantScreen.kt` · 213 · - · AssistantScreen, StatusRow · uses: AppLog.kt, Device.kt, Langs.kt, Prefs.kt, JarvisAccessibilityService.kt, VoskModels.kt
- `ChatScreen.kt` · 285 · The model's live thinking and the answer as it is being written. · ChatScreen, ThinkingCard, ActionBar, Bubble · uses: ChatStore.kt, ImageUtil.kt, CustomProviders.kt, ModelCatalog.kt, Prefs.kt, Speaker.kt
- `CodeScreen.kt` · 456 · Code & documents workspace: the agent writes real files in an editor, you can browse, edit · CodeBg, CodeText, CodeDim, Mono, viewable, rememberSaveFile, NameDialog · uses: CodeAgent.kt, TaskStateStore.kt, Workspace.kt, AppLog.kt, CustomProviders.kt, Prefs.kt
- `ConsoleScreen.kt` · 110 · Every error, crash and event of the app, with time, file, line, function and stack. Copy o · ErrRed, WarnAmber, copy, ConsoleScreen · uses: AppLog.kt
- `ControlScreen.kt` · 151 · - · ControlScreen, Tile, SliderRow, PermRow · uses: Device.kt
- `CursorLabScreen.kt` · 165 · Cursor Lab: proves the normalized coordinate system (center 0,0; right/up positive; 2 deci · targets, targetNames, CursorLabScreen · uses: CursorController.kt, CursorGeometry.kt, CursorPanel.kt, JarvisAccessibilityService.kt, Components.kt
- `CursorMemoryScreen.kt` · 92 · Cursor memory: the click lessons JARVIS learned (ui_clicks.json / ui_clicks.md). The user  · CursorMemoryScreen · uses: ClickMemory.kt, CursorGeometry.kt, Components.kt
- `DiagnosticsScreen.kt` · 191 · Diagnostics (bugfix guide Phase 1). One screen with the live evidence: microphone state +  · DiagnosticsScreen, Card · uses: CursorController.kt, Prefs.kt, CallHost.kt, Diag.kt, MicCaptureHub.kt, JarvisAccessibilityService.kt
- `HomeScreen.kt` · 224 · - · Boot, HomeScreen, BootRings, StatTile, QTile, LoadingDialog · uses: Startup.kt, Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, AuroraScreens.kt
- `LiveVoiceScreen.kt` · 318 · - · has, LiveVoiceScreen, LiveCallContent, CameraPip, RoundBtn, RouteChips · uses: AppLog.kt, AudioRouter.kt, ImageUtil.kt, Langs.kt, ModelCatalog.kt, Prefs.kt
- `MemoryScreen.kt` · 129 · - · MemoryScreen · uses: MemoryStore.kt, ChatStore.kt, Prefs.kt
- `Onboarding.kt` · 194 · - · StepHeader, LanguageScreen, PermissionsScreen, PRow, ApiSetupScreen, SetupScreen · uses: Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, Startup.kt
- `ProfileScreen.kt` · 255 · - · ProfileScreen, PrefRowV, PrefRow · uses: ModelCatalog.kt, Prefs.kt
- `ProvidersScreen.kt` · 357 · Add models from any other AI provider. Anything that offers an OpenAI-compatible API works · ProvidersScreen, ProviderList, ProviderEditor, Field, SwitchRow, ModelRow · uses: PcmPlayer.kt, AppLog.kt, CustomProviders.kt, GeminiClient.kt, ModelCatalogFile.kt, Prefs.kt
- `RouterLogScreen.kt` · 59 · Shows the smart router's real local health: what it used today, what is cooling down and h · RouterWarn, RouterLogScreen, maybeProvider · uses: ModelRouter.kt
- `SkillsScreen.kt` · 172 · Teach the agent new skills: short instruction packs it opens when a task needs them. · SkillsScreen, SkillList, Color2, SkillEditor · uses: Skills.kt, AppLog.kt
- `TapTestScreen.kt` · 213 · Tap Test (bugfix Masla A / §2.6). Five targets (centre + 4 corners). For each one it puts  · TapTestScreen, TapRow, TapTestView · uses: CursorController.kt, CursorGeometry.kt, Diag.kt, JarvisAccessibilityService.kt, Components.kt, Color.kt
- `ThemeLabScreen.kt` · 117 · Debug screen: shows every theme at once with live state/level/sound controls. · ThemeLabScreen · uses: AudioLevel.kt, Prefs.kt, Components.kt, AssistantState.kt, AssistantTheme.kt, ThemeRepository.kt
- `ThemeScreen.kt` · 88 · - · ThemeScreen · uses: Prefs.kt
- `UiStyleScreen.kt` · 153 · 50 interface styles: 25 native-Android premium and 25 iOS premium. · StyleInfo, STYLES, UiStyleScreen, StyleRow, Mini, MiniModern · uses: Prefs.kt, IfaceCatalog.kt
- `VoiceScreen.kt` · 241 · - · VOICES, VoiceScreen, VoiceCard, AddVoiceDialog · uses: AppLog.kt, PcmPlayer.kt, Speaker.kt, Prefs.kt, Langs.kt, Voices.kt
- `WelcomeScreen.kt` · 45 · - · WelcomeScreen

### ui/styles/
- `AuroraScreens.kt` · 226 · AURORA · Home: one big hero card with the orb and a "Talk now" pill, quick chips, ring gau · AuroraHome, AuroraControl, AuroraSlider, AuroraSettings · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `FolioScreens.kt` · 197 · FOLIO: a warm, paper-like magazine. Serif headlines, navigation as text tabs at the TOP (n · FolioTabs, FolioHeader, Figure, FolioHome, FolioControl, FolioLevel, FolioSettings · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `IfaceCatalog.kt` · 114 · One extra interface style. All of these use the premium "Iface" engine (engine 5). · IfaceDef, IfaceCfg, IfaceCatalog · uses: Color.kt
- `IfaceScreens.kt` · 604 · ========================================================================================== · curCfg, cornerDp, greeting, homeActions, IfaceOrb, IfaceHome, ActionCircles · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `NovaScreens.kt` · 207 · NOVA · Home: search-style "Ask" bar, a tonal assistant card, circular quick actions and a  · NovaHome, NovaControl, NovaSlider, NovaSettings · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `PulseScreens.kt` · 264 · PULSE: pure-black HUD look. Monospace text, sharp corners, a raised center "talk" button,  · PText, Sharp4, PulseNav, PulseTab, PulseHeader, PulseHome, Card · uses: Device.kt, Prefs.kt
- `StyleKit.kt` · 201 · What the Home screen controller hands to every interface style. · HomeUi, Warn, gemStatus, AuroraNav, NovaNav, AuroraHeader, NovaHeader · uses: ModelCatalog.kt, Prefs.kt, Device.kt, Components.kt

### ui/theme/
- `AssistantState.kt` · 5 · The four states every assistant theme must react to. · AssistantState
- `AssistantTheme.kt` · 19 · One selectable assistant visual. [Render] draws it for the current state and voice [level] · ThemeCategory, AssistantTheme · uses: FxProfile.kt
- `ClassicTheme.kt` · 32 · Keeps the app's original animated orb available as a theme (the default). · ClassicTheme · uses: Prefs.kt, SiriOrb.kt, FxProfile.kt
- `Color.kt` · 130 · 50 color styles. The first one is the original Figma "Royal Gold". · Palette, Palettes, Bg
- `PerformanceTier.kt` · 20 · Decides whether to draw the lighter version (fewer particles/details) on weak or saving de · PerformanceTier
- `Theme.kt` · 33 · - · JarvisTheme
- `ThemeRegistry.kt` · 32 · The only place themes are listed. · ThemeRegistry · uses: FxProfile.kt, HudThemes.kt, OrbShaders.kt, OrbThemes.kt
- `ThemeRepository.kt` · 35 · Persists the selected assistant theme id using the app's existing settings storage (Prefs) · ThemeRepository, Fx · uses: Prefs.kt, FxProfile.kt, FxPlayer.kt
- `Type.kt` · 10 · Figma uses Playfair Display (headings) and Inter (body). · JarvisSerif, JarvisSans

### ui/theme/anim/
- `AnimUtils.kt` · 68 · Time that advances at a changeable speed WITHOUT jumping when the speed changes. · rememberPhase, speedFor, arcRing, tickRing · uses: AssistantState.kt

### ui/theme/fx/
- `FxPlayer.kt` · 48 · Single entry point for sound effects. It never throws and always falls back to the synth. · FxPlayer
- `FxProfile.kt` · 34 · - · FxEvent, FxSpec, FxProfile, FxPresets
- `SynthFx.kt` · 46 · - · SynthFx

### ui/theme/hud/
- `HudThemes.kt` · 228 · ------------------------------------------------------------------- Arc Reactor · label, HudTheme, HudArcReactor, HudRadar, HudGlobe, HudAmber, HudFrost · uses: AssistantState.kt, AssistantTheme.kt, AnimUtils.kt, FxProfile.kt, ParticleSphere.kt

### ui/theme/orbs/
- `OrbShaders.kt` · 128 · AGSL (RuntimeShader) sources, Android 13+ only. Every source declares and uses resolution, · OrbShaders
- `OrbThemes.kt` · 26 · - · ShaderOrbTheme, ParticleSphereTheme · uses: AssistantTheme.kt, AssistantState.kt, FxProfile.kt
- `ParticleSphere.kt` · 63 · - · fibonacciSphere, ParticleSphere · uses: AssistantState.kt, AnimUtils.kt
- `ShaderOrb.kt` · 65 · - · ShaderOrb, ShaderOrbApi33, FallbackOrb · uses: AssistantState.kt, AnimUtils.kt

## Resources, manifest, build, CI, docs

- `.gitignore` · 9 lines
- `Complete.md` · 33 lines
- `FEATURES.md` · 51 lines
- `Goal.md` · 8 lines
- `Pending.md` · 37 lines
- `Planning.md` · 42 lines
- `README.md` · 110 lines
- `Roadmap.md` · 17 lines
- `Task.md` · 24 lines
- `build.gradle.kts` · 5 lines
- `gradle.properties` · 5 lines
- `settings.gradle.kts` · 8 lines
- `.github/workflows/apply-edits.yml` · 21 lines
- `.github/workflows/build.yml` · 24 lines
- `.github/workflows/map.yml` · 21 lines
- `.github/workflows/pack.yml` · 34 lines
- `app/build.gradle.kts` · 48 lines
- `app/src/main/AndroidManifest.xml` · 130 lines
- `app/src/main/assets/model_catalog.json` · 12 lines
- `app/src/main/assets/skills_pack.json` · 766 lines
- `app/src/main/res/values/colors.xml` · 2 lines
- `app/src/main/res/values/strings.xml` · 5 lines
- `app/src/main/res/values/themes.xml` · 15 lines
- `app/src/main/res/xml/accessibility_service_config.xml` · 11 lines
- `app/src/main/res/xml/device_admin.xml` · 7 lines
- `app/src/main/res/xml/recognition_service.xml` · 3 lines
- `app/src/main/res/xml/voice_interaction_service.xml` · 7 lines
- `design/DESIGN_SPEC.md` · 53 lines
- `design/icons/_icon_sheet.svg` · 1 lines
- `design/icons/badge.svg` · 1 lines
- `design/icons/chevron.svg` · 1 lines
- `design/icons/nav_assistant.svg` · 1 lines
- `design/icons/nav_control.svg` · 1 lines
- `design/icons/nav_home.svg` · 1 lines
- `design/icons/nav_profile.svg` · 1 lines
- `design/icons/pref_api.svg` · 1 lines
- `design/icons/pref_memory.svg` · 1 lines
- `design/icons/pref_palette.svg` · 1 lines
- `design/icons/pref_voice.svg` · 1 lines
- `design/icons/pref_wake.svg` · 1 lines
- `docs/AI_GUIDE.md` · 68 lines
- `docs/ARCHITECTURE.md` · 52 lines
- `docs/CHANGES_20261016_custom-provider-models.md` · 28 lines
- `docs/CHANGES_20261016_network-resilience-phase4.md` · 51 lines
- `docs/CHANGES_20261016_provider-discovery-phase1.md` · 45 lines
- `docs/CHANGES_20261016_skills-memory-phase5.md` · 43 lines
- `docs/CHANGES_20261016_smart-router-phase3.md` · 49 lines
- `docs/CHANGES_20261016_task-state-resume-phase2.md` · 44 lines
- `docs/CHANGES_20261017_build-fixes.md` · 19 lines
- `docs/CHANGES_20261017_cursor-phase-A1-coordinates.md` · 26 lines
- `docs/CHANGES_20261017_cursor-phase-A2-overlay.md` · 28 lines
- `docs/CHANGES_20261017_cursor-phase-A3-A4-panel.md` · 21 lines
- `docs/CHANGES_20261017_cursor-phase-A5-ai-tools.md` · 25 lines
- `docs/CHANGES_20261017_cursor-phase-A6-memory.md` · 37 lines
- `docs/CHANGES_20261017_interfaces-50.md` · 24 lines
- `docs/CHANGES_20261017_interfaces-layouts.md` · 34 lines
- `docs/CHANGES_20261017_live-call-phase1-lag.md` · 40 lines
- `docs/CHANGES_20261017_live-call-phase2-live-models.md` · 32 lines
- `docs/CHANGES_20261017_live-call-phase3-minimize.md` · 36 lines
- `docs/CHANGES_20261017_live-call-phase4-recorder.md` · 36 lines
- `docs/CHANGES_20261017_live-call-phase5-ai-control.md` · 23 lines
- `docs/CHANGES_20261017_live-phase5-6-resumption.md` · 33 lines
- `docs/CHANGES_20261017_orb-styles-50.md` · 30 lines
- `docs/CHANGES_20261017_orb100-themes50.md` · 21 lines
- `docs/CHANGES_20261017_phase1-providers-catalog.md` · 38 lines
- `docs/CHANGES_20261017_phases2-5-router-resume-network-skills.md` · 48 lines
- `docs/CHANGES_20261017_provider-json-import.md` · 25 lines
- `docs/CHANGES_20261017_screenstream-phase1-4-single-projection.md` · 35 lines
- `docs/CHANGES_20261017_visual-themes-soundfx.md` · 51 lines
- `docs/CHANGES_20261017_workspace-provider-only.md` · 50 lines
- `docs/CHANGES_20261018_bugfix-masla1-mic-hub.md` · 35 lines
- `docs/CHANGES_20261018_bugfix-masla2-cursor-mainthread.md` · 34 lines
- `docs/CHANGES_20261018_bugfix-masla3-call-stability.md` · 46 lines
- `docs/CHANGES_20261018_bugfix-phase1-diagnostics.md` · 50 lines
- `docs/CHANGES_20261018_bugfix-round2-step1-click-truthtest.md` · 92 lines
- `docs/CHANGES_20261018_bugfix-round3-mic-and-tap.md` · 94 lines
- `docs/HANDOFF_STATE.md` · 52 lines
- `docs/HOW_TO_WORK_WITH_AI.md` · 53 lines
- `docs/REPORT_20261018_bugfix-final-summary.md` · 57 lines
- `docs/REPORT_20261018_masla1_mic_recording.md` · 63 lines
- `docs/REPORT_20261018_masla2_cursor.md` · 50 lines
- `docs/REPORT_20261018_masla3_call_stalling.md` · 65 lines
- `docs/REQUEST_TEMPLATE.md` · 20 lines
- `docs/START_PROMPT.md` · 16 lines
- `docs/skills/add-feature.md` · 12 lines
- `docs/skills/delivery-format.md` · 10 lines
- `docs/skills/fix-bug.md` · 10 lines
- `docs/skills/light-model-boost.md` · 8 lines
- `docs/skills/ui-design.md` · 12 lines
- `tools/apply_edits.py` · 25 lines
- `tools/make_map.py` · 120 lines
- `tools/pack_context.py` · 108 lines
- `app/src/main/res/drawable/ic_*` · 12 icon files
- `app/src/main/res/mipmap-anydpi-v26/ic_*` · 2 icon files
