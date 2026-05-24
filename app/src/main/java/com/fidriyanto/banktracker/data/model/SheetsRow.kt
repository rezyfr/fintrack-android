package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class SheetsRow(
    val tab: SheetTab,
    val date: LocalDate,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val channel: String,
    val note: String? = null
)
