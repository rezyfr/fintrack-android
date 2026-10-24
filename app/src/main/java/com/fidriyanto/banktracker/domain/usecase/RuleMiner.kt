package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.AmountRange
import com.fidriyanto.banktracker.domain.model.DateMatch
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.TextMatch
import com.fidriyanto.banktracker.domain.model.TextMode
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate

enum class SuggestionKind { TEXT, AMOUNT, RECURRING }

// A rule the saved transactions point to. rows is how many transactions still in Other it would sort.
// The hint is the category that the transactions with a known category agree on, when there is one.
data class RuleSuggestion(
    val key: String,
    val kind: SuggestionKind,
    val conditions: RuleConditions,
    val rows: Int,
    val repeats: Int,
    val day: Int? = null,
    val months: Int = 0,
    val hintCategory: String? = null,
    val hintRows: Int = 0,
    val hintOf: Int = 0,
)

// Looks for patterns in transactions that are still in Other: a text, an amount or a day of the month that repeats.
object RuleMiner {
    const val DEFAULT_CATEGORY = "Other"
    private const val MIN_REPEATS = 4
    private const val MIN_RECURRING = 3
    private const val MIN_ROWS = 3
    private const val TEXT_AMOUNT_SHARE = 0.6
    private const val DAY_SHARE = 0.8
    private const val HINT_SHARE = 0.8
    private const val HINT_MIN = 3

    fun suggest(rows: List<TransactionUiModel>, existing: List<Rule>, limit: Int = 15): List<RuleSuggestion> {
        val usable = rows.filter { it.txType != "transfer" && it.item.isNotBlank() }
        val open = usable.filter { it.category == DEFAULT_CATEGORY && RuleEngine.apply(existing, input(it)).category == null }
        val labeled = usable.filter { it.category != DEFAULT_CATEGORY }
        val recurring = recurring(open, labeled)
        val taken = recurring.map { it.key }.toSet()
        val found = recurring + amounts(open, labeled, taken) + texts(open)
        val counted = found.map { recount(it, open) }.filter { it.first.rows >= MIN_ROWS }
        // A text rule that sorts the same rows as an amount rule says nothing more, so the amount rule stays.
        // An amount rule whose rows are only a part of a text rule is dropped, because the text explains more.
        val kept = counted.filterNot { (s, ids) ->
            counted.any { (o, oIds) ->
                (s.kind == SuggestionKind.TEXT && o.kind != SuggestionKind.TEXT && oIds == ids) ||
                    (s.kind == SuggestionKind.AMOUNT && o.kind == SuggestionKind.TEXT && ids.size < oIds.size && oIds.containsAll(ids))
            }
        }
        return kept.map { it.first }.distinctBy { it.conditions }.sortedByDescending { it.rows }.take(limit)
    }

    private fun input(r: TransactionUiModel) =
        RuleInput(r.item, r.amount, r.txType, r.wallet, runCatching { LocalDate.parse(r.dateIso.take(10)) }.getOrDefault(LocalDate.now()).atStartOfDay())

    // The rows the conditions match, counted with the same engine that applies a rule later.
    private fun recount(s: RuleSuggestion, open: List<TransactionUiModel>): Pair<RuleSuggestion, Set<Long>> {
        val probe = Rule("probe", "probe", true, s.conditions, RuleAction(category = "x"))
        val ids = open.filter { RuleEngine.matches(probe, input(it)) }.map { it.id }.toSet()
        return s.copy(rows = ids.size) to ids
    }

    private fun accounts(wallet: String?) = listOfNotNull(wallet)

    private fun hint(labeled: List<TransactionUiModel>, wallet: String?, amount: Double): Triple<String?, Int, Int> {
        val same = labeled.filter { it.wallet == wallet && it.amount == amount }
        val top = same.groupBy { it.category }.maxByOrNull { it.value.size } ?: return Triple(null, 0, same.size)
        val ok = top.value.size >= HINT_MIN && top.value.size.toDouble() / same.size >= HINT_SHARE
        return Triple(top.key.takeIf { ok }, top.value.size, same.size)
    }

    private fun recurring(open: List<TransactionUiModel>, labeled: List<TransactionUiModel>): List<RuleSuggestion> =
        open.groupBy { it.wallet to it.amount }.mapNotNull { (key, group) ->
            val days = group.mapNotNull { it.dateIso.take(10).takeIf { d -> d.length == 10 }?.substring(8)?.toIntOrNull() }
            val top = days.groupBy { it }.maxByOrNull { it.value.size } ?: return@mapNotNull null
            val months = group.map { it.dateIso.take(7) }.toSet().size
            if (group.size < MIN_RECURRING || months < MIN_RECURRING || top.value.size.toDouble() / group.size < DAY_SHARE) return@mapNotNull null
            val (cat, n, of) = hint(labeled, key.first, key.second)
            RuleSuggestion(
                "rec|${key.first}|${key.second}", SuggestionKind.RECURRING,
                RuleConditions(amount = AmountRange(key.second, key.second), date = DateMatch(day = top.key), accounts = accounts(key.first)),
                group.size, group.size, top.key, months, cat, n, of,
            )
        }

    private fun amounts(open: List<TransactionUiModel>, labeled: List<TransactionUiModel>, taken: Set<String>): List<RuleSuggestion> =
        open.groupBy { it.wallet to it.amount }.filter { it.value.size >= MIN_REPEATS && "rec|${it.key.first}|${it.key.second}" !in taken }.map { (key, group) ->
            val (cat, n, of) = hint(labeled, key.first, key.second)
            RuleSuggestion(
                "amt|${key.first}|${key.second}", SuggestionKind.AMOUNT,
                RuleConditions(amount = AmountRange(key.second, key.second), accounts = accounts(key.first)),
                group.size, group.size, null, 0, cat, n, of,
            )
        }

    // A text only counts when one amount makes up most of its rows. A text that fits many different payments is too vague.
    private fun texts(open: List<TransactionUiModel>): List<RuleSuggestion> =
        open.groupBy { it.item.trim().lowercase().trimEnd('.', ' ', ':') }.filter { it.key.length >= 3 && it.value.size >= MIN_REPEATS }.mapNotNull { (text, group) ->
            val topAmount = group.groupBy { it.amount }.maxOf { it.value.size }
            if (topAmount.toDouble() / group.size < TEXT_AMOUNT_SHARE) return@mapNotNull null
            RuleSuggestion("txt|$text", SuggestionKind.TEXT, RuleConditions(text = TextMatch(TextMode.CONTAINS, text)), group.size, group.size)
        }
}
