package com.fidriyanto.banktracker.ui.feed

import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class FeedUiState(
    val items: List<TransactionUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class DeleteFailureEvent(val id: Long)
data class EditFailureEvent(val id: Long, val edit: TransactionEdit)

internal fun currentMonthPrefix(): String {
    val d = LocalDate.now()
    return "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
}

internal fun monthDisplayLabel(ym: String): String {
    val (y, m) = ym.split("-").map { it.toInt() }
    val name = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    return "$name ${(y % 100).toString().padStart(2, '0')}"
}
