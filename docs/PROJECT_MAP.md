# PROJECT MAP (generated: do not edit by hand)

Updated 2026-10-16 16:31 UTC · 93 files. Read docs/AI_GUIDE.md first, then this map, then ONLY the files you need.

## Changed since the previous map
- nothing

## Kotlin source (`app/src/main/java/com/jarvis/app/`)
format: `path` · lines · what it is · declares · uses (other project files)


### (root)/
- `AssistantActivity.kt` · 112 · Floating Siri-style assistant that opens over whatever is on screen (started by the wake p · AssistantActivity, Overlay, Circle · uses: MainActivity.kt, AudioRouter.kt, ModelCatalog.kt, Prefs.kt, CallEngine.kt, LiveClient.kt, WakeService.kt, Components.kt
- `MainActivity.kt` · 128 · A screen requested from outside the UI (floating bubble, popup, notification). · Launch, MainActivity, bottomRoutes, JarvisApp · uses: AssistantActivity.kt, Speaker.kt, Skills.kt, Workspace.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt, Device.kt

### assistant/
- `Assistant.kt` · 42 · Registers the app as a real Android "digital assistant" (the same slot Google Assistant us · JarvisInteractionService, JarvisSessionService, JarvisSession, JarvisRecognitionService · uses: AssistantActivity.kt, AppLog.kt
- `DeviceAdmin.kt` · 32 · Lets the app lock the screen on your command. It can do nothing else (the policy list only · JarvisAdminReceiver, DeviceAdmin · uses: AppLog.kt

### audio/
- `AudioRouter.kt` · 129 · Call audio routing. AUTO = wired headset > Bluetooth > earpiece when the phone is at your  · Route, AudioRouter · uses: AppLog.kt
- `PcmPlayer.kt` · 47 · Plays 16-bit mono PCM and returns when playback has finished (or the coroutine is cancelle · PcmPlayer · uses: AppLog.kt
- `Speaker.kt` · 79 · Speaks ONLY with Gemini's own voices. There is deliberately no phone / Google default-voic · Speaker · uses: PcmPlayer.kt, AppLog.kt, GeminiClient.kt, ModelCatalog.kt, Prefs.kt
- `WavUtil.kt` · 60 · Small helpers to move audio between phone PCM and the WAV files that speech servers use. · WavUtil

### code/
- `AgentTools.kt` · 118 · The tools the code agent works with: a real project folder, an editor, document makers, sk · AgentTools · uses: CodeAgent.kt, DocGen.kt, Skills.kt, Workspace.kt, AppLog.kt, CustomProviders.kt
- `CodeAgent.kt` · 248 · The coding / document agent. It uses the model you chose in Chat (a Gemini model or a cust · CodeAgent · uses: AgentTools.kt, Skills.kt, Workspace.kt, AppLog.kt, CustomProviders.kt, GeminiClient.kt, ModelCatalog.kt, Prefs.kt
- `DocGen.kt` · 362 · Real file generators that run on the phone: PDF and PNG are DRAWN (shapes, gradients, icon · DocGen · uses: CustomProviders.kt
- `Skills.kt` · 100 · Skills are short instruction packs. The agent sees only each skill's name and one line; wh · Skills · uses: AppLog.kt, CustomProviders.kt
- `Workspace.kt` · 236 · Projects on the phone: folders of real files that you and the AI can create, edit, delete, · Workspace · uses: AppLog.kt, CustomProviders.kt

### data/
- `AppLog.kt` · 130 · The app-wide console. Every handled error (every catch block), every crash and the importa · AppLog · uses: CustomProviders.kt
- `ChatStore.kt` · 255 · Throws the last answer away and asks the same question again. · Msg, SessionMeta, ChatStore · uses: AppLog.kt, CustomProviders.kt, GeminiClient.kt, ModelCatalog.kt, Prefs.kt, RealFacts.kt, Voices.kt
- `CustomProviders.kt` · 355 · An AI model from any provider that speaks the OpenAI-compatible chat API (OpenAI, OpenRout · JSONArray, modelJson, ProviderModel, CustomProvider, CustomProviders, OpenAiCompat · uses: WavUtil.kt, AppLog.kt, GeminiClient.kt, Prefs.kt, PhoneTools.kt
- `Device.kt` · 142 · Real phone controls. Android does not let apps flip Wi-Fi/Bluetooth/Airplane directly, so  · Stats, DeviceStats, DeviceControl · uses: AppLog.kt
- `GeminiClient.kt` · 255 · Minimal Gemini API client (REST, v1beta). No model names are hard-coded: the list of usabl · GeminiClient · uses: AppLog.kt, CustomProviders.kt
- `ImageUtil.kt` · 31 · - · ImageUtil · uses: AppLog.kt
- `Langs.kt` · 45 · Sample sentence for voice previews, with grammar that matches the voice's gender. · Lang, Langs
- `ModelCatalog.kt` · 109 · Real models available for the user's key (from models.list) + automatic per-task selection · ModelCatalog · uses: Speaker.kt, AppLog.kt, GeminiClient.kt, Prefs.kt
- `PhoneActions.kt` · 132 · Things the assistant can do on the phone for you (browser, apps, screen). Used by the live · PhoneActions · uses: DeviceAdmin.kt, AppLog.kt, JarvisAccessibilityService.kt, AccessScreen.kt
- `Prefs.kt` · 115 · All user settings, stored privately on the device. · CustomVoice, Prefs · uses: AppLog.kt, CustomProviders.kt, Color.kt
- `RealFacts.kt` · 58 · The real state of the phone, read from Android at the moment it is needed. It is given to  · RealFacts · uses: AppLog.kt, Device.kt, Weather.kt
- `Startup.kt` · 105 · The prepared "system active" briefing: text + voice audio, cached so Home can play it inst · Greeting, GreetingPlayer, Startup · uses: PcmPlayer.kt, Speaker.kt, AppLog.kt, Device.kt, GeminiClient.kt, Langs.kt, ModelCatalog.kt, Prefs.kt
- `Voices.kt` · 20 · Gender of each Gemini prebuilt voice + the language rule that keeps grammar matching the v · Voices · uses: Prefs.kt
- `Weather.kt` · 72 · Today's weather from Open-Meteo (free, no key) using the phone's last known location. · Weather · uses: AppLog.kt

### live/
- `CallEngine.kt` · 46 · What every kind of voice call looks like to the screens, the floating bubble and the notif · CallEngine · uses: Prefs.kt, LiveClient.kt, PipelineClient.kt
- `CallHost.kt` · 87 · Owns the one live call of the whole process.  The call is NOT tied to a screen: it keeps r · CallHost · uses: AssistantActivity.kt, AudioRouter.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt, ModelCatalog.kt, Prefs.kt, CallEngine.kt
- `CallService.kt` · 67 · Keeps the live call (and its microphone) alive while the app is in the background, as Andr · CallService · uses: MainActivity.kt, AppLog.kt, Prefs.kt, CallHost.kt
- `JarvisAccessibilityService.kt` · 185 · Lets the assistant see what is on the screen and tap, type, scroll and press Back / Home o · JarvisAccessibilityService
- `LiveClient.kt` · 310 · Real-time voice call with Gemini (Live API over WebSocket). Mic PCM 16 kHz streams up cont · LiveClient, Http · uses: AppLog.kt, CustomProviders.kt, Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, RealFacts.kt, Voices.kt
- `PhoneTools.kt` · 129 · Everything the assistant can do on the phone, described once and used by BOTH call engines · PhoneTools · uses: AppLog.kt, CustomProviders.kt, Device.kt, PhoneActions.kt, RealFacts.kt, Weather.kt, CallHost.kt, JarvisAccessibilityService.kt
- `PipelineClient.kt` · 169 · A voice call with ANY provider: your speech is cut out of the microphone, turned into text · PipelineClient · uses: PcmPlayer.kt, Speaker.kt, WavUtil.kt, AppLog.kt, ChatStore.kt, CustomProviders.kt, GeminiClient.kt, ModelCatalog.kt
- `ScreenShareRequestActivity.kt` · 40 · An invisible screen that shows Android's own "start recording or casting?" question, so th · ScreenShareRequestActivity · uses: AppLog.kt, CallHost.kt, ScreenShareService.kt
- `ScreenShareService.kt` · 95 · Foreground service that captures the screen (MediaProjection) and hands ~1 JPEG frame/seco · ScreenShareService · uses: AppLog.kt, ImageUtil.kt, CallHost.kt
- `VoskModels.kt` · 61 · The small offline speech models (about 40 MB each) used by the keyless wake phrase engine. · VoskModels · uses: AppLog.kt
- `WakeService.kt` · 356 · Listens in the background for your wake phrase (any name / command you chose). When heard, · WakeService · uses: AssistantActivity.kt, MainActivity.kt, AppLog.kt, CustomProviders.kt, Prefs.kt, CallHost.kt, VoskModels.kt

### overlay/
- `DragLayout.kt` · 62 · Root view of a floating window. Taps go to the Compose content (buttons); as soon as a fin · DragLayout
- `Overlay.kt` · 254 · Floating windows drawn over every app (needs "Display over other apps"): - the call bubble · Overlay · uses: AssistantActivity.kt, MainActivity.kt, AppLog.kt, CallHost.kt, DragLayout.kt, OverlayLifecycleOwner.kt, OverlayUi.kt, Theme.kt
- `OverlayLifecycleOwner.kt` · 35 · Compose needs a lifecycle, a saved-state registry and a view-model store to run. An overla · OverlayLifecycleOwner

### ui/components/
- `Components.kt` · 257 · The bottom bar follows the chosen interface style. · goldBrush, JText, Hairline, JarvisOrb, GoldButton, GoldIconBox · uses: SiriOrb.kt, FolioScreens.kt, PulseScreens.kt, StyleKit.kt, Color.kt, Type.kt
- `SiriOrb.kt` · 242 · mode: 0 idle, 1 listening (reacts to level), 2 thinking, 3 speaking. style: 0..15 (see ORB · ORB_STYLES, SiriOrb, glow, ball, soft, warm · uses: Prefs.kt
- `VoiceWave.kt` · 47 · Siri-style layered voice waves that follow the voice level. · VoiceWave

### ui/overlay/
- `OverlayUi.kt` · 122 · The floating call bubble: your own orb, draggable anywhere. Tap it for mute / open / end. · Danger, BubbleUi, Controls, RoundIcon, CloseZoneUi, PopupUi · uses: ChatStore.kt, Prefs.kt, CallEngine.kt, CallHost.kt, LiveClient.kt, Components.kt, SiriOrb.kt, Color.kt

### ui/screens/
- `AccessScreen.kt` · 91 · One place for every permission the assistant needs. Tap a row that is not allowed to fix i · Access, AccessScreen · uses: DeviceAdmin.kt, Device.kt, Weather.kt, JarvisAccessibilityService.kt, Components.kt, Color.kt
- `ApiScreen.kt` · 132 · - · ApiScreen, AutoRow · uses: ModelCatalog.kt, Prefs.kt, Components.kt, Color.kt
- `AssistantScreen.kt` · 213 · - · AssistantScreen, StatusRow · uses: AppLog.kt, Device.kt, Langs.kt, Prefs.kt, JarvisAccessibilityService.kt, VoskModels.kt, WakeService.kt, Components.kt
- `ChatScreen.kt` · 298 · The model's live thinking and the answer as it is being written. · ChatScreen, ThinkingCard, ActionBar, Bubble · uses: Speaker.kt, ChatStore.kt, CustomProviders.kt, ImageUtil.kt, ModelCatalog.kt, Prefs.kt, Components.kt, Color.kt
- `CodeScreen.kt` · 418 · Code & documents workspace: the agent writes real files in an editor, you can browse, edit · CodeBg, CodeText, CodeDim, Red, TEXT_EXT, Mono · uses: CodeAgent.kt, Workspace.kt, AppLog.kt, CustomProviders.kt, Components.kt, Color.kt
- `ConsoleScreen.kt` · 110 · Every error, crash and event of the app, with time, file, line, function and stack. Copy o · ErrRed, WarnAmber, copy, ConsoleScreen · uses: AppLog.kt, Components.kt, Color.kt
- `ControlScreen.kt` · 151 · - · ControlScreen, Tile, SliderRow, PermRow · uses: Device.kt, Components.kt, Color.kt
- `HomeScreen.kt` · 216 · - · Boot, HomeScreen, BootRings, StatTile, QTile, LoadingDialog · uses: Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, Startup.kt, Voices.kt, Components.kt, SiriOrb.kt
- `LiveVoiceScreen.kt` · 290 · - · has, LiveVoiceScreen, LiveCallContent, CameraPip, RoundBtn, RouteChips · uses: AudioRouter.kt, Speaker.kt, AppLog.kt, CustomProviders.kt, ImageUtil.kt, Langs.kt, ModelCatalog.kt, Prefs.kt
- `MemoryScreen.kt` · 99 · - · MemoryScreen · uses: ChatStore.kt, Prefs.kt, Components.kt, Color.kt
- `Onboarding.kt` · 194 · - · StepHeader, LanguageScreen, PermissionsScreen, PRow, ApiSetupScreen, SetupScreen · uses: Device.kt, Langs.kt, ModelCatalog.kt, Prefs.kt, Startup.kt, Components.kt, Color.kt
- `ProfileScreen.kt` · 127 · - · ProfileScreen, PrefRowV, PrefRow · uses: Skills.kt, ModelCatalog.kt, Prefs.kt, Components.kt, AccessScreen.kt, Color.kt
- `ProvidersScreen.kt` · 303 · Add models from any other AI provider. Anything that offers an OpenAI-compatible API works · ProvidersScreen, ProviderList, ProviderEditor, ModelChips, Field, SwitchRow · uses: PcmPlayer.kt, AppLog.kt, CustomProviders.kt, GeminiClient.kt, Prefs.kt, Components.kt, VoiceScreen.kt, Color.kt
- `SkillsScreen.kt` · 86 · Teach the agent new skills: short instruction packs it opens when a task needs them. · SkillsScreen, SkillList, SkillEditor · uses: Skills.kt, AppLog.kt, Components.kt, Color.kt
- `ThemeScreen.kt` · 88 · - · ThemeScreen · uses: Prefs.kt, Components.kt, SiriOrb.kt, Color.kt
- `UiStyleScreen.kt` · 105 · Pick the whole design of the app. Everything changes at once: layout, navigation, icons, c · StyleInfo, STYLES, UiStyleScreen, Mini · uses: Prefs.kt, Components.kt, Color.kt
- `VoiceScreen.kt` · 241 · - · VOICES, VoiceScreen, VoiceCard, AddVoiceDialog · uses: PcmPlayer.kt, Speaker.kt, AppLog.kt, Langs.kt, Prefs.kt, Voices.kt, Components.kt, SiriOrb.kt
- `WelcomeScreen.kt` · 45 · - · WelcomeScreen · uses: Components.kt

### ui/styles/
- `AuroraScreens.kt` · 226 · AURORA · Home: one big hero card with the orb and a "Talk now" pill, quick chips, ring gau · AuroraHome, AuroraControl, AuroraSlider, AuroraSettings · uses: Skills.kt, Workspace.kt, Device.kt, Prefs.kt, Voices.kt, JarvisAccessibilityService.kt, Components.kt, SiriOrb.kt
- `FolioScreens.kt` · 197 · FOLIO: a warm, paper-like magazine. Serif headlines, navigation as text tabs at the TOP (n · FolioTabs, FolioHeader, Figure, FolioHome, FolioControl, FolioLevel · uses: Skills.kt, Device.kt, Prefs.kt, JarvisAccessibilityService.kt, Components.kt, SiriOrb.kt, AccessScreen.kt, StyleKit.kt
- `NovaScreens.kt` · 207 · NOVA · Home: search-style "Ask" bar, a tonal assistant card, circular quick actions and a  · NovaHome, NovaControl, NovaSlider, NovaSettings · uses: Skills.kt, Device.kt, Prefs.kt, Voices.kt, JarvisAccessibilityService.kt, Components.kt, SiriOrb.kt, AccessScreen.kt
- `PulseScreens.kt` · 264 · PULSE: pure-black HUD look. Monospace text, sharp corners, a raised center "talk" button,  · PText, Sharp4, PulseNav, PulseTab, PulseHeader, PulseHome · uses: Device.kt, ModelCatalog.kt, Prefs.kt, JarvisAccessibilityService.kt, SiriOrb.kt, StyleKit.kt, Color.kt
- `StyleKit.kt` · 201 · What the Home screen controller hands to every interface style. · HomeUi, Warn, gemStatus, AuroraNav, NovaNav, AuroraHeader · uses: Device.kt, ModelCatalog.kt, Prefs.kt, Components.kt, AuroraScreens.kt, FolioScreens.kt, NovaScreens.kt, PulseScreens.kt

### ui/theme/
- `Color.kt` · 90 · 15 color styles. The first one is the original Figma "Royal Gold". · Palette, Palettes, Bg, Surface1, Surface2, Gold · uses: Prefs.kt
- `Theme.kt` · 33 · - · JarvisTheme · uses: Color.kt
- `Type.kt` · 10 · Figma uses Playfair Display (headings) and Inter (body). · JarvisSerif, JarvisSans

## Other files
- `AndroidManifest.xml`
- `res/drawable/ic_badge.xml`
- `res/drawable/ic_chevron.xml`
- `res/drawable/ic_launcher_foreground.xml`
- `res/drawable/ic_nav_assistant.xml`
- `res/drawable/ic_nav_control.xml`
- `res/drawable/ic_nav_home.xml`
- `res/drawable/ic_nav_profile.xml`
- `res/drawable/ic_pref_api.xml`
- `res/drawable/ic_pref_memory.xml`
- `res/drawable/ic_pref_palette.xml`
- `res/drawable/ic_pref_voice.xml`
- `res/drawable/ic_pref_wake.xml`
- `res/mipmap-anydpi-v26/ic_launcher.xml`
- `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `res/values/colors.xml`
- `res/values/strings.xml`
- `res/values/themes.xml`
- `res/xml/accessibility_service_config.xml`
- `res/xml/device_admin.xml`
- `res/xml/recognition_service.xml`
- `res/xml/voice_interaction_service.xml`

## Build / design files
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `.github/workflows`
- `design/DESIGN_SPEC.md`
