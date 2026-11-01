package com.quickfind.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Mirrors core:data ThemeMode without depending on it. */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

// "Aurora" brand palette: deep violet with magenta and cyan accents.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6A3DF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFFC0369E),
    secondaryContainer = Color(0xFFFFD8EE),
    onSecondaryContainer = Color(0xFF3C0030),
    tertiary = Color(0xFF00869B),
    tertiaryContainer = Color(0xFFC7F5FF),
    onTertiaryContainer = Color(0xFF001F25),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFEDE6F7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC6B2FF),
    onPrimary = Color(0xFF34148C),
    primaryContainer = Color(0xFF4B2BB8),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFFFAEE2),
    secondaryContainer = Color(0xFF5E1A4D),
    onSecondaryContainer = Color(0xFFFFD8EE),
    tertiary = Color(0xFF7FE3F5),
    tertiaryContainer = Color(0xFF004F5C),
    onTertiaryContainer = Color(0xFFC7F5FF),
    background = Color(0xFF0E0B16),
    surface = Color(0xFF14111D),
    surfaceVariant = Color(0xFF2A2536),
    onSurfaceVariant = Color(0xFFCBC3DA),
)

/** Material 3 with dynamic color on Android 12+ and dark mode (spec 7). */
@Composable
fun QuickFindTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
