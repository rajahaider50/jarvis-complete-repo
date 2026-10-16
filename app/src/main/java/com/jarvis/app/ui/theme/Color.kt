package com.jarvis.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Palette(
    val name: String, val accent: Long, val light: Long, val dark: Long,
    val bg: Long, val s1: Long, val s2: Long,
    val text: Long = 0xFFF5F0E6, val muted: Long = 0xFF8E897D, val isLight: Boolean = false
)

/** 50 color styles. The first one is the original Figma "Royal Gold". */
object Palettes {
    val all = listOf(
        Palette("Royal Gold", 0xFFC9A84C, 0xFFEBD48F, 0xFFA98531, 0xFF0B0B0D, 0xFF151518, 0xFF202024),
        Palette("Rose Gold", 0xFFD4A28E, 0xFFF2CBB8, 0xFFA8725F, 0xFF0D0A0A, 0xFF181314, 0xFF241C1E),
        Palette("Platinum", 0xFFC9CED6, 0xFFEEF1F5, 0xFF8E96A3, 0xFF0A0B0D, 0xFF141518, 0xFF1F2126),
        Palette("Emerald", 0xFF3FBF8F, 0xFF8FE8C3, 0xFF1F8A63, 0xFF070D0B, 0xFF0F1814, 0xFF17251F),
        Palette("Sapphire", 0xFF4C8DEB, 0xFF9CC3FA, 0xFF2C62B5, 0xFF070A10, 0xFF0F1420, 0xFF171E30),
        Palette("Amethyst", 0xFFA57BEA, 0xFFD2B8FA, 0xFF7447B8, 0xFF0B080F, 0xFF15101E, 0xFF20182E),
        Palette("Ruby", 0xFFE0455A, 0xFFF59AA7, 0xFFA82A3C, 0xFF0F0709, 0xFF1C0F13, 0xFF2A171C),
        Palette("Copper", 0xFFC8773E, 0xFFEBAA7A, 0xFF94521F, 0xFF0E0A07, 0xFF19120D, 0xFF261C15),
        Palette("Teal", 0xFF2FB5BF, 0xFF8CE0E6, 0xFF1B7F88, 0xFF060D0E, 0xFF0E1A1C, 0xFF15272A),
        Palette("Sunset", 0xFFF08A4B, 0xFFFFC08F, 0xFFC2551F, 0xFF0F0A07, 0xFF1B120C, 0xFF291B12),
        Palette("Ice Blue", 0xFF7FC8F0, 0xFFC6E8FA, 0xFF4C93BF, 0xFF070B0F, 0xFF101820, 0xFF18242F),
        Palette("Champagne", 0xFFE6D3A8, 0xFFFAF0D6, 0xFFB79F68, 0xFF0C0B09, 0xFF171512, 0xFF231F1A),
        Palette("Onyx Mono", 0xFFE8E8E8, 0xFFFFFFFF, 0xFF9A9A9A, 0xFF050505, 0xFF101010, 0xFF1A1A1A),
        Palette("Orchid", 0xFFE58FD0, 0xFFF6C4EA, 0xFFB5589F, 0xFF0D080C, 0xFF1B1019, 0xFF291A27),
        Palette("Olive Luxe", 0xFFB5B85A, 0xFFDADE8C, 0xFF7F8230, 0xFF0B0C07, 0xFF15170E, 0xFF202316),
        Palette("Midnight Blue", 0xFF5E8BFF, 0xFFA9C2FF, 0xFF3457C4, 0xFF070A14, 0xFF0E1424, 0xFF161E34),
        Palette("Sky", 0xFF32ADE6, 0xFF9BDCFF, 0xFF1C6FA8, 0xFF06101A, 0xFF0C1A28, 0xFF142438),
        Palette("Graphite", 0xFF8E8E93, 0xFFD8D8DD, 0xFF5A5A5F, 0xFF0A0A0B, 0xFF141416, 0xFF1E1E22),
        Palette("Coral", 0xFFFF6B5B, 0xFFFFB0A6, 0xFFC43F31, 0xFF140807, 0xFF20100E, 0xFF2E1816),
        Palette("Lavender", 0xFFB18CFF, 0xFFDCC8FF, 0xFF7B54D6, 0xFF0C0914, 0xFF150F24, 0xFF1F1734),
        Palette("Mint", 0xFF34D399, 0xFF9CF0CE, 0xFF159A6D, 0xFF06110D, 0xFF0C1C16, 0xFF142A22),
        Palette("Peach", 0xFFFF9E64, 0xFFFFC9A3, 0xFFC9662E, 0xFF120B06, 0xFF1E140C, 0xFF2C1E13),
        Palette("Indigo", 0xFF5B5BE6, 0xFFA9A9FF, 0xFF3A3AB8, 0xFF080814, 0xFF101024, 0xFF181834),
        Palette("Crimson", 0xFFE23E57, 0xFFF59AA9, 0xFFA82238, 0xFF0F0609, 0xFF1B0D12, 0xFF29151C),
        Palette("Amber", 0xFFFFB020, 0xFFFFD688, 0xFFC47F00, 0xFF100C04, 0xFF1C1508, 0xFF2A1F0E),
        Palette("Arctic", 0xFF6FD3FF, 0xFFC2ECFF, 0xFF2E9CC7, 0xFF060E14, 0xFF0D1822, 0xFF162430),
        Palette("Forest", 0xFF2E8B57, 0xFF88CFA8, 0xFF1B5E39, 0xFF070F0A, 0xFF0E1A12, 0xFF16271B),
        Palette("Wine", 0xFF8E2B4B, 0xFFCB8AA0, 0xFF651732, 0xFF0D0509, 0xFF180B11, 0xFF24131B),
        Palette("Stone", 0xFFB0A99F, 0xFFE0DAD0, 0xFF7C7469, 0xFF0B0A08, 0xFF161411, 0xFF211E1A),
        Palette("Aqua", 0xFF22D3EE, 0xFF9AF0FA, 0xFF0E9CB4, 0xFF04101A, 0xFF0A1C28, 0xFF122838),
        Palette("Sand", 0xFFD8B877, 0xFFF0DCAC, 0xFFA88A44, 0xFF0C0A05, 0xFF18140C, 0xFF241E12),
        Palette("Berry", 0xFFC42D8C, 0xFFF090CC, 0xFF8E1863, 0xFF100610, 0xFF1C0D1C, 0xFF2A1628),
        Palette("Steel", 0xFF6E8CA8, 0xFFB4C7D8, 0xFF47607A, 0xFF080B0F, 0xFF10151C, 0xFF181F28),
        Palette("Neon Green", 0xFF39FF88, 0xFF9BFFC4, 0xFF17B85B, 0xFF04100A, 0xFF081A10, 0xFF0E2818),
        Palette("Neon Purple", 0xFFB14BFF, 0xFFD9A6FF, 0xFF7A1FC0, 0xFF0A0614, 0xFF120A20, 0xFF1C1030),
        Palette("Neon Pink", 0xFFFF3D9A, 0xFFFF9AC8, 0xFFC0166E, 0xFF12060C, 0xFF1E0C16, 0xFF2C1422),
        Palette("Neon Blue", 0xFF3D8BFF, 0xFF9AC2FF, 0xFF1656C0, 0xFF050A14, 0xFF0A1224, 0xFF121C34),
        Palette("Ocean Deep", 0xFF1E90FF, 0xFF7FC0FF, 0xFF0B5CA8, 0xFF04101C, 0xFF081A2C, 0xFF0E2840),
        Palette("Blush", 0xFFE88AA0, 0xFFF7C4D0, 0xFFB85A72, 0xFF0F080A, 0xFF1A0F13, 0xFF26171D),
        Palette("Cobalt", 0xFF2853D6, 0xFF8AA4F0, 0xFF1839A0, 0xFF060914, 0xFF0C1224, 0xFF141C34),
        Palette("Turquoise", 0xFF14B8A6, 0xFF80E6DA, 0xFF0A7F72, 0xFF04100E, 0xFF0A1C1A, 0xFF102A26),
        Palette("Fuchsia", 0xFFD946EF, 0xFFEFA8F8, 0xFFA020B8, 0xFF0E0612, 0xFF180C1E, 0xFF24122C),
        Palette("Bronze", 0xFFB08D57, 0xFFDCC49A, 0xFF806234, 0xFF0B0905, 0xFF16110A, 0xFF221A10),
        Palette("Slate Blue", 0xFF7C83D6, 0xFFBCC2F0, 0xFF545BA8, 0xFF080910, 0xFF10121C, 0xFF181B28),
        Palette("Lime Luxe", 0xFFA3D900, 0xFFD2F278, 0xFF76A000, 0xFF0A0D04, 0xFF141A08, 0xFF1E2810),
        Palette("Candy", 0xFFFF5FA2, 0xFFFFA8CC, 0xFFC42F72, 0xFF11070D, 0xFF1D0D16, 0xFF2B1420),
        Palette("Glacier", 0xFF88C0E0, 0xFFC8E6F5, 0xFF5A94B8, 0xFF070C10, 0xFF0E161E, 0xFF16222C),
        Palette("Espresso", 0xFF8A6242, 0xFFC9A183, 0xFF5E3F26, 0xFF0A0705, 0xFF15100B, 0xFF201810),
        Palette("Royal Purple", 0xFF6A2FD6, 0xFFB48AF0, 0xFF4A1FA0, 0xFF090614, 0xFF120C24, 0xFF1C1234),
        Palette("Aurora Teal", 0xFF1FBFA8, 0xFF8CE8DA, 0xFF0F8A78, 0xFF04100D, 0xFF0A1C18, 0xFF102A24)
    )

