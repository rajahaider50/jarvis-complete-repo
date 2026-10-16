package com.jarvis.app.data

data class Lang(val code: String, val label: String, val english: String, val sample: String)

object Langs {
    val all = listOf(
        Lang("en-US", "English", "English", "Good evening. I am Jarvis, your private assistant. How may I help you?"),
        Lang("ur-PK", "اردو", "Urdu", "شام بخیر۔ میں جاروس ہوں، آپ کا ذاتی اسسٹنٹ۔ میں آپ کی کیا مدد کر سکتا ہوں؟"),
        Lang("hi-IN", "हिन्दी", "Hindi", "शुभ संध्या। मैं जार्विस हूँ, आपका निजी सहायक। मैं आपकी क्या मदद कर सकता हूँ?"),
        Lang("ar-SA", "العربية", "Arabic", "مساء الخير. أنا جارفيس، مساعدك الشخصي. كيف يمكنني مساعدتك؟"),
        Lang("es-ES", "Español", "Spanish", "Buenas noches. Soy Jarvis, tu asistente personal. ¿En qué puedo ayudarte?"),
        Lang("fr-FR", "Français", "French", "Bonsoir. Je suis Jarvis, votre assistant personnel. Comment puis-je vous aider ?"),
        Lang("tr-TR", "Türkçe", "Turkish", "İyi akşamlar. Ben Jarvis, kişisel asistanınız. Size nasıl yardımcı olabilirim?"),
        Lang("ru-RU", "Русский", "Russian", "Добрый вечер. Я Джарвис, ваш личный помощник. Чем могу помочь?")
    )
    fun of(code: String) = all.firstOrNull { it.code == code } ?: all[0]

    /** Sample sentence for voice previews, with grammar that matches the voice's gender. */
    fun sampleFor(code: String, female: Boolean, name: String): String {
        val n = name.ifBlank { "Jarvis" }
        return when (code) {
            "ur-PK" -> if (female) "شام بخیر۔ میں ${n} ہوں، آپ کی ذاتی اسسٹنٹ۔ میں آپ کی کیا مدد کر سکتی ہوں؟"
                       else "شام بخیر۔ میں ${n} ہوں، آپ کا ذاتی اسسٹنٹ۔ میں آپ کی کیا مدد کر سکتا ہوں؟"
            "hi-IN" -> if (female) "शुभ संध्या। मैं ${n} हूँ, आपकी निजी सहायक। मैं आपकी क्या मदद कर सकती हूँ?"
                       else "शुभ संध्या। मैं ${n} हूँ, आपका निजी सहायक। मैं आपकी क्या मदद कर सकता हूँ?"
            "ar-SA" -> if (female) "مساء الخير. أنا ${n}، مساعدتك الشخصية. كيف يمكنني مساعدتك؟"
                       else "مساء الخير. أنا ${n}، مساعدك الشخصي. كيف يمكنني مساعدتك؟"
            "es-ES" -> "Buenas noches. Soy ${n}, tu asistente personal. ¿En qué puedo ayudarte?"
            "fr-FR" -> if (female) "Bonsoir. Je suis ${n}, votre assistante personnelle. Comment puis-je vous aider ?"
                       else "Bonsoir. Je suis ${n}, votre assistant personnel. Comment puis-je vous aider ?"
            "tr-TR" -> "İyi akşamlar. Ben ${n}, kişisel asistanınız. Size nasıl yardımcı olabilirim?"
            "ru-RU" -> if (female) "Добрый вечер. Я ${n}, ваша личная помощница. Чем могу помочь?"
                       else "Добрый вечер. Я ${n}, ваш личный помощник. Чем могу помочь?"
            else -> "Good evening. I am ${n}, your private assistant. How may I help you?"
        }
    }

    fun bootLine(code: String, name: String, battery: Int, temp: Int, freeGb: Int): String = when (code) {
        "ur-PK" -> "سسٹم آن لائن ہے۔ خوش آمدید $name۔ بیٹری $battery فیصد ہے، درجہ حرارت $temp ڈگری ہے، اور $freeGb گیگابائٹ جگہ خالی ہے۔ تمام سسٹم تیار ہیں۔"
        "hi-IN" -> "सिस्टम ऑनलाइन है। स्वागत है $name। बैटरी $battery प्रतिशत है, तापमान $temp डिग्री है, और $freeGb जीबी स्टोरेज खाली है। सभी सिस्टम तैयार हैं।"
        "ar-SA" -> "النظام يعمل الآن. أهلاً $name. البطارية $battery بالمئة، الحرارة $temp درجة، ومساحة التخزين الخالية $freeGb جيجابايت. جميع الأنظمة جاهزة."
        else -> "System online. Welcome, $name. Battery at $battery percent, temperature $temp degrees, and $freeGb gigabytes of storage free. All systems are ready."
    }
}
