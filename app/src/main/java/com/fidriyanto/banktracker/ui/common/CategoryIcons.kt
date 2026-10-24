package com.fidriyanto.banktracker.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

// ac: transaction-category-icons — a Material icon for each known category, matched by name without
// case. Unknown or custom categories get the generic Category icon.
fun categoryIcon(category: String): ImageVector = when (category.trim().lowercase()) {
    "food & drink", "food", "dining" -> Icons.Outlined.Restaurant
    "groceries" -> Icons.Outlined.LocalGroceryStore
    "transport" -> Icons.Outlined.DirectionsCar
    "bills", "rental" -> Icons.Outlined.Home
    "utilities" -> Icons.Outlined.ElectricBolt
    "shopping" -> Icons.Outlined.ShoppingBag
    "clothing" -> Icons.Outlined.Checkroom
    "entertainment" -> Icons.Outlined.Movie
    "health & wellbeing", "health" -> Icons.Outlined.HealthAndSafety
    "subscriptions" -> Icons.Outlined.Subscriptions
    "travel" -> Icons.Outlined.Flight
    "gift", "gifts" -> Icons.Outlined.CardGiftcard
    "salary", "bonus", "income" -> Icons.Outlined.Payments
    "freelance", "business" -> Icons.Outlined.Work
    "investment", "dividends" -> Icons.Outlined.TrendingUp
    "transfer" -> Icons.Outlined.SwapHoriz
    else -> Icons.Outlined.Category
}
