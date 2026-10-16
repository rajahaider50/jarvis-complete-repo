# AI کے ساتھ کام کرنے کا طریقہ (موبائل پر، کم ٹوکن میں)

## مسئلہ کیوں ہوتا ہے
پوری ZIP دیں تو AI سب فائلیں پڑھتا ہے اور جواب سے پہلے لمٹ ختم۔ حل: AI کو **گائیڈ + نقشہ + صرف متعلقہ فائلیں** دیں۔

## ایک بار کا سیٹ اپ
1. GitHub پر پرائیویٹ ریپو بنائیں۔ اس ZIP کے سارے فولڈر اور فائلیں اس میں اپلوڈ کریں (Task.md وغیرہ ریپو کے روٹ میں، `docs/`, `tools/`, `.github/`)۔
2. Actions ← "Update project map" ← Run workflow (ایک بار)۔ اس کے بعد ہر پش پر نقشہ خود اپڈیٹ ہوگا۔
3. Settings ← Actions ← General ← Workflow permissions ← **Read and write** کریں (ورنہ map اور edits والے workflows پش نہیں کر سکیں گے)۔

## ہر نئے کام کے 5 قدم
1. Actions ← "Pack context for an AI" ← goal اور keywords (مثلاً `chat bubble`) لکھ کر Run۔
2. رن کے آخر میں artifact `context-pack` ڈاؤن لوڈ کریں (چھوٹی ZIP)۔
3. Claude/کسی بھی AI میں **نئی چیٹ** کھولیں، ZIP لگائیں اور `docs/START_PROMPT.md` کا پرامپٹ A پیسٹ کریں اور اپنا کام لکھیں۔
4. AI بدلی ہوئی فائلوں کی ZIP دے گا۔ اسے ریپو پر Upload files سے ڈالیں۔ اگر `docs/edits/001.json` ملے تو اسے `docs/edits/` میں ڈالیں، پھر Actions ← "Apply AI edits" ← فائل کا راستہ لکھیں۔
5. Actions ← "Build APK" چلائیں۔ ایرر ہو تو لاگ اسی چیٹ میں دیں (پرامپٹ C)۔
(نوٹ: "Apply AI edits" اور map کے پش خود بخود APK نہیں بناتے، Build APK ہاتھ سے چلائیں۔)

## لمٹ ختم ہو جائے تو
دوسرے اکاؤنٹ میں نئی چیٹ، نیا پیک، اور پرامپٹ B۔ پرانی چیٹ پیسٹ نہ کریں؛ `HANDOFF_STATE.md`, `Task.md`, `Planning.md` کافی ہیں۔

## چھ ٹریکنگ فائلیں (AI خود اپڈیٹ کرتا ہے)
Task (موجودہ کام) · Planning (قدم) · Roadmap (ترتیب) · Pending (باقی) · Complete (مکمل) · Goal (اصل مقصد)۔ AI کام ختم کرے تو Pending سے ہٹا کر Complete میں لکھتا ہے۔ ان فائلوں کو ریپو میں اپڈیٹ ہو کر لگانا آپ کا کام ہے (AI کی ZIP میں آئیں گی)۔

## ہلکے ماڈل (DeepSeek وغیرہ) کے ساتھ
پہلے منطق کا کام کروائیں، پھر الگ چیٹ میں پرامپٹ D سے UI پالش۔ `docs/skills/light-model-boost.md` بھی پیک میں جاتی ہے۔

## جو نہ کریں
- پوری ZIP یا "سارا کوڈ ریویو کرو" نہ کہیں۔
- ایک چیٹ میں کئی بڑے فیچر نہ مانگیں۔
- API key یا ٹوکن چیٹ میں نہ ڈالیں۔
- AI خود GitHub لنک سے ریپو نہیں پڑھتا؛ اسی لیے context-pack ZIP استعمال کریں۔
