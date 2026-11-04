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

// FindPal brand palette (docs/brand). Dark is the signature look: Night background, cocoa cards,
// cream text and Pal Orange. Light keeps the same warmth on a cream base.
private val LightColors = lightColorScheme(
    primary = Color(0xFFD9572A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCAE),
    onPrimaryContainer = Color(0xFF3A1A06),
    secondary = Color(0xFF6E5039),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8C2),
    onSecondaryContainer = Color(0xFF1D140E),
    tertiary = Color(0xFF2E7D4F),
    tertiaryContainer = Color(0xFFD3F0DD),
    onTertiaryContainer = Color(0xFF0B2A17),
    background = Color(0xFFFFF6E6),
    onBackground = Color(0xFF1D140E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D140E),
    surfaceVariant = Color(0xFFFFE8C2),
    onSurfaceVariant = Color(0xFF6E5039),
    outlineVariant = Color(0xFFF0D9B4),
    inverseSurface = Color(0xFF120D09),
    inverseOnSurface = Color(0xFFFFF1DC),
    inversePrimary = Color(0xFFFFC48A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF07A2E),
    onPrimary = Color(0xFF1D140E),
    primaryContainer = Color(0xFF3A2414),
    onPrimaryContainer = Color(0xFFFFE8C2),
    secondary = Color(0xFFFFC48A),
    onSecondary = Color(0xFF1D140E),
    secondaryContainer = Color(0xFF2A1D14),
    onSecondaryContainer = Color(0xFFFFE8C2),
    tertiary = Color(0xFF7ACC5A),
    tertiaryContainer = Color(0xFF1F3D2C),
    onTertiaryContainer = Color(0xFFD3F0DD),
    background = Color(0xFF120D09),
    onBackground = Color(0xFFFFF1DC),
    surface = Color(0xFF1F160F),
    onSurface = Color(0xFFFFF1DC),
    surfaceVariant = Color(0xFF2A1D14),
    onSurfaceVariant = Color(0xFFBFAE98),
    outlineVariant = Color(0xFF3A2A1D),
    // The top hit is a cream card on the dark screen, like the website's cream bands.
    inverseSurface = Color(0xFFFFE8C2),
    inverseOnSurface = Color(0xFF1D140E),
    inversePrimary = Color(0xFFD9572A),
)

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
