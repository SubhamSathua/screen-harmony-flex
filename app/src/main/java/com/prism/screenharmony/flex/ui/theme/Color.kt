package com.prism.screenharmony.flex.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =========================================================================
// 1. BRAND COLOR PALETTES DEFINITIONS
// =========================================================================

enum class AppColorPalette(val label: String, val primaryColor: Color) {
    MONOCHROME("Monochrome", Color(0xFF555555)),
    PINK("Pink", Color(0xFFE91E63)),
    ROSE("Rose", Color(0xFFC2185B)),
    RED("Red", Color(0xFFB3261E)),
    ORANGE("Orange", Color(0xFFE65100)),
    YELLOW("Yellow", Color(0xFFFBC02D)),
    CHARTREUSE("Chartreuse", Color(0xFF7CB342)),
    GREEN("Green", Color(0xFF2E7D32)),
    TEAL("Teal", Color(0xFF00897B)),
    CYAN("Cyan", Color(0xFF00838F)),
    BLUE("Blue", Color(0xFF0B57D0)),
    INDIGO("Indigo", Color(0xFF3949AB)),
    PURPLE("Purple", Color(0xFF6750A4)),
    VIOLET("Violet", Color(0xFF8E24AA)),
    MAGENTA("Magenta", Color(0xFFC2185B)),
    MATERIAL_YOU("Dynamic (System)", Color(0xFF6750A4))
}

// -------------------------------------------------------------------------
// 1. MONOCHROME
// -------------------------------------------------------------------------
val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF141414),
    secondary = Color(0xFF5E5E5E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1C1C1C),
    tertiary = Color(0xFF4A4A4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6D6D6),
    onTertiaryContainer = Color(0xFF101010),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF474747),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE5E5E5),
    surfaceContainerHighest = Color(0xFFDDDDDD),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFC7C7C7)
)

val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF1C1C1C),
    primaryContainer = Color(0xFF3D3D3D),
    onPrimaryContainer = Color(0xFFEAEAEA),
    secondary = Color(0xFFC4C4C4),
    onSecondary = Color(0xFF2B2B2B),
    secondaryContainer = Color(0xFF424242),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFD0D0D0),
    onTertiary = Color(0xFF262626),
    tertiaryContainer = Color(0xFF4A4A4A),
    onTertiaryContainer = Color(0xFFF0F0F0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF474747),
    onSurfaceVariant = Color(0xFFC7C7C7),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF474747)
)

// -------------------------------------------------------------------------
// 2. PINK
// -------------------------------------------------------------------------
val PinkLightColorScheme = lightColorScheme(
    primary = Color(0xFF984061),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF201A1B),
    surfaceContainer = Color(0xFFF9EBEF),
    surfaceContainerHigh = Color(0xFFF3E5E9),
    surfaceContainerHighest = Color(0xFFEDDFE3),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6)
)

val PinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFE0E2),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFE0E2),
    surfaceContainer = Color(0xFF22171A),
    surfaceContainerHigh = Color(0xFF2D2124),
    surfaceContainerHighest = Color(0xFF382C2E),
    outline = Color(0xFF9D8C90),
    outlineVariant = Color(0xFF514347)
)

// -------------------------------------------------------------------------
// 3. ROSE
// -------------------------------------------------------------------------
val RoseLightColorScheme = lightColorScheme(
    primary = Color(0xFF9C3A5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E4),
    onPrimaryContainer = Color(0xFF3F001B),
    secondary = Color(0xFF75565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E3),
    onSecondaryContainer = Color(0xFF2C151C),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF21191B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF21191B),
    surfaceContainer = Color(0xFFF9EAEF),
    surfaceContainerHigh = Color(0xFFF3E4E9),
    surfaceContainerHighest = Color(0xFFEDDEE3),
    outline = Color(0xFF847377),
    outlineVariant = Color(0xFFD6C2C6)
)

val RoseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFAFCE),
    onPrimary = Color(0xFF60092D),
    primaryContainer = Color(0xFF7F2243),
    onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFE4BDC7),
    onSecondary = Color(0xFF432931),
    secondaryContainer = Color(0xFF5B3F48),
    onSecondaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF1A1114),
    onBackground = Color(0xFFF0DFE2),
    surface = Color(0xFF1A1114),
    onSurface = Color(0xFFF0DFE2),
    surfaceContainer = Color(0xFF23171A),
    surfaceContainerHigh = Color(0xFF2E2124),
    surfaceContainerHighest = Color(0xFF392C2F),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF524347)
)

