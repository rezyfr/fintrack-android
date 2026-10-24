package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate

// Everything buildBudgetGlance needs, loaded by GetBudgetGlanceUseCase.
// currency is null for the combined view. rates converts any currency to the home currency.
// balances holds each checking account's balance by its currency; it is null for a past cycle.
internal data class BudgetGlanceInput(
    val currency: String?,
    val rates: RateTable,
    val cycle: PayCycle.Range,
    val prevCycle: PayCycle.Range,
    val isCurrentCycle: Boolean,
    val lines: List<BudgetLine>,
    val currencyByWallet: Map<String, String>,
    val rows: List<TransactionUiModel>,
    val prevRows: List<TransactionUiModel>,
    val olderCycles: List<Pair<PayCycle.Range, List<TransactionUiModel>>> = emptyList(),
    val balances: List<Pair<String, Double>>?,
    val today: LocalDate = LocalDate.now(),
)

// ac: android-budget-daily-allowance — flex lines for one currency over a pay cycle, with a safe daily
// allowance of what is left divided by the days left.
// ac: budget-combined-currency - the combined view shows every currency's lines in the home currency.
internal fun buildBudgetGlance(input: BudgetGlanceInput): BudgetGlance = with(input) {
    fun inView(c: String) = currency == null || c == currency
    fun toView(c: String, v: Double) = if (currency == null) rates.toHome(c, v) else v
    val flex = lines.filter { it.kind == "flex" && inView(it.currency) }
    // Attribute against ALL lines (override then first-match-wins across fixed+flex), keep this
    // currency's flex. ac: budget-line-details-and-assignment — carry up to 3 recent rows per line.
    fun attribute(r: List<TransactionUiModel>) = BudgetAttribution.attribute(r, lines, rates, currencyByWallet)
    val agg = attribute(rows)
    // ac: budget-spend-comparison-chart — the same attribution over the previous cycle
    val compare = buildBudgetCompare(
        cycle, prevCycle, flex, agg, attribute(prevRows), olderCycles.map { (r, rs) -> r to attribute(rs) }, mergeSameName = currency == null,
    ) { row, line ->
        toView(line.currency, BudgetAttribution.amountInLine(row, line, rates, currencyByWallet))
    }
    fun spendOf(l: BudgetLine) = BudgetLineSpend(
        l.copy(target = toView(l.currency, l.target)), toView(l.currency, agg[l.id]?.spent ?: 0.0), agg[l.id]?.rows?.take(3) ?: emptyList(),
    )
    // Sort after conversion, so a combined list compares targets in one currency
    val lineSpends = flex.map(::spendOf).sortedByDescending { it.line.target }
    // ac: budget-fixed-bills — fixed lines for this currency, with the same attributed spend
    val fixedSpends = lines.filter { it.kind == "fixed" && inView(it.currency) }.map(::spendOf).sortedByDescending { it.line.target }
    // ac: budget-unbudgeted-line — spend no line claims, in the currency of the wallet it was spent from
    val rowCurrency: (TransactionUiModel) -> String = { currencyByWallet[it.wallet] ?: rates.homeCode }
    val unbudgeted = BudgetAttribution.unattributed(rows, lines, currencyByWallet, rates.homeCode).filter { inView(rowCurrency(it)) }
    val unbudgetedSpent = unbudgeted.sumOf { toView(rowCurrency(it), it.amount) }
    val totalTarget = lineSpends.sumOf { it.line.target }
    val totalSpent = lineSpends.sumOf { it.spent } + unbudgetedSpent
    // ac: budget-count-overspend — what is left is the total target minus all spend, so an overspent
    // line and unbudgeted spend both reduce it
    val remaining = (totalTarget - totalSpent).coerceAtLeast(0.0)
    // ac: budget-cap-at-balance — the daily allowance never spends more than the checking balance
    val balance = balances?.filter { inView(it.first) }?.sumOf { toView(it.first, it.second) }
    val spendable = balance?.let { minOf(remaining, it.coerceAtLeast(0.0)) } ?: remaining
    val daysLeft = if (isCurrentCycle) ((cycle.end.toEpochDay() - today.toEpochDay()).toInt() + 1).coerceAtLeast(1) else 0
    BudgetGlance(
        currency = currency ?: rates.homeCode,
        totalTarget = totalTarget,
        totalSpent = totalSpent,
        remaining = remaining,
        daysLeft = daysLeft,
        dailyAllowance = if (daysLeft > 0) spendable / daysLeft else spendable,
        lines = lineSpends,
        fixed = fixedSpends,
        allCompare = compare.all,
        mergedChips = currency == null,
        lineCompare = compare.byLine,
        isCurrentCycle = isCurrentCycle,
        unbudgetedSpent = unbudgetedSpent,
        unbudgetedRecent = unbudgeted.sortedByDescending { it.dateIso }.take(3),
        balance = balance,
    )
}
