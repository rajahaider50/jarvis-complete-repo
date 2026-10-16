# SKILL: ui-design (read before writing or changing any UI)
Goal: new UI must look like it was always part of the app.
1. Find the closest existing screen (list: `ui/screens/*`). Open ONLY that one + `ui/components/Components.kt`. Copy its structure: `ScreenScaffold`/`SubHeader` -> scrolling Column -> cards/rows.
2. Use only shared components and palette colours (`Gold`, `Ivory`, `Muted`, `Surface1`, `Surface2`, `Bg`). No hard-coded colours, no new fonts, no raw Material `Button`/`TextField`/`Switch`.
3. Sizes: cards 18-24dp radius, buttons 30dp, chips 18-20dp, bubbles 18dp (tail corner 4dp). Spacing in multiples of 4dp, same side padding as the copied screen.
4. Text: `JText` only (check its parameters first). Headings serif/Playfair look, body Inter look, as `JText` already provides.
5. Icons: existing `JIcon`/`GoldIconBox` + `res/drawable/ic_*.xml`. New icon = new vector xml in `res/drawable` (24dp grid, 1.7 stroke, round caps).
6. Tab screens and settings lists must work in all 5 styles: Classic Gold, Aurora, Nova, Pulse, Folio (`ui/styles/StyleKit.kt`: `StyledHome/Control/Settings`). A plain sub-screen using shared components is enough; a new settings row must be added to ProfileScreen + the four `*Screens.kt` settings lists.
7. States: always handle empty, loading and error states with short sentences (same tone as existing ones).
8. Do NOT touch any existing screen's design while adding something new. Design polish is a separate task with its own request.
Self-check: no `Color(0x...)` in new code, no unused imports, previews not required.
