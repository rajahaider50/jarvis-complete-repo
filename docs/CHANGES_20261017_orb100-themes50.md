# CHANGES 2026-10-17 · 100 voice orb styles + 50 color themes

## What changed
- **`ui/theme/Color.kt`**: `Palettes.all` grew from 15 to **50** themes (added 35: Midnight Blue, Sky, Graphite, Coral, Lavender, Mint, Peach, Indigo, Crimson, Amber, Arctic, Forest, Wine, Stone, Aqua, Sand, Berry, Steel, Neon Green, Neon Purple, Neon Pink, Neon Blue, Ocean Deep, Blush, Cobalt, Turquoise, Fuchsia, Bronze, Slate Blue, Lime Luxe, Candy, Glacier, Espresso, Royal Purple, Aurora Teal).
- **`ui/components/SiriOrb.kt`**: `ORB_STYLES` grew from 50 to **100** (added 50 iOS/premium styles, indexes 50..99) plus 50 new `DrawScope` draw functions.
- **`ui/screens/ThemeScreen.kt`**: no change needed — it already renders `ORB_STYLES.size` and `Palettes.list().size`.

## Why
Request: add 50 more (paid iPhone/iOS style) voice orbs to reach 100, and add 35 more color themes to reach 50.

## Approach / safety
- Existing 50 orb styles and 15 themes are untouched; new items are appended after the old ones, so saved indices/history stay valid.
- `Prefs.applyTheme()` already does `coerceIn(0, lastIndex)`, and orb rendering already does `coerceIn(0, ...)`, so bounds are safe.
- Reused only existing primitives (`ball`, `soft`, `warm`, Compose brushes/Stroke/rotate) and existing theme colours — no new imports, no layout changes.

## How to test
Appearance should show **VOICE ORB STYLE · 100** and **COLOR STYLE · 50 THEMES**; tap through new orbs and themes.

## Untested
Not compiled locally; verification is the GitHub Actions `Build APK` job + a phone pass.
