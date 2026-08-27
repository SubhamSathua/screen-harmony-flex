package com.prism.screenharmony.flex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// 1. Nunito - Whole App Font
val nunitoFontName = GoogleFont("Nunito")
val NunitoFontFamily = FontFamily(
    Font(googleFont = nunitoFontName, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = nunitoFontName, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = nunitoFontName, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoFontName, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = nunitoFontName, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

// 2. Playfair Display - Branding & Logo Font
val playfairFontName = GoogleFont("Playfair Display")
val PlayfairFontFamily = FontFamily(
    Font(googleFont = playfairFontName, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = playfairFontName, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = playfairFontName, fontProvider = fontProvider, weight = FontWeight.Black)
)

// 3. JetBrains Mono - Numbers, Version Codes, Date & Times
val jetbrainsMonoFontName = GoogleFont("JetBrains Mono")
val JetBrainsMonoFontFamily = FontFamily(
    Font(googleFont = jetbrainsMonoFontName, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = jetbrainsMonoFontName, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = jetbrainsMonoFontName, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// App Typography configured with Nunito as default
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)