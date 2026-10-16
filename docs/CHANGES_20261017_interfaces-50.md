# CHANGES 2026-10-17 · Interface styles 5 -> 50 (25 native Android + 25 iOS)

## What changed
- **`ui/styles/IfaceCatalog.kt`** (new): 45 extra interface styles (20 native-Android premium + 25 iOS premium), each with its own premium palette and a layout `engine` (1 Aurora glass / 2 Nova Material / 3 Pulse HUD).
- **`ui/theme/Color.kt`**: added `Palettes.engine` and `Palettes.serif` state (which layout engine + serif titles the current style uses).
- **`data/Prefs.kt`**: `applyTheme()` now maps styles 5..49 to their palette/engine/serif via `IfaceCatalog`; styles 0..4 behave exactly as before.
- **`ui/components/Components.kt`**: `BottomNav`, `SubHeader`, the Folio top-tabs and the serif rule now key off `Palettes.engine`/`Palettes.serif` instead of `Palettes.style` (so extra styles route correctly).
- **`ui/styles/StyleKit.kt`**: `StyledHome/Control/Settings` now switch on `Palettes.engine`.
- **`ui/screens/UiStyleScreen.kt`**: shows all **50** styles in two groups (NATIVE ANDROID · PREMIUM 0..24, iOS · PREMIUM 25..49) with a live mini preview per style.

## Why
Request: 5 interface styles -> 50, i.e. 25 native-Android premium + 25 iOS premium.

## Approach / safety
- The 5 built-in styles (Classic Gold, Aurora, Nova, Pulse, Folio) are untouched; new styles are indexes 5..49 and reuse the existing, already-working layout engines with a dedicated palette each, so nothing new can break rendering.
- `Prefs.applyTheme()` guard (`uiStyle >= 5`, `forStyle(...) != null`) falls back to the old path safely.
- `Palettes.engine` defaults to 0 (Classic) and equals the style for 0..4, so pre-existing behaviour is byte-for-byte the same.

## How to test
Profile > Appearance > Interface style shows a grouped list of 50; picking any of the 45 new ones switches the whole app (nav, home, control, settings) into that premium look instantly.

## Untested
Not compiled locally; verification is the GitHub Actions `Build APK` job + a phone pass.
