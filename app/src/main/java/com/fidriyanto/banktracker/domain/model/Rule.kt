package com.fidriyanto.banktracker.domain.model

import java.time.LocalDateTime

enum class TextMode { CONTAINS, STARTS_WITH, EQUALS }

data class TextMatch(val mode: TextMode, val value: String)

// A missing bound means no limit on that side. At least one bound is set.
data class AmountRange(val min: Double?, val max: Double?)

// Minutes after midnight. When from is later than to, the range crosses midnight.
data class TimeRange(val fromMinutes: Int, val toMinutes: Int)

// Every part that is set must match. Weekdays use 1 for Monday to 7 for Sunday.
// A day with a dayTo is a range of days of the month. When dayTo is lower than day, the range crosses the end of the month.
data class DateMatch(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
    val weekdays: List<Int> = emptyList(),
    val dayTo: Int? = null,
)

// A condition that is missing matches every transaction. More conditions make a rule more detailed.
// A rule with no condition at all matches nothing.
data class RuleConditions(
    val text: TextMatch? = null,
    val amount: AmountRange? = null,
    val time: TimeRange? = null,
    val date: DateMatch? = null,
    val accounts: List<String> = emptyList(),
    val type: String? = null,
)

// The description may hold {merchant} and {amount}. An ignore rule sets nothing else.
data class RuleAction(
    val category: String? = null,
    val subcategory: String? = null,
    val description: String? = null,
    val ignore: Boolean = false,
)

data class Rule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val conditions: RuleConditions,
    val action: RuleAction,
)

// What a rule sees of a transaction. The text is the merchant or the description.
data class RuleInput(
    val text: String,
    val amount: Double,
    val type: String,
    val account: String?,
    val at: LocalDateTime,
)

data class RuleOutcome(
    val category: String? = null,
    val subcategory: String? = null,
    val description: String? = null,
    val ignore: Boolean = false,
    val ruleIds: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = category == null && description == null && !ignore
}
