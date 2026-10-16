# PROJECT MAP (generated: do not edit by hand)

Updated 2026-10-16 18:36 · 148 files. Read docs/AI_GUIDE.md first, then this map, then ONLY the files you need.

## Changed since the previous map
- FEATURES.md
- app/src/main/java/com/jarvis/app/code/CodeTaskService.kt
- app/src/main/java/com/jarvis/app/code/MemoryStore.kt
- app/src/main/java/com/jarvis/app/code/NetworkMonitor.kt
- app/src/main/java/com/jarvis/app/code/TaskStateStore.kt
- app/src/main/java/com/jarvis/app/data/ModelRouter.kt
- docs/CHANGES_20261016_custom-provider-models.md
- docs/CHANGES_20261016_network-resilience-phase4.md
- docs/CHANGES_20261016_provider-discovery-phase1.md
- docs/CHANGES_20261016_skills-memory-phase5.md
- docs/CHANGES_20261016_smart-router-phase3.md
- docs/CHANGES_20261016_task-state-resume-phase2.md

## Kotlin source (`app/src/main/java/com/jarvis/app/`)
format: `path` · lines · what it is · declares · uses (other project files)


### (root)/
- `AssistantActivity.kt` · 112 · Floating Siri-style assistant that opens over whatever is on screen (started by the wake p · AssistantActivity, Overlay, Circle · uses: AudioRouter.kt, ModelCatalog.kt, Prefs.kt, CallEngine.kt, LiveClient.kt, WakeService.kt
- `MainActivity.kt` · 128 · A screen requested from outside the UI (floating bubble, popup, notification). · Launch, MainActivity, bottomRoutes, JarvisApp · uses: AppLog.kt, CustomProviders.kt, Speaker.kt, ChatStore.kt, Device.kt, ModelCatalog.kt

### assistant/
- `Assistant.kt` · 42 · Registers the app as a real Android "digital assistant" (the same slot Google Assistant us · JarvisInteractionService, JarvisSessionService, JarvisSession, JarvisRecognitionService · uses: AssistantActivity.kt
- `DeviceAdmin.kt` · 32 · Lets the app lock the screen on your command. It can do nothing else (the policy list only · JarvisAdminReceiver, DeviceAdmin · uses: AppLog.kt

### audio/
- `AudioRouter.kt` · 129 · Call audio routing. AUTO = wired headset > Bluetooth > earpiece when the phone is at your  · Route, AudioRouter · uses: AppLog.kt
- `PcmPlayer.kt` · 47 · - · PcmPlayer · uses: AppLog.kt
- `Speaker.kt` · 79 · Speaks ONLY with Gemini's own voices. There is deliberately no phone / Google default-voic · Speaker · uses: AppLog.kt, GeminiClient.kt, ModelCatalog.kt, Prefs.kt
- `WavUtil.kt` · 60 · Small helpers to move audio between phone PCM and the WAV files that speech servers use. · WavUtil

### code/
- `AgentTools.kt` · 118 · The tools the code agent works with: a real project folder, an editor, document makers, sk · AgentTools · uses: AppLog.kt
- `CodeAgent.kt` · 247 · The coding / document agent. It uses the model you chose in Chat (a Gemini model or a cust · CodeAgent · uses: AppLog.kt, CustomProviders.kt, GeminiClient.kt, ModelCatalog.kt, Prefs.kt
- `CodeTaskService.kt` · 34 · Keeps the app process at foreground priority while a coding task is running. · CodeTaskService
- `DocGen.kt` · 362 · Real file generators that run on the phone: PDF and PNG are DRAWN (shapes, gradients, icon · DocGen
- `MemoryStore.kt` · 62 · Topic-based markdown memory. It never runs content and rejects obvious secret material. · MemoryStore
- `NetworkMonitor.kt` · 57 · Process-level network state used by coding mode; callbacks are delivered on the main threa · NetworkMonitor · uses: AppLog.kt
- `Skills.kt` · 100 · Skills are short instruction packs. The agent sees only each skill's name and one line; wh · Skills · uses: AppLog.kt
- `TaskStateStore.kt` · 122 · Crash-safe local state for the one coding task that may need to resume. · TaskStateStore · uses: AppLog.kt
- `Workspace.kt` · 236 · Projects on the phone: folders of real files that you and the AI can create, edit, delete, · Workspace · uses: AppLog.kt

### data/
- `AppLog.kt` · 130 · The app-wide console. Every handled error (every catch block), every crash and the importa · AppLog
- `ChatStore.kt` · 232 · - · Msg, SessionMeta, ChatStore
- `CustomProviders.kt` · 289 · An AI model from any provider that speaks the OpenAI-compatible chat API (OpenAI, OpenRout · CustomProvider, CustomProviders, OpenAiCompat · uses: WavUtil.kt, PhoneTools.kt
- `Device.kt` · 142 · Real phone controls. Android does not let apps flip Wi-Fi/Bluetooth/Airplane directly, so  · Stats, DeviceStats, DeviceControl
- `GeminiClient.kt` · 255 · Minimal Gemini API client (REST, v1beta). No model names are hard-coded: · GeminiClient
- `ImageUtil.kt` · 31 · - · ImageUtil
- `Langs.kt` · 45 · - · Lang, Langs
- `ModelCatalog.kt` · 109 · Real models available for the user's key (from models.list) + automatic per-task selection · ModelCatalog
- `ModelRouter.kt` · 138 · Scores real provider models and remembers temporary health/quota state locally. · ModelRouter · uses: AppLog.kt
- `PhoneActions.kt` · 132 · Things the assistant can do on the phone for you (browser, apps, screen). Used by the live · PhoneActions · uses: JarvisAccessibilityService.kt
- `Prefs.kt` · 115 · All user settings, stored privately on the device. · CustomVoice, Prefs · uses: Color.kt
- `RealFacts.kt` · 58 · The real state of the phone, read from Android at the moment it is needed. It is given to  · RealFacts
- `Startup.kt` · 105 · The prepared "system active" briefing: text + voice audio, cached so Home can play it inst · Greeting, GreetingPlayer, Startup · uses: PcmPlayer.kt, Speaker.kt
- `Voices.kt` · 20 · Gender of each Gemini prebuilt voice + the language rule that keeps grammar matching the v · Voices
- `Weather.kt` · 72 · Today's weather from Open-Meteo (free, no key) using the phone's last known location. · Weather

### live/
- `CallEngine.kt` · 46 · What every kind of voice call looks like to the screens, the floating bubble and the notif · CallEngine · uses: Prefs.kt
- `CallHost.kt` · 87 · Owns the one live call of the whole process. · CallHost · uses: AudioRouter.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt, ModelCatalog.kt, Prefs.kt
- `CallService.kt` · 67 · Keeps the live call (and its microphone) alive while the app is in the background, as Andr · CallService · uses: AppLog.kt, MainActivity.kt, Prefs.kt
- `JarvisAccessibilityService.kt` · 185 · Lets the assistant see what is on the screen and tap, type, scroll and press Back / Home o · JarvisAccessibilityService
- `LiveClient.kt` · 310 · Real-time voice call with Gemini (Live API over WebSocket). · LiveClient, Http · uses: AppLog.kt, Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, Voices.kt
- `PhoneTools.kt` · 129 · Everything the assistant can do on the phone, described once and used by BOTH call engines · PhoneTools · uses: AppLog.kt, Device.kt, PhoneActions.kt, Weather.kt
- `PipelineClient.kt` · 169 · A voice call with ANY provider: your speech is cut out of the microphone, turned into text · PipelineClient · uses: PcmPlayer.kt, Speaker.kt, WavUtil.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt
- `ScreenShareRequestActivity.kt` · 40 · An invisible screen that shows Android's own "start recording or casting?" question, so th · ScreenShareRequestActivity · uses: AppLog.kt
- `ScreenShareService.kt` · 95 · Foreground service that captures the screen (MediaProjection) and hands ~1 JPEG frame/seco · ScreenShareService · uses: AppLog.kt, ImageUtil.kt
- `VoskModels.kt` · 61 · The small offline speech models (about 40 MB each) used by the keyless wake phrase engine. · VoskModels · uses: AppLog.kt
- `WakeService.kt` · 356 · Listens in the background for your wake phrase (any name / command you chose). When heard, · WakeService · uses: AppLog.kt, AssistantActivity.kt, MainActivity.kt, Prefs.kt

### overlay/
- `DragLayout.kt` · 62 · Root view of a floating window. Taps go to the Compose content (buttons); as soon as a fin · DragLayout
- `Overlay.kt` · 254 · Floating windows drawn over every app (needs "Display over other apps"): · Overlay · uses: AppLog.kt, MainActivity.kt, CallHost.kt, OverlayUi.kt, Theme.kt
- `OverlayLifecycleOwner.kt` · 35 · Compose needs a lifecycle, a saved-state registry and a view-model store to run. An overla · OverlayLifecycleOwner

### ui/components/
- `Components.kt` · 257 · The bottom bar follows the chosen interface style. · goldBrush, JText, Hairline, JarvisOrb, GoldButton, GoldIconBox, ScreenScaffold
- `SiriOrb.kt` · 242 · mode: 0 idle, 1 listening (reacts to level), 2 thinking, 3 speaking. style: 0..15 (see ORB · ORB_STYLES, SiriOrb, glow, ball, soft, warm, blobs · uses: Prefs.kt
- `VoiceWave.kt` · 47 · Siri-style layered voice waves that follow the voice level. · VoiceWave

### ui/overlay/
- `OverlayUi.kt` · 122 · The floating call bubble: your own orb, draggable anywhere. Tap it for mute / open / end. · Danger, BubbleUi, Controls, RoundIcon, CloseZoneUi, PopupUi · uses: ChatStore.kt, Prefs.kt, CallEngine.kt, CallHost.kt, LiveClient.kt

### ui/screens/
- `AccessScreen.kt` · 91 · One place for every permission the assistant needs. Tap a row that is not allowed to fix i · Access, AccessScreen · uses: Device.kt, JarvisAccessibilityService.kt
- `ApiScreen.kt` · 132 · - · ApiScreen, AutoRow · uses: ModelCatalog.kt, Prefs.kt
- `AssistantScreen.kt` · 213 · - · AssistantScreen, StatusRow · uses: AppLog.kt, Device.kt, Langs.kt, Prefs.kt, JarvisAccessibilityService.kt, VoskModels.kt
- `ChatScreen.kt` · 285 · The model's live thinking and the answer as it is being written. · ChatScreen, ThinkingCard, ActionBar, Bubble · uses: ChatStore.kt, ImageUtil.kt, CustomProviders.kt, ModelCatalog.kt, Prefs.kt, Speaker.kt
- `CodeScreen.kt` · 418 · Code & documents workspace: the agent writes real files in an editor, you can browse, edit · CodeBg, CodeText, CodeDim, Mono, viewable, rememberSaveFile, NameDialog · uses: CodeAgent.kt, Workspace.kt, AppLog.kt, CustomProviders.kt
- `ConsoleScreen.kt` · 110 · Every error, crash and event of the app, with time, file, line, function and stack. Copy o · ErrRed, WarnAmber, copy, ConsoleScreen · uses: AppLog.kt
- `ControlScreen.kt` · 151 · - · ControlScreen, Tile, SliderRow, PermRow · uses: Device.kt
- `HomeScreen.kt` · 216 · - · Boot, HomeScreen, BootRings, StatTile, QTile, LoadingDialog · uses: Startup.kt, Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, AuroraScreens.kt
- `LiveVoiceScreen.kt` · 290 · - · has, LiveVoiceScreen, LiveCallContent, CameraPip, RoundBtn, RouteChips · uses: AppLog.kt, AudioRouter.kt, ImageUtil.kt, Langs.kt, ModelCatalog.kt, Prefs.kt
- `MemoryScreen.kt` · 99 · - · MemoryScreen · uses: ChatStore.kt, Prefs.kt
- `Onboarding.kt` · 194 · - · StepHeader, LanguageScreen, PermissionsScreen, PRow, ApiSetupScreen, SetupScreen · uses: Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, Startup.kt
- `ProfileScreen.kt` · 127 · - · ProfileScreen, PrefRowV, PrefRow · uses: ModelCatalog.kt, Prefs.kt
- `ProvidersScreen.kt` · 234 · Add models from any other AI provider. Anything that offers an OpenAI-compatible API works · ProvidersScreen, ProviderList, ProviderEditor, Field, SwitchRow · uses: PcmPlayer.kt, AppLog.kt, CustomProviders.kt, GeminiClient.kt, Prefs.kt
- `SkillsScreen.kt` · 86 · Teach the agent new skills: short instruction packs it opens when a task needs them. · SkillsScreen, SkillList, SkillEditor · uses: Skills.kt, AppLog.kt
- `ThemeScreen.kt` · 88 · - · ThemeScreen · uses: Prefs.kt
- `UiStyleScreen.kt` · 105 · Pick the whole design of the app. Everything changes at once: layout, navigation, icons, c · StyleInfo, STYLES, UiStyleScreen, Mini · uses: Prefs.kt
- `VoiceScreen.kt` · 241 · - · VOICES, VoiceScreen, VoiceCard, AddVoiceDialog · uses: AppLog.kt, PcmPlayer.kt, Speaker.kt, Prefs.kt, Langs.kt, Voices.kt
- `WelcomeScreen.kt` · 45 · - · WelcomeScreen

### ui/styles/
- `AuroraScreens.kt` · 226 · AURORA · Home: one big hero card with the orb and a "Talk now" pill, quick chips, ring gau · AuroraHome, AuroraControl, AuroraSlider, AuroraSettings · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `FolioScreens.kt` · 197 · FOLIO: a warm, paper-like magazine. Serif headlines, navigation as text tabs at the TOP (n · FolioTabs, FolioHeader, Figure, FolioHome, FolioControl, FolioLevel, FolioSettings · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `NovaScreens.kt` · 207 · NOVA · Home: search-style "Ask" bar, a tonal assistant card, circular quick actions and a  · NovaHome, NovaControl, NovaSlider, NovaSettings · uses: Device.kt, Prefs.kt, JarvisAccessibilityService.kt
- `PulseScreens.kt` · 264 · PULSE: pure-black HUD look. Monospace text, sharp corners, a raised center "talk" button,  · PText, Sharp4, PulseNav, PulseTab, PulseHeader, PulseHome, Card · uses: Device.kt, Prefs.kt
- `StyleKit.kt` · 201 · What the Home screen controller hands to every interface style. · HomeUi, Warn, gemStatus, AuroraNav, NovaNav, AuroraHeader, NovaHeader · uses: ModelCatalog.kt, Prefs.kt, Device.kt, Components.kt

### ui/theme/
- `Color.kt` · 90 · 15 color styles. The first one is the original Figma "Royal Gold". · Palette, Palettes, Bg
- `Theme.kt` · 33 · - · JarvisTheme
- `Type.kt` · 10 · Figma uses Playfair Display (headings) and Inter (body). · JarvisSerif, JarvisSans

## Resources, manifest, build, CI, docs

- `.gitignore` · 9 lines
- `Complete.md` · 8 lines
- `FEATURES.md` · 22 lines
- `Goal.md` · 8 lines
- `Pending.md` · 7 lines
- `Planning.md` · 8 lines
- `README.md` · 110 lines
- `Roadmap.md` · 17 lines
- `Task.md` · 12 lines
- `build.gradle.kts` · 5 lines
- `gradle.properties` · 5 lines
- `settings.gradle.kts` · 8 lines
- `.github/workflows/apply-edits.yml` · 21 lines
- `.github/workflows/build.yml` · 24 lines
- `.github/workflows/map.yml` · 21 lines
- `.github/workflows/pack.yml` · 34 lines
- `app/build.gradle.kts` · 48 lines
- `app/src/main/AndroidManifest.xml` · 113 lines
- `app/src/main/res/values/colors.xml` · 2 lines
- `app/src/main/res/values/strings.xml` · 5 lines
- `app/src/main/res/values/themes.xml` · 15 lines
- `app/src/main/res/xml/accessibility_service_config.xml` · 10 lines
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
- `docs/HANDOFF_STATE.md` · 14 lines
- `docs/HOW_TO_WORK_WITH_AI.md` · 53 lines
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
