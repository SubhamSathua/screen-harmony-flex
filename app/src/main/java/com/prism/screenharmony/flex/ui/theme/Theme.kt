package com.prism.screenharmony.flex.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

class ThemeState(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    isAmoled: Boolean = false,
    palette: AppColorPalette = AppColorPalette.TEAL_SAGE
) {
    var themeMode by mutableStateOf(themeMode)
    var isAmoled by mutableStateOf(isAmoled)
    var palette by mutableStateOf(palette)
}

val LocalThemeState = compositionLocalOf { ThemeState() }

@Composable
fun ScreenHarmonyFlexTheme(
    themeState: ThemeState = LocalThemeState.current,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeState.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val baseScheme: ColorScheme = when (themeState.palette) {
        AppColorPalette.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) TealDarkColorScheme else TealLightColorScheme
            }
        }
        AppColorPalette.TEAL_SAGE -> if (isDark) TealDarkColorScheme else TealLightColorScheme
        AppColorPalette.OCEAN_BLUE -> if (isDark) BlueDarkColorScheme else BlueLightColorScheme
        AppColorPalette.EMERALD_GREEN -> if (isDark) GreenDarkColorScheme else GreenLightColorScheme
        AppColorPalette.SUNSET_CORAL -> if (isDark) CoralDarkColorScheme else CoralLightColorScheme
        AppColorPalette.LAVENDER_PURPLE -> if (isDark) PurpleDarkColorScheme else PurpleLightColorScheme
        AppColorPalette.ROSE_PINK -> if (isDark) PinkDarkColorScheme else PinkLightColorScheme
        AppColorPalette.AMBER_GOLD -> if (isDark) AmberDarkColorScheme else AmberLightColorScheme
    }

    val finalColorScheme = if (isDark && themeState.isAmoled) {
        baseScheme.toAmoled()
    } else {
        baseScheme
    }

    // Synchronize system Status Bar and Navigation Bar icon contrast with active theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Light mode (!isDark = true)  -> Dark icons
                // Dark mode  (!isDark = false) -> Light icons
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalThemeState provides themeState) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            content = content
        )
    }
}