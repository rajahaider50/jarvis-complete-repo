# Provider discovery — Phase 1

## What changed

Custom providers اب `/models` response سے حقیقی metadata محفوظ کرتے ہیں: model id، pricing سے free/paid/unknown classification، context window، supported tools، source اور verification time۔ Existing `providers.json` schema backward-compatible رکھا گیا ہے؛ پرانے providers cache metadata کے بغیر load ہوں گے اور refresh/save پر update ہو سکتے ہیں۔

Provider editor میں **Free models only** default ON ہے۔ Verified free models الگ دکھتے ہیں، unknown-price models **Unverified** group میں الگ رہتے ہیں، اور switch OFF کرنے پر unverified models بھی دیکھے جا سکتے ہیں۔ Manual model override میں id، context window، tools support، free flag اور quality tier محفوظ کیے جا سکتے ہیں؛ manual values API results پر غالب رہتی ہیں۔

Provider save/refresh catalog کو local provider file میں cache کرتا ہے، اس لیے آخری fetched list offline بھی دستیاب رہتی ہے۔ Chat model picker اور Auto selection صرف موجودہ eligibility filter کے models استعمال کرتے ہیں؛ Gemini/core اور voice call flows کو نہیں بدلا گیا۔ `FEATURES.md` اب موجودہ اور planned capabilities کا registry ہے۔

## Changed files

- `FEATURES.md`
- `Task.md`
- `Planning.md`
- `Pending.md`
- `Complete.md`
- `app/src/main/java/com/jarvis/app/data/CustomProviders.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/ChatScreen.kt`
- `docs/HANDOFF_STATE.md`
- `docs/CHANGES_20261016_provider-discovery-phase1.md`

## How to test on the phone

1. Profile > Custom providers میں provider URL/key دیں اور Save یا Refresh دبائیں۔
2. Free only ON رکھ کر verified free models دیکھیں؛ unknown pricing والے models Unverified میں الگ رہنے چاہئیں۔ Switch OFF کر کے انہیں list میں دیکھیں۔
3. Manual Model Override میں id، context، tools، free اور quality values دے کر Add manual model، پھر Save کریں۔ دوبارہ editor کھول کر values برقرار رہنی چاہئیں۔
4. Chat کے model menu میں provider Auto یا eligible model منتخب کریں اور request بھیجیں۔ Provider کا cached catalog network بند ہونے پر بھی editor میں موجود رہنا چاہیے۔

## Verification

GitHub Actions Build APK run `38932098213` کامیاب ہوا؛ `compileDebugKotlin` اور debug APK artifact دونوں بنے۔

## Untested / limitations

- Phone پر حقیقی OpenAI-compatible providers، pricing shapes اور offline restart ابھی manually test نہیں کیے گئے۔
- Providers کا local JSON cache موجود ہے؛ Room database یا remote-updatable bundled catalog ابھی شامل نہیں کیا گیا کیونکہ project میں Room موجود نہیں اور اس phase میں نئی dependency نہیں ڈالی گئی۔
- Probe latency/tool-call tests، scored cross-provider router، exact task checkpoints/resume، network callback auto-resume، اور GitHub skill import guide کے اگلے phases ہیں۔
- `/models` endpoint OpenAI-compatible `data[]` response مانتا ہے؛ غیر معمولی provider schema کو Unverified/error state میں رکھا جائے گا، fake metadata نہیں بنایا جائے گا۔

## Noticed, not changed

Guide کے باقی phases intentionally اس task میں نہیں ملائے گئے تاکہ موجودہ Chat، Gemini startup، voice اور custom call behavior محفوظ رہیں۔ انہیں اگلے الگ phase میں implement کرنا چاہیے۔
