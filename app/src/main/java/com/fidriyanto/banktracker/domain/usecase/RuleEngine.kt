package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.RuleOutcome
import com.fidriyanto.banktracker.domain.model.TextMode
import java.util.Locale

// Decides what the rules do to one transaction. For each field the most detailed matching rule wins.
// A tie goes to the rule that is higher in the list. A less detailed rule still fills the fields a more
// detailed rule leaves empty.
object RuleEngine {
    fun detail(c: RuleConditions): Int =
        listOfNotNull(c.text, c.amount, c.time, c.date, c.type).size + if (c.accounts.isNotEmpty()) 1 else 0

    fun matches(rule: Rule, input: RuleInput): Boolean {
        val c = rule.conditions
        return rule.enabled && detail(c) > 0 &&
            (c.text?.let { matchText(it.mode, it.value, input.text) } ?: true) &&
            (c.amount?.let { (it.min == null || input.amount >= it.min) && (it.max == null || input.amount <= it.max) } ?: true) &&
            (c.time?.let { inTime(it.fromMinutes, it.toMinutes, input.at.hour * 60 + input.at.minute) } ?: true) &&
            (c.date?.let { d ->
                (d.day == null || inDays(d.day, d.dayTo ?: d.day, input.at.dayOfMonth)) && (d.month == null || input.at.monthValue == d.month) &&
                    (d.year == null || input.at.year == d.year) && (d.weekdays.isEmpty() || input.at.dayOfWeek.value in d.weekdays)
            } ?: true) &&
            (c.accounts.isEmpty() || input.account in c.accounts) &&
            (c.type?.let { it == input.type } ?: true)
    }

    fun apply(rules: List<Rule>, input: RuleInput): RuleOutcome {
        val hits = rules.withIndex().filter { matches(it.value, input) }
            .sortedWith(compareByDescending<IndexedValue<Rule>> { detail(it.value.conditions) }.thenBy { it.index })
            .map { it.value }
        val decider = hits.firstOrNull { it.action.ignore || it.action.category != null }
        if (decider?.action?.ignore == true) return RuleOutcome(ignore = true, ruleIds = listOf(decider.id))
        val category = decider?.action?.category
        val sub = category?.let { c -> hits.firstOrNull { it.action.subcategory != null && it.action.category == c }?.action?.subcategory }
        val description = hits.firstOrNull { !it.action.description.isNullOrBlank() }?.action?.description?.let { render(it, input) }
        val used = hits.filter { (category != null && it.action.category == category) || (description != null && it.action.description != null) }
        return RuleOutcome(category, sub, description, false, used.map { it.id })
    }

    private fun matchText(mode: TextMode, value: String, text: String): Boolean {
        val v = value.trim().lowercase()
        val t = text.trim().lowercase()
        return when (mode) {
            TextMode.CONTAINS -> t.contains(v)
            TextMode.STARTS_WITH -> t.startsWith(v)
            TextMode.EQUALS -> t == v
        }
    }

    // A range of days with from later than to crosses the end of the month.
    fun inDays(from: Int, to: Int, day: Int): Boolean = if (from <= to) day in from..to else day >= from || day <= to

    // A range with from later than to crosses midnight.
    fun inTime(from: Int, to: Int, minute: Int): Boolean = if (from <= to) minute in from..to else minute >= from || minute <= to

    private fun render(template: String, input: RuleInput): String {
        val amount = if (input.amount % 1.0 == 0.0) String.format(Locale.US, "%.0f", input.amount) else String.format(Locale.US, "%.2f", input.amount)
        return template.replace("{merchant}", input.text.trim()).replace("{amount}", amount)
    }
}