// -------------------------------------------------------------------------
// 4. RED
// -------------------------------------------------------------------------
val RedLightColorScheme = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF9DEDC),
    onPrimaryContainer = Color(0xFF410E0B),
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    background = Color(0xFFFEF7F6),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFEF7F6),
    onSurface = Color(0xFF231918),
    surfaceContainer = Color(0xFFF9EAE8),
    surfaceContainerHigh = Color(0xFFF3E4E2),
    surfaceContainerHighest = Color(0xFFEDDEDC),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF)
)

val RedDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2B8B5),
    onPrimary = Color(0xFF601410),
    primaryContainer = Color(0xFF8C1D18),
    onPrimaryContainer = Color(0xFFF9DEDC),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDD),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDD),
    surfaceContainer = Color(0xFF231716),
    surfaceContainerHigh = Color(0xFF2E2120),
    surfaceContainerHighest = Color(0xFF392C2A),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341)
)

// -------------------------------------------------------------------------
// 5. ORANGE
// -------------------------------------------------------------------------
val OrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFFA04000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF77574E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF2C160F),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231A17),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231A17),
    surfaceContainer = Color(0xFFFAECE7),
    surfaceContainerHigh = Color(0xFFF4E6E1),
    surfaceContainerHighest = Color(0xFFEFE0DC),
    outline = Color(0xFF85736E),
    outlineVariant = Color(0xFFD8C2BC)
)

val OrangeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5B1B00),
    primaryContainer = Color(0xFF813100),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFE7BEAF),
    onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFDBCE),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF1A110E),
    onSurface = Color(0xFFF1DFD9),
    surfaceContainer = Color(0xFF231713),
    surfaceContainerHigh = Color(0xFF2E211D),
    surfaceContainerHighest = Color(0xFF3A2B27),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F)
)

// -------------------------------------------------------------------------
// 6. YELLOW
// -------------------------------------------------------------------------
val YellowLightColorScheme = lightColorScheme(
    primary = Color(0xFF765B00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDF96),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF6A5D3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E1BB),
    onSecondaryContainer = Color(0xFF231B04),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF1E1C14),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF1E1C14),
    surfaceContainer = Color(0xFFF6EEDF),
    surfaceContainerHigh = Color(0xFFF0E8D9),
    surfaceContainerHighest = Color(0xFFEAE2D3),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD1C5B4)
)

val YellowDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8C349),
    onPrimary = Color(0xFF3E2E00),
    primaryContainer = Color(0xFF594300),
    onPrimaryContainer = Color(0xFFFFDF96),
    secondary = Color(0xFFD6C5A0),
    onSecondary = Color(0xFF3A2F15),
    secondaryContainer = Color(0xFF51452A),
    onSecondaryContainer = Color(0xFFF3E1BB),
    background = Color(0xFF171309),
    onBackground = Color(0xFFE9E2D4),
    surface = Color(0xFF171309),
    onSurface = Color(0xFFE9E2D4),
    surfaceContainer = Color(0xFF1F1A0F),
    surfaceContainerHigh = Color(0xFF2A2419),
    surfaceContainerHighest = Color(0xFF352F23),
    outline = Color(0xFF999080),
    outlineVariant = Color(0xFF4D4639)
)

// -------------------------------------------------------------------------
// 7. CHARTREUSE
// -------------------------------------------------------------------------
val ChartreuseLightColorScheme = lightColorScheme(
    primary = Color(0xFF566500),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7EC6E),
    onPrimaryContainer = Color(0xFF181E00),
    secondary = Color(0xFF5F6146),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E6C3),
    onSecondaryContainer = Color(0xFF1C1E08),
    background = Color(0xFFFAFAE6),
    onBackground = Color(0xFF1B1D12),
    surface = Color(0xFFFAFAE6),
    onSurface = Color(0xFF1B1D12),
    surfaceContainer = Color(0xFFF1F2DC),
    surfaceContainerHigh = Color(0xFFEBECD6),
    surfaceContainerHighest = Color(0xFFE5E6D0),
    outline = Color(0xFF787968),
    outlineVariant = Color(0xFFC8C8B4)
)

val ChartreuseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBBD054),
    onPrimary = Color(0xFF2C3400),
    primaryContainer = Color(0xFF3F4C00),
    onPrimaryContainer = Color(0xFFD7EC6E),
    secondary = Color(0xFFC8CAA8),
    onSecondary = Color(0xFF31331B),
    secondaryContainer = Color(0xFF474A30),
    onSecondaryContainer = Color(0xFFE5E6C3),
    background = Color(0xFF131508),
    onBackground = Color(0xFFE5E6D9),
    surface = Color(0xFF131508),
    onSurface = Color(0xFFE5E6D9),
    surfaceContainer = Color(0xFF1B1D0E),
    surfaceContainerHigh = Color(0xFF262818),
    surfaceContainerHighest = Color(0xFF313322),
    outline = Color(0xFF929381),
    outlineVariant = Color(0xFF474838)
)

