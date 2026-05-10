package com.fidriyanto.banktracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onBackground = OnPrimary,
    onSurface = OnPrimary,
    error = Destructive,
)

@Composable
fun BankTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography, content = content)
}
