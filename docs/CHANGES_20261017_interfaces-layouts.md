# CHANGES 2026-10-17 · Premium interface styles now have different LAYOUTS (not just colours)

## Feedback fixed
Earlier the 45 new interface styles only swapped the palette; the layout was shared. Now every premium style
has its own layout recipe (archetype + navigation bar + corner radius), so no two look the same.

## What changed
- **`ui/styles/IfaceCatalog.kt`**: `IfaceDef` dropped the shared-engine idea — every premium style runs on the new
  engine (5). Added `IfaceCfg(layout, nav, corner)` + `cfgFor(style)` which derives a unique recipe per style
  (native uses flat/segmented/icon/tab bars + sharp→medium corners; iOS uses pill/segmented bars + round→pill corners).
- **`ui/styles/IfaceScreens.kt`** (new): the premium engine.
  - 4 Home archetypes: Spotlight (big centred orb), Command (search + hero card + action pills + ring stats),
    Dashboard (2x2 stat tiles + hero + action circles), List (compact header, action rows, stat chips).
  - 3 Control archetypes: rounded Material tiles, square icon grid, list rows with state pills.
  - 3 Settings archetypes: search + grouped cards, profile header + grouped cards, two-column tile grid.
  - 5 navigation bars: label bar, floating glass pill, segmented track, icon bar with indicator, text tabs.
  - 2 headers (large-left or centred) picked by corner style.
- **`ui/theme/Color.kt` / `data/Prefs.kt`**: unchanged behaviour; `Palettes.engine` is 5 for the premium styles.
- **`ui/styles/StyleKit.kt`** + **`ui/components/Components.kt`**: engine `5` routes Home/Control/Settings and the
  bottom bar + sub-header to the new engine.
- **`ui/screens/UiStyleScreen.kt`**: the mini preview now reflects each style's nav variant and corner radius.

## Safety
- Old 5 styles untouched (engine 0..4 same as before).
- New engine only used when `Palettes.engine == 5`; all new code is additive.
- Only existing Compose APIs/imports already used in the project.

## How to test
Profile > Appearance > Interface style: pick any of the 45 premium styles — nav bar, home layout, control and
settings should each look different, and switching styles rebuilds the whole app in that look.

## Untested
Not compiled locally; verification is the GitHub Actions `Build APK` job + a phone pass.
