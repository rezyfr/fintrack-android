package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class ParsedTransaction(
    val merchant: String,
    val amount: Double,
    val date: LocalDate,
    val channel: String,
    val referenceNo: String,
    val rawFields: Map<String, String> = emptyMap()
)
