package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate
import javax.inject.Inject

// A saved transaction and the values one rule would give it.
data class RuleChange(val row: TransactionUiModel, val category: String, val subcategory: String?, val item: String)

data class ApplyResult(val done: Int, val failed: Int)

// Whether a rule can be tried on saved transactions. A saved transaction has a date but no time.
enum class PastSupport { OK, HAS_TIME, IGNORE }

class ApplyRulesToPastUseCase @Inject constructor(
    private val transactions: TransactionRepository,
) {
    fun support(rule: Rule): PastSupport = when {
        rule.action.ignore -> PastSupport.IGNORE
        rule.conditions.time != null -> PastSupport.HAS_TIME
        else -> PastSupport.OK
    }

    // The saved transactions of the last days that the rule would change. Transfers stay out unless the rule asks for them.
    suspend fun preview(rule: Rule, days: Int, today: LocalDate = LocalDate.now()): Result<List<RuleChange>> {
        if (support(rule) != PastSupport.OK) return Result.success(emptyList())
        return transactions.fetch(month = null, wallet = null, txType = null, dateFrom = today.minusDays(days.toLong()).toString(), dateTo = today.toString())
            .map { rows -> rows.mapNotNull { change(rule, it) } }
    }

    private fun change(rule: Rule, row: TransactionUiModel): RuleChange? {
        if (row.txType == "transfer" && rule.conditions.type != "transfer") return null
        val date = runCatching { LocalDate.parse(row.dateIso.take(10)) }.getOrNull() ?: return null
        val out = RuleEngine.apply(listOf(rule), RuleInput(row.item, row.amount, row.txType, row.wallet, date.atStartOfDay()))
        val category = out.category ?: return null
        val sub = out.subcategory
        val item = out.description ?: row.item
        val same = category == row.category && sub == row.subcategory && item == row.item
        return if (same) null else RuleChange(row, category, sub, item)
    }

    // One edit per row. A failed row does not stop the rest.
    suspend fun apply(changes: List<RuleChange>, onProgress: (Int) -> Unit = {}): ApplyResult = send(changes, onProgress) {
        edit(it.row, it.category, it.subcategory, it.item)
    }

    // Puts the rows back to the values they had before apply.
    suspend fun undo(changes: List<RuleChange>, onProgress: (Int) -> Unit = {}): ApplyResult = send(changes, onProgress) {
        edit(it.row, it.row.category, it.row.subcategory, it.row.item)
    }

    private suspend fun send(changes: List<RuleChange>, onProgress: (Int) -> Unit, build: (RuleChange) -> TransactionEdit): ApplyResult {
        var done = 0
        var failed = 0
        changes.forEachIndexed { i, c ->
            if (transactions.editTransaction(c.row.id, build(c)).isSuccess) done++ else failed++
            onProgress(i + 1)
        }
        return ApplyResult(done, failed)
    }

    private fun edit(row: TransactionUiModel, category: String, subcategory: String?, item: String) = TransactionEdit(
        amount = row.amount, item = item, category = category, dateIso = row.dateIso, wallet = row.wallet, txType = row.txType,
        toWallet = row.toWallet, toAmount = row.toAmount, subcategory = subcategory,
    )
}
