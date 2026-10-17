# Changes — 2026-10-17 — Visual themes + sound FX system (JARVIS_VISUAL_THEMES_GUIDE)

Implements the full guide: 10 animated assistant themes + original app look (classic),
sound FX, a Theme Lab test screen, an audio-level helper and device performance tier.
All additive — the existing look/screens are untouched; the classic visual stays the default.

## Core
- `ui/theme/AssistantState.kt` — IDLE / LISTENING / THINKING / SPEAKING.
- `ui/theme/AssistantTheme.kt` — `AssistantTheme` interface + `ThemeCategory`.
- `ui/theme/ThemeRegistry.kt` — the single theme list (classic first + 10 themes).
- `ui/theme/ThemeRepository.kt` — persists selected id (Prefs) + `Fx` global FX player.
- `ui/theme/ClassicTheme.kt` — wraps the original `SiriOrb` (default, id `classic`).
- `ui/theme/PerformanceTier.kt` — low-RAM / battery-saver detection.
- `ui/theme/anim/AnimUtils.kt` — `rememberPhase`, `speedFor`, `arcRing`, `tickRing`.

## Orbs (AGSL RuntimeShader, API 33+) + fallback
- `ui/theme/orbs/OrbShaders.kt` — GLASS / FIRE / SUN / GOLD AGSL sources (all declare+use resolution, time, level).
- `ui/theme/orbs/ShaderOrb.kt` — API 33 host + `FallbackOrb` (gradient pulse) for older devices; bad shader → fallback.
- `ui/theme/orbs/ParticleSphere.kt` — pure-Compose 1400-dot sphere.
- `ui/theme/orbs/OrbThemes.kt` — `ShaderOrbTheme`, `ParticleSphereTheme`.

## Jarvis HUDs (pure Compose, all API levels)
- `ui/theme/hud/HudThemes.kt` — `HudTheme` + Arc Reactor, Radar Sweep, Holo Globe, Stark Amber, Minimal Frost.

## Sound FX
- `ui/theme/fx/FxProfile.kt` — `FxEvent`, `FxSpec` (Raw/Synth), `FxProfile`, `FxPresets` (tech/warm/soft).
- `ui/theme/fx/SynthFx.kt` — generates PCM tones with AudioTrack (no asset files needed).
- `ui/theme/fx/FxPlayer.kt` — SoundPool + synth; never throws.
- `audio/AudioLevel.kt` — RMS / boosted / simulated loudness helpers.

## Theme Lab + wiring
- `ui/screens/ThemeLabScreen.kt` — all themes in a 2-column grid, state chips, level slider,
  simulate-speech, sound buttons, FPS counter, tap = preview, long-press = set active.
- `Prefs` — `assistantTheme`, `fxEnabled`, `fxVolume`.
- `MainActivity` — `Fx.init` + BOOT sound + `themelab` route.
- `ProfileScreen` — "Theme Lab" entry.
- `HomeScreen` / `LiveVoiceScreen` — render the selected theme (classic unchanged when selected).

## Files
New: `ui/theme/{AssistantState,AssistantTheme,ThemeRegistry,ThemeRepository,ClassicTheme,PerformanceTier}.kt`,
`ui/theme/anim/AnimUtils.kt`, `ui/theme/orbs/{OrbShaders,ShaderOrb,ParticleSphere,OrbThemes}.kt`,
`ui/theme/hud/HudThemes.kt`, `ui/theme/fx/{FxProfile,SynthFx,FxPlayer}.kt`,
`ui/screens/ThemeLabScreen.kt`, `audio/AudioLevel.kt`.
Changed: `data/Prefs.kt`, `MainActivity.kt`, `ui/screens/{ProfileScreen,HomeScreen,LiveVoiceScreen}.kt`.

## Verify
1. Profile > Theme Lab. Move the level slider — every theme changes.
2. Tap a theme to preview; long-press to set it active. Home orb now uses it.
3. Sound buttons play synth tones with no asset files. Toggle/volume in Prefs.
4. On API < 33 or a broken shader, orbs show the gradient fallback (no crash).
