package com.fidriyanto.banktracker.ui.theme

import androidx.compose.ui.graphics.Color

// Category colours identify a category. They never carry a judgement.
//
// Red and green are reserved for meaning: red for a problem (over budget, overdue, owed) and green
// for income. So the category palette is muted and shifted off those two hues. A category colour is
// always less saturated than a semantic one, which keeps the brightest thing on a screen the thing
// that matters. The earlier palette fought this: its Food & Drink was a pure red beside a red
// expense amount, and its Groceries a bright green beside a green income amount.
private val categoryColorMap = mapOf(
    "Food & Drink" to Color(0xFFD97757), // clay, shifted orange and away from the expense red
    "Transport" to Color(0xFF3FA9A0), // teal
    "Bills" to Color(0xFF3D8FB0), // blue
    "Shopping" to Color(0xFFD6A13C), // amber
    "Entertainment" to Color(0xFF9173D6), // violet
    "Groceries" to Color(0xFF6BA67F), // sage, lighter and greyer than the income green
    "Health & Wellbeing" to Color(0xFF5B82C7), // indigo
    "Travel" to Color(0xFFD18A3E), // orange
    "Business" to Color(0xFFB5636B), // dusty rose
    "Subscriptions" to Color(0xFFA98F5E), // bronze
    "Gifts" to Color(0xFFC2719E), // pink
    "Family" to Color(0xFF4FA98F), // sea green
    "Other" to Color(0xFF7D8A99), // slate
)

private val fallbackColors = listOf(
    Color(0xFF7E6BC4), Color(0xFF9B8BD1), Color(0xFFBE7596), Color(0xFFC08662),
)

fun categoryColor(category: String): Color =
    categoryColorMap[category] ?: fallbackColors[category.length % fallbackColors.size]

// A darker companion to each category colour. The palette above is tuned to read as fills, next to
// each other, in a chart. As an icon or a label on a light ground it needs more contrast, so the
// colour is darkened. On a dark ground the fill colour is already right.
private const val INK_DARKEN = 0.72f

fun categoryInk(category: String, onDarkTheme: Boolean): Color {
    val c = categoryColor(category)
    return if (onDarkTheme) {
        c
    } else {
        Color(red = c.red * INK_DARKEN, green = c.green * INK_DARKEN, blue = c.blue * INK_DARKEN)
    }
}
