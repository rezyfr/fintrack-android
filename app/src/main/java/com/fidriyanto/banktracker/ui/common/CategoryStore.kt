package com.fidriyanto.banktracker.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES

// ac: manage-categories — the category/subcategory list read by plain Composables (no ViewModel
// of their own), backed by the categories table. Falls back to DEFAULT_CATEGORIES until any
// screen that depends on GetCategoriesUseCase has loaded at least once, matching this app's
// original hardcoded behavior.
object CategoryStore {
    var all by mutableStateOf(DEFAULT_CATEGORIES)
        private set

    fun hydrate(categories: List<Category>) {
        if (categories.isNotEmpty()) all = categories
    }

    val expenseCategoryNames: List<String> get() = all.map { it.name }
}
