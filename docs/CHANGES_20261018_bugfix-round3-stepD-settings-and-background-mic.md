# Round 3 — Step D: Android Settings ko allow karna + §4 background mic (mic zero)

Date: 2026-10-18
Commit: `7f54f6b1f989` (build 39145691181)

---

## D1 — Android Settings "activate" (share + click memory)

### Kya masla tha
Do jagah `com.android.settings` **blocklist** me tha, is liye:

1. `live/ScreenShareService.kt` → Settings khulte hi screen frames **bhejna band** aur share "paused" ho jata
   (`sensitive = true` → `continue`, kuch bhi send nahi).
2. `live/ClickMemory.kt` → Settings me AI kuch bhi **seekhta/yaad nahi** karta tha (lessons band).

Accessibility config me koi `packageNames` filter nahi hai aur `canTakeScreenshot="true"` hai, is liye service Settings ka
poora tree dekh sakti hai — sirf ye do blocklist entries rok rahi thi.

### Kya kiya
- `ScreenShareService.blocklist` se `com.android.settings` **hata diya**. Ab Settings ke frames jaate hain, aur
  `open_settings` → `get_clickable_elements` → `click_element` poora kaam karta hai.
- `ClickMemory.blockedPackages` se `com.android.settings` + `com.google.android.settings` **hata diye**.
- **Privacy waise hi mehfooz hai:**
  - `JarvisAccessibilityService.isSensitiveScreen()` — screen par koi **password field** focused ho to share turant
    **pause** ho jata hai (`paused = sensitive`), Settings ke andar bhi.
  - `ScreenShareService.blocklist` me bank / paytm / phonepe / gpay / gms.auth / keepass / 1password / bitwarden /
    lastpass **jaise the waise** hain.
  - `ClickMemory.blockedWords` me `bank, wallet, payment, upi, password, passcode, pin, otp, locker, card` … sab
    **jaise the waise** hain, aur `com.android.vending`, `walletnfcrel`, `paypal`, `chrome.incognito` bhi blocked hain.
- System prompt me saaf line add ki: *"Android Settings is fully allowed: open_settings (wifi, bluetooth, display,
  sound, battery, apps, location, accessibility, about, …) then get_clickable_elements + click_element."*

**Kabhi-kabhi wale screen (device admin / screen-lock type) protected rehte hain** kyunki unme password field hota hai.

---

## D2 — §4 background mic: `press_button home` ke ~3 s baad mic ZERO

### Root cause (code se proven, 4 cheezein)
1. **Wake listener call ke doran mic le leta tha.** `WakeService.setPaused(false)` par `pausedState` turant false ho
   jata tha, aur "should listen" condition (`voskTick` / `porcupineTick` / basic `listen`) me **call ka koi zikr nahi**
   tha — sirf `!pausedState && !foreignMic && !inPhoneCall()`, aur `inPhoneCall()` jaan-boojh kar `false` deta hai jab
   `CallHost.active` ho. Matlab: ek baar unpause hote hi Vosk/Porcupine doosra **AudioRecord** khol leta tha aur
   `MicCaptureHub` **exact zeros** padhne lagta tha.
2. **`AssistantActivity.onDestroy()`** unconditionally `WakeService.setPaused(false)` karta tha — assistant overlay band
   hote hi, live call chalne ke bawajood, wake engine wapas mic par aa jata tha.
3. **`WakeService.trigger()` ka 3.5 s fallback**: `if (!AssistantActivity.alive) { pausedState = false; ... }` —
   call ki parwah kiye bina apne aap ko unpause kar leta tha.
4. **Background mute ka asli Android rule**: background me mic sirf tab chalta hai jab **"microphone" type ka foreground
   service chal raha ho**. Agar `CallService` kisi wajah se na chal raha ho (start fail / os ne maara), to Android mic
   ko chup kara deta hai — code me iska **koi check hi nahi tha**.

