package com.porarrirr.sumahohikakuku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = Color(0xFF001C35),
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = BrandTealContainer,
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = BrandAmber,
    onTertiary = Color.White,
    tertiaryContainer = BrandAmberContainer,
    onTertiaryContainer = Color(0xFF2D1800),
    surface = LightSurface,
    onSurface = Color(0xFF111927),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF425466),
    background = LightBackground,
    onBackground = Color(0xFF101828),
    outline = Color(0xFF6C7B8A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF002A4E),
    primaryContainer = Color(0xFF003C6E),
    onPrimaryContainer = Color(0xFFD7E8FF),
    secondary = Color(0xFF9ED7CC),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF1A554D),
    onSecondaryContainer = Color(0xFFB9F4E8),
    tertiary = Color(0xFFFFC86E),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3B00),
    onTertiaryContainer = Color(0xFFFFDDA8),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E9F3),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4C4D4),
    background = DarkBackground,
    onBackground = Color(0xFFE2E9F3),
    outline = Color(0xFF8696A8)
)

@Composable
fun SumahohikakukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
