# ARCHITECTURE (short). Paths are relative to `app/src/main/java/com/jarvis/app/`

## Feature -> files (open only the row you need)
| Feature | Files |
|---|---|
| Routes / navigation / back handling | `MainActivity.kt` |
| Colours, palettes, 5 interface styles | `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `ui/components/Components.kt`, `ui/styles/StyleKit.kt` |
| Style layouts (Aurora, Nova, Pulse, Folio) | `ui/styles/AuroraScreens.kt`, `ui/styles/NovaScreens.kt`, `ui/styles/PulseScreens.kt`, `ui/styles/FolioScreens.kt`, `ui/screens/UiStyleScreen.kt` |
| Orb and voice wave visuals | `ui/components/SiriOrb.kt`, `ui/components/VoiceWave.kt`, `ui/screens/ThemeScreen.kt` |
| Onboarding / welcome | `ui/screens/Onboarding.kt`, `ui/screens/WelcomeScreen.kt` |
| Home / Control / Profile tabs | `ui/screens/HomeScreen.kt`, `ui/screens/ControlScreen.kt`, `ui/screens/ProfileScreen.kt` |
| Chat UI (bubbles, thinking, actions) | `ui/screens/ChatScreen.kt`, `data/ChatStore.kt` |
| Gemini requests (chat, stream, tts, probe) | `data/GeminiClient.kt`, `data/ModelCatalog.kt`, `ui/screens/ApiScreen.kt` |
| Custom AI providers (OpenAI-compatible) | `data/CustomProviders.kt`, `ui/screens/ProvidersScreen.kt` |
| Voices and speaking | `audio/Speaker.kt`, `audio/PcmPlayer.kt`, `audio/WavUtil.kt`, `ui/screens/VoiceScreen.kt`, `data/Voices.kt`, `data/Langs.kt` |
| Live call (Gemini Live) | `live/LiveClient.kt`, `live/CallEngine.kt`, `live/CallHost.kt`, `live/CallService.kt`, `audio/AudioRouter.kt`, `ui/screens/LiveVoiceScreen.kt` |
| Call with any provider | `live/PipelineClient.kt`, `data/CustomProviders.kt`, `audio/WavUtil.kt` |
| Phone tools the AI can use | `live/PhoneTools.kt`, `data/PhoneActions.kt`, `live/JarvisAccessibilityService.kt`, `data/Device.kt` |
| Floating call bubble and mini popup | `overlay/Overlay.kt`, `overlay/DragLayout.kt`, `overlay/OverlayLifecycleOwner.kt`, `ui/overlay/OverlayUi.kt` |
| Wake word (offline vosk / porcupine / basic) | `live/WakeService.kt`, `live/VoskModels.kt`, `ui/screens/AssistantScreen.kt`, `AssistantActivity.kt` |
| Default assistant, lock screen | `assistant/Assistant.kt`, `assistant/DeviceAdmin.kt` |
| Screen sharing | `live/ScreenShareService.kt`, `live/ScreenShareRequestActivity.kt` |
| Permissions page | `ui/screens/AccessScreen.kt` |
| Real phone facts, weather, time | `data/RealFacts.kt`, `data/Weather.kt`, `data/Device.kt` |
| Console / error log | `data/AppLog.kt`, `ui/screens/ConsoleScreen.kt` |
| Startup briefing | `data/Startup.kt` |
| Memory / instructions screen | `ui/screens/MemoryScreen.kt` |
| Code and documents workspace | `code/Workspace.kt`, `code/CodeAgent.kt`, `code/AgentTools.kt`, `code/DocGen.kt`, `code/Skills.kt`, `ui/screens/CodeScreen.kt`, `ui/screens/SkillsScreen.kt` |
| Settings storage | `data/Prefs.kt` |
| Manifest / build | `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `.github/workflows/build.yml` |

## Routes (MainActivity NavHost)
welcome, language, permissions, apisetup, setup (onboarding) · home, chat, control, profile (tabs) · live, api, providers, voices, themes, memory, assistant, code, skills, uistyle, access, console.

## Flows in one line each
- App start: `MainActivity` inits Prefs, AppLog, CustomProviders, RealFacts, Workspace, Skills, then `JarvisApp()` NavHost.
- Style switch: `Prefs.uiStyle` -> `Palettes.style` -> `Palettes.current`; `ui/components/Components.kt` picks nav bar and header per style; Home/Control/Profile call `StyledHome/Control/Settings`.
- Chat: `ChatScreen` -> `ChatStore.ask` -> Gemini models in order (good model first, next on 429/503) or the chosen custom provider.
- Call: UI -> `CallHost.begin` -> `LiveClient` (Gemini Live websocket) or `PipelineClient` (STT -> provider chat with tools -> provider voice); `CallService` keeps mic alive; `Overlay` shows the bubble when app is in background.
- Tools: both engines use `PhoneTools.all` and `PhoneTools.execute`.
- Wake: `WakeService` (foreground, microphone) -> `AssistantActivity` panel; paused while a call runs.
- Workspace agent: `CodeAgent` loops model <-> `AgentTools` over a `Workspace` project.

## Conventions
- UI blocks in `ui/components/Components.kt` (`JText`, `JField`, `GoldButton`, `OutlineButton`, `JToggle`, `ScreenScaffold`, `SubHeader`). Colours from `Gold`, `Ivory`, `Muted`, `Surface1`, `Surface2`, `Bg`. Never hard-code colours.
- Logging: `AppLog.i/w/e`; user-visible errors are short sentences.
- Cross-thread UI state: Compose `mutableStateOf` inside objects.

## Fragile areas (change only when the request needs it)
`live/LiveClient.kt` (setup message, retry, model fallback), `live/WakeService.kt` (engine selection, pausing), `data/Prefs.kt` (keys must stay stable), `AndroidManifest.xml` (service types), `code/DocGen.kt` (Office XML must stay valid).