// -------------------------------------------------------------------------
// 8. GREEN
// -------------------------------------------------------------------------
val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF236B2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F4A4),
    onPrimaryContainer = Color(0xFF002105),
    secondary = Color(0xFF526350),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8D1),
    onSecondaryContainer = Color(0xFF101F10),
    background = Color(0xFFF7FBF2),
    onBackground = Color(0xFF181D17),
    surface = Color(0xFFF7FBF2),
    onSurface = Color(0xFF181D17),
    surfaceContainer = Color(0xFFEBF2E8),
    surfaceContainerHigh = Color(0xFFE5ECE2),
    surfaceContainerHighest = Color(0xFFDFE6DC),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD)
)

val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD78A),
    onPrimary = Color(0xFF003A0F),
    primaryContainer = Color(0xFF045316),
    onPrimaryContainer = Color(0xFFA7F4A4),
    secondary = Color(0xFFB9CCB5),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3B4B39),
    onSecondaryContainer = Color(0xFFD5E8D1),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DE),
    surfaceContainer = Color(0xFF181D18),
    surfaceContainerHigh = Color(0xFF222822),
    surfaceContainerHighest = Color(0xFF2D332C),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940)
)

// -------------------------------------------------------------------------
// 9. TEAL
// -------------------------------------------------------------------------
val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF73F8E7),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF05201C),
    background = Color(0xFFF4FBFA),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4FBFA),
    onSurface = Color(0xFF161D1C),
    surfaceContainer = Color(0xFFE8F2F0),
    surfaceContainerHigh = Color(0xFFE1ECE9),
    surfaceContainerHighest = Color(0xFFDAE5E3),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBFC9C6)
)

val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF53DBCB),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF73F8E7),
    secondary = Color(0xFFB0CCC7),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E3),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDEE4E2),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDEE4E2),
    surfaceContainer = Color(0xFF161D1C),
    surfaceContainerHigh = Color(0xFF1D2625),
    surfaceContainerHighest = Color(0xFF232E2D),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4947)
)

// -------------------------------------------------------------------------
// 10. CYAN
// -------------------------------------------------------------------------
val CyanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00677D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4EBFF),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF4C626A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F0),
    onSecondaryContainer = Color(0xFF071E26),
    background = Color(0xFFF5FAFD),
    onBackground = Color(0xFF171D1F),
    surface = Color(0xFFF5FAFD),
    onSurface = Color(0xFF171D1F),
    surfaceContainer = Color(0xFFE9F1F5),
    surfaceContainerHigh = Color(0xFFE2EBEF),
    surfaceContainerHighest = Color(0xFFDCE5E9),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFBFC8CC)
)

val CyanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BD5F6),
    onPrimary = Color(0xFF003542),
    primaryContainer = Color(0xFF004E60),
    onPrimaryContainer = Color(0xFFB4EBFF),
    secondary = Color(0xFFB3CAD4),
    onSecondary = Color(0xFF1E333B),
    secondaryContainer = Color(0xFF344A52),
    onSecondaryContainer = Color(0xFFCFE6F0),
    background = Color(0xFF0E1417),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0E1417),
    onSurface = Color(0xFFDEE3E6),
    surfaceContainer = Color(0xFF171D1F),
    surfaceContainerHigh = Color(0xFF1F2528),
    surfaceContainerHighest = Color(0xFF2A3033),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C)
)

// -------------------------------------------------------------------------
// 11. BLUE
// -------------------------------------------------------------------------
val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceContainer = Color(0xFFECF0F9),
    surfaceContainerHigh = Color(0xFFE6EAF3),
    surfaceContainerHighest = Color(0xFFE0E4ED),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF)
)

val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253141),
    secondaryContainer = Color(0xFF3C4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceContainer = Color(0xFF191C20),
    surfaceContainerHigh = Color(0xFF23262B),
    surfaceContainerHighest = Color(0xFF2E3136),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E)
)

// -------------------------------------------------------------------------
// 12. INDIGO
// -------------------------------------------------------------------------
val IndigoLightColorScheme = lightColorScheme(
    primary = Color(0xFF3E4DB8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF000C62),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1A1B23),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1A1B23),
    surfaceContainer = Color(0xFFEFEFFB),
    surfaceContainerHigh = Color(0xFFE9E9F5),
    surfaceContainerHighest = Color(0xFFE3E3EF),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC7C5D0)
)

val IndigoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBDC2FF),
    onPrimary = Color(0xFF001B98),
    primaryContainer = Color(0xFF2333A0),
    onPrimaryContainer = Color(0xFFDFE0FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF43455A),
    onSecondaryContainer = Color(0xFFE0E1F9),
    background = Color(0xFF12121A),
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF12121A),
    onSurface = Color(0xFFE4E1EC),
    surfaceContainer = Color(0xFF1B1B23),
    surfaceContainerHigh = Color(0xFF25252D),
    surfaceContainerHighest = Color(0xFF303038),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F)
)

// -------------------------------------------------------------------------
// 13. PURPLE
// -------------------------------------------------------------------------
val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceContainer = Color(0xFF1D1B20),
    surfaceContainerHigh = Color(0xFF28252E),
    surfaceContainerHighest = Color(0xFF332F37),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// -------------------------------------------------------------------------
// 14. VIOLET
// -------------------------------------------------------------------------
val VioletLightColorScheme = lightColorScheme(
    primary = Color(0xFF763FA5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2DAFF),
    onPrimaryContainer = Color(0xFF2D004F),
    secondary = Color(0xFF67586F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFDCF6),
    onSecondaryContainer = Color(0xFF22162A),
    background = Color(0xFFFFF7FD),
    onBackground = Color(0xFF1E1A20),
    surface = Color(0xFFFFF7FD),
    onSurface = Color(0xFF1E1A20),
    surfaceContainer = Color(0xFFF6EBF8),
    surfaceContainerHigh = Color(0xFFF0E5F2),
    surfaceContainerHighest = Color(0xFFEADFEC),
    outline = Color(0xFF7C757F),
    outlineVariant = Color(0xFFCCC4CF)
)

val VioletDarkColorScheme = darkColorScheme(
    primary = Color(0xFFDFB7FF),
    onPrimary = Color(0xFF450674),
    primaryContainer = Color(0xFF5D258B),
    onPrimaryContainer = Color(0xFFF2DAFF),
    secondary = Color(0xFFD2C1DA),
    onSecondary = Color(0xFF382B40),
    secondaryContainer = Color(0xFF4F4157),
    onSecondaryContainer = Color(0xFFEFDCF6),
    background = Color(0xFF161118),
    onBackground = Color(0xFFE8E0E8),
    surface = Color(0xFF161118),
    onSurface = Color(0xFFE8E0E8),
    surfaceContainer = Color(0xFF1F1A21),
    surfaceContainerHigh = Color(0xFF2A242C),
    surfaceContainerHighest = Color(0xFF352E37),
    outline = Color(0xFF968E99),
    outlineVariant = Color(0xFF4A454E)
)

// -------------------------------------------------------------------------
// 15. MAGENTA
// -------------------------------------------------------------------------
val MagentaLightColorScheme = lightColorScheme(
    primary = Color(0xFF933B85),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD7F4),
    onPrimaryContainer = Color(0xFF390035),
    secondary = Color(0xFF71576A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCD9F0),
    onSecondaryContainer = Color(0xFF291525),
    background = Color(0xFFFFF7FA),
    onBackground = Color(0xFF201A1F),
    surface = Color(0xFFFFF7FA),
    onSurface = Color(0xFF201A1F),
    surfaceContainer = Color(0xFFF8EBEF),
    surfaceContainerHigh = Color(0xFFF2E5EC),
    surfaceContainerHighest = Color(0xFFECDCE6),
    outline = Color(0xFF82737E),
    outlineVariant = Color(0xFFD3C2CD)
)

val MagentaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFAFD9),
    onPrimary = Color(0xFF5B0853),
    primaryContainer = Color(0xFF77216C),
    onPrimaryContainer = Color(0xFFFFD7F4),
    secondary = Color(0xFFDFBDD4),
    onSecondary = Color(0xFF402A3B),
    secondaryContainer = Color(0xFF584052),
    onSecondaryContainer = Color(0xFFFCD9F0),
    background = Color(0xFF191118),
    onBackground = Color(0xFFEFE0E8),
    surface = Color(0xFF191118),
    onSurface = Color(0xFFEFE0E8),
    surfaceContainer = Color(0xFF221721),
    surfaceContainerHigh = Color(0xFF2D212B),
    surfaceContainerHighest = Color(0xFF382C36),
    outline = Color(0xFF9C8C97),
    outlineVariant = Color(0xFF4F434D)
)

// -------------------------------------------------------------------------
// Helper: AMOLED Black Theme Transformation
// -------------------------------------------------------------------------
fun ColorScheme.toAmoled(): ColorScheme {
    return this.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerHigh = Color(0xFF121212),
        surfaceContainerHighest = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFF181818)
    )
}