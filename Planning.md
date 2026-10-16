# PLANNING (active task only)
Task: interface styles 5 -> 50 (25 native + 25 iOS premium).

## Approach
- Decouple routing from `Palettes.style` by adding `Palettes.engine` (layout engine) + `Palettes.serif`.
  For styles 0..4 engine == style; for 5..49 engine comes from the new catalog.
- New `IfaceCatalog` (styles package): 45 `IfaceDef(name, about, palette, engine, serif)`.
  Native set uses 20 dark Material palettes (from `Palettes.all[15..34]`); iOS set uses 25 dedicated light/dark palettes.
- Route BottomNav/SubHeader/ScreenScaffold/StyledHome/Control/Settings and JText serif through `engine`/`serif`.
- `Prefs.applyTheme()`: for `uiStyle >= 5` use the catalog palette/engine/serif; else keep old logic.
- `UiStyleScreen`: list 50, two group headers, mini preview (custom for 0..4, palette-based for 5..49).

## Safety
- Append-only indexes; old behaviour identical because engine == style for 0..4.
- New styles reuse the existing 4 layout engines, so no new draw/render code can crash.
- Guard against `forStyle` returning null -> fall back to old path.

## Risk / check
- `Palettes.engine` must be set before first composition (done in `Prefs.init` -> `applyTheme`).
- Verify with GitHub Actions "Build APK".
