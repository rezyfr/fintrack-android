package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import java.time.LocalDate
import javax.inject.Inject

const val FREQUENT_DAYS = 90
const val FREQUENT_LIMIT = 6

// ac: category-picker - the categories used most in the last 90 days for one type of transaction.
// A failed fetch gives an empty list, so the picker shows the normal order.
class GetFrequentCategoriesUseCase @Inject constructor(
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(
        txType: String,
        names: List<String>,
        limit: Int = FREQUENT_LIMIT,
        today: LocalDate = LocalDate.now(),
    ): List<String> =
        transactions.fetch(
            month = null, wallet = null, txType = txType,
            dateFrom = today.minusDays(FREQUENT_DAYS.toLong()).toString(), dateTo = today.toString(),
        ).map { rows -> rankCategories(rows.map { it.category to it.dateIso }, names, limit) }
            .getOrElse { emptyList() }

    // One ranking for each type id, for a screen that loads them once when it opens.
    suspend fun byType(namesByType: Map<String, List<String>>): Map<String, List<String>> =
        namesByType.mapValues { (txType, names) -> invoke(txType, names) }
}
