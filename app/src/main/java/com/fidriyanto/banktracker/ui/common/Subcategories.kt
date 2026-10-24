package com.fidriyanto.banktracker.ui.common

// ac: manage-categories — subcategories are sourced from CategoryStore (backed by the
// categories/subcategories tables) instead of a compiled-in map. Subcategory is always optional.
fun subcategoriesFor(category: String): List<String> =
    CategoryStore.all.firstOrNull { it.name == category }?.subcategories ?: emptyList()

// All distinct subcategories, for the transactions filter dropdown.
val ALL_SUBCATEGORIES: List<String>
    get() = CategoryStore.all.flatMap { it.subcategories }.distinct().sorted()
