# Jarvis - Luxury AI Assistant (Android, Jetpack Compose)

Figma: https://www.figma.com/design/0voJxKEm57v1bjJLRXXCV4

## Update 4
- First run: Intro -> Language -> Permissions -> Gemini API -> System setup (real 0-100% loading) -> Dashboard. Next runs open Home directly.
- Home: premium loading popup on app start (real stages: connect, device status, weather, briefing, voice) then Jarvis speaks automatically.
  Coming back to Home plays the cached briefing instantly. The speaker button replays it. Tapping the current tab does nothing (no flicker).
- Live call audio: Auto / Speaker / Ear / Bluetooth / Wired. Auto = loudspeaker normally, earpiece when the phone is at your ear (proximity sensor),
  Bluetooth or wired headset when connected.
- Assistant name + wake phrase (Profile > Assistant name & wake phrase). Background listening opens a Siri-style panel over any screen.
- Custom premium icon set (design/icons/*.svg, res/drawable/ic_*.xml). Import design/icons/_icon_sheet.svg into Figma to edit them.

## What works
- **Home**: animated boot sequence (orb, rotating rings, status lines), Jarvis speaks a greeting with REAL phone data:
  battery %, battery temperature + thermal status, free storage, free RAM (no special permission needed). Equal-size tiles.
- **Live Call** (mic button): Gemini Live API over WebSocket. Continuous voice-to-voice (no push-to-talk), Siri-style orb + voice wave layers,
  language chips (8 languages) you can tap at any time, mic mute, **front/back camera**, **screen sharing** (MediaProjection), phone tools by voice
  (flashlight, volume, device status).
- **Chat**: real Gemini replies, **automatic model per task** (quick / photo / complex), optional manual model menu, **chat history** (new / open / delete),
  **photo upload**.
- **Control**: real flashlight, volume, ringer, brightness, Do Not Disturb, quick links to Wi-Fi / Bluetooth / Airplane / Location panels
  (Android does not allow apps to switch Wi-Fi/Bluetooth directly), permission status + grant buttons.
- **Profile**: Gemini API (auto models), Voice (16 voices, 8 languages, loading popup 0-100%, custom voices), Appearance (16 orb styles + 15 colors), Memory.

## Gemini API (no fake models)
1. Free key: https://aistudio.google.com -> Get API key.
2. App -> Profile -> Gemini API -> paste key -> Save & Connect.
3. The model list comes live from Google (`models.list`) for YOUR key. Jarvis picks the best available model for each job automatically
   (fast / vision / reasoning / voice / live). Nothing is hard-coded.

## Build the APK from a phone (no PC)
1. Create a GitHub repo and upload all files of this zip (keep folders). Easiest on phone: Termux
   ```
   pkg install git unzip
   unzip figma-screen.zip && cd figma-screen
   git init && git add . && git commit -m "Jarvis"
   git branch -M main
   git remote add origin https://github.com/USERNAME/REPO.git
   git push -u origin main
   ```
2. Repo -> Actions -> "Build APK" -> Run. After 2-5 min download Artifacts -> `jarvis-debug-apk`.
3. Install `app-debug.apk`. Allow Microphone / Camera / Notifications when asked.

## Notes
- minSdk 26 (Android 8+). Needs internet.
- Live API and Gemini voices need a key/plan that allows them; the app shows exactly what your key supports.
- If the Live connection fails the app retries with alternative setup layouts and shows Google's error message.
- This code could not be compiled/tested in the authoring environment. If the first build fails, send the Actions log.
- Accessibility Service IS included: `live/JarvisAccessibilityService.kt` reads the screen, taps by text or by element, types, scrolls and presses Back/Home/Recents. Switch it on in Android Settings > Accessibility (shortcut in Access & permissions). It is what powers the AI cursor, `tap_at`, `click_element` and screen sharing via `takeScreenshot`.

## Update 5
- Removed the phone/Google text-to-speech completely. All speech now comes from Gemini voices (`audio/Speaker.kt`).
- `Speaker.synth` picks the voice model automatically from your API key's model list and tries the next one on quota/errors.
- Wake word: hears the phrase in the right language (Assistant > Wake phrase language), fuzzy matching, beep muting, full-screen-notification backup, battery row. Allow: Microphone, Display over other apps, Battery no restrictions.

## Update 6 - floating call bubble, mini chat popup, safer background listening
- `live/CallHost.kt`: one process-wide live call (not tied to a screen). `live/CallService.kt` keeps the mic alive in the background (foreground service + notification with an "End call" button).
- Press Home during a call: the call continues and your orb appears as a draggable bubble (`overlay/Overlay.kt`). Tap it for mute / open app / end. Drag it onto the X at the bottom to hang up.
- After hang-up from the bubble, a small chat card appears: "Continue chat", call again, X. Drag it UP to open the full app, DOWN to dismiss.
- Every spoken turn of a call is saved to the current chat, so "Continue chat" really continues.
- Wake word pauses itself while another app is in a call (WhatsApp, Meet, phone) and resumes afterwards.
- Needs: "Display over other apps" (Profile > Assistant, or the hint on the call screen).

## Update 7 - console, screen control, offline wake word, 3 interface styles
- **Console** (Profile > Console): every handled error (all catch blocks), crash and key event with time, file:line, function, message and stack. Filter, copy one / copy all, select + delete, clear all. Saved on the phone (`data/AppLog.kt`, `ui/screens/ConsoleScreen.kt`).
- **Screen control**: `live/JarvisAccessibilityService.kt` (read screen, tap by text, type, scroll, Back/Home/Recents). Switch on in Android Settings > Accessibility (shortcut in Access & permissions).
- **Live-call tools**: web search (Google Search grounding), weather by city, read/tap/type/scroll screen, press Back/Home, open app, open website. Chat also uses web search when the model supports it.
- **Wake word**: offline engine (Picovoice Porcupine, `ai.picovoice:porcupine-android:3.0.1`): no internet, no start/stop beeps, low battery. Needs the free AccessKey from console.picovoice.ai. Basic Google-speech mode is still there as a fallback.
- **Default assistant**: app registers as a real Android digital assistant (`assistant/Assistant.kt`). Choose it in Settings > Default apps > Digital assistant app; then power-button long-press / home gesture opens it, even when closed.
- **Interface styles** (Profile > Interface style): 0 Classic Gold, 1 Aurora (dark glass, floating nav, rounded icons, ring gauges, settings as separate tiles), 2 Nova (light, Material-style, filled icons, search bar, grouped settings). Own palettes per style. Code: `ui/styles/*`, `Palettes.style`.
- **Access & permissions** page lists every permission with one-tap fix.

## Update 8 - API fix, custom providers, 5 interface styles, more screen control, keyless wake phrase
- **API fix**: the 429 "quota exceeded" in the console came from the Google Search tool that Update 7 added to every chat call (new free keys have no quota for it). Search is now OFF by default (Gemini API > Web search). Chat also falls back by itself to the next model of your key (and to "backup" custom providers) when one is out of quota.
- **Gemini API screen** now lists every real model of the key (chat, voice, live).
- **Custom providers** (Gemini API > Custom providers): any OpenAI-compatible API: name, base URL, API key, model id, optional headers / system prompt / temperature, photo support, "use as backup". Fetch model list + Test connection. Choose it in the Chat model menu. (`data/CustomProviders.kt`, `ui/screens/ProvidersScreen.kt`)
- **Interface styles**: now 5: Classic Gold, Aurora, Nova, **Pulse** (AMOLED HUD, monospace, sharp icons, raised center talk key) and **Folio** (paper magazine, serif, text tabs at the top, accordion settings).
- **Screen control tools** for the live call: swipe, tap at a spot, double tap, long press, submit (Enter/Search/Send), paste, play/pause/next/mute/volume, wait, open Chrome/URL, open any app. On Android 13+ a sideloaded APK must first be allowed under Settings > Apps > Jarvis > menu > "Allow restricted settings", then switch Accessibility on.
- **Wake phrase**: new default engine "Offline phrase" (Vosk): your own English/Hindi phrase, no key, no internet, no beeps. Needs the one-time model download (about 40 MB) in Assistant settings. Picovoice and Basic engines remain.

## Update 9 - API diagnosis, model fallback, voice calls with ANY provider
- **Gemini errors explained**: HTTP errors now carry the quota name and retry time from Google. Chat tries up to 10 models (last working one first, a busy model is asked twice), and when every model is out of quota the message says so and what to do. Gemini API > "Test every chat model" tests each model of the key and shows which ones work.
- **Gemini Live**: tries every Live model of the key before giving up, logs every close (code, reason, model) in the Console, and can hand the call over to a provider marked "backup".
- **Calls with a custom provider** (`live/PipelineClient.kt`): your speech is cut out of the mic -> the provider's speech-to-text (`/audio/transcriptions`, Whisper style; Gemini is used if none is set) -> the provider's chat model with the phone tools (OpenAI tool calling) -> the provider's own voice (`/audio/speech`, WAV). Turn-based: it listens while you speak and talks while it answers.
- Each provider has: speech-to-text model, text-to-speech model, its voice list (tap a voice to hear it), "borrow speech from another provider", "use phone tools", "use for voice calls". Presets for OpenAI and Groq fill the speech fields.
- `live/CallEngine.kt` is the common call type; `live/PhoneTools.kt` holds the phone tools once for both engines.
- Recovered fallbacks (a busy voice model, a model out of quota) are logged as INFO instead of WARN.

## Update 10 - system control by voice (Accessibility, screen sharing, device admin, power menu)
- **Phone calls**: tools `end_phone_call` (Android's TelecomManager.endCall when "Answer & end phone calls" is allowed, otherwise the red End / Hang up / Decline button found through Accessibility: text, description or view id), `answer_phone_call`, `end_assistant_call` (hangs up the live call with Jarvis; the call screen closes by itself).
- **Screen lock**: `lock_screen` uses DevicePolicyManager.lockNow() (`assistant/DeviceAdmin.kt`, policy list contains only `force-lock`); without the admin right it falls back to the Accessibility lock action (Android 9+). Switch on in Access & permissions; remove the right there before uninstalling.
- **Power**: `power_menu` / `press_button power_menu` = GLOBAL_ACTION_POWER_DIALOG. It only SHOWS the menu; Android does not let an app shut the phone down, so you tap the option.
- **System buttons**: back, home, recents, notifications, quick settings, lock screen, screenshot (Accessibility global actions).
- **Screen sharing**: MediaProjection + VirtualDisplay + ImageReader, 1 JPEG frame per second (720 px) into the Gemini Live socket (already present). New: `share_screen` / `stop_screen_share` voice tools open an invisible screen that shows Android's own "Start recording?" question (`live/ScreenShareRequestActivity.kt`); frames follow the current call even when the call screen is closed.
- **Intent actions**: `open_settings` (wifi, bluetooth, display, sound, battery, apps, location, accessibility, date, language, storage, security, privacy, airplane, nfc, network, about), `set_timer`, `set_alarm`, `dial_number` (opens the dialer, you press call).
- Limits: during a phone call Android gives the microphone to the call, so the wake word cannot hear you then. "Answer / decline" by voice works while the phone is ringing; during a call use the floating bubble or the Jarvis call. Custom-provider calls (non-Gemini) cannot see the screen.

## Update 11 - code & documents workspace, real phone facts, chat actions, live thinking
- **Workspace** (Profile > Code & documents workspace): projects are real folders on the phone. The code agent uses the model chosen in Chat (Gemini or a custom provider) with tool calling and works in an editor panel instead of the chat: list/read/write files, replace lines, replace text, delete, move, search, make documents, open skills, verify, finish. Live "thinking" panel with every step, typed-code editor view, a final connection check (missing files, broken links/imports, bad JSON, unbalanced brackets) that sends the agent back to fix problems (up to 2 rounds).
- **Files tab**: browse the tree, edit and save any file, new file/folder, delete, import files or a ZIP (zip-slip safe), make a ZIP. **Output tab / cards**: one file -> View + Download; many files -> ZIP (download only). PDFs and images preview, text files open in a viewer; Download uses the system "save as" dialog.
- **Documents on the phone** (`code/DocGen.kt`): PDF and PNG are drawn from a JSON description (rectangles, rounded boxes, gradients, circles, lines, text with Urdu/Arabic support, tables, images, SVG-path logos and 16 built-in icons); DOCX, XLSX (formulas, header styling) and PPTX are written as real Office files. The agent learns the format from the built-in skill "Documents & PDFs".
- **Skills** (Profile > Skills): 9 built-in skills (documents, website design, Python, Android, bug fixing, README, code review, Excel, Urdu documents), switch on/off, edit, add your own, load instructions from a text file. The agent only reads a skill when it needs it (saves tokens).
- **Real phone facts**: `data/RealFacts.kt` reads time, battery, storage, memory, temperature, Wi-Fi, Bluetooth, volume, brightness and weather from the phone and puts them in every chat and call prompt; the model is told never to invent them. Weather now uses GPS/network location, then your internet location as a fallback, with feels-like, humidity and wind. New `get_time` tool.
- **Default voice** is now male (Charon). Phones that still had the old default (Zephyr) are moved once.
- **Chat**: streaming answers with the model's own live thinking (Gemini thought summaries / provider reasoning), "Show thinking" afterwards; under each message: copy, listen (Gemini voice), like, dislike with a note, regenerate; under your messages: copy, edit, resend. Custom-provider voice calls speak sentence by sentence and stop listening sooner, so replies start faster.

## Working with AI assistants on a big project (read this before asking an AI for changes)
`docs/AI_GUIDE.md` (the rules every AI must follow), `docs/ARCHITECTURE.md` (feature -> files), `docs/PROJECT_MAP.md` (generated, one line per file), `docs/HANDOFF_STATE.md` (progress between accounts), `docs/HOW_TO_WORK_WITH_AI.md` (owner's phone workflow, Urdu), `tools/pack_context.py` + `.github/workflows/pack.yml` (make a small zip with only the files a task needs), `tools/apply_edits.py` + `.github/workflows/apply-edits.yml` (apply small AI edits to big files), `.github/workflows/map.yml` (keeps the map up to date). Tracking files in the root: `Task.md`, `Planning.md`, `Roadmap.md`, `Pending.md`, `Complete.md`, `Goal.md`; AI skills in `docs/skills/`; ready prompts in `docs/START_PROMPT.md`.
