package com.fidriyanto.banktracker.ui.common

// ac: add-transaction-subcategory — fixed subcategory lists per category, mirroring web
// (web/src/constants/transaction.js). Categories not listed here (for example Groceries) have no
// subcategories. Subcategory is always optional.
val SUBCATEGORIES: Map<String, List<String>> = mapOf(
    "Transport" to listOf("Ride-hailing", "Fuel", "Toll", "E-money"),
    "Food & Drink" to listOf("Restaurant", "Cafe / coffee", "Food delivery", "Snacks"),
    "Bills" to listOf("Rent", "Electricity", "Water", "Internet", "Mobile / telco", "Insurance"),
    "Subscriptions" to listOf("Streaming", "Music", "Software / cloud"),
    "Family" to listOf("Mom", "Dad", "Wife", "Kids", "Household"),
    "Health & Wellbeing" to listOf("Gym / fitness", "Pharmacy", "Doctor / medical", "Sports gear"),
    "Shopping" to listOf("Clothing", "Electronics", "Home", "Personal care"),
    "Travel" to listOf("Flights", "Hotels", "Local transport", "Activities"),
    "Entertainment" to listOf("Movies", "Games", "Events", "Hobbies"),
    "Gifts" to listOf("Family", "Friends", "Charity"),
    "Business" to listOf("Supplies", "Services", "Fees"),
)

// ac: add-transaction-subcategory — subcategories available for a category, empty when it has none
fun subcategoriesFor(category: String): List<String> = SUBCATEGORIES[category] ?: emptyList()

// All distinct subcategories, for the transactions filter dropdown.
val ALL_SUBCATEGORIES: List<String> = SUBCATEGORIES.values.flatten().distinct().sorted()
