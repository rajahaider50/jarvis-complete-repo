package com.jarvis.app.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.Prefs
import com.jarvis.app.ui.components.ORB_STYLES
import com.jarvis.app.ui.styles.IfaceCatalog
import com.jarvis.app.ui.theme.Palettes
import com.jarvis.app.ui.theme.ThemeRegistry

/**
 * The floating option popups the assistant shows during a call ("5-5"): orb styles, colour themes and
 * interface styles. The assistant opens a page of five, the user answers by voice ("aur dikhao", "teesra"),
 * and the assistant calls [select].
 */
object AiPanel {
    data class Opt(val id: String, val label: String)

    var visible by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var category by mutableStateOf("")
        private set
    var options by mutableStateOf(listOf<Opt>())
        private set
    var selectedId by mutableStateOf<String?>(null)

    val categories = listOf("orb", "theme", "interface", "color")

    private fun titleOf(category: String) = when (category) {
        "orb" -> "Orb style"
        "theme" -> "Assistant theme"
        "interface" -> "Interface style"
        "color" -> "Colour theme"
        else -> category
    }

    fun optionsFor(category: String): List<Opt> = when (category.lowercase()) {
        "orb", "orb_style", "orbstyle" -> ORB_STYLES.mapIndexed { i, n -> Opt(i.toString(), n) }
        "theme" -> ThemeRegistry.all.map { Opt(it.id, it.displayName) }
        "interface", "iface", "ui" -> (
            listOf(Opt("0", "Classic Gold"), Opt("1", "Aurora"), Opt("2", "Nova"), Opt("3", "Pulse"), Opt("4", "Folio")) +
                IfaceCatalog.list.mapIndexed { i, d -> Opt((5 + i).toString(), d.name) }
            )
        "color", "colour", "palette" -> Palettes.list().mapIndexed { i, p -> Opt(i.toString(), p.name) }
        else -> emptyList()
    }

    /** Opens the given page (0-based) of five options and returns a short text list for the assistant to read/choose from. */
    fun show(category: String, page: Int): String? {
        val all = optionsFor(category)
        if (all.isEmpty()) return null
        val pages = (all.size + 4) / 5
        val p = page.coerceIn(0, pages - 1)
        val slice = all.drop(p * 5).take(5)
        visible = true
        selectedId = null
        this.category = category.lowercase()
        title = "${titleOf(category)} · page ${p + 1}/$pages"
        options = slice
        val lines = slice.mapIndexed { i, o -> "${i + 1}. ${o.label} (id=${o.id})" }
        return "Showing $title:\n" + lines.joinToString("\n")
    }

    fun close() { visible = false; selectedId = null }

    /** Applies an option. Returns a confirmation, or null if the category/id is unknown. */
    fun select(category: String, id: String): String? {
        val all = optionsFor(category)
        val opt = all.firstOrNull { it.id == id } ?: return null
        when (category.lowercase()) {
            "orb", "orb_style", "orbstyle" -> { Prefs.orbStyle = id.toIntOrNull() ?: return null; Prefs.assistantTheme = "classic" }
            "theme" -> Prefs.assistantTheme = id
            "interface", "iface", "ui" -> { Prefs.uiStyle = id.toIntOrNull() ?: return null; Prefs.theme = 0; Prefs.applyTheme() }
            "color", "colour", "palette" -> { Prefs.theme = id.toIntOrNull() ?: return null; Prefs.applyTheme() }
            else -> {}
        }
        Prefs.save()
        selectedId = id
        return "Done: ${titleOf(category)} is now \"${opt.label}\"."
    }
}
