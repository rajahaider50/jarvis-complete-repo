# PLANNING (active task only)
Task: give each of the 45 premium interface styles its own layout.

## Approach
- Add a new premium engine (Palettes.engine == 5) in ui/styles/IfaceScreens.kt.
- Each style gets a recipe IfaceCfg(layout, nav, corner) from IfaceCatalog.cfgFor(style) so combos are unique.
  - 4 Home archetypes, 3 Control archetypes, 3 Settings archetypes, 5 nav bars, 4 corner radii.
- IfaceDef.engine is now always 5 (default param); applyTheme already copies it to Palettes.engine.
- Route engine 5 in StyleKit (StyledHome/Control/Settings) and Components (BottomNav/SubHeader).
- Reuse only Compose APIs already present in the project; new file is purely additive.

## Risk / check
- Old styles unchanged; premium code only runs for engine 5.
- Verify with GitHub Actions "Build APK".
