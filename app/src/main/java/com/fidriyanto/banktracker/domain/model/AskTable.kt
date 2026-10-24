package com.fidriyanto.banktracker.domain.model

// One line of a result table: what it is, its value, and a small note under it, such as a date.
data class AskRow(val label: String, val value: String, val note: String? = null)

// A group of rows, for example one currency.
data class AskSection(val heading: String?, val rows: List<AskRow>)

// What a tool found, ready to show as a list. The app builds it from exact numbers, so it needs no help from the model.
data class AskTable(val title: String, val subtitle: String?, val sections: List<AskSection>)
