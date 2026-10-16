# Smart model router — Phase 3

## What changed

Custom-provider Chat اور coding requests اب eligible provider models کو ایک ساتھ score کرتے ہیں۔ Score میں real catalog quality tier، context capacity، tools support، recent success rate، observed latency اور error streak شامل ہیں۔ Context اور tools eligibility پہلے apply ہوتی ہے؛ unknown context کو small/limited سمجھا جاتا ہے اور `supportsTools=false` والے models coding/tool tasks سے خارج رہتے ہیں۔

ہر provider/model کی health private `model_router.json` میں محفوظ ہوتی ہے: daily estimated tokens، success/failure counts، error streak، cooldown deadline اور latency. Retryable HTTP errors (`402`, `408`, `409`, `425`, `429`, `5xx`) اور timeout/connection-reset/empty-response حالات پر exponential/defined cooldown لگتا ہے، پھر اگلا scored model یا fallback provider آزمایا جاتا ہے۔ Quota-like `402` errors اگلے UTC day تک cooldown ہو سکتے ہیں۔ Router events Profile > Console کے AppLog میں آتے ہیں۔

Provider editor میں optional daily token limit شامل ہے؛ limit پوری ہونے سے پہلے model eligibility سے نکل جاتا ہے۔ Existing Free only filter، manual model metadata، Phase 2 task state، Gemini startup اور voice/call flows برقرار رکھے گئے ہیں۔

## Changed files

- `Task.md`
- `Planning.md`
- `Complete.md`
- `Pending.md`
- `FEATURES.md`
- `app/src/main/java/com/jarvis/app/MainActivity.kt`
- `app/src/main/java/com/jarvis/app/code/CodeAgent.kt`
- `app/src/main/java/com/jarvis/app/data/ChatStore.kt`
- `app/src/main/java/com/jarvis/app/data/CustomProviders.kt`
- `app/src/main/java/com/jarvis/app/data/ModelRouter.kt`
- `app/src/main/java/com/jarvis/app/ui/screens/ProvidersScreen.kt`
- `docs/HANDOFF_STATE.md`
- `docs/CHANGES_20261016_smart-router-phase3.md`

## How to test on the phone

1. Add at least two eligible models/providers, leave one provider/model on Auto, and set an optional daily token limit.
2. Send Chat requests and run a small Code task. The best scored candidate should appear in the active model/status flow.
3. Simulate or use a provider that returns `429`, `503`, timeout, or empty response. The failed model should be cooled down, a next scored model tried, and a Router entry should appear in Profile > Console.
4. Set a small daily token limit, make a request, then confirm that model is skipped after the estimated usage reaches the limit.

## Verification

GitHub Actions Build APK run `38936841893` passed after the required ModelRouter imports were added. `git diff --check` passed before push.

## Untested / limitations

- Real phone simulation of 429/5xx/timeouts, provider-specific quota headers, and multi-provider failover is still pending.
- Usage is an estimate based on text length when provider usage fields are not returned; the app does not claim exact billing tokens.
- This phase scores custom-provider routes and uses them as Gemini fallbacks; Gemini's own internal model fallback remains its existing flow rather than being merged into the same provider score table.
- Retry-After response headers are not yet parsed; fixed cooldowns are used for rate limits.
- Phase 4 network/power-loss monitor and auto-resume are not included.

## Noticed, not changed

No UI restyle or voice/call routing change was made. The existing provider model catalog remains the only source for quality/context/tools metadata; unknown values are not invented.
