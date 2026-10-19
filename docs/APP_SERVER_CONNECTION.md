# How JARVIS (the app) connects to the brain server — step-by-step modification guide

Rule for every step below: **the app must keep working with no server at all.** The server is an
add-on, never a requirement. Each step ends with a green `Build APK` and a green `Server CI`, and moves
its line from `Pending.md` to `Complete.md`.

## The one design rule

```
        ┌────────────────────────── app decides ──────────────────────────┐
 chat / live call ─► local memory (Always) ─► + server memory (if reachable) ─► merged context
        └─────────────────────────────────────────────────────────────────┘
                         server down? use local only, and say so in Diagnostics
```

---

## S2 — Talk to the server (this is the first app change)

**Files**

| File | Change |
| --- | --- |
| `data/Prefs.kt` | add `serverUrl`, `serverToken`, `serverEnabled` (default off), `serverSyncMemories`; load/save like the existing keys |
| `net/JarvisServer.kt` *(new)* | real HTTP over `HttpURLConnection` (same style as `data/CustomProviders.kt`): `health()`, `state()`, `addMemory()`, `recall()`, `skills()`, `learn()`, `jobs()`, `affectEvent()`, `sync()`; every call returns a small result object with `ok`, `status`, `error`, `body`; short timeouts; never blocks the UI thread |
| `ui/screens/ServerScreen.kt` *(new)* | "Brain server" page: URL + token fields, **Test connection** (calls `/health` and shows the real version/counts/mood), toggles, last sync time, and a clear "not connected → app runs on local memory" line |
| `MainActivity.kt` | add `composable("server") { ServerScreen(back) }` |
| `ui/screens/ProfileScreen.kt` | add one `PrefRowV(Icons.Outlined.Dns, "Brain server") { onNav("server") }` row |
| `ui/screens/DiagnosticsScreen.kt` | a "Brain server" card: reachable?, version, memories/skills, last error (real text) |

**Acceptance for S2:** with the server stopped the page shows "unreachable" with the real exception text
and the rest of the app behaves exactly as before; with Termux running it shows the real version and
counts. No fake "connected" state anywhere.

## S3 — Memory sync

- On app start and after every call/chat turn (debounced), push new local memories
  (`data/MemoryStore` + `live/ClickMemory`) to `POST /api/sync`, and store what the server returns in a
  new local table/file `memory/server_memories.json`.
- `ChatStore.systemPrompt()` and `LiveClient`'s system instruction get a short "what I remember" block
  built from **local + server** recall (`POST /api/recall`), capped to a few hundred characters.
- Diagnostics shows `server memories: n · last sync: hh:mm:ss · pushed: n · pulled: n` (real counters).

## S4 — "Learn this while I am busy"

- New tool + UI action: long-press a chat message, or say *"yeh seekh lo, main busy hoon"* → the app
  posts `POST /api/learn` with the text/task; the server's learner thread distils it.
- A "Learning jobs" list (real rows from `GET /api/jobs`, with `status/result/error`).
- When a job finishes, the app shows a notification (reuse the existing foreground-service notification
  channel) and the new memories appear in the Memory screen.

## S5 — Skills

- `code/Skills.kt` gains a server source: `GET /api/skills` → merged into the existing skill list;
  editing a skill pushes `PUT /api/skills/<name>`.
- When the AI actually uses a skill during a task, increment `uses` (`PUT` with the new count) — a real
  usage counter, not a decoration.

## S6 — Affect (honest emotions)

- The app posts **real** events: `call_ok`/`call_fail` (from `CallHost.end`), `tool_ok`/`tool_fail`
  (`Diag.gestureOutcome`/tool results), `praise`/`anger` (only when the user's words clearly say so —
  the model decides from the transcript, never guessed), `idle` (from the wake service's idle timer).
- `GET /api/affect` drives a small state readout on the call screen and a subtle orb-tint change:
  the numbers shown are the server's real numbers (`valence/arousal/energy/trust/focus` + label).

## S7 — Server-Driven UI

- `ui/screens/ServerPagesScreen.kt` renders `/api/pages` with a renderer that maps **only** the
  whitelisted components (`title, text, status, list, button, toggle, number, memory, affect`) to
  existing app components (`JText`, `GoldButton`, `OutlineButton`, `Card`).
- Buttons dispatch only whitelisted actions (`recall, learn, sync, open_page, none`) — the server can
  never open a file, place a call or touch accessibility. This is enforced in code and tested.
- So the owner can add a small screen by editing `server/jarvis_server/sdui.py` and pushing — no APK.

## S8 — Step-by-step Profile (the UI refactor the owner asked for)

Current Profile is one long list. Target: a small hub with one page per system, each page kept short:

```
Profile
├── Assistant      (name, voice, style, wake word)
├── Brain server   (connection, sync, learning jobs)      ← S2/S4
├── Memory         (local + server memory, forget, export)
├── Skills         (list, edit, push/pull)
├── Cursor & control (AI cursor, cursor lab, cursor memory)
├── Call & audio   (volume, auto earpiece, echo guard, recording)
├── Screen         (share quality, recording, debug frame)
├── Privacy        (blocklists, sensitive screens, what is never stored)
├── Appearance     (themes, orbs, interfaces)
└── Diagnostics    (counters, dumps) · Console · Access
```

Method (so nothing breaks):
1. Move whole existing rows into new `ScreenScaffold` pages, one group per commit, each with its own
   `composable("...")` route — the rows themselves are unchanged, only their home changes.
2. Keep every old route working (`themes`, `voices`, `cursorlab`, …) so deep links still work.
3. After each group: `Build APK` + a quick pass in all 5 interface styles (the hub uses the same
   components, so styles keep working).

---

## Where the app code will live

```
net/JarvisServer.kt            one HTTP client, one place, no scattered urls
data/Prefs.kt                  url / token / enabled / sync flags
ui/screens/ServerScreen.kt     connection + sync + jobs
ui/screens/ServerPagesScreen.kt SDUI renderer (S7)
live/Diag.kt                   server counters + last error (evidence)
```

## Testing the connection by hand (before any UI exists)

```sh
# on a PC on the same Wi-Fi as the phone running Termux
curl -s http://<phone-ip>:8787/health | python -m json.tool
```
If that returns JSON, the app's `Test connection` will succeed as long as the URL and token match.
