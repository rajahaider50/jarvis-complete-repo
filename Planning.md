# PLANNING (active task only)
Task: 16 -> 50 voice-orb styles (34 new professional/premium).

## Approach
- Keep existing 16 styles and their look untouched (indexes 0..15).
- Append 34 new styles at indexes 16..49 in `ui/components/SiriOrb.kt`.
- Reuse the same drawing primitives already in the file: `ball()`, `soft()`, `glow()`, `warm()`,
  `Brush.radial/linear/sweep/verticalGradient`, `Stroke`, `rotate`, `drawPath/drawCircle/drawLine/drawArc/drawOval/drawRect`.
- Only use existing theme colours (`Gold`, `GoldLight`, `GoldDark`, `warm()`, `Color.White`, `Color.Transparent`)
  and the existing dark ball backgrounds already used by `glass`. No new hard-coded brand colours.
- `ThemeScreen` reads `ORB_STYLES.size` and `chunked(4)`, so it auto-shows 50; no UI change needed there.
- `Prefs.orbStyle` is a plain Int; no change needed.

## Edits
1. `ORB_STYLES`: add 34 names (total 50).
2. Doc comment: style range 0..15 -> 0..49.
3. `when (style.coerceIn(0, 15))` -> `coerceIn(0, 49)` and add cases 16..49.
4. Append 34 `DrawScope` drawing functions + small `polygon()` helper.

## New style names (16..49)
Solar Crown, Lotus Bloom, Hex Weave, Golden Torus, Prism Light, Plasma Veil, Comet Trail, Binary Rain,
Rose Window, Spirograph, Nebula, Pendulum, Barcode Ring, Wavelength, Kaleidoscope, Sonic Rings,
Crystal Core, Aurora Ribbon, Quantum Dots, Infinity Loop, Gear Ring, Facet Diamond, Particle Storm, Liquid Metal,
Pulse Grid, Star Lattice, Halo Orbits, Vortex, Ripple Mesh, Aurora Curtains, Obsidian, Golden Spiral,
Prism Sweep, Supernova.

## Risk / check
- Compile: only stdlib + already-imported Compose APIs; no new imports needed.
- `when` uses `else` for index 49 so it stays exhaustive over Int.
- Verify via GitHub Actions "Build APK".
