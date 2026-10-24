package com.fidriyanto.banktracker.domain.model

// [tables] hold what the tools found as lists, built from exact numbers. [source] is the tool text behind an
// answer that has no table, so the user can check the numbers. [context] says which tool call made the answer,
// so a follow-up such as "break it down" can keep its category and period.
data class AskMessage(
    val fromUser: Boolean,
    val text: String,
    val source: String? = null,
    val tables: List<AskTable> = emptyList(),
    val context: String? = null,
)
