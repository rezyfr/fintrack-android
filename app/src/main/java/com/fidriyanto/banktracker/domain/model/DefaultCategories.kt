package com.fidriyanto.banktracker.domain.model

// The category/subcategory list this app originally shipped with. Seeds a fresh server
// and is the offline fallback before the categories table has synced at least once.
// Family's subcategories were originally named after specific household members; seeded here as a
// single generic "Household" placeholder instead, since those names do not belong in a public seed.
val DEFAULT_CATEGORIES: List<Category> = listOf(
    Category("Bills", listOf("Rent", "Electricity", "Water", "Internet", "Mobile / telco", "Insurance"), true),
    Category("Subscriptions", listOf("Streaming", "Music", "Software / cloud"), true),
    Category("Entertainment", listOf("Movies", "Games", "Events", "Hobbies"), true),
    Category("Food & Drink", listOf("Restaurant", "Cafe / coffee", "Food delivery", "Snacks"), true),
    Category("Groceries", emptyList(), true),
    Category("Health & Wellbeing", listOf("Gym / fitness", "Pharmacy", "Doctor / medical", "Sports gear"), true),
    Category("Family", listOf("Household"), true),
    Category("Other", emptyList(), true),
    Category("Shopping", listOf("Clothing", "Electronics", "Home", "Personal care"), true),
    Category("Transport", listOf("Ride-hailing", "Fuel", "Toll", "E-money"), true),
    Category("Travel", listOf("Flights", "Hotels", "Local transport", "Activities"), true),
    Category("Business", listOf("Supplies", "Services", "Fees"), true),
    Category("Gifts", listOf("Family", "Friends", "Charity"), true),
)
