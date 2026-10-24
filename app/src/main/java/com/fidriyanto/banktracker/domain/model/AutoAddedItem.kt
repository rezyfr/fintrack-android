package com.fidriyanto.banktracker.domain.model

// Where an automatic transaction came from
enum class AutoSource { SCHEDULED, NOTIFICATION }

// ac: auto-added-notice - one transaction the app added by itself, shown in the notice on Home until the user dismisses it
data class AutoAddedItem(
    val id: Long,
    val addedAtMs: Long,
    val item: String,
    val amount: Double,
    val txType: String,
    val wallet: String?,
    val source: AutoSource,
)
