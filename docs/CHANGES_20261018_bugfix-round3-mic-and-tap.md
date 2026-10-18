# Round 3 bugfix — Step A (mic / stalling) and Step B+C (tap / elements / small)

Date: 2026-10-18
Commits: Step A `cfbec539f527` (build 39133424319 GREEN), Step B+C `1ebbf9d537ad`.

Round-2 Step 1 (truthful `cursor_click` + Tap Test) was already delivered. Round 3 works through the
user's list in the requested order: **A** mic first, then **B** tap, then **C** small items.

---

## Step A — microphone / "baar baar ruk kar shuru"

Root causes (all code-visible, from the user's read-out):

| # | Problem | Fix |
|---|---|---|
| A1 (M1) | `MicCaptureHub.recover()` moved to the NEXT audio source on the first try (`VOICE_COMMUNICATION → MIC`), so the echo canceller was thrown away — and the dump showed interrupts explode right after that switch (1 before, 17 after). | Recovery attempts **1–2 re-create the AudioRecord on the SAME source** (AEC kept). Only from attempt 3 does it try the next source, and then it raises `aecLost`, logs it, and tells the UI. |
| A2 (M2) | `AcousticEchoCanceler` / `NoiseSuppressor` objects were created and dropped — an unreferenced `AudioEffect` can be released by the GC, silently killing AEC. | Both are now held in fields (`aec`, `ns`), released in `closeRec()`, and `open()` logs `aecAvailable / aecCreated / aecEnabled / nsEnabled`. |
| A3 (M3/M4) | `AudioRouter.apply()` forced `STREAM_VOICE_CALL` to **MAX on every route change**; AUTO jumped speaker↔earpiece on every proximity event. | Volume is set **once per call** (`applyVolume()`, default 70%, Settings "Call volume"). Proximity needs **1.5 s at the ear** before switching, and "Auto earpiece" is off by default. Every route change is logged (`route: call audio -> …`). |
| A4 (M5) | The mic streamed to the server even while the assistant was speaking, and `START_OF_ACTIVITY_INTERRUPTS` turned that echo into a cut-off answer. | New **half-duplex echo guard** (`Prefs.halfDuplex` = auto/on/off). While the assistant's PCM is going out (and 300 ms after), mic buffers are held back — **unless** the user's level is clearly loud (> 0.35), so barge-in still works. |
| A5 (M6) | On `goAway` the client closed the old socket first, waited, then reconnected → 2–3 s of silence. | **Make-before-break**: the old socket stays live (its frames keep playing) while the new connection with the resumption handle is set up; on the new `setupComplete` the socket is swapped and only then the old one is closed. Real drops keep the old path. |
| A6 (M7) | Playback pre-buffered only once at call start; the AudioTrack buffer was 0.5 s. | Pre-buffer at the start of **every answer** (~150 ms, max 250 ms) and a **~1 s** AudioTrack buffer; underrun and pre-buffer are logged. |
| A7 | No detail on *why* an answer was interrupted. | Every `interrupted` now logs mic level, how long ago the assistant last spoke, route, `aec`, half-duplex state, echo-guard state and the websocket queue size; the same string is in the dump (`last_interrupt=`). |
| A8 (M8) | No evidence that the call foreground service actually started with the `microphone` type. | `CallService` logs `fgs: started type=microphone…`; a zero-mic recovery now retries the same source first (A1). |
| A9 (M9) | `gemini-robotics-er-2-streaming-preview` answered `code=1007 … response modalities (AUDIO) is not supported` and kept being tried. | Such models are remembered (`Prefs.noAudioModels`), **filtered out of `ModelCatalog.liveCandidates()`**, and shown as "No audio support (code 1007)" in Profile > Gemini API. |
| A10 | No guided A/B test. | A short note under the call settings: same call with headphones vs speaker, then "Let me interrupt" off/on. |

New settings (Profile > live call block): **Call volume** (tap to cycle 40…100%), **Auto earpiece**, **Echo guard (half-duplex)**, **Screen-share detail**.

### Step A evidence to look for
`JARVIS_DIAG`:
```
mic … aecAvail=true aecOn=true aecLost=0
mic: MicCaptureHub START source=VOICE_COMMUNICATION …
mic: silent: recovery attempt 1/6 source=VOICE_COMMUNICATION sameSource=true
route=SPEAKER (selected=AUTO wired=false bt=false near=false vol=70%)
fgs: started type=microphone at_foreground=true sdk=…
Live: interrupted -> flush playback (mic=… assistant_spoke_until=…ms_ago route=SPEAKER aec=true halfDuplex=false echoGuard=false wsQueue=…B)
ws: reconnect #1 (server goAway 5s) — make-before-break: keeping the old socket until the new one is ready
ws: seamless swap done: the old socket is closed, the call never stopped
```

---

## Step B — tap / cursor ("screen par tap nahi hota")

| # | Problem | Fix |
|---|---|---|
| B1 (T1) | The cursor window was made `NOT_TOUCHABLE` and the gesture was dispatched in the same breath — `updateViewLayout` may not have applied yet, so the cursor could swallow the tap it was aiming at. `touchableAtClick` reported the flag from *before* the toggle, so it always looked fine. | Cursor windows are **always `FLAG_NOT_TOUCHABLE`** while the AI drives (set at `add()`), so there is no race at all. Hand-dragging needs the panel's new **"Move"** switch. `touchableAtClick` is now read **after** the flag is forced and immediately before dispatch, and a `WARNING` line is logged if it is ever true. |
| B3 (T2) | `tap_at` / `double_tap` / `swipe_screen` used `resources.displayMetrics` (can differ from the real display), and only reported "tapped" when `dispatchGesture` was *accepted*. | They use `CursorGeometry.screenSize()` (same as the cursors) and the real `GestureOutcome`: `cancelled` is reported as "Android cancelled the tap (nothing was tapped)". |
| B4 (T3) | Share frames were sent at 720 px and looked tiny to the model (322x720 or less). | Default **1280 px long side, q70** (800 px / q55 when hot); "Fast" keeps 720 px / q60. Profile toggle **Screen-share detail**. |
| B5 (T4) | `find_element` returned only the first match; the element tools did not exist; guessing coordinates was the only option. | New `get_clickable_elements` (label, view id, class, cursor centre, bounds, clickable/enabled), `click_element(id_or_label)` (accessibility click, then a real tap at the centre if nothing changed — with the truth either way), `cursor_move_to_element(id_or_label)`. `find_element` now returns up to 5 matches. |
| B6 | `click_on_screen` said "tapped" whenever `ACTION_CLICK` was accepted. | It now verifies with accessibility events for ~800 ms and says "the screen did not change — nothing happened" when nothing moved. |
| B7 (T7) | No way to see whether the cursor/grid is inside the frames the model gets. | Diagnostics > **"Save one share frame to a file"** writes a PNG to the app's files dir and logs the path plus the cursor positions. |
| B8 (T6) | Density was a guess. | The dump now prints the service's `density`/`densityDpi`, the real screen size, the cursor size and move mode. |
| B9 (T5) | No guidance for OEMs that kill the accessibility service. | Access & permissions has a "Keep JARVIS alive" card (battery unrestricted, app-info battery, autostart/auto-launch, lock in recents) with buttons. |

Also: `PhoneTools` now marks only the **necessary** parameters `required` (previously every parameter was required,
which forced the model to invent `target`/`type`/`page`…), and `click_on_screen` falls back to a real gesture at the
element's centre when nothing in the tree is clickable.

### Step B evidence to look for
```
gesture: click id=1 type=TAP outcome=COMPLETED px=(540,806) norm=+0.00,-0.00 touchableAtClick=false (wasTouchable=false moveMode=false) effect=changed window[gotCentre=(540,806) expectedCentre=(540,806) …] under[pkg=… class=… clickable=true …]
cursor: move mode OFF (cursors are NOT_TOUCHABLE: a tap always reaches the app)
share: frame #11 210KB 720x1612->max1280 q70 burst=false hot=false detailed=true
```
Tap Test: 5 rows, `diff ≤ 5 px`, no "cursor swallowed the tap".

---

## Step C — small items

| # | Problem | Fix |
|---|---|---|
| C1 | `CallHost.begin()` launched on `Dispatchers.Main.immediate`; nothing in the block suspends, so the whole "heavy setup" (overlay hide, locks, router, service) ran inline on the main thread before the first frame. | The block now runs on `Dispatchers.Default`; only `Overlay.hideAll()` hops back with `withContext(Dispatchers.Main)`. |
| C2 | README still said "Not included: Accessibility Service". | Corrected: the accessibility service is included and is what powers the cursor, element tools and screen sharing. |

---

## What still needs the phone (device-only evidence)
- Tap Test table values (`B2`) and the live `cursor_click` dump.
- `aecOn=true` / `source=VOICE_COMMUNICATION` after a recovery, and the `interrupted` lines with their new context.
- Whether the overlay appears in the saved debug frame (`B7`).
- `zeroRuns` / recovery behaviour in the background (`§4` of the round-2 list, not yet implemented).

### Logcat commands
```
adb logcat -c
adb logcat -v time JARVIS_DIAG:V *:S
```
