# Jarvis - Design Spec (from Figma)
Figma: https://www.figma.com/design/0voJxKEm57v1bjJLRXXCV4
Frame size: 360 x 800 dp

## Colors
bg 0B0B0D | surface 151518 | surface2 202024 | gold C9A84C | gold light EBD48F | gold dark A98531 | ivory F5F0E6 | muted 8E897D

## Fonts
Headings: Playfair Display (Medium)  |  Body: Inter (Regular / Medium / Semi Bold)

## Radius
Cards 18-24dp | Buttons 30dp | Chips 18-20dp | Bubbles 18dp (tail corner 4dp)

## Screens
01 Welcome -> WelcomeScreen.kt
02 Home -> HomeScreen.kt
03 Assistant Chat -> ChatScreen.kt
04 Smart Home Control -> ControlScreen.kt
05 Profile & Membership -> ProfileScreen.kt

## Added screens (Figma frames 06-12)
06 Live Voice - Listening  -> LiveVoiceScreen.kt
07 Live Voice - Speaking   -> LiveVoiceScreen.kt
08 App Launcher Icon       -> res/drawable/ic_launcher_foreground.xml
09 Gemini API Config       -> ApiScreen.kt
10 Voice Selection (15)    -> VoiceScreen.kt
11 Color Styles (15)       -> ThemeScreen.kt
12 Memory & Instructions   -> MemoryScreen.kt

## Update 3 (code ahead of Figma - Figma frames for these are pending)
Home animated boot + device stats | Chat history / model menu / photo | Control real | Live call (waves, camera, screen share, language chips)
Voice preview popup (0-100%) + custom voice | Orb styles (16)

## Update 4 (Figma frames pending)
Onboarding: Language, Permissions, Gemini API, System setup | Home loading popup | Live audio route chips | Assistant overlay (wake word) | Assistant settings
Icons: design/icons/*.svg (duotone, 24px grid, 1.7 stroke, round caps) - import _icon_sheet.svg into Figma.

## Update 5 (code only - Figma frames pending, same style)
- Voice: Gemini voices only (no phone/Google default voice). Voice model chosen automatically with fallback; female/male label on each voice; gender grammar rule sent to Gemini for chat, live call and briefing.
- Home: voice starts the moment loading reaches 100% together with the dashboard animation; briefing plays once per app launch (tap orb/speaker to replay).
- Assistant settings: wake phrase language chips (Auto / languages), Battery row, background-start fix, beep muting.
- Live call opens instantly (heavy start-up runs after the first frame; wake service releases the mic).

## Update 6 (code only - Figma frames pending)
New floating UI, same gold/ivory style: Call bubble (68dp orb, gold ring, controls pill: mic / open / end), Close target (62dp, turns red when hot), Mini chat card (gold-outlined, 26dp radius, handle, orb, last reply, "Continue chat" gold button, mic button).

## Update 7 (code only - Figma frames pending)
Interface styles added: Aurora (6 dark palettes, floating pill nav, 32dp hero card, ring gauges, tile settings, Rounded icons) and Nova (6 light palettes, flat nav with tonal indicator, search bar, list-style controls, grouped settings, Filled icons, sans-serif titles). Style is switched in Profile > Interface style; the whole app re-themes instantly.

## Update 8 (code only - Figma frames pending)
Pulse: #000 background, one neon accent, monospace uppercase text, 4dp corners, Sharp icons, bottom bar with a raised 68dp circular talk key, waveform under a 250dp orb, tabbed settings with numbered rows.
Folio: warm paper backgrounds, ink text, serif headlines, top text-tab navigation with a date line, key-figure row separated by rules, TwoTone icons, accordion settings.
