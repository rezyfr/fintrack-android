package com.fidriyanto.banktracker.domain.model

// What the model read from one bank notification.
data class AiNotificationReply(
    val isIncome: Boolean,
    val amount: Double,
    val currency: String,
    val merchant: String,
    val category: String,
    val subcategory: String?,
)

data class CategorySuggestion(val category: String, val subcategory: String?)
