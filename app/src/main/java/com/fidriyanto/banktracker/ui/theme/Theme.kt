package com.fidriyanto.banktracker.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = DarkGold,
    onPrimary = DarkOnPrimary,
    // Container roles drive the FAB, selected chips and the nav indicator. Left undefined they
    // fall back to Material's default lilac, so they are pinned to pine tones here.
    primaryContainer = Color(0xFF16302A),
    onPrimaryContainer = Color(0xFF8FE0C8),
    secondary = DarkGold,
    onSecondary = DarkOnPrimary,
    secondaryContainer = Color(0xFF16302A),
    onSecondaryContainer = Color(0xFF8FE0C8),
    tertiary = Color(0xFFC8A96E),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVar,
    onSurface = DarkOnBackground,
    onSurfaceVariant = DarkOnSurfaceVar,
    outline = DarkOutline,
    outlineVariant = Color(0xFF2E3B34),
    // Surface-container roles: unset, Material derives lilac-tinted neutrals from its default seed.
    surfaceContainerLowest = Color(0xFF0B120F),
    surfaceContainerLow = Color(0xFF16201B),
    surfaceContainer = Color(0xFF1A241E),
    surfaceContainerHigh = Color(0xFF202B24),
    surfaceContainerHighest = Color(0xFF26332C),
    error = DarkError,
    errorContainer = Color(0xFF3A241F),
    onErrorContainer = Color(0xFFE88A7C),
)

// ac: light-pine-theme — both schemes define the full token set so text stays legible on either ground
private val LightColors = lightColorScheme(
    primary = LightGold,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFDCEBE4),
    onPrimaryContainer = Color(0xFF0B4638),
    secondary = LightGold,
    onSecondary = LightOnPrimary,
    secondaryContainer = Color(0xFFDCEBE4),
    onSecondaryContainer = Color(0xFF0B4638),
    tertiary = Color(0xFFB98D4E),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVar,
    onSurface = LightOnBackground,
    onSurfaceVariant = LightOnSurfaceVar,
    outline = LightOutline,
    outlineVariant = Color(0xFFC9D2CB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F6F2),
    surfaceContainer = Color(0xFFEFF2ED),
    surfaceContainerHigh = Color(0xFFE9EDE8),
    surfaceContainerHighest = Color(0xFFE3E8E2),
    error = LightError,
    errorContainer = Color(0xFFF6E4E0),
    onErrorContainer = Color(0xFFB23A2E),
)

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalThemeToggle = compositionLocalOf<() -> Unit> { {} }
val LocalIsDynamicColorEnabled = compositionLocalOf { false }
val LocalDynamicColorToggle = compositionLocalOf<() -> Unit> { {} }

private const val GROWTH_BAND_SHIFT = 0.35f

@Composable
fun BankTrackerTheme(
    darkTheme: Boolean = false,
    isDynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Dynamic color needs Android 12 (API 31). Older versions keep the pine scheme.
    val useDynamic = isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val baseColors = if (darkTheme) DarkAppColors else LightAppColors
    // The hero card and progress tracks follow the wallpaper colors. Gold, green, red, blue and
    // warning keep their meaning (saved, income, expense, info, caution), so they stay fixed.
    val appColors = if (useDynamic) {
        baseColors.copy(
            heroBg = colorScheme.primary,
            heroOn = colorScheme.onPrimary,
            progressTrack = colorScheme.surfaceContainerHighest,
            // ac: account-growth-chart — the growth bands follow the wallpaper too. Investment is the
            // tertiary colour shifted away from the checking band in lightness, so the two stay apart
            // for a wallpaper whose primary and tertiary are close in tone.
            growthChecking = colorScheme.primary,
            growthInvestment = lerp(colorScheme.tertiary, if (darkTheme) Color.Black else Color.White, GROWTH_BAND_SHIFT),
        )
    } else {
        baseColors
    }
    CompositionLocalProvider(LocalAppColors provides appColors) {
        // Shapes are passed too, so Card, Button and Chip round the same way as the surfaces
        // the screens build by hand. Left unset, Material uses its own radii and the two disagree.
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
