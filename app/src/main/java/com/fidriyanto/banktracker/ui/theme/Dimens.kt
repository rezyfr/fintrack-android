package com.fidriyanto.banktracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Spacing and corner tokens. Every gap, inset and corner in the UI comes from here, so the
// screens share one rhythm. Before these existed the app wrote 11 gap values, 9 inset values
// and 13 corner radii as literals, and no two screens agreed.
//
// Element sizes are not spacing. A 56dp button or a 6dp progress bar keeps its own number.

object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object AppShape {
    val xs = RoundedCornerShape(4.dp) // swatches and small marks
    val sm = RoundedCornerShape(8.dp) // inline chips
    val md = RoundedCornerShape(12.dp) // compact cards, toggles, avatars
    val lg = RoundedCornerShape(16.dp) // the standard card
    val xl = RoundedCornerShape(20.dp) // hero cards and sheets
    val xxl = RoundedCornerShape(28.dp) // the floating toolbar
    val pill = RoundedCornerShape(percent = 50) // pills, tracks and badges
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) // a sheet that rises from the bottom
}

// Material draws Card, Button, Chip and the menus from these slots, so they match the shapes
// the screens set by hand. There is no Material slot for AppShape.xl; hero cards name it directly.
val AppShapes = Shapes(
    extraSmall = AppShape.xs,
    small = AppShape.sm,
    medium = AppShape.md,
    large = AppShape.lg,
    extraLarge = AppShape.xxl,
)
