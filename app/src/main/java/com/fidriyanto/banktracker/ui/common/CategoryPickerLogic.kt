package com.fidriyanto.banktracker.ui.common

const val PICKER_CHIP_COUNT = 6

// ac: category-picker - a category with its subcategories, as the picker shows it
data class PickerCategory(val name: String, val subcategories: List<String> = emptyList())

// The names of the chips in the compact row. The selected category comes first.
// The most used categories follow. If fewer than count are known, the normal order fills the rest.
fun chipNames(all: List<PickerCategory>, frequent: List<String>, selected: String, count: Int = PICKER_CHIP_COUNT): List<String> {
    val names = all.map { it.name }
    val top = (frequent.filter { it in names } + names).distinct().take(count)
    return (listOf(selected).filter { it.isNotBlank() } + top).distinct()
}

// The categories that match the query by their name or by one of their subcategories.
// A category that matches by subcategory keeps only the matching subcategories.
// A category that matches by name keeps all of its subcategories.
fun filterCategories(all: List<PickerCategory>, query: String): List<PickerCategory> {
    val q = query.trim()
    if (q.isEmpty()) return all
    return all.mapNotNull { c ->
        if (c.name.contains(q, ignoreCase = true)) c
        else c.subcategories.filter { it.contains(q, ignoreCase = true) }.takeIf { it.isNotEmpty() }?.let { c.copy(subcategories = it) }
    }
}
