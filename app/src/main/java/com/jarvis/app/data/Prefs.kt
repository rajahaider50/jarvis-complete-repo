package com.jarvis.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.ui.theme.Palettes
import org.json.JSONArray
import org.json.JSONObject

data class CustomVoice(val name: String, val base: String, val style: String)

/** All user settings, stored privately on the device. */
object Prefs {
    private lateinit var sp: SharedPreferences

    var apiKey by mutableStateOf("")
    var manualModel by mutableStateOf("")      // "" = Auto (best model for each task)
    var voice by mutableStateOf("Charon")      // Gemini prebuilt voice name
    var voiceLabel by mutableStateOf("Charon") // shown name (custom voices have their own)
    var voiceStyle by mutableStateOf("")       // style direction for custom voices
    var lang by mutableStateOf("en-US")
    var theme by mutableStateOf(0)
    var orbStyle by mutableStateOf(0)
    var userName by mutableStateOf("")
    var instructions by mutableStateOf("")
    var rememberChats by mutableStateOf(true)
    var aiCursor by mutableStateOf(false)       // master switch for the AI cursor / screen control
    var cursorClickRate by mutableStateOf(5)    // max cursor clicks per second
    var liveResume by mutableStateOf(true)      // keep the live call alive across drops (session resumption)
    var liveCompress by mutableStateOf(true)    // let the server compress a long call context
    var resumeHandle by mutableStateOf("")      // last Gemini Live session-resumption handle
    var resumeAt by mutableStateOf(0L)          // when the handle was received (valid ~30 min)
    var assistantName by mutableStateOf("Jarvis")
    var wakePhrase by mutableStateOf("")
    var wakeEnabled by mutableStateOf(false)
    var wakeLang by mutableStateOf("auto")     // language used to hear the wake phrase
    var onboarded by mutableStateOf(false)
    var uiStyle by mutableStateOf(0)            // 0 Classic Gold, 1 Aurora, 2 Nova
    var wakeEngine by mutableStateOf("vosk")   // vosk = offline phrase (no key) | porcupine = offline, needs key | basic = Android speech loop
    var callProvider by mutableStateOf("")      // id of the custom provider used for voice calls ("" = Gemini Live)
    var goodChat by mutableStateOf("")          // last Gemini chat model that worked
    var workspaceModel by mutableStateOf("")    // Workspace coding route: "" = Auto (all providers, step-by-step), "custom:<id>" = one provider (manual)
    var webSearch by mutableStateOf(false)      // Google Search grounding (needs a plan that includes it)
    var voskLang by mutableStateOf("en")        // language of the offline wake-word model
    var pvKey by mutableStateOf("")             // Picovoice AccessKey (free, for the offline wake word)
    var pvKeyword by mutableStateOf("JARVIS")   // built-in keyword name, or "custom" (your own .ppn file)
    var greetOnHome by mutableStateOf(true)
    var catalogUrl by mutableStateOf("")        // remote model_catalog.json URL ("" = bundled only)
    var assistantTheme by mutableStateOf("classic")  // selected AssistantTheme id
    var aiControl by mutableStateOf(true)       // let the assistant change orb/theme/interface and recording from a call
    var fxEnabled by mutableStateOf(true)       // sound effects on/off
    var fxVolume by mutableStateOf(0.7f)        // sound effects volume 0..1
    val memories = mutableStateListOf<String>()
    val customVoices = mutableStateListOf<CustomVoice>()
    val liveModels = mutableStateListOf<String>()   // saved order of Gemini Live models used for calls (first = tried first)