    /** Aurora interface: deep, cool, glassy dark palettes. */
    val aurora = listOf(
        Palette("Aurora Violet", 0xFF8B7CFF, 0xFFC0B8FF, 0xFF5B4BD6, 0xFF0A0A1A, 0xFF14142B, 0xFF1E1E3D, 0xFFEDEBFF, 0xFF8C8AB0),
        Palette("Aurora Ocean", 0xFF35D0F5, 0xFF9BE9FB, 0xFF1493B8, 0xFF06111A, 0xFF0D1E2C, 0xFF152B3D, 0xFFE6F6FD, 0xFF7F9FB2),
        Palette("Aurora Mint", 0xFF3DE0B5, 0xFF9AF2DA, 0xFF16A381, 0xFF06130F, 0xFF0D211B, 0xFF143027, 0xFFE4F8F1, 0xFF7FA699),
        Palette("Aurora Magenta", 0xFFFF5FB4, 0xFFFFA8D8, 0xFFC02F7F, 0xFF14060F, 0xFF220D19, 0xFF311625, 0xFFFFE8F4, 0xFFB38399),
        Palette("Aurora Sunrise", 0xFFFFA24C, 0xFFFFCB94, 0xFFD0661A, 0xFF140A05, 0xFF22140B, 0xFF32200F, 0xFFFFEFDF, 0xFFB09478),
        Palette("Aurora Graphite", 0xFF9FB4FF, 0xFFD0DBFF, 0xFF6277D6, 0xFF0A0B0F, 0xFF15171E, 0xFF1F222C, 0xFFEDEFF7, 0xFF8A8F9E)
    )

