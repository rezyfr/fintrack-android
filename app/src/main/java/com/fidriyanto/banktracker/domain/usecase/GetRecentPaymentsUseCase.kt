package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate
import javax.inject.Inject

const val RECENT_PAYMENT_DAYS = 60

// ac: installment-plan-add - the expenses of the last days on one account, newest first.
// The user picks one to fill in a new plan.
class GetRecentPaymentsUseCase @Inject constructor(
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(walletId: String, today: LocalDate = LocalDate.now()): Result<List<TransactionUiModel>> =
        transactions.fetch(
            month = null, wallet = walletId, txType = "expense",
            dateFrom = today.minusDays(RECENT_PAYMENT_DAYS.toLong()).toString(), dateTo = today.toString(),
        ).map { rows -> rows.sortedByDescending { it.dateIso } }
}
