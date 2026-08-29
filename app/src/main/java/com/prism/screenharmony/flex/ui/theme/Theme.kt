package com.prism.screenharmony.flex.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
    palette: AppColorPalette = AppColorPalette.TEAL,
    var onStateChanged: ((ThemeState) -> Unit)? = null
) {
    private var _themeMode by mutableStateOf(themeMode)
    var themeMode: AppThemeMode
        get() = _themeMode
        set(value) {
            _themeMode = value
            onStateChanged?.invoke(this)
        }

    private var _isAmoled by mutableStateOf(isAmoled)
    var isAmoled: Boolean
        get() = _isAmoled
        set(value) {
            _isAmoled = value
            onStateChanged?.invoke(this)
        }

    private var _palette by mutableStateOf(palette)
    var palette: AppColorPalette
        get() = _palette
        set(value) {
            _palette = value
            onStateChanged?.invoke(this)
        }
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
        AppColorPalette.MONOCHROME -> if (isDark) MonochromeDarkColorScheme else MonochromeLightColorScheme
        AppColorPalette.PINK -> if (isDark) PinkDarkColorScheme else PinkLightColorScheme
        AppColorPalette.ROSE -> if (isDark) RoseDarkColorScheme else RoseLightColorScheme
        AppColorPalette.RED -> if (isDark) RedDarkColorScheme else RedLightColorScheme
        AppColorPalette.ORANGE -> if (isDark) OrangeDarkColorScheme else OrangeLightColorScheme
        AppColorPalette.YELLOW -> if (isDark) YellowDarkColorScheme else YellowLightColorScheme
        AppColorPalette.CHARTREUSE -> if (isDark) ChartreuseDarkColorScheme else ChartreuseLightColorScheme
        AppColorPalette.GREEN -> if (isDark) GreenDarkColorScheme else GreenLightColorScheme
        AppColorPalette.TEAL -> if (isDark) TealDarkColorScheme else TealLightColorScheme
        AppColorPalette.CYAN -> if (isDark) CyanDarkColorScheme else CyanLightColorScheme
        AppColorPalette.BLUE -> if (isDark) BlueDarkColorScheme else BlueLightColorScheme
        AppColorPalette.INDIGO -> if (isDark) IndigoDarkColorScheme else IndigoLightColorScheme
        AppColorPalette.PURPLE -> if (isDark) PurpleDarkColorScheme else PurpleLightColorScheme
        AppColorPalette.VIOLET -> if (isDark) VioletDarkColorScheme else VioletLightColorScheme
        AppColorPalette.MAGENTA -> if (isDark) MagentaDarkColorScheme else MagentaLightColorScheme
    }

    val finalColorScheme = if (isDark && themeState.isAmoled) {
        baseScheme.toAmoled()
    } else {
        baseScheme
    }

    // Dynamic Status Bar and Navigation Bar icon contrast
    // Light Mode -> Dark Icons (!isDark = true)
    // Dark Mode  -> Light Icons (!isDark = false)
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(isDark) {
            val activity = view.context as? ComponentActivity
            if (activity != null) {
                activity.enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
            onDispose {}
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