    /** Nova interface: clean light palettes in the style of Google's own apps. */
    val nova = listOf(
        Palette("Nova Blue", 0xFF1A73E8, 0xFF8AB4F8, 0xFF1557B0, 0xFFFFFFFF, 0xFFF1F3F9, 0xFFE3E8F3, 0xFF1B1F2A, 0xFF5F6675, true),
        Palette("Nova Green", 0xFF188038, 0xFF81C995, 0xFF0D652D, 0xFFFBFDF9, 0xFFEFF5EE, 0xFFDFEBDD, 0xFF1B2A1E, 0xFF5E6E61, true),
        Palette("Nova Coral", 0xFFE8503A, 0xFFF4A094, 0xFFB93A27, 0xFFFFFBFA, 0xFFFBEFEC, 0xFFF4DFDA, 0xFF2D1D1A, 0xFF7A625E, true),
        Palette("Nova Purple", 0xFF7B3FE4, 0xFFC1A5F4, 0xFF5A24B8, 0xFFFDFBFF, 0xFFF4EFFB, 0xFFE8DFF6, 0xFF241B33, 0xFF6C6280, true),
        Palette("Nova Teal", 0xFF00897B, 0xFF7DCFC5, 0xFF00695C, 0xFFF9FDFC, 0xFFEBF5F3, 0xFFDAEBE8, 0xFF16302C, 0xFF58706C, true),
        Palette("Nova Slate", 0xFF37474F, 0xFF90A4AE, 0xFF263238, 0xFFFAFAFA, 0xFFF0F2F3, 0xFFE2E6E8, 0xFF1D2428, 0xFF627078, true)
    )

