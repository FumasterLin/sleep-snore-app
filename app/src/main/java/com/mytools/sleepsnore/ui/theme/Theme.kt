package com.mytools.sleepsnore.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SleepSnoreTheme(
    themeMode: String = "dark", // "dark", "light", "system"
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light" -> false
        "system" -> systemInDark
        else -> true // default dark
    }

    val colors = if (isDark) DarkAppColors else LightAppColors

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.primaryAccent,
            secondary = colors.secondaryAccent,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceVariant,
            onPrimary = Color.Black,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.primaryAccent,
            secondary = colors.secondaryAccent,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceVariant,
            onPrimary = Color.White,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity
            val window = (view.context as Activity).window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            activity?.enableEdgeToEdge(
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
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !isDark
            insets.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
