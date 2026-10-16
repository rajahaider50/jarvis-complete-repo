package com.jarvis.app.ui.styles

import com.jarvis.app.ui.theme.Palette
import com.jarvis.app.ui.theme.Palettes

/** One extra interface style. All of these use the premium "Iface" engine (engine 5). */
data class IfaceDef(
    val name: String,
    val about: String,
    val palette: Palette,
    val engine: Int = 5,
    val serif: Boolean = false
)

/** A style's layout recipe: which home/control/settings archetype and which nav bar + corner style. */
data class IfaceCfg(val layout: Int, val nav: Int, val corner: Int)

object IfaceCatalog {

    // ------------------------------------------------------------------ iOS premium palettes
    private val iOSBlue = Palette("iOS Blue", 0xFF0A84FF, 0xFF66B2FF, 0xFF0060CC, 0xFFF7F8FB, 0xFFFFFFFF, 0xFFEEF1F6, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSIndigo = Palette("iOS Indigo", 0xFF5E5CE6, 0xFF9C9BF2, 0xFF4040B8, 0xFFF8F8FC, 0xFFFFFFFF, 0xFFEEEEF7, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSPurple = Palette("iOS Purple", 0xFFAF52DE, 0xFFD19AF0, 0xFF8A38B8, 0xFFFBF8FC, 0xFFFFFFFF, 0xFFF2EBF6, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSPink = Palette("iOS Pink", 0xFFFF2D55, 0xFFFF7A93, 0xFFCC1F40, 0xFFFDF8F9, 0xFFFFFFFF, 0xFFF8EAEE, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSRed = Palette("iOS Red", 0xFFFF3B30, 0xFFFF827A, 0xFFCC2B22, 0xFFFDF8F8, 0xFFFFFFFF, 0xFFF8ECEB, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSOrange = Palette("iOS Orange", 0xFFFF9500, 0xFFFFBA5C, 0xFFCC7600, 0xFFFDFBF7, 0xFFFFFFFF, 0xFFF8F1E6, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSYellow = Palette("iOS Yellow", 0xFFF7B500, 0xFFFFD25C, 0xFFC49000, 0xFFFDFCF7, 0xFFFFFFFF, 0xFFF8F4E4, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSGreen = Palette("iOS Green", 0xFF34C759, 0xFF7FDCA0, 0xFF209A40, 0xFFF7FCF8, 0xFFFFFFFF, 0xFFE9F6EC, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSMint = Palette("iOS Mint", 0xFF00C7BE, 0xFF66DED8, 0xFF009A94, 0xFFF7FCFC, 0xFFFFFFFF, 0xFFE7F5F4, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSTeal = Palette("iOS Teal", 0xFF30B0C7, 0xFF80D0E0, 0xFF20889A, 0xFFF7FBFD, 0xFFFFFFFF, 0xFFE8F2F6, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSCyan = Palette("iOS Cyan", 0xFF32ADE6, 0xFF83CCF2, 0xFF1C83B8, 0xFFF7FAFD, 0xFFFFFFFF, 0xFFE7F1F8, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSBrown = Palette("iOS Brown", 0xFFA2845E, 0xFFC7B093, 0xFF7A6244, 0xFFFBF9F6, 0xFFFFFFFF, 0xFFF2EDE5, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSGraphite = Palette("iOS Graphite", 0xFF48484A, 0xFF8E8E93, 0xFF2C2C2E, 0xFFF7F7F8, 0xFFFFFFFF, 0xFFEBEBED, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSMidnight = Palette("iOS Midnight", 0xFF0A84FF, 0xFF66B2FF, 0xFF0060CC, 0xFF0A0A0F, 0xFF141419, 0xFF1E1E24, 0xFFF2F2F7, 0xFF8E8E93, false)
    private val iOSSpaceGray = Palette("iOS Space Gray", 0xFF64D2FF, 0xFFA6E4FF, 0xFF3AA8CC, 0xFF0C0C0E, 0xFF17171A, 0xFF212126, 0xFFF2F2F7, 0xFF8E8E93, false)
    private val iOSNightPurple = Palette("iOS Night Purple", 0xFFBF5AF2, 0xFFD99AF6, 0xFF9438C4, 0xFF0B0810, 0xFF160F1E, 0xFF20172A, 0xFFF2F2F7, 0xFF8E8E93, false)
    private val iOSNightGreen = Palette("iOS Night Green", 0xFF32D74B, 0xFF80E68F, 0xFF22A838, 0xFF07100A, 0xFF0E1C12, 0xFF162A1C, 0xFFF2F2F7, 0xFF8E8E93, false)
    private val iOSRose = Palette("iOS Rose", 0xFFFF6482, 0xFFFFA0B4, 0xFFCC4660, 0xFFFDF8F9, 0xFFFFFFFF, 0xFFF8EAEE, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSSky = Palette("iOS Sky", 0xFF5AC8FA, 0xFFA0E0FC, 0xFF3A9CC4, 0xFFF8FBFD, 0xFFFFFFFF, 0xFFE9F3F8, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSPlum = Palette("iOS Plum", 0xFF9A4D8E, 0xFFC48CBC, 0xFF743A6A, 0xFFFBF8FA, 0xFFFFFFFF, 0xFFF2E9F0, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSOlive = Palette("iOS Olive", 0xFF8B8B2C, 0xFFBEBE6B, 0xFF6A6A1E, 0xFFFBFAF6, 0xFFFFFFFF, 0xFFF0F0E2, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSCobalt = Palette("iOS Cobalt", 0xFF1D6FE0, 0xFF6AA0EC, 0xFF1452A8, 0xFFF7FAFD, 0xFFFFFFFF, 0xFFE8F0F8, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSCoral = Palette("iOS Coral", 0xFFFF7250, 0xFFFFA894, 0xFFCC5033, 0xFFFDF9F8, 0xFFFFFFFF, 0xFFF8EDE9, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSIce = Palette("iOS Ice", 0xFF64B5F6, 0xFFA8D4FA, 0xFF4290CC, 0xFFF8FBFD, 0xFFFFFFFF, 0xFFEAF2F9, 0xFF1C1C1E, 0xFF8E8E93, true)
    private val iOSSteel = Palette("iOS Steel", 0xFF546E7A, 0xFF9DB0B8, 0xFF3A4E58, 0xFFF8FAFB, 0xFFFFFFFF, 0xFFEBF0F2, 0xFF1C1C1E, 0xFF8E8E93, true)

    private val iOSAbout = "Apple-style premium: floating glass tabs, big rounded hero and cards."

    /** 45 extra styles. Index 0 = interface style 5. */
    val list: List<IfaceDef> = listOf(
        // ---- 20 native Android premium (Material-style nav + sharp/medium corners) ----
        IfaceDef("Material Midnight", "Native Android: top title, tonal cards, label tabs. Deep blue surfaces.", Palettes.all[15]),
        IfaceDef("Material Sky", "Native Android: clean blue Material cards and label tabs.", Palettes.all[16]),
        IfaceDef("Material Graphite", "Native Android: neutral graphite Material surfaces.", Palettes.all[17]),
        IfaceDef("Material Coral", "Native Android: coral Material cards with a compact icon bar.", Palettes.all[18]),
        IfaceDef("Material Lavender", "Native Android: soft lavender tonal cards.", Palettes.all[19]),
        IfaceDef("Material Mint", "Native Android: fresh mint Material palette.", Palettes.all[20]),
        IfaceDef("Material Peach", "Native Android: warm peach Material cards.", Palettes.all[21]),
        IfaceDef("Material Indigo", "Native Android: deep indigo Material look.", Palettes.all[22]),
        IfaceDef("Material Crimson", "Native Android: crimson Material cards with text tabs.", Palettes.all[23]),
        IfaceDef("Material Amber", "Native Android: amber Material surfaces.", Palettes.all[24]),
        IfaceDef("Material Arctic", "Native Android: icy arctic Material cards.", Palettes.all[25]),
        IfaceDef("Material Forest", "Native Android: forest green Material look.", Palettes.all[26]),
        IfaceDef("Material Wine", "Native Android: rich wine Material surfaces.", Palettes.all[27]),
        IfaceDef("Material Stone", "Native Android: stone Material cards with an icon bar.", Palettes.all[28]),
        IfaceDef("Material Aqua", "Native Android: aqua Material cards.", Palettes.all[29]),
        IfaceDef("Material Sand", "Native Android: soft sand Material palette.", Palettes.all[30]),
        IfaceDef("Material Berry", "Native Android: berry Material surfaces.", Palettes.all[31]),
        IfaceDef("Material Steel", "Native Android: cool steel Material look.", Palettes.all[32]),
        IfaceDef("Material Neon", "Native Android: neon green on black, icon bar.", Palettes.all[33]),
        IfaceDef("Material Violet", "Native Android: vivid violet Material cards.", Palettes.all[34]),

        // ---- 25 iOS premium (floating / segmented nav + round/pill corners) ----
        IfaceDef("iOS Blue", iOSAbout, iOSBlue),
        IfaceDef("iOS Indigo", iOSAbout, iOSIndigo),
        IfaceDef("iOS Purple", iOSAbout, iOSPurple),
        IfaceDef("iOS Pink", iOSAbout, iOSPink),
        IfaceDef("iOS Red", iOSAbout, iOSRed),
        IfaceDef("iOS Orange", iOSAbout, iOSOrange),
        IfaceDef("iOS Yellow", iOSAbout, iOSYellow),
        IfaceDef("iOS Green", iOSAbout, iOSGreen),
        IfaceDef("iOS Mint", iOSAbout, iOSMint),
        IfaceDef("iOS Teal", iOSAbout, iOSTeal),
        IfaceDef("iOS Cyan", iOSAbout, iOSCyan),
        IfaceDef("iOS Brown", iOSAbout, iOSBrown),
        IfaceDef("iOS Graphite", iOSAbout, iOSGraphite),
        IfaceDef("iOS Midnight", iOSAbout, iOSMidnight),
        IfaceDef("iOS Space Gray", iOSAbout, iOSSpaceGray),
        IfaceDef("iOS Night Purple", iOSAbout, iOSNightPurple),
        IfaceDef("iOS Night Green", iOSAbout, iOSNightGreen),
        IfaceDef("iOS Rose", iOSAbout, iOSRose),
        IfaceDef("iOS Sky", iOSAbout, iOSSky),
        IfaceDef("iOS Plum", iOSAbout, iOSPlum),
        IfaceDef("iOS Olive", iOSAbout, iOSOlive),
        IfaceDef("iOS Cobalt", iOSAbout, iOSCobalt),
        IfaceDef("iOS Coral", iOSAbout, iOSCoral),
        IfaceDef("iOS Ice", iOSAbout, iOSIce),
        IfaceDef("iOS Steel", iOSAbout, iOSSteel)
    )

    fun forStyle(style: Int): IfaceDef? = list.getOrNull(style - 5)

    /** Distinct layout recipe per style so no two premium styles look the same. */
    fun cfgFor(style: Int): IfaceCfg {
        val j = (style - 5).coerceIn(0, list.size - 1)
        return if (j < 20) {
            IfaceCfg(layout = (j / 4) % 4, nav = intArrayOf(0, 2, 3, 4)[j % 4], corner = intArrayOf(0, 1, 2)[j % 3])
        } else {
            val k = j - 20
            IfaceCfg(layout = (k / 4) % 4, nav = intArrayOf(1, 2, 4, 1)[k % 4], corner = intArrayOf(2, 3, 3)[k % 3])
        }
    }
}
