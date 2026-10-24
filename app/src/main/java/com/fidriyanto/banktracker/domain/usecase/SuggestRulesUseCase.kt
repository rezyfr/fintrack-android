package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class SuggestRulesUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val rules: RuleRepository,
) {
    // Rules the transactions of the last days point to. A transaction that an existing rule already sorts does not count.
    suspend operator fun invoke(days: Int = 365, today: LocalDate = LocalDate.now()): Result<List<RuleSuggestion>> =
        transactions.fetch(month = null, wallet = null, txType = null, dateFrom = today.minusDays(days.toLong()).toString(), dateTo = today.toString())
            .map { RuleMiner.suggest(it, rules.observe().first()) }
}
