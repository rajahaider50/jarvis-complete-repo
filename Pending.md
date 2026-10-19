# PENDING (not done yet). Delete a line when finished and add it to Complete.md.
- [ ] Phone test: AI cursor — Profile > Cursor Lab (grid + 5 targets, real tap lands on the ring); add/drag cursors (up to 10, colours/numbers); panel nudge + Tap/Dbl/Long/All + PANIC; joystick speed 1-15; grid overlay.
- [ ] Phone test: AI cursor by voice — "screen par cursor dikhao", "yahan click karo", "do cursor banao aur dono click karo"; cursor moves smoothly and does not swallow its own tap.
- [ ] Phone test: cursor memory — lessons appear in Profile > Cursor memory with ok counts; a wrong tap fixed with panel "Save spot" replaces the lesson; banking/password screen saves nothing.
- [ ] Phone test: screen share + record together on one MediaProjection — start recording, then "share screen" with no second dialog; both keep running when the call is minimised.
- [ ] Phone test: call resumption — toggle airplane mode for a few seconds mid-call; the call returns with "Reconnecting…" and remembers the conversation; a fresh call starts clean.
- [ ] Phone test: live call opens instantly and connects in about a second using the saved Live Call Models (ApiScreen).
- [ ] Phone test: down-arrow minimises the call; mini bar shows on other screens; call keeps running; camera keeps working.
- [ ] Phone test: record a call (video + both voices) and confirm the file in Movies/Jarvis; mic-silent warning shows.
- [ ] Phone test: AI control — "orb/theme/interface/colors dikhao", "aur dikhao", "teesra"; camera/recording/share/navigate by voice; Profile AI-control toggle off blocks it.
- [ ] Phone test: Workspace coding with two providers — Auto should switch models/provider when a limit is hit; Manual uses only the chosen provider; with no provider it must refuse (no Gemini).
- [ ] Phone test: Memory screen shows live AGENT MEMORY entries while a coding task writes memory.
- [ ] Phone test: Profile > Theme Lab — all 11 visuals animate in every state, level slider reacts, long-press sets active, FX sounds + volume work; API<33 shows fallback orbs.
- [ ] Open generated PPTX once in PowerPoint/Google Slides to confirm it opens.
- [ ] Phone test of both call engines (Gemini Live and provider pipeline).
- [ ] Figma frames for updates 3-8 (design spec says "pending").
- [ ] Phone test: import a provider .json in Custom providers and confirm all models appear.
- [ ] Phone test: Appearance shows "VOICE ORB STYLE · 100" and "COLOR STYLE · 50 THEMES"; each new orb animates/reacts to voice and each theme applies.
- [ ] Phone test: Profile > Appearance > Interface style lists 50 (25 native + 25 iOS) and switching any new style changes the whole app.
- [ ] Bugfix Diag: open Profile > Diagnostics during a live call and confirm the counters move (mic level, ws=live, toolCalls, frames). Run `adb logcat -v time JARVIS_DIAG:V *:S` while reproducing each masla and share the file.
- [ ] Bugfix Masla 3 (call stalling) soak test: 30 min + 60 min call, network switch, barge-in, long answer; call must not drop; underruns/interrupted/reconnects visible in Diagnostics.
- [ ] Bugfix Masla 1 (mic+recording): Jarvis recorder on → user voice reaches AI and BOTH voices land in Movies/Jarvis; mic never goes silent on record on/off; red warning only on real silence; 10-min A/V sync.
- [ ] Bugfix Masla 2 (cursor): Diagnostics > Cursor Test shows centre + 4 corners; by voice "screen ke center me cursor lagao"; 3 cursors add/move/remove; service off → clean error (no crash).
- [ ] Bugfix Round 2 step 1 — Profiles > Diagnostics > **Tap Test**: 5 targets; each row must show `sent(px)`, `recv(px)` and `diff` in pixels. Report the values (diff ≤ 5 px = good). If a row says "cursor swallowed the tap", that proves Masla 2.2 must be applied.
- [ ] Bugfix Round 2 step 1 — live call: ask JARVIS to click something and confirm the `cursor_click` result contains `outcome=`, `under_cursor:` and `effect=`, and that `adb logcat -v time JARVIS_DIAG:V *:S` shows `gesture: click ... outcome=... touchableAtClick=... effect=...`. Share that dump before the next round (echo/interrupt, mic source, background mic).

