package com.fidriyanto.banktracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

private val DarkColors = darkColorScheme(
    primary = DarkGold,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVar,
    onSurface = DarkOnBackground,
    onSurfaceVariant = DarkOnSurfaceVar,
    outline = DarkOutline,
    error = DarkError,
)

private val LightColors = lightColorScheme(
    primary = LightGold,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVar,
    onSurface = LightOnBackground,
    onSurfaceVariant = LightOnSurfaceVar,
    outline = LightOutline,
    error = LightError,
)

val LocalIsDarkTheme = compositionLocalOf { true }
val LocalThemeToggle = compositionLocalOf<() -> Unit> { {} }

@Composable
fun BankTrackerTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
