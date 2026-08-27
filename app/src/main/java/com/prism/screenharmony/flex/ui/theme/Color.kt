package com.prism.screenharmony.flex.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =========================================================================
// 1. BRAND COLOR PALETTES DEFINITIONS
// =========================================================================

enum class AppColorPalette(val label: String, val primaryColor: Color) {
    TEAL_SAGE("Teal Sage (#498783)", Color(0xFF498783)),
    OCEAN_BLUE("Ocean Blue", Color(0xFF2D6A9F)),
    EMERALD_GREEN("Emerald Green", Color(0xFF2E7D32)),
    SUNSET_CORAL("Sunset Coral", Color(0xFFC85A32)),
    LAVENDER_PURPLE("Lavender Purple", Color(0xFF7C5295)),
    ROSE_PINK("Rose Pink", Color(0xFFB84A62)),
    AMBER_GOLD("Amber Gold", Color(0xFFB8860B)),
    MATERIAL_YOU("Dynamic (System)", Color(0xFF6750A4))
}

// -------------------------------------------------------------------------
// Palette 1: TEAL SAGE (Brand Accent #498783)
// -------------------------------------------------------------------------
val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF246965),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7EAE6),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A6361),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E5),
    onSecondaryContainer = Color(0xFF051F1E),
    tertiary = Color(0xFF4A607B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E4FF),
    onTertiaryContainer = Color(0xFF031D34),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4F9F8),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4F9F8),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainer = Color(0xFFE8F2F0),
    surfaceContainerLow = Color(0xFFEEF6F4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE1ECE9),
    surfaceContainerHighest = Color(0xFFDAE5E3),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBFC9C7)
)

val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF82D5CF),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF004F4B),
    onPrimaryContainer = Color(0xFFC7EAE6),
    secondary = Color(0xFFB0CCC9),
    onSecondary = Color(0xFF1C3533),
    secondaryContainer = Color(0xFF324B4A),
    onSecondaryContainer = Color(0xFFCCE8E5),
    tertiary = Color(0xFFB2C8E8),
    onTertiary = Color(0xFF1B324B),
    tertiaryContainer = Color(0xFF324962),
    onTertiaryContainer = Color(0xFFD2E4FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDEE4E3),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDEE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBFC9C7),
    surfaceContainer = Color(0xFF161D1C),
    surfaceContainerLow = Color(0xFF121918),
    surfaceContainerLowest = Color(0xFF090E0E),
    surfaceContainerHigh = Color(0xFF1D2625),
    surfaceContainerHighest = Color(0xFF232E2D),
    outline = Color(0xFF899392),
    outlineVariant = Color(0xFF3F4948)
)

// -------------------------------------------------------------------------
// Palette 2: OCEAN BLUE (#2D6A9F)
// -------------------------------------------------------------------------
val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6097),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF526070),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E4F7),
    onSecondaryContainer = Color(0xFF0F1D2A),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF251432),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainer = Color(0xFFEBF1FA),
    surfaceContainerLow = Color(0xFFF1F6FD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE4EBF4),
    surfaceContainerHighest = Color(0xFFDEE5EE),
    outline = Color(0xFF72787E),
    outlineVariant = Color(0xFFC2C7CE)
)

val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF003355),
    primaryContainer = Color(0xFF004A78),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFFB9C8DA),
    onSecondary = Color(0xFF243240),
    secondaryContainer = Color(0xFF3B4857),
    onSecondaryContainer = Color(0xFFD5E4F7),
    tertiary = Color(0xFFD6BEE5),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E2E8),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE0E2E8),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CE),
    surfaceContainer = Color(0xFF181C20),
    surfaceContainerLow = Color(0xFF14181C),
    surfaceContainerLowest = Color(0xFF0B0F12),
    surfaceContainerHigh = Color(0xFF22262B),
    surfaceContainerHighest = Color(0xFF2D3136),
    outline = Color(0xFF8C9198),
    outlineVariant = Color(0xFF42474E)
)

