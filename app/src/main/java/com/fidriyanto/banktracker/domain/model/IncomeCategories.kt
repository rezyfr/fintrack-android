package com.fidriyanto.banktracker.domain.model

// The categories an income transaction can have. The Add form and the scheduled transactions both use this list.
val INCOME_CATEGORIES = listOf("Salary", "Freelance", "Business", "Dividends", "Rental", "Bonus", "Gift", "Other")

// Transfers use this category, matching the web app and the server.
const val TRANSFER_CATEGORY = "Transfer"
