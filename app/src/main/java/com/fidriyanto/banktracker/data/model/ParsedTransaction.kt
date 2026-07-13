package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class ParsedTransaction(
    val item: String,
    val amount: Double,
    val date: LocalDate,
    val referenceNo: String,
    val timestampMs: Long,
    val wallet: String = "BBL",
    val category: String = "Other",
    val rawFields: Map<String, String> = emptyMap()
)