// -------------------------------------------------------------------------
// Palette 3: EMERALD GREEN (#2E7D32)
// -------------------------------------------------------------------------
val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF216C2E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F5A7),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF526350),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8D0),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF39656B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF2),
    onTertiaryContainer = Color(0xFF001F23),
    background = Color(0xFFF7FBF2),
    onBackground = Color(0xFF181D17),
    surface = Color(0xFFF7FBF2),
    onSurface = Color(0xFF181D17),
    surfaceVariant = Color(0xFFDEE5DA),
    onSurfaceVariant = Color(0xFF424940),
    surfaceContainer = Color(0xFFEBF2E8),
    surfaceContainerLow = Color(0xFFF1F8EE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE5ECE2),
    surfaceContainerHighest = Color(0xFFDFE6DC),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD)
)

val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD88D),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF00531A),
    onPrimaryContainer = Color(0xFFA7F5A7),
    secondary = Color(0xFFB9CCB5),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3B4B39),
    onSecondaryContainer = Color(0xFFD5E8D0),
    tertiary = Color(0xFFA0CFD5),
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1F4D53),
    onTertiaryContainer = Color(0xFFBCEBF2),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    surfaceContainer = Color(0xFF181D18),
    surfaceContainerLow = Color(0xFF141914),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerHigh = Color(0xFF222822),
    surfaceContainerHighest = Color(0xFF2D332C),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940)
)

// -------------------------------------------------------------------------
// Palette 4: SUNSET CORAL (#C85A32)
// -------------------------------------------------------------------------
val CoralLightColorScheme = lightColorScheme(
    primary = Color(0xFFA23F17),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF77574B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF2C160D),
    tertiary = Color(0xFF6B5D2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E1A7),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231915),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231915),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    surfaceContainer = Color(0xFFFAEBE6),
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4E5E0),
    surfaceContainerHighest = Color(0xFFEEDFDB),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BB)
)

val CoralDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59B),
    onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF832902),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFE7BEAF),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFFFDBCE),
    tertiary = Color(0xFFD7C58D),
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF52451A),
    onTertiaryContainer = Color(0xFFF5E1A7),
    background = Color(0xFF1A110E),
    onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF1A110E),
    onSurface = Color(0xFFF1DFD9),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BB),
    surfaceContainer = Color(0xFF231713),
    surfaceContainerLow = Color(0xFF1E1310),
    surfaceContainerLowest = Color(0xFF140C09),
    surfaceContainerHigh = Color(0xFF2E211D),
    surfaceContainerHighest = Color(0xFF3A2C27),
    outline = Color(0xFFA08C86),
    outlineVariant = Color(0xFF53433E)
)

// -------------------------------------------------------------------------
// Palette 5: LAVENDER PURPLE (#7C5295)
// -------------------------------------------------------------------------
val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6E4389),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1DAFF),
    onPrimaryContainer = Color(0xFF290044),
    secondary = Color(0xFF66596F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEDBFA),
    onSecondaryContainer = Color(0xFF21172A),
    tertiary = Color(0xFF80515B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF321019),
    background = Color(0xFFFFF7FD),
    onBackground = Color(0xFF1E1A20),
    surface = Color(0xFFFFF7FD),
    onSurface = Color(0xFF1E1A20),
    surfaceVariant = Color(0xFFE9DFEB),
    onSurfaceVariant = Color(0xFF4A454E),
    surfaceContainer = Color(0xFFF6EBF8),
    surfaceContainerLow = Color(0xFFFCF1FD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0E5F2),
    surfaceContainerHighest = Color(0xFFEADFEC),
    outline = Color(0xFF7C757F),
    outlineVariant = Color(0xFFCCC4CF)
)

val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFDFB7FF),
    onPrimary = Color(0xFF3E1258),
    primaryContainer = Color(0xFF562B6F),
    onPrimaryContainer = Color(0xFFF1DAFF),
    secondary = Color(0xFFD2C1DC),
    onSecondary = Color(0xFF372C40),
    secondaryContainer = Color(0xFF4E4257),
    onSecondaryContainer = Color(0xFFEEDBFA),
    tertiary = Color(0xFFF2B7C2),
    onTertiary = Color(0xFF4B252E),
    tertiaryContainer = Color(0xFF653B44),
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = Color(0xFF161217),
    onBackground = Color(0xFFE8E0E8),
    surface = Color(0xFF161217),
    onSurface = Color(0xFFE8E0E8),
    surfaceVariant = Color(0xFF4A454E),
    onSurfaceVariant = Color(0xFFCCC4CF),
    surfaceContainer = Color(0xFF1F1A21),
    surfaceContainerLow = Color(0xFF1A161C),
    surfaceContainerLowest = Color(0xFF100C12),
    surfaceContainerHigh = Color(0xFF2A242B),
    surfaceContainerHighest = Color(0xFF352E36),
    outline = Color(0xFF968E99),
    outlineVariant = Color(0xFF4A454E)
)

