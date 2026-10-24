package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.model.AskRow
import com.fidriyanto.banktracker.domain.model.AskSection
import com.fidriyanto.banktracker.domain.model.AskTable
import com.fidriyanto.banktracker.domain.model.LlmTool
import com.fidriyanto.banktracker.domain.model.LlmToolParam
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.formatMoney
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val PERIOD_HELP = "One of: ${PeriodResolver.KEYWORDS}. Or the time words exactly as the user said them, for example: ${PeriodPhrases.EXAMPLES}. " +
    "Leave empty for the pay cycle in progress."
private const val DEFAULT_LIMIT = 5
private const val MAX_LIMIT = 10
private const val TABLE_ROWS = 8
private const val LIST_ROWS = 15
private const val DEFAULT_MONTHS = 6
private const val MAX_MONTHS = 12
private const val RECURRING_DAYS = 365L
private const val DEFAULT_RECURRING = 8

// The tools the model may call to answer a question. Each tool reads rows, counts them in code and
// returns a short sentence with exact amounts.
class FinanceToolBox(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val currencySettings: CurrencySettingsRepository,
    private val budgetLines: BudgetLineRepository,
    private val walletBalances: WalletBalanceRepository,
    private val today: LocalDate = LocalDate.now(),
    // A number the question asked for, such as the 3 in "my 3 biggest expenses". It wins over what the model passes.
    private val questionLimit: Int? = null,
) {
    // What the tools returned during this question, in call order.
    val results = mutableListOf<String>()

    // What the tools found, as lists the screen shows. They are built from exact numbers.
    val tables = mutableListOf<AskTable>()

    // The tool calls of this question with their arguments, in call order. A follow-up question can reuse them.
    val calls = mutableListOf<String>()

    private val period = LlmToolParam("period", PERIOD_HELP)
    private val from = LlmToolParam("from", "Start date yyyy-MM-dd. Use only when the user gave exact dates.")
    private val to = LlmToolParam("to", "End date yyyy-MM-dd. Use only when the user gave exact dates.")
    // The category list is in the instructions once, not in every tool, to keep the tools short for the small model.
    private val category = LlmToolParam("category", "Pass this whenever the question names a kind of spending. A category or subcategory from the list in the instructions.")

    fun tools(): List<LlmTool> = rawTools().map { t ->
        t.copy(run = { a ->
            calls += "${t.name}(${a.entries.filter { it.value.isNotBlank() }.joinToString(", ") { "${it.key}=${it.value}" }})"
            t.run(a).also { results += it }
        })
    }

    private fun rawTools(): List<LlmTool> = listOf(
        LlmTool("spending_total", "Total spending, optionally for one category.", listOf(category, period, from, to)) { a ->
            withRows(a) { rows, cur, span ->
                val list = SpendingQueries.expenses(rows, a["category"])
                "Spending${a["category"]?.let { " on $it" }.orEmpty()} ${span.label()}: " +
                    "${SpendingQueries.describe(SpendingQueries.totals(list, cur))}, ${list.size} transactions."
            }
        },
        LlmTool("spending_by_category", "Spending per category. With a category, the split by subcategory inside it.", listOf(category, period, from, to)) { a ->
            withRows(a) { rows, cur, span ->
                val q = a["category"]?.trim().orEmpty()
                if (q.isNotEmpty()) return@withRows breakdown(q, SpendingQueries.expenses(rows, q), cur, span)
                val byCat = SpendingQueries.byCategory(SpendingQueries.expenses(rows, null), cur)
                if (byCat.isEmpty()) "No spending ${span.label()}."
                else {
                    show("Spending by category", span, byCat.map { (c, list) -> AskSection(c, list.take(TABLE_ROWS).map { AskRow(it.first, formatMoney(c, it.second)) }) })
                    "Spending by category ${span.label()}:\n" + byCat.entries.joinToString("\n") { (c, list) ->
                        "$c: " + list.take(DEFAULT_LIMIT).joinToString(", ") { "${it.first} ${formatMoney(c, it.second)}" }
                    }
                }
            }
        },
        LlmTool("compare_spending", "Compare total spending of two periods.", listOf(
            LlmToolParam("period_a", PERIOD_HELP, required = true), LlmToolParam("period_b", PERIOD_HELP, required = true), category,
        )) { a -> compare(a) },
        LlmTool("largest_expenses", "The largest single expenses, one row each. For the biggest or most expensive expenses.", listOf(
            period, from, to, LlmToolParam("limit", "How many rows, 1 to $MAX_LIMIT. Use the number the user said."),
        )) { a ->
            withRows(a) { rows, cur, span ->
                val list = SpendingQueries.expenses(rows, null)
                if (list.isEmpty()) "No spending ${span.label()}."
                else {
                    // The order compares amounts in the home currency, so "3 biggest" gives 3 rows in all.
                    val rates = rateTable()
                    val top = list.sortedByDescending { rates.toHome(cur(it), it.amount) }.take(limit(a))
                    show("Largest expenses", span, listOf(AskSection(null, top.map { AskRow(it.item, formatMoney(cur(it), it.amount), niceDate(it.dateIso)) })))
                    "Largest expenses ${span.label()}:\n" + top.joinToString("\n") { "${it.item} ${formatMoney(cur(it), it.amount)} on ${it.dateIso}" }
                }
            }
        },
        LlmTool("find_transactions", "List the individual expenses, newest first, by a word in the name, a category, an account or an amount. For list, show the transactions, or break it down.", listOf(
            LlmToolParam("text", "A word in the name. Leave empty to search by the other fields."), category,
            LlmToolParam("account", "The name of one account."),
            LlmToolParam("min_amount", "Only expenses of at least this amount."), LlmToolParam("max_amount", "Only expenses of at most this amount."),
            period, from, to,
        )) { a ->
            val text = a["text"]?.trim()?.ifEmpty { null }
            val wallet = accountId(a["account"])
            if (!a["account"].isNullOrBlank() && wallet == null) return@LlmTool "No account matches \"${a["account"]}\"."
            val narrow = Narrow(text, wallet, a["min_amount"]?.toDoubleOrNull(), a["max_amount"]?.toDoubleOrNull())
            withRows(a.withDefaultPeriod("last_90_days"), narrow) { rows, cur, span ->
                val list = SpendingQueries.expenses(rows, a["category"])
                if (list.isNotEmpty()) {
                    val what = listOfNotNull(a["category"]?.takeIf { it.isNotBlank() }?.let { "on $it" }, text?.let { "matching \"$it\"" }).joinToString(" ")
                    val rows = list.take(LIST_ROWS).map { AskRow(it.item, formatMoney(cur(it), it.amount), niceDate(it.dateIso)) } +
                        listOfNotNull(if (list.size > LIST_ROWS) AskRow("and ${list.size - LIST_ROWS} more", "") else null)
                    show("${list.size} expenses${if (what.isEmpty()) "" else " $what"}", span, listOf(AskSection(null, rows)))
                }
                "Found ${list.size} expenses${text?.let { " matching \"$it\"" }.orEmpty()} ${span.label()}, total ${SpendingQueries.describe(SpendingQueries.totals(list, cur))}." +
                    list.take(DEFAULT_LIMIT).joinToString("") { "\n${it.item} ${formatMoney(cur(it), it.amount)} on ${it.dateIso}" }
            }
        },
        LlmTool("income_total", "Total money received, optionally from one name.", listOf(
            LlmToolParam("text", "Part of the name of who paid. Leave empty for all income."), period, from, to,
        )) { a ->
            val text = a["text"]?.trim()?.ifEmpty { null }
            withRows(a, Narrow(search = text)) { rows, cur, span ->
                val list = InsightQueries.income(rows, text)
                if (list.isNotEmpty()) {
                    show("Income${text?.let { " from $it" }.orEmpty()}", span, listOf(AskSection(null, list.sortedByDescending { it.amount }.take(TABLE_ROWS).map { AskRow(it.item, formatMoney(cur(it), it.amount), niceDate(it.dateIso)) })))
                }
                "Income${text?.let { " from $it" }.orEmpty()} ${span.label()}: ${SpendingQueries.describe(SpendingQueries.totals(list, cur))}, ${list.size} transactions." +
                    list.sortedByDescending { it.amount }.take(3).joinToString("") { "\n${it.item} ${formatMoney(cur(it), it.amount)} on ${it.dateIso}" }
            }
        },
        LlmTool("cashflow", "Money in, money out and what is left, for a period.", listOf(period, from, to)) { a ->
            withRows(a) { rows, cur, span ->
                val flow = InsightQueries.flow(rows, cur)
                if (flow.isEmpty()) "No income or spending ${span.label()}."
                else {
                    show("Cash flow", span, flow.map { (c, f) ->
                        AskSection(c, listOf(AskRow("Money in", formatMoney(c, f.income)), AskRow("Money out", formatMoney(c, f.expense)), AskRow("Net", formatMoney(c, f.net, showSign = true))))
                    })
                    "Cash flow ${span.label()}:\n" + flow.entries.joinToString("\n") { (c, f) ->
                        "$c: in ${formatMoney(c, f.income)}, out ${formatMoney(c, f.expense)}, net ${formatMoney(c, f.net, showSign = true)}"
                    }
                }
            }
        },
        LlmTool("spending_stats", "How many expenses, the total and the average, for a category or a word in the name.", listOf(
            category, LlmToolParam("text", "A word in the name."), period, from, to,
        )) { a ->
            val text = a["text"]?.trim()?.ifEmpty { null }
            withRows(a, Narrow(search = text)) { rows, cur, span ->
                val list = SpendingQueries.expenses(rows, a["category"])
                val what = listOfNotNull(a["category"]?.takeIf { it.isNotBlank() }, text?.let { "\"$it\"" }).joinToString(" ")
                if (list.isEmpty()) "No spending${if (what.isEmpty()) "" else " on $what"} ${span.label()}."
                else {
                    val by = list.groupBy(cur)
                    show("Spending${if (what.isEmpty()) "" else " on $what"}", span, by.map { (c, l) ->
                        AskSection(c, listOf(
                            AskRow("Transactions", l.size.toString()), AskRow("Total", formatMoney(c, l.sumOf { it.amount })),
                            AskRow("Average", formatMoney(c, l.sumOf { it.amount } / l.size)), AskRow("Largest", formatMoney(c, l.maxOf { it.amount })),
                        ))
                    })
                    "Spending${if (what.isEmpty()) "" else " on $what"} ${span.label()}:\n" + by.entries.joinToString("\n") { (c, l) ->
                        "$c: ${l.size} transactions, total ${formatMoney(c, l.sumOf { it.amount })}, average ${formatMoney(c, l.sumOf { it.amount } / l.size)}, " +
                            "largest ${formatMoney(c, l.maxOf { it.amount })}"
                    }
                }
            }
        },
        LlmTool("top_merchants", "The total for each shop or person name, largest first. For where the user spent the most.", listOf(
            category, period, from, to, LlmToolParam("limit", "How many names, 1 to $MAX_LIMIT."),
        )) { a ->
            withRows(a) { rows, cur, span ->
                val list = SpendingQueries.expenses(rows, a["category"])
                if (list.isEmpty()) "No spending ${span.label()}."
                else {
                    val top = InsightQueries.merchants(list, cur, rateTable(), limit(a))
                    show("Top names", span, listOf(AskSection(null, top.map { AskRow(it.name, formatMoney(it.currency, it.total), "${it.times} times") })))
                    "Top names ${span.label()}:\n" + top.joinToString("\n") { "${it.name} ${formatMoney(it.currency, it.total)} (${it.times} times)" }
                }
            }
        },
        LlmTool("monthly_trend", "Spending month by month for the last months, for a category.", listOf(
            category, LlmToolParam("months", "How many months, 1 to 12. The default is 6."),
        )) { a -> trend(a) },
        LlmTool("recurring_payments", "Payments that repeat every month, from the last 12 months.", emptyList()) { recurring() },
        LlmTool("budget_left", "How much budget is left in the pay cycle in progress, in total or for one budget line.", listOf(
            LlmToolParam("currency", "A currency code. Leave empty for all accounts together."),
            LlmToolParam("line", "A budget line or a category name. Leave empty for the total."),
        )) { a ->
            val g = loadBudgetGlance(
                a["currency"]?.trim()?.uppercase()?.ifEmpty { null }, 0, budgetLines, transactions, accounts, currencySettings, walletBalances,
            )
            val q = a["line"]?.trim().orEmpty()
            if (q.isEmpty()) {
                "Budget ${g.currency}: spent ${formatMoney(g.currency, g.totalSpent)} of ${formatMoney(g.currency, g.totalTarget)}. " +
                    "Left ${formatMoney(g.currency, g.remaining)}, ${g.daysLeft} days to payday, about ${formatMoney(g.currency, g.dailyAllowance)} per day."
            } else {
                val all = g.lines + g.fixed
                val hit = InsightQueries.budgetLine(all, q)
                if (hit == null) "No budget line matches \"$q\". The lines are: ${all.joinToString(", ") { it.line.name }}."
                else {
                    val c = g.currency
                    "Budget line ${hit.line.name}: spent ${formatMoney(c, hit.spent)} of ${formatMoney(c, hit.line.target)}. " +
                        if (hit.isOver) "Over by ${formatMoney(c, hit.spent - hit.line.target)}." else "Left ${formatMoney(c, hit.remaining)}."
                }
            }
        },
    )

    private suspend fun trend(a: Map<String, String>): String {
        val n = (a["months"]?.toIntOrNull() ?: DEFAULT_MONTHS).coerceIn(1, MAX_MONTHS)
        val span = PeriodResolver.Span(today.withDayOfMonth(1).minusMonths(n - 1L), today)
        val cur = currencyOf()
        val series = InsightQueries.monthly(SpendingQueries.expenses(rows(span), a["category"]), cur)
        val what = a["category"]?.takeIf { it.isNotBlank() }?.let { " on $it" }.orEmpty()
        if (series.isEmpty()) return "No spending$what ${span.label()}."
        val thisMonth = today.toString().take(7)
        show("Spending$what by month", span, series.map { (c, months) ->
            AskSection(c, months.entries.map { (m, v) -> AskRow(m, formatMoney(c, v), if (m == thisMonth) "not complete" else null) })
        })
        return "Spending$what by month ${span.label()}. The month $thisMonth is not complete:\n" + series.entries.joinToString("\n") { (c, months) ->
            val line = months.entries.joinToString(", ") { (m, v) -> "$m ${formatMoney(c, v)}" }
            val done = months.filterKeys { it != thisMonth }.values.toList()
            val change = if (done.size >= 2 && done[done.size - 2] > 0) {
                val pct = Math.round((done.last() - done[done.size - 2]) / done[done.size - 2] * 100)
                ". Last full month against the one before: ${if (pct >= 0) "+" else ""}$pct%"
            } else ""
            "$c: $line$change"
        }
    }

    private suspend fun recurring(): String {
        val span = PeriodResolver.Span(today.minusDays(RECURRING_DAYS), today)
        val list = InsightQueries.recurring(rows(span), currencyOf(), DEFAULT_RECURRING)
        if (list.isEmpty()) return "No payment repeats every month in the last 12 months."
        show("Payments that repeat every month", span, listOf(AskSection(null, list.map {
            AskRow(it.name, formatMoney(it.currency, it.amount), "around day ${it.day}, ${it.months} months, last ${niceDate(it.lastDate)}")
        })))
        return "Payments that repeat every month, from the last 12 months:\n" + list.joinToString("\n") {
            "${it.name} ${formatMoney(it.currency, it.amount)}, around day ${it.day}, ${it.months} months, last on ${it.lastDate}"
        }
    }

    // What is inside one category: its subcategories, or the names when the rows have no subcategory.
    private fun breakdown(category: String, list: List<TransactionUiModel>, cur: (TransactionUiModel) -> String, span: PeriodResolver.Span): String {
        if (list.isEmpty()) return "No spending on $category ${span.label()}."
        val noSub = "No subcategory"
        val bySub = list.groupBy { cur(it) to (it.subcategory?.takeIf { s -> s.isNotBlank() } ?: noSub) }
        val useNames = bySub.keys.all { it.second == noSub }
        val groups = if (useNames) list.groupBy { cur(it) to it.item.trim() } else bySub
        val sections = groups.entries.groupBy { it.key.first }.map { (c, entries) ->
            AskSection(c, entries.map { (k, l) -> Triple(k.second, l.sumOf { it.amount }, l.size) }.sortedByDescending { it.second }.take(TABLE_ROWS)
                .map { (name, total, n) -> AskRow(name, formatMoney(c, total), "$n ${if (n == 1) "transaction" else "transactions"}") })
        }
        show("Spending on $category by ${if (useNames) "name" else "subcategory"}", span, sections)
        return "Spending on $category by ${if (useNames) "name" else "subcategory"} ${span.label()}:\n" + sections.joinToString("\n") { sec ->
            "${sec.heading}: " + sec.rows.joinToString(", ") { "${it.label} ${it.value}" }
        }
    }

    private suspend fun compare(a: Map<String, String>): String {
        val x = PeriodResolver.resolve(a["period_a"], null, null, today) ?: return unknownPeriod()
        val y = PeriodResolver.resolve(a["period_b"], null, null, today) ?: return unknownPeriod()
        val cur = currencyOf()
        fun total(rows: List<TransactionUiModel>) = SpendingQueries.totals(SpendingQueries.expenses(rows, a["category"]), cur)
        val ta = total(rows(x))
        val tb = total(rows(y))
        return "${x.label()}: ${SpendingQueries.describe(ta)}. ${y.label()}: ${SpendingQueries.describe(tb)}."
    }

    // What the server narrows down before the code counts: a word in the name, one account and an amount range.
    private data class Narrow(val search: String? = null, val wallet: String? = null, val min: Double? = null, val max: Double? = null)

    private suspend fun withRows(
        a: Map<String, String>,
        narrow: Narrow = Narrow(),
        block: suspend (List<TransactionUiModel>, (TransactionUiModel) -> String, PeriodResolver.Span) -> String,
    ): String {
        val span = PeriodResolver.resolve(a["period"], a["from"], a["to"], today) ?: return unknownPeriod()
        return block(rows(span, narrow), currencyOf(), span)
    }

    private suspend fun rows(span: PeriodResolver.Span, n: Narrow = Narrow()) = transactions
        .fetch(
            month = null, wallet = n.wallet, txType = null, search = n.search, amountMin = n.min, amountMax = n.max,
            dateFrom = span.from.toString(), dateTo = span.to.toString(),
        ).getOrThrow()

    // The id of the account the user means, by its id or a part of its name.
    private suspend fun accountId(name: String?): String? {
        val q = name?.trim().orEmpty().ifEmpty { return null }
        return accounts.observeAll().first().firstOrNull { it.id.equals(q, true) || it.displayName.contains(q, true) }?.id
    }

    private suspend fun currencyOf(): (TransactionUiModel) -> String {
        val byWallet = accounts.observeAll().first().associate { it.id to it.currency }
        val home = currencySettings.observe().first().homeCode
        return { row -> byWallet[row.wallet] ?: home }
    }

    // How many rows to list. The number in the question wins, then the one the model passed.
    private fun limit(a: Map<String, String>): Int = (questionLimit ?: a["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)

    private suspend fun rateTable(): RateTable {
        val byWallet = accounts.observeAll().first().associate { it.id to it.currency }
        return transactions.rateTable(byWallet, currencySettings.observe().first())
    }

    private fun show(title: String, span: PeriodResolver.Span, sections: List<AskSection>) {
        tables += AskTable(title, niceRange(span), sections)
    }

    private fun niceDate(iso: String): String = runCatching { niceDate(LocalDate.parse(iso.take(10))) }.getOrDefault(iso)

    private fun niceDate(d: LocalDate) = "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${d.year}"

    private fun niceRange(s: PeriodResolver.Span): String =
        if (s.from == s.to) niceDate(s.from) else if (s.from.year == s.to.year) "${niceDate(s.from).dropLast(5)} to ${niceDate(s.to)}" else "${niceDate(s.from)} to ${niceDate(s.to)}"

    private fun Map<String, String>.withDefaultPeriod(p: String) =
        if (this["period"].isNullOrBlank() && this["from"].isNullOrBlank()) this + ("period" to p) else this

    private fun PeriodResolver.Span.label() = "from $from to $to"
    private fun unknownPeriod() = "Unknown period. Use one of: ${PeriodResolver.KEYWORDS}, or time words such as: ${PeriodPhrases.EXAMPLES}."
}
