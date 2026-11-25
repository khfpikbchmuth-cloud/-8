package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = Color(0xFF1E1600),
    primaryContainer = RoyalGoldDark,
    onPrimaryContainer = RoyalGold,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF001E2B),
    secondaryContainer = ElectricCyanDark,
    onSecondaryContainer = ElectricCyan,
    tertiary = IrisViolet,
    onTertiary = Color(0xFF101338),
    tertiaryContainer = IrisVioletDark,
    onTertiaryContainer = IrisViolet,
    background = ObsidianBackground,
    onBackground = TextOnDarkPrimary,
    surface = ObsidianSurface,
    onSurface = TextOnDarkPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextOnDarkSecondary,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    error = ErrorDark,
    onError = OnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = AmberCrown,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = IvoryBackground,
    onBackground = TextOnLightPrimary,
    surface = IvorySurface,
    onSurface = TextOnLightPrimary,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = TextOnLightSecondary,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = IvoryOutline,
    error = ErrorLight,
    onError = Color.White
)

private val SamiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemePreference {
    DARK,
    LIGHT,
    SYSTEM
}

@Composable
fun SamiKingTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SamiShapes,
        content = content
    )
}
