package com.fidriyanto.banktracker.ui.theme

import androidx.compose.ui.graphics.Color

// Distinct, accessible colors for spending category arcs — defined here to satisfy G3
private val categoryColorMap = mapOf(
    "Food & Drink"       to Color(0xFFFF6B6B),
    "Transport"          to Color(0xFF4ECDC4),
    "Bills"              to Color(0xFF45B7D1),
    "Shopping"           to Color(0xFFFECA57),
    "Entertainment"      to Color(0xFFBB86FC),
    "Groceries"          to Color(0xFF66D9A0),
    "Health & Wellbeing" to Color(0xFF54A0FF),
    "Travel"             to Color(0xFFFF9F43),
    "Business"           to Color(0xFFFC5C65),
    "Subscriptions"      to Color(0xFFC8A96E),
    "Gifts"              to Color(0xFFFF78C4),
    "Family"             to Color(0xFF1DD1A1),
    "Other"              to Color(0xFF8395A7),
)

private val fallbackColors = listOf(
    Color(0xFF6C5CE7), Color(0xFFA29BFE), Color(0xFFFD79A8), Color(0xFFE17055),
)

fun categoryColor(category: String): Color =
    categoryColorMap[category] ?: fallbackColors[category.length % fallbackColors.size]