### Fix (WakeService = "mic ka doosra owner nahi banega")
- `setPaused(p)` ab: `busy = CallHost.active || MicCaptureHub.isRunning` → `pausedState = p || busy`, aur
  `forcedPause = !p && busy`.
  - Call ke doran "resume" aaye to **resume ignore** ho jata hai (log: `resume ignored -> a live call owns the mic`).
  - Overlay assistant band hone par bhi mic tab tak free nahi hota jab tak `MicCaptureHub` chalu hai.
- **`releaseForcedPause()`** — har wake tick (1 s) par: jab `CallHost.active` aur `MicCaptureHub.isRunning` dono false
  ho jayein to pause khud-ba-khud khul jata hai (log: `the mic is free again -> wake word resumes by itself`). Is liye
  wake word **stuck off nahi hota**.
- `shouldRun` (teeno engines) me `!CallHost.active` add — belt-and-braces.
- `trigger()` me `|| CallHost.active` guard, aur fallback ab:
  `if (!AssistantActivity.alive) { if (CallHost.active) "staying paused" else resume }`.
- `AssistantActivity.onDestroy()` → `WakeService.setPaused(false)` (guard ab `setPaused` ke andar hai), comment ke saath.
- `WakeService.micOpen` naya **evidence flag**: sach me mic khula hai ya nahi (`vosk startListening` / `porcupine start` /
  `SpeechRecognizer created` par true, stop/destroy par false). Dump me chhapta hai.

### Fix (MicCaptureHub = cause batao, aur khud theek karo)
- `silenceCause()` naya: zeros ki wajah **naam se** batata hai:
  - `the call foreground service is NOT running (Android mutes the mic in the background)`
  - `the microphone app-op is X, not MODE_ALLOWED(0)` — `AppOpsManager` se
    (`unsafeCheckOpNoThrow` API 29+, warna deprecated `checkOpNoThrow`)
  - `Jarvis's own wake listener has the mic open`
  - `another app is silencing Jarvis's mic` / `another app is recording (N active captures)`
  - warna: `no obvious cause (source=…, aec=…)`
- Wajah `Diag.micZeroCause` me jaati hai aur dump me `mic zeroCause=…` chhapti hai.
- `recover()` ab:
  - **attempt 1**: agar `CallService.running == false` → `CallService.start()` kar ke **SAME source** par retry
    (mic ka background mute theek hota hai, echo canceller bachta hai).
  - **attempt 1**: agar `WakeService.micOpen` → `WakeService.setPaused(true)` kar ke **SAME source** par retry.
  - `keepSource = attempt <= 2 || backgroundIssue` → background/wake ki wajah se **AEC kabhi nahi jaata**.
  - `MAX_ATTEMPTS` ke baad bhi **har ~15 s** par ek koshish (slow watch), taaki theek hui halat pakdi jaye.
- `foreignRecorder()` ab batata hai jab extra capture **Jarvis ka hi wake listener** ho.

### Dump me naye lines (proof)
```
mic zeroCause=the call foreground service is NOT running (Android mutes the mic in the background)
wake micOpen=false paused=true callFgs=true callActive=true
mic: ZERO buffers -> cause: …
mic: zeros with NO call foreground service -> starting it and retrying the SAME source
mic: silent: recovery attempt 1/6 source=VOICE_COMMUNICATION sameSource=true
wake: resume ignored -> a live call owns the mic
wake: the mic is free again -> wake word resumes by itself
```

### Diagnostics screen
Microphone card me 3 nayi rows: **why silent (cause)**, **call foreground service (running/NOT running)**,
**wake listener (`micOpen=… paused=…`)**.

---

## Phone test (naya)
1. Call ke doran `press_button home` (ya khud Home dabayein) — 30–60 s JARVIS se baat karein; mic **zero na** ho.
   Dump me `zeroRuns` na badhe aur `callFgs=running` ho.
2. Wake word ON rakhein, call ke doran "Hey Jarvis" (ya jo phrase) bolein — wake engine mic **na** le
   (`wake micOpen=false` rehna chahiye, `interrupted` nahi badhna chahiye).
3. Call khatam karne ke baad wake word phir se kaam kare (`wake: the mic is free again -> wake word resumes by itself`).
4. "settings kholo, phir wahan se Wi-Fi dikhao" — frames pause na hon, `click_element` se Wi-Fi khule.
