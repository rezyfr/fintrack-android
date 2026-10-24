package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.AmountRange
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.TimeRange

// Finds two rules that can match the same transaction with the same detail and set different results.
// Such a pair is decided by list order only, so the user should know about it.
object RuleConflicts {
    fun find(rules: List<Rule>): List<Pair<Rule, Rule>> {
        val on = rules.filter { it.enabled }
        return on.flatMapIndexed { i, a ->
            on.drop(i + 1).filter { b -> RuleEngine.detail(a.conditions) == RuleEngine.detail(b.conditions) && differ(a, b) && mayOverlap(a.conditions, b.conditions) }
                .map { a to it }
        }
    }

    private fun differ(a: Rule, b: Rule): Boolean = a.action.ignore != b.action.ignore ||
        (a.action.category != null && b.action.category != null && (a.action.category != b.action.category || a.action.subcategory != b.action.subcategory))

    private fun mayOverlap(a: RuleConditions, b: RuleConditions): Boolean =
        (a.text == null || b.text == null || a.text.value.trim().lowercase().let { x -> b.text.value.trim().lowercase().let { y -> x.contains(y) || y.contains(x) } }) &&
            (a.amount == null || b.amount == null || amountsOverlap(a.amount, b.amount)) &&
            (a.time == null || b.time == null || timesOverlap(a.time, b.time)) &&
            (a.date == null || b.date == null || datesOverlap(a, b)) &&
            (a.accounts.isEmpty() || b.accounts.isEmpty() || a.accounts.any { it in b.accounts }) &&
            (a.type == null || b.type == null || a.type == b.type)

    private fun amountsOverlap(a: AmountRange, b: AmountRange): Boolean =
        (a.min ?: Double.NEGATIVE_INFINITY) <= (b.max ?: Double.POSITIVE_INFINITY) && (b.min ?: Double.NEGATIVE_INFINITY) <= (a.max ?: Double.POSITIVE_INFINITY)

    private fun timesOverlap(a: TimeRange, b: TimeRange): Boolean =
        RuleEngine.inTime(a.fromMinutes, a.toMinutes, b.fromMinutes) || RuleEngine.inTime(b.fromMinutes, b.toMinutes, a.fromMinutes)

    private fun datesOverlap(a: RuleConditions, b: RuleConditions): Boolean {
        val x = a.date!!
        val y = b.date!!
        return (x.day == null || y.day == null || daysOverlap(x.day, x.dayTo ?: x.day, y.day, y.dayTo ?: y.day)) && (x.month == null || y.month == null || x.month == y.month) &&
            (x.year == null || y.year == null || x.year == y.year) && (x.weekdays.isEmpty() || y.weekdays.isEmpty() || x.weekdays.any { it in y.weekdays })
    }

    private fun daysOverlap(af: Int, at: Int, bf: Int, bt: Int): Boolean =
        RuleEngine.inDays(af, at, bf) || RuleEngine.inDays(bf, bt, af)
}
