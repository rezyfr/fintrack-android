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
    val heroBg: Color,
    val heroOn: Color,
    val heroOver: Color,
    val heroOverBg: Color,
    // ac: account-growth-chart — the two bands of the growth chart. They differ in lightness, not in hue alone.
    val growthChecking: Color,
    val growthInvestment: Color,
)

// gold is demoted to a small "saved / kept" accent; green = income, red = expense (clay).
internal val DarkAppColors = AppColors(
    gold = Color(0xFFC8A96E),
    green = Color(0xFF5FC0A5),
    red = Color(0xFFE88A7C),
    blue = Color(0xFF6B9EEE),
    warning = Color(0xFFD9B256),
    progressTrack = Color(0xFF26332C),
    heroBg = Color(0xFF0F5D4C),
    heroOn = Color(0xFFF3FAF6),
    // the expense red is as dark as the pine ground, so a problem on the hero uses a light coral
    heroOver = Color(0xFFFFB4A6),
    heroOverBg = Color(0xFF6B2016),
    growthChecking = Color(0xFF2F8F78),
    growthInvestment = Color(0xFFE2C27B),
)

internal val LightAppColors = AppColors(
    gold = Color(0xFFB98D4E),
    green = Color(0xFF1C7A57),
    red = Color(0xFFB23A2E),
    blue = Color(0xFF2155C4),
    warning = Color(0xFF9A6B12),
    progressTrack = Color(0xFFDDE3DE),
    heroBg = Color(0xFF0F5D4C),
    heroOn = Color(0xFFF3FAF6),
    heroOver = Color(0xFFFFB4A6),
    heroOverBg = Color(0xFF6B2016),
    growthChecking = Color(0xFF0F5D4C),
    growthInvestment = Color(0xFFE2C27B),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
