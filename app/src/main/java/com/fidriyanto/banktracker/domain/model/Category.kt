package com.fidriyanto.banktracker.domain.model

data class Category(
    val name: String,
    val subcategories: List<String>,
    val isDefault: Boolean,
)
