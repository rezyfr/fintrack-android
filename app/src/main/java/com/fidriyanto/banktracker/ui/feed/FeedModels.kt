package com.fidriyanto.banktracker.ui.feed

import androidx.annotation.StringRes
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.fidriyanto.banktracker.ui.common.AccountStore

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

// ac: transaction-list-date-groups — the four sections, in display order
internal enum class DateGroup(@StringRes val labelRes: Int) {
    TODAY(R.string.feed_group_today),
    YESTERDAY(R.string.feed_group_yesterday),
    LAST_7_DAYS(R.string.feed_group_last_7_days),
    OLDER(R.string.feed_group_older),
}

// ac: transaction-list-date-groups — each row goes to the section for its date
// Future dates count as Today. Last 7 days covers 2 to 7 days ago.
internal fun dateGroupOf(dateIso: String, today: LocalDate): DateGroup {
    val date = runCatching { LocalDate.parse(dateIso.take(10)) }.getOrNull() ?: return DateGroup.OLDER
    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(date, today)
    return when {
        daysAgo <= 0 -> DateGroup.TODAY
        daysAgo == 1L -> DateGroup.YESTERDAY
        daysAgo <= 7 -> DateGroup.LAST_7_DAYS
        else -> DateGroup.OLDER
    }
}

// ac: transaction-list-date-groups — empty sections are dropped, so no header shows without rows
// Keeps the incoming row order inside each section.
internal fun groupByDate(items: List<TransactionUiModel>, today: LocalDate): List<Pair<DateGroup, List<TransactionUiModel>>> {
    val byGroup = items.groupBy { dateGroupOf(it.dateIso, today) }
    return DateGroup.values().mapNotNull { g -> byGroup[g]?.let { g to it } }
}

// ac: feed-sticky-date-header — a date header carries that group's outgoing total, so scrolling the
// list answers "what did I spend today" without adding the rows up by eye. Rows in several
// currencies cannot be added together, so a mixed group reports no total.
internal fun groupOutgoing(rows: List<TransactionUiModel>, fallbackCurrency: String): Pair<String, Double>? {
    val spend = rows.filter { it.txType != "income" && it.txType != "transfer" }
    if (spend.isEmpty()) return null
    val currencies = spend.map { AccountStore.currencyOf(it.wallet, fallbackCurrency) }.distinct()
    if (currencies.size != 1) return null
    return currencies.first() to spend.sumOf { it.amount }
}

// ac: feed-filter-apply — the filter sheet edits a draft of these values. Apply sets them all at once,
// and closing the sheet without Apply drops the draft.
internal data class FeedFilters(
    val month: String? = null,
    val wallet: String? = null,
    val type: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val search: String = "",
    val amountMin: String = "",
    val amountMax: String = "",
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val noBudgetOnly: Boolean = false,
)
