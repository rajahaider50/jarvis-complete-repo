# Custom provider model catalog and retry update

## What changed
- ہر custom provider کو save کرتے وقت اس کے OpenAI-compatible `/models` endpoint سے پورا model catalog fetch کر کے `providers.json` میں محفوظ کرنے کی logic شامل کی گئی۔
- Provider list میں ہر provider کا folder-style model section، `Auto` option اور تمام fetched models دکھائے جاتے ہیں۔ Chat picker میں بھی provider کے نیچے Auto اور model entries آتی ہیں۔
- Provider Auto میں `modelId` خالی رہتا ہے اور Chat/Code پہلے available model سے request شروع کرتے ہیں۔ کسی model کو چننے پر صرف وہی model استعمال ہوتا ہے۔
- Custom chat کے لیے HTTP 402، 408، 409، 425، 429 اور 5xx پر زیادہ سے زیادہ 8 attempts، بڑھتے ہوئے delay کے ساتھ، شامل کیے گئے ہیں؛ non-transient errors پر اگلے model/fallback کی طرف جایا جاتا ہے۔
- Code agent اور provider voice tool calls بھی Auto provider کے پہلے available model کو استعمال کرتے ہیں۔ Existing provider JSON backward-compatible ہے؛ پرانے records میں model catalog خالی رہے گا جب تک provider دوبارہ save/fetch نہ کیا جائے۔

## Changed files
- `app/src/main/java/com/jarvis/app/data/CustomProviders.kt`
- `app/src/main/java/com/jarvis/app/data/ChatStore.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/ChatScreen.kt`
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `Task.md`, `Planning.md`, `Complete.md`, `docs/HANDOFF_STATE.md`

## How to test on the phone
1. Profile > Custom providers > Add provider میں label, base URL اور API key دیں، پھر Save کریں۔ Save کے بعد provider card میں models اور Auto دکھنا چاہیے۔
2. Chat کے model menu میں provider کا Auto منتخب کریں، ایک گفتگو بھیجیں، پھر اسی provider کا کوئی specific model منتخب کر کے دوبارہ بھیجیں۔
3. ایسے server پر test کریں جو عارضی HTTP 402/429/5xx دے: thinking state میں retry progress دکھنا چاہیے اور 8 attempts کے بعد ہی request fail/fallback ہونی چاہیے۔
4. Code workspace میں اسی provider کے Auto selection کے ساتھ ایک مختصر task چلائیں۔

## Untested / limitations
- اس sandbox میں Android/Gradle compiler یا حقیقی provider endpoint دستیاب نہیں، اس لیے GitHub Actions build اور حقیقی phone/network behavior ابھی untested ہے۔
- Retry کی حد 8 attempts ہے تاکہ مستقل outage پر phone data/battery لامحدود استعمال نہ ہو؛ cancellation کے ذریعے user request روک سکتا ہے۔
- `/models` response OpenAI-compatible `data[].id` format میں ہونا ضروری ہے۔