// -------------------------------------------------------------------------
// Palette 6: ROSE PINK (#B84A62)
// -------------------------------------------------------------------------
val PinkLightColorScheme = lightColorScheme(
    primary = Color(0xFF973E54),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3E0016),
    secondary = Color(0xFF75565B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF2B1519),
    tertiary = Color(0xFF7A5733),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
    surfaceContainer = Color(0xFFF8EBEB),
    surfaceContainerLow = Color(0xFFFEF1F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2E5E6),
    surfaceContainerHighest = Color(0xFFECDCE0),
    outline = Color(0xFF847376),
    outlineVariant = Color(0xFFD6C2C4)
)

val PinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2BF),
    onPrimary = Color(0xFF5C1027),
    primaryContainer = Color(0xFF7A273D),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE4BDC2),
    onSecondary = Color(0xFF42292E),
    secondaryContainer = Color(0xFF5B3F44),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFECBE91),
    onTertiary = Color(0xFF462A0A),
    tertiaryContainer = Color(0xFF5F401E),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFE0E1),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFE0E1),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C4),
    surfaceContainer = Color(0xFF221719),
    surfaceContainerLow = Color(0xFF1D1315),
    surfaceContainerLowest = Color(0xFF130C0E),
    surfaceContainerHigh = Color(0xFF2C2123),
    surfaceContainerHighest = Color(0xFF372C2E),
    outline = Color(0xFF9E8C8F),
    outlineVariant = Color(0xFF524345)
)

// -------------------------------------------------------------------------
// Palette 7: AMBER GOLD (#B8860B)
// -------------------------------------------------------------------------
val AmberLightColorScheme = lightColorScheme(
    primary = Color(0xFF755B00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE082),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF695E40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E2BB),
    onSecondaryContainer = Color(0xFF231B04),
    tertiary = Color(0xFF466654),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8ECCF),
    onTertiaryContainer = Color(0xFF022114),
    background = Color(0xFFFFFAEE),
    onBackground = Color(0xFF1E1C13),
    surface = Color(0xFFFFFAEE),
    onSurface = Color(0xFF1E1C13),
    surfaceVariant = Color(0xFFECE1CF),
    onSurfaceVariant = Color(0xFF4D4639),
    surfaceContainer = Color(0xFFF5EEDF),
    surfaceContainerLow = Color(0xFFFBF4E5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEFE8D9),
    surfaceContainerHighest = Color(0xFFE9E2D4),
    outline = Color(0xFF7E7667),
    outlineVariant = Color(0xFFD0C5B4)
)

val AmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFECC248),
    onPrimary = Color(0xFF3E2E00),
    primaryContainer = Color(0xFF594400),
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = Color(0xFFD5C6A1),
    onSecondary = Color(0xFF393016),
    secondaryContainer = Color(0xFF50462A),
    onSecondaryContainer = Color(0xFFF2E2BB),
    tertiary = Color(0xFFACCFA8),
    onTertiary = Color(0xFF183728),
    tertiaryContainer = Color(0xFF2F4E3D),
    onTertiaryContainer = Color(0xFFC8ECCF),
    background = Color(0xFF16130B),
    onBackground = Color(0xFFE8E2D4),
    surface = Color(0xFF16130B),
    onSurface = Color(0xFFE8E2D4),
    surfaceVariant = Color(0xFF4D4639),
    onSurfaceVariant = Color(0xFFD0C5B4),
    surfaceContainer = Color(0xFF1E1A11),
    surfaceContainerLow = Color(0xFF1A160E),
    surfaceContainerLowest = Color(0xFF100D07),
    surfaceContainerHigh = Color(0xFF29241B),
    surfaceContainerHighest = Color(0xFF342E25),
    outline = Color(0xFF989080),
    outlineVariant = Color(0xFF4D4639)
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