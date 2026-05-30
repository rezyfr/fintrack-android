package com.fidriyanto.banktracker.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val gold: Color,
    val green: Color,
    val red: Color,
    val blue: Color,
    val warning: Color,
    val progressTrack: Color,
)

internal val DarkAppColors = AppColors(
    gold = Color(0xFFC8A96E),
    green = Color(0xFF58C884),
    red = Color(0xFFE86464),
    blue = Color(0xFF6B9EEE),
    warning = Color(0xFFF4A261),
    progressTrack = Color(0xFF1E1E28),
)

internal val LightAppColors = AppColors(
    gold = Color(0xFF9A7240),
    green = Color(0xFF1A7A42),
    red = Color(0xFFC23636),
    blue = Color(0xFF2155C4),
    warning = Color(0xFFC47010),
    progressTrack = Color(0xFFDCD8D0),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
