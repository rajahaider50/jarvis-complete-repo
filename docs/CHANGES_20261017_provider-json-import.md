# CHANGES 2026-10-17 · Custom provider .json import

## What changed
- **`data/CustomProviders.kt`**: added `ImportSummary` and `CustomProviders.importJson(text)`. It reads a provider `.json` file and adds **one provider per model** (all at once), reusing the file's base URL and API key. Re-importing the same file updates the same entries instead of duplicating them.
- **`ui/screens/ProvidersScreen.kt`**: added an **"Import models from .json"** button at the top of Custom providers. It opens the Android file picker (`ActivityResultContracts.OpenDocument`), reads the file on `Dispatchers.IO`, calls `importJson`, and shows "N models imported from <name>.".

## Why
The owner builds provider `.json` files (baseURL + apiKey + a models list) and wants every model available automatically instead of typing each one.

## Accepted file shapes
- Our export: `{ "provider": { "name","baseURL","apiKey" }, "models": [ { "id","name","capabilities":{...} }, ... ] }`
- OpenAI `/v1/models` reply: `{ "data": [ { "id": ... }, ... ] }` (needs a base URL present)
- A plain array of model ids or model objects.
- A model item may be a string (the id) or an object with `id`/`name`/`display_name` + optional `capabilities.vision` / `capabilities.tools` (also reads `modalities.input` for vision).

## How to test
1. Build + install, open **Custom providers**.
2. Put a provider `.json` (e.g. the generated `provider.json` / `provider-xkiro.json`) on the phone.
3. Tap **Import models from .json**, pick the file.
4. Expect: "N models imported from <name>." and the list fills with the models; each is selectable in the Chat model menu.

## Untested
- Not compiled (no build in the authoring environment); owner should run the GitHub Actions build and share any log errors.
- File picker on a real device and very large lists (100+ providers) were not run.