    fun init(c: Context) {
        sp = c.applicationContext.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
        apiKey = sp.getString("key", "") ?: ""
        manualModel = sp.getString("manual", "") ?: ""
        voice = sp.getString("voice", "Charon") ?: "Charon"
        voiceLabel = sp.getString("vlabel", voice) ?: voice
        if (!sp.getBoolean("vmig", false)) {            // the old default was a female voice: move it to the new male default once
            if (voice == "Zephyr") { voice = "Charon"; voiceLabel = "Charon" }
            sp.edit().putBoolean("vmig", true).putString("voice", voice).putString("vlabel", voiceLabel).apply()
        }
        voiceStyle = sp.getString("vstyle", "") ?: ""
        lang = sp.getString("lang", "en-US") ?: "en-US"
        theme = sp.getInt("theme", 0)
        orbStyle = sp.getInt("orb", 0)
        userName = sp.getString("name", "") ?: ""
        instructions = sp.getString("instr", "") ?: ""
        rememberChats = sp.getBoolean("rem", true)
        assistantName = sp.getString("aname", "Jarvis") ?: "Jarvis"
        wakePhrase = sp.getString("wake", "") ?: ""
        wakeEnabled = sp.getBoolean("wakeOn", false)
        wakeLang = sp.getString("wlang", "auto") ?: "auto"
        onboarded = sp.getBoolean("onb", false)
        uiStyle = sp.getInt("ui", 0)
        wakeEngine = sp.getString("weng", "vosk") ?: "vosk"
        webSearch = sp.getBoolean("wsearch", false)
        callProvider = sp.getString("callprov", "") ?: ""
        goodChat = sp.getString("goodchat", "") ?: ""
        workspaceModel = sp.getString("wsmodel", "") ?: ""
        voskLang = sp.getString("vlang", "en") ?: "en"
        pvKey = sp.getString("pvkey", "") ?: ""
        pvKeyword = sp.getString("pvkw", "JARVIS") ?: "JARVIS"
        greetOnHome = sp.getBoolean("greet", true)
        catalogUrl = sp.getString("catUrl", "") ?: ""
        assistantTheme = sp.getString("atheme", "classic") ?: "classic"
        aiControl = sp.getBoolean("aicontrol", true)
        aiCursor = sp.getBoolean("aicursor", false)
        cursorClickRate = sp.getInt("cursorrate", 5)
        liveResume = sp.getBoolean("liveresume", true)
        liveCompress = sp.getBoolean("livecompress", true)
        resumeHandle = sp.getString("resumehandle", "") ?: ""
        resumeAt = sp.getLong("resumeat", 0L)
        fxEnabled = sp.getBoolean("fxon", true)
        fxVolume = sp.getFloat("fxvol", 0.7f)
        memories.clear()
        try {
            val a = JSONArray(sp.getString("mem", "[]"))
            for (i in 0 until a.length()) memories.add(a.getString(i))
        } catch (e: Exception) { AppLog.w(e); }
        customVoices.clear()
        try {
            val a = JSONArray(sp.getString("cv", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                customVoices.add(CustomVoice(o.getString("n"), o.getString("b"), o.getString("s")))
            }
        } catch (e: Exception) { AppLog.w(e); }
        liveModels.clear()
        try {
            val a = JSONArray(sp.getString("livemodels", "[]"))
            for (i in 0 until a.length()) liveModels.add(a.getString(i))
        } catch (e: Exception) { AppLog.w(e); }
        applyTheme()
    }

    fun applyTheme() {
        Palettes.style = uiStyle
        if (uiStyle >= 5) {
            val d = com.jarvis.app.ui.styles.IfaceCatalog.forStyle(uiStyle)
            if (d != null) {
                Palettes.engine = d.engine
                Palettes.serif = d.serif
                Palettes.current = d.palette
                return
            }
        }
        Palettes.engine = uiStyle
        Palettes.serif = uiStyle == 0 || uiStyle == 4
        val l = Palettes.list()
        Palettes.current = l[theme.coerceIn(0, l.lastIndex)]
    }

    fun save() {
        val cv = JSONArray()
        customVoices.forEach { cv.put(JSONObject().put("n", it.name).put("b", it.base).put("s", it.style)) }
        sp.edit()
            .putString("key", apiKey).putString("manual", manualModel)
            .putString("voice", voice).putString("vlabel", voiceLabel).putString("vstyle", voiceStyle)
            .putString("lang", lang).putInt("theme", theme).putInt("orb", orbStyle)
            .putString("name", userName).putString("instr", instructions).putBoolean("rem", rememberChats)
            .putString("aname", assistantName).putString("wake", wakePhrase).putBoolean("wakeOn", wakeEnabled).putString("wlang", wakeLang)
            .putBoolean("onb", onboarded).putBoolean("greet", greetOnHome)
            .putString("callprov", callProvider).putString("goodchat", goodChat).putBoolean("wsearch", webSearch).putString("vlang", voskLang).putInt("ui", uiStyle).putString("weng", wakeEngine).putString("pvkey", pvKey).putString("pvkw", pvKeyword)
            .putString("wsmodel", workspaceModel)
            .putString("catUrl", catalogUrl)
            .putString("atheme", assistantTheme).putBoolean("fxon", fxEnabled).putFloat("fxvol", fxVolume)
            .putBoolean("aicontrol", aiControl)
            .putBoolean("aicursor", aiCursor).putInt("cursorrate", cursorClickRate)
            .putBoolean("liveresume", liveResume).putBoolean("livecompress", liveCompress)
            .putString("resumehandle", resumeHandle).putLong("resumeat", resumeAt)
            .putString("mem", JSONArray(memories.toList()).toString()).putString("cv", cv.toString())
            .putString("livemodels", JSONArray(liveModels.toList()).toString())
            .apply()
    }
}
