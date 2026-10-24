package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class ParsedTransaction(
    val item: String,
    val amount: Double,
    val date: LocalDate,
    val referenceNo: String,
    val timestampMs: Long,
    val wallet: String,
    val category: String = "Other",
    val txType: String = "expense",
    val subcategory: String? = null,
    val rawFields: Map<String, String> = emptyMap()
)
