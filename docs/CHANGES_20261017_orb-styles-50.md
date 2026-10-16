# CHANGES 2026-10-17 · Voice orb styles 16 -> 50 (34 new premium styles)

## What changed
- **`ui/components/SiriOrb.kt`**
  - `ORB_STYLES` grew from 16 to **50** names.
  - `when (style.coerceIn(0, 15))` -> `when (style.coerceIn(0, 49))` with 34 new branches (indexes 16..49).
  - Appended 34 new `DrawScope` drawing functions plus a small `polygon()` helper.
- **`ui/screens/ThemeScreen.kt`**: no change needed — it renders `ORB_STYLES.size` and `chunked(4)`, so the grid auto-shows 50.

## New styles (16..49)
Solar Crown, Lotus Bloom, Hex Weave, Golden Torus, Prism Light, Plasma Veil, Comet Trail, Binary Rain,
Rose Window, Spirograph, Nebula, Pendulum, Barcode Ring, Wavelength, Kaleidoscope, Sonic Rings,
Crystal Core, Aurora Ribbon, Quantum Dots, Infinity Loop, Gear Ring, Facet Diamond, Particle Storm, Liquid Metal,
Pulse Grid, Star Lattice, Halo Orbits, Vortex, Ripple Mesh, Aurora Curtains, Obsidian, Golden Spiral,
Prism Sweep, Supernova.

## Why
Request: Profile > Appearance > Voice orb style had 16 styles; owner wanted 34 more professional/premium styles to complete 50.

## Approach / safety
- Existing 16 styles and their look are untouched (same indexes 0..15).
- Reused only the file's own primitives (`ball`, `soft`, `warm`, gradients, `Stroke`, `rotate`) and existing theme colours (`Gold`, `GoldLight`, `GoldDark`, `warm()`, `Color.White`) — no new brand colours, no new imports.
- `Prefs.orbStyle` is a plain `Int`, so no prefs change was required.

## How to test
Profile > Appearance > Voice orb style should read **VOICE ORB STYLE · 50**; tap through all 50 and check each one draws and reacts to voice.

## Untested
Not compiled/run locally; verification is the GitHub Actions `Build APK` job + a phone pass.