    /** Pulse interface: pure AMOLED black with one neon color. */
    val pulse = listOf(
        Palette("Pulse Lime", 0xFFC6FF3D, 0xFFE2FF8F, 0xFF8FC000, 0xFF000000, 0xFF0D0D0D, 0xFF1A1A1A, 0xFFEFEFEF, 0xFF7C7C7C),
        Palette("Pulse Cyan", 0xFF00F0FF, 0xFF8FF8FF, 0xFF00A8B5, 0xFF000000, 0xFF0A0F10, 0xFF132023, 0xFFEAFBFC, 0xFF6E8588),
        Palette("Pulse Pink", 0xFFFF2E93, 0xFFFF8FC4, 0xFFC4005F, 0xFF000000, 0xFF120A0E, 0xFF22121A, 0xFFFFEAF3, 0xFF8A6C79),
        Palette("Pulse Orange", 0xFFFF7A00, 0xFFFFB066, 0xFFC25A00, 0xFF000000, 0xFF120C06, 0xFF22160B, 0xFFFFF1E4, 0xFF8A7560),
        Palette("Pulse Violet", 0xFFB26BFF, 0xFFD6AEFF, 0xFF7F2FE0, 0xFF000000, 0xFF0E0A14, 0xFF1B1226, 0xFFF3EAFF, 0xFF7E7390),
        Palette("Pulse White", 0xFFFFFFFF, 0xFFFFFFFF, 0xFFBBBBBB, 0xFF000000, 0xFF0E0E0E, 0xFF1C1C1C, 0xFFFFFFFF, 0xFF808080)
    )

    /** Folio interface: warm paper light palettes, ink text. */
    val folio = listOf(
        Palette("Folio Terracotta", 0xFFC2410C, 0xFFE59A73, 0xFF8F2F06, 0xFFFAF6EF, 0xFFF1EADD, 0xFFE6DCCA, 0xFF2B2118, 0xFF7A6B58, true),
        Palette("Folio Sage", 0xFF4D7C5B, 0xFF9CC3A8, 0xFF356043, 0xFFF6F7F1, 0xFFECEFE4, 0xFFDEE4D2, 0xFF1F2A22, 0xFF65725F, true),
        Palette("Folio Ink", 0xFF1F3A5F, 0xFF8FA8C9, 0xFF14263F, 0xFFF7F7F5, 0xFFEDEDE8, 0xFFDEDED6, 0xFF1A1F29, 0xFF666B75, true),
        Palette("Folio Rose", 0xFFB4455B, 0xFFE0A0AD, 0xFF862F40, 0xFFFBF4F4, 0xFFF4E8E8, 0xFFEAD6D6, 0xFF2D1B1F, 0xFF7E6468, true),
        Palette("Folio Mustard", 0xFFB8860B, 0xFFE0C068, 0xFF8A6408, 0xFFFBF8EC, 0xFFF3EDD5, 0xFFE8DFBC, 0xFF2A2410, 0xFF7B7248, true),
        Palette("Folio Plum", 0xFF6B3E75, 0xFFB896C0, 0xFF4B2753, 0xFFF9F5FA, 0xFFF0E9F2, 0xFFE3D8E6, 0xFF271B2B, 0xFF706478, true)
    )

    /** 0 = Classic Gold, 1 = Aurora, 2 = Nova, 3 = Pulse, 4 = Folio. Set by Prefs. */
    var style by mutableStateOf(0)
    fun list(): List<Palette> = when (style) { 1 -> aurora; 2 -> nova; 3 -> pulse; 4 -> folio; else -> all }

    var current by mutableStateOf(all[0])
}

// Dynamic colors: every screen reads these, so changing the palette updates the whole app.
val Bg: Color get() = Color(Palettes.current.bg)
val Surface1: Color get() = Color(Palettes.current.s1)
val Surface2: Color get() = Color(Palettes.current.s2)
val Gold: Color get() = Color(Palettes.current.accent)
val GoldLight: Color get() = Color(Palettes.current.light)
val GoldDark: Color get() = Color(Palettes.current.dark)
val Ivory: Color get() = Color(Palettes.current.text)
val Muted: Color get() = Color(Palettes.current.muted)
