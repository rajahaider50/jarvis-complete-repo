# ARCHITECTURE (hand-written, short). Paths are inside `app/src/main/java/com/jarvis/app/`.

Android app "Jarvis": Kotlin 1.9.24, Jetpack Compose (BOM 2024.09, compiler 1.5.14), minSdk 26, compileSdk 34, package `com.jarvis.app`. One module `app`. No DI framework: Kotlin `object` singletons with Compose `mutableStateOf` state. The code is never compiled by the AI authoring environment (no internet): re-read what you write.

## Feature -> files (start here, open only what the task touches)
| Feature | Files |
|---|---|
| App entry, routes, deep links from bubble/notification | `MainActivity.kt` (NavHost; `Launch.route`) |
| Preferences (every setting) | `data/Prefs.kt` (a setting = property + load in `init` + save in `save`) |
| Colors, 5 interface styles | `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/components/Components.kt`, `ui/styles/StyleKit.kt`, `ui/styles/AuroraScreens.kt`, `ui/styles/NovaScreens.kt`, `ui/styles/PulseScreens.kt`, `ui/styles/FolioScreens.kt`, `ui/screens/UiStyleScreen.kt` |
| Shared widgets (JText, JField, GoldButton, ScreenScaffold, SiriOrb) | `ui/components/Components.kt`, `ui/components/SiriOrb.kt` |
| Home / Control / Profile tabs | `ui/screens/HomeScreen.kt`, `ui/screens/ControlScreen.kt`, `ui/screens/ProfileScreen.kt` (classic) + the `Styled*` functions in `ui/styles/StyleKit.kt` |
| Text chat + history + actions | `ui/screens/ChatScreen.kt`, `data/ChatStore.kt` |
| Gemini REST (chat, stream, TTS, probe) | `data/GeminiClient.kt`, `data/ModelCatalog.kt` |
| Other AI providers (OpenAI-compatible: chat, tools, STT, TTS) | `data/CustomProviders.kt` (holds `OpenAiCompat`; `CustomProviders.importJson(text)` parses a provider .json with baseURL/apiKey + a models list and adds one provider per model), `ui/screens/ProvidersScreen.kt`, `ui/screens/ApiScreen.kt` |
| Voices (Gemini TTS) | `audio/Speaker.kt`, `audio/PcmPlayer.kt`, `audio/WavUtil.kt`, `ui/screens/VoiceScreen.kt`, `data/Voices.kt`, `data/Langs.kt` |
| Voice call, Gemini Live (WebSocket) | `live/LiveClient.kt`, `live/CallEngine.kt`, `live/CallHost.kt`, `live/CallService.kt`, `ui/screens/LiveVoiceScreen.kt` |
| Voice call with any provider (STT -> chat -> TTS) | `live/PipelineClient.kt`, `data/CustomProviders.kt` |
| Phone tools the AI can call | `live/PhoneTools.kt` (declarations + execute), `data/PhoneActions.kt`, `live/JarvisAccessibilityService.kt`, `data/Device.kt` (DeviceControl, DeviceStats) |
| Floating call bubble + after-call popup | `overlay/Overlay.kt`, `overlay/DragLayout.kt`, `overlay/OverlayLifecycleOwner.kt`, `ui/overlay/OverlayUi.kt` |
| Wake word (vosk / porcupine / basic) | `live/WakeService.kt`, `live/VoskModels.kt`, `ui/screens/AssistantScreen.kt`, `AssistantActivity.kt` |
| Default-assistant slot, screen lock | `assistant/Assistant.kt`, `assistant/DeviceAdmin.kt` |
| Screen sharing | `live/ScreenShareService.kt`, `live/ScreenShareRequestActivity.kt` |
| Permissions page | `ui/screens/AccessScreen.kt` |
| Console / error log | `data/AppLog.kt`, `ui/screens/ConsoleScreen.kt` |
| Real phone facts, weather | `data/RealFacts.kt`, `data/Weather.kt` |
| Startup briefing + loading | `data/Startup.kt` |
| Code & documents workspace (agent, editor, files) | `code/Workspace.kt`, `code/CodeAgent.kt`, `code/AgentTools.kt`, `code/DocGen.kt`, `code/Skills.kt`, `ui/screens/CodeScreen.kt`, `ui/screens/SkillsScreen.kt` |
| Memory + custom instructions | `ui/screens/MemoryScreen.kt` |

## Routes (MainActivity NavHost)
welcome, language, permissions, apisetup, setup (onboarding) · home, chat, control, profile (tabs) · live, api, providers, voices, themes, memory, assistant, code, skills, uistyle, access, console. (`code/Skills.kt` is the APP's in-app skills feature; the AI-work skills are in `docs/skills/`.)

## Core flows
- **Chat**: `ChatScreen` -> `ChatStore.ask()` -> tries `CustomProviders.selected()` or Gemini models from `ModelCatalog.chatCandidates()` (streams via `GeminiClient.chatStream`) -> falls back to the next model on quota errors -> `messages` list (Compose state) -> saved per session.
- **Call**: `CallHost.begin()` picks `LiveClient` (Gemini Live) or `PipelineClient` (provider chosen in `Prefs.callProvider`), starts `CallService` (foreground, microphone), pauses `WakeService`. Both extend `CallEngine`. Tools run through `PhoneTools.execute()`. Home pressed -> `MainActivity.onStop` -> `Overlay.showBubble()`.
- **Agent**: `CodeScreen` -> `CodeAgent.start()` -> backend (Gemini function calling or OpenAI tool calling) loops over `AgentTools` -> files in `Workspace` (folder `filesDir/projects/<id>`) -> `verify` -> deliver zip/file to `out_<id>`.
- **Errors**: every `catch` calls `AppLog.w(e)`; real failures call `AppLog.e(tag, ...)`; shown in Profile > Console.

## Conventions that must not break
1. New screen: add `composable("route")` in `MainActivity.kt` AND a row in the five settings hubs (`ui/screens/ProfileScreen.kt` + the four `*Settings` in `ui/styles/`).
2. New setting: `Prefs` property + `init` load + `save` write (all three, same key string).
3. New AI phone tool: add to `PhoneTools.all` and to the `when` in `PhoneTools.execute` (both call engines use it). Say it in the Live system text only if the model must be told how to use it.
4. New permission/service/activity: `AndroidManifest.xml` as well. New drawable/string/xml: it must exist in `res/`.
5. Use `JText` / `JField` / theme colors (`Gold`, `Ivory`, `Muted`, `Bg`, `Surface1/2`), never hard-coded colors, except the code editor panel and status red.
6. Never put API keys in code. Models come from the user's own key (`ModelCatalog`), never invented names.
7. Background work: `Dispatchers.IO` for network/files; Compose state may be written from any thread, but Android views/windows only on main.
8. Every `catch` must log through `AppLog` (an empty catch hides bugs).

## Fragile areas (change only when the task is about them)
`live/WakeService.kt` (three engines, mic ownership), `live/LiveClient.kt` (connect/retry/model fallback in `ended()`), `overlay/Overlay.kt` (window flags and touch handling), `data/ChatStore.kt` (`ask()` fallback chain), `code/Workspace.kt` (path safety: `file()` must keep refusing paths outside the project).
