# ROOT CAUSE REPORT — Masla 2: AI cursor screen par nazar nahi aata
Date: 2026-10-18 · Evidence layer: Phase 1 (`Diag`, tag `JARVIS_DIAG`).

## Khulasa (asan zuban)
AI ka cursor is liye nahi aata tha ke **AI ke tool call ek worker thread par chalte hain** (pehle OkHttp ke reader
thread par, ab `live-tools` thread par), aur `CursorController.add()` seedha `WindowManager.addView(...)` aur Compose
ki state **usi worker thread** se badal raha tha. Android me `addView`/`updateViewLayout` **sirf main thread** par
chalte hain; warna `CalledFromWrongThreadException` aata hai. Wo exception `CursorController` ke `try/catch` me
**chup chap nigal** liya jata tha aur `-1` return kar deta tha, is liye screen par kuch nazar nahi aata tha.

Isi liye **Cursor Lab / panel ke buttons se cursor aata tha** (wo main thread par chalte hain) lekin **AI se nahi**
(worker thread). Ye bilkul wahi isolation hai jo aap ke guide ka "Cursor Test" check karta hai.

`moveAnimated` me ek doosra bug bhi tha: `Choreographer.getInstance()` worker thread par **Looper** na hone ki wajah
se throw karta hai ("The current thread must have a looper!") — yaani `cursor_move` bhi fail hota.

## Evidence (code se)
| # | Fact | Where |
|---|---|---|
| C-1 | AI tool call handle hota hai worker thread par (`onMessage` → `onFrame` → `handle` → `onTool`) | `LiveClient.kt` |
| C-2 | `CursorController.add` → `wm!!.addView(v, lp)` **binā main thread**, `catch { AppLog.w(e); return -1 }` (error chhupa) | `CursorController.kt` (old `add`) |
| C-3 | `place()` → `wm?.updateViewLayout` same problem | `CursorController.kt` (old `place`) |
| C-4 | `moveAnimated` → `Choreographer.getInstance()` worker thread par chalta hai → Looper nahi → throw | `CursorController.kt` (old `moveAnimated`) |
| C-5 | `CursorPanel.refresh()` TextView `.text` worker thread se set karta hai | `CursorPanel.kt` |
| C-6 | Tool names declared (`PhoneTools`) aur dispatched (`AppControl`) match karte hain; unknown name "Unknown command" deta tha lekin **log nahi** karta tha | `PhoneTools.kt:61-73`, `AppControl.kt` |
| C-7 | `onServiceConnected`/`onUnbind` kuch log nahi karte the (MIUI service ko maar deta hai — pata nahi chalta) | `JarvisAccessibilityService.kt` |

## Fixes
- **F2-1 (RC) main-thread marshalling.** `CursorController` me `onMain` (blocking, 900 ms timeout — `add` ka id chahiye)
  aur `postMain` (fire-and-forget). `add / move / moveAnimated / remove / clear / showGrid / setTouchable / panic`
  sab wrap ho gaye: agar main thread par hain to seedha chalte hain, warna main par chaltay hain. `CursorPanel.refresh()`
  bhi marshalled.
- **F2-2 (chain) unknown tool log + error.** `AppControl.execute` unknown name par `Diag.log("tool","UNKNOWN ...")`
  karta hai aur saaf error deta hai.
- **F2-3 (chain) saaf errors AI tak.** AI cursor off / accessibility off par AI ko actionable message jata hai
  ("enable Jarvis in Android Settings > Accessibility"); `cursor_create` fail par kaunsi wajah thi wo batata hai.
- **F2-4 (chain) service zinda / bahal.** `onServiceConnected`/`onUnbind` log hote hain; unbind par cursors ka
  snapshot ban jata hai aur service dobara connect hone par cursors **bahal** ho jate hain.
- Cursor Test button Phase 1 me Diagnostics par maujood hai (centre + 4 corners) — isolation ke liye.

## Confirm on the phone
Command:
```
adb logcat -c
adb logcat -v time JARVIS_DIAG:V AndroidRuntime:E *:S
```
Chain dekhne ke liye: `setup built: ... cursorTools=true` → `tool: -> cursor_create` → `cursor: add(...) thread=main`
→ `cursor: added id=..` → `tool: <- cursor_create = cursor id=..`. Agar thread pehle koi aur tha ya
`FAIL cursor.addView` aata tha, wahi root cause confirm karta hai.
