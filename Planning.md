# PLANNING (active task only)
Task: 50 -> 100 orbs (new iOS/premium) + 15 -> 50 themes.

## Approach
- `Color.kt`: append 35 `Palette(...)` entries to `Palettes.all` (dark premium palettes). First entry (index 0) unchanged. No API/UI change.
- `SiriOrb.kt`:
  - `ORB_STYLES`: add 50 names (indexes 50..99).
  - `when (style.coerceIn(0, 49))` -> `coerceIn(0, 99)`, add cases 50..99 (`else` keeps 99).
  - Append 50 `DrawScope` draw functions using only existing primitives/brushes/theme colours.
- ThemeScreen already dynamic -> no edits.

## Safety
- Append-only, so existing indices remain valid; `Prefs.applyTheme` and orb render both clamp indices.
- Colours drawn via `Gold/GoldLight/GoldDark/warm()/Color.White` (existing helpers) to match each theme automatically.
- No new imports.

## Risk / check
- Watch array sizes in drawing loops (fixed `plasmaOrb` colour list length).
- Verify via GitHub Actions "Build APK".