## Round 3 (mic first, then tap) — code done, phone evidence pending
- [ ] Round 3 Step A (mic): start a call on the speaker, hold it 2+ minutes of long answers. Dump header must show `aecAvail=true aecOn=true aecLost=0`, `source` must stay `VOICE_COMMUNICATION` (never flip to `MIC`), and every `interrupted` line must carry `mic=… assistant_spoke_until=…ms_ago route=… aec=… halfDuplex=… wsQueue=…`. `zeroRuns` recovery must retry the SAME source first.
- [ ] Round 3 Step A (settings): tap "Call volume" (cycles 40→100%), toggle "Auto earpiece" (earpiece only after 1.5 s at the ear), tap "Echo guard" (auto/on/off), run the A/B test note (headphones vs speaker, interrupt off/on).
- [ ] Round 3 Step A (model list): confirm `gemini-robotics-er-2-streaming-preview` shows "No audio support (code 1007)" in Profile > Gemini API and is skipped by the call.
- [ ] Round 3 Step B (tap): Diagnostics > Tap Test — 5 rows, each with `sent`, `recv`, `diff`. Required: `diff ≤ 5 px` on all five and **no** "cursor swallowed the tap" row. Also check the log line `touchableAtClick=false` for every row.
- [ ] Round 3 Step B (truthful click): panel "Move" switch ON → you can drag a cursor; switch OFF → a tap on a cursor's ring reaches the app. `cursor_click` must report `outcome=completed`, a real `under_cursor:` and `effect=changed`; a failed click must say so.
- [ ] Round 3 Step B (elements): say "settings kholo, phir wahan se Wi-Fi dikhao" and confirm the model uses `get_clickable_elements` → `click_element` and only claims success when the screen changed.
- [ ] Round 3 Step B (frames): Diagnostics > "Save one share frame to a file" — open the PNG and report whether the cursor/grid overlay is visible in it (B7).
- [ ] Round 3 Step C: confirm the call screen opens with no freeze and the first frame is instant (`Timing: call_setup_services …ms` after `call_setup_begin`).

## Round 3 Step D — Settings unblock + background mic (§4)
- [ ] Round 3 Step D (Settings): "settings kholo, phir wahan se Wi-Fi dikhao" — Settings ke frames share hone chahiye (koi pause nahi) aur AI `get_clickable_elements` → `click_element` se Wi-Fi khole. Phir ek Settings ke andar password field par cursor le jaayein → share turant pause ho.
- [ ] Round 3 Step D (background mic §4): call ke doran Home dabayein (`press_button home` ya khud) aur 30–60 s baat karein — mic **zero na** ho; dump me `zeroRuns` na badhe, `callFgs=running`, `wake micOpen=false`.
- [ ] Round 3 Step D (wake vs call): wake word ON rakhein aur call ke doran wake phrase bolein — wake engine mic **na** le (`wake micOpen=false`), `interrupted` na badhe.
- [ ] Round 3 Step D (wake wapas): call khatam hone ke baad wake word phir kaam kare (`wake: the mic is free again -> wake word resumes by itself`).
- [ ] Round 3 Step D (cause line): agar mic phir bhi zero ho to dump se `mic zeroCause=…` bhejein — uska matlab poora naam se likha hoga (FGS / app-op / wake listener / doosri app).

## Recording (both voices + phone recorders) — code done, phone evidence pending
- [ ] Record a call with the in-app Record button (Gemini Live **aur** ek custom provider dono) — Movies/Jarvis ki file mein **dono awaazein** aani chahiye, aur aawaz video ke saath sync honi chahiye (pehle sirf picture thi).
- [ ] Profile > Diagnostics > **Call recording**: `YOUR voice fed in` aur `JARVIS's voice fed in` dono non-zero; agar koi 0B ho to screen/dump ka WARNING line bata dega kaun sa side missing hai.
- [ ] `adb logcat -v time JARVIS_DIAG:V *:S` mein `rec: video first pts=… -> normalised to 0`, `rec: audio first pts=…` aur har 2 s ka `rec: mic=…B out=…B mixed=…B muxStarted=…` dikhna chahiye.
- [ ] Android ka apna screen recorder: dialog mein **"Media sounds" ON** rakhein → system recording mein **JARVIS ki awaaz** aani chahiye. Agar recorder microphone bhi le le to JARVIS red "microphone is silent" warning dikhayega aur recorder band hone par mic khud wapas le lega.
- [ ] Profile > "Phone recorders can hear JARVIS" ON (media stream) par bhi awaaz suni jaani chahiye aur "Call volume" ka percent kaam karna chahiye; OFF karne par voice-call route (earpiece/BT) waisa hi rehna chahiye.
- [ ] "Call recording voices" = Both voices / My voice only — dono modes check karein.
