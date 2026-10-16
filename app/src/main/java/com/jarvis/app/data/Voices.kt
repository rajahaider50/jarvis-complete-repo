package com.jarvis.app.data

/** Gender of each Gemini prebuilt voice + the language rule that keeps grammar matching the voice. */
object Voices {
    private val FEMALE = setOf(
        "Zephyr", "Kore", "Leda", "Aoede", "Erinome", "Sulafat", "Gacrux", "Despina",
        "Callirrhoe", "Autonoe", "Laomedeia", "Achernar", "Pulcherrima", "Vindemiatrix"
    )
    fun isFemale(voice: String) = voice in FEMALE

    fun genderRule(voice: String = Prefs.voice): String =
        if (isFemale(voice))
            "Your voice is a woman's voice and you are a woman: always speak about yourself with FEMININE grammar and never masculine " +
                "(Urdu/Hindi: \"کر رہی ہوں\", \"کر سکتی ہوں\", \"کرتی ہوں\" / \"कर रही हूँ\", \"कर सकती हूँ\"; never \"کر رہا ہوں\" or \"سکتا ہوں\"; " +
                "use feminine forms in Arabic, French, Spanish and Russian as well). "
        else
            "Your voice is a man's voice and you are a man: always speak about yourself with MASCULINE grammar and never feminine " +
                "(Urdu/Hindi: \"کر رہا ہوں\", \"کر سکتا ہوں\" / \"कर रहा हूँ\", \"कर सकता हूँ\"; never \"کر رہی ہوں\" or \"سکتی ہوں\"). "
}
