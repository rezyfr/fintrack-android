package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class ParsedTransaction(
    val merchant: String,
    val amount: Double,
    val date: LocalDate,
    val referenceNo: String,
    val wallet: String = "BBL",
    val category: String = "Other",
    val rawFields: Map<String, String> = emptyMap()
)
