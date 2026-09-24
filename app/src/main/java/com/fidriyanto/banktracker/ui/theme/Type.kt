@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.fidriyanto.banktracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R

// Fonts are bundled as variable TrueType files under res/font. Each weight selects the
// wght axis through FontVariation so the one file serves every weight. This avoids the
// Google downloadable-fonts provider, whose on-device catalog does not always carry
// Fraunces or Hanken Grotesk, which made both fall back to the system sans.

private fun hanken(weight: FontWeight, wght: Int) = Font(
    R.font.hanken_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(wght)),
)

private fun fraunces(weight: FontWeight, wght: Int) = Font(
    R.font.fraunces,
    weight = weight,
    // opsz 40 gives the high-contrast display look the mockup uses for large figures.
    variationSettings = FontVariation.Settings(
        FontVariation.weight(wght),
        FontVariation.Setting("opsz", 40f),
    ),
)

// Body / UI text.
val HankenGrotesk = FontFamily(
    hanken(FontWeight.Normal, 400),
    hanken(FontWeight.Medium, 500),
    hanken(FontWeight.SemiBold, 600),
    hanken(FontWeight.Bold, 700),
)

// Display / headings and large figures.
val Fraunces = FontFamily(
    fraunces(FontWeight.Medium, 500),
    fraunces(FontWeight.SemiBold, 600),
    fraunces(FontWeight.Bold, 700),
)

val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    displaySmall  = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    headlineLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineMedium= TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge  = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge   = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall   = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
