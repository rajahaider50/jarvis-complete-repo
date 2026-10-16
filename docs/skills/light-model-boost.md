# SKILL: light-model-boost (for fast/cheap models: DeepSeek, Haiku, Flash ...)
You code fast; design is where you slip. So:
1. Do not invent UI. Copy the nearest existing screen's structure and spacing, swap the content.
2. Never write a raw colour, font size or shape that is not already in `Components.kt` / `Color.kt` / DESIGN_SPEC.
3. Work in two passes: (A) logic/data/wiring, (B) UI using only shared components. Do not mix.
4. After writing, re-read your diff against `docs/skills/ui-design.md` and fix violations.
5. If unsure about an API or a symbol, ask for the file; do not guess.
