package com.fidriyanto.banktracker.ui.settings.rules

import com.fidriyanto.banktracker.domain.model.AmountRange
import com.fidriyanto.banktracker.domain.model.DateMatch
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.TextMatch
import com.fidriyanto.banktracker.domain.model.TextMode
import com.fidriyanto.banktracker.domain.model.TimeRange

enum class WhenError { NONE, TEXT, AMOUNT, TIME, DATE, ACCOUNT }

enum class ThenError { CATEGORY, DESCRIPTION }

// What the editor holds while the user fills in a rule. An optional part counts only when its switch is on.
data class RuleDraft(
    val id: String? = null,
    val enabled: Boolean = true,
    val name: String = "",
    val textOn: Boolean = true,
    val textMode: TextMode = TextMode.CONTAINS,
    val text: String = "",
    val amountOn: Boolean = false,
    val amountMin: String = "",
    val amountMax: String = "",
    val timeOn: Boolean = false,
    val timeFrom: String = "",
    val timeTo: String = "",
    val dateOn: Boolean = false,
    val day: String = "",
    val dayTo: String = "",
    val month: String = "",
    val year: String = "",
    val weekdays: Set<Int> = emptySet(),
    val accountOn: Boolean = false,
    val accounts: Set<String> = emptySet(),
    val typeOn: Boolean = false,
    val type: String = "expense",
    val category: String? = null,
    val subOn: Boolean = false,
    val subcategory: String? = null,
    val descOn: Boolean = false,
    val description: String = "",
    val ignore: Boolean = false,
) {
    // The number of conditions that are on. A rule needs at least one.
    val detail: Int get() = listOf(textOn, amountOn, timeOn, dateOn, accountOn, typeOn).count { it }

    fun whenError(): WhenError? = when {
        detail == 0 -> WhenError.NONE
        textOn && text.isBlank() -> WhenError.TEXT
        amountOn && amountRange() == null -> WhenError.AMOUNT
        timeOn && timeRange() == null -> WhenError.TIME
        dateOn && dateMatch() == null -> WhenError.DATE
        accountOn && accounts.isEmpty() -> WhenError.ACCOUNT
        else -> null
    }

    fun thenError(): ThenError? = when {
        ignore -> null
        category == null -> ThenError.CATEGORY
        descOn && description.isBlank() -> ThenError.DESCRIPTION
        else -> null
    }

    private fun amountRange(): AmountRange? {
        val min = amountMin.trim().takeIf { it.isNotEmpty() }?.let { it.toDoubleOrNull() ?: return null }
        val max = amountMax.trim().takeIf { it.isNotEmpty() }?.let { it.toDoubleOrNull() ?: return null }
        if (min == null && max == null) return null
        if ((min ?: 0.0) < 0.0 || (max ?: 0.0) < 0.0 || (min != null && max != null && min > max)) return null
        return AmountRange(min, max)
    }

    private fun timeRange(): TimeRange? {
        val from = parseMinutes(timeFrom) ?: return null
        val to = parseMinutes(timeTo) ?: return null
        return TimeRange(from, to)
    }

    private fun dateMatch(): DateMatch? {
        val d = day.trim().takeIf { it.isNotEmpty() }?.let { (it.toIntOrNull() ?: return null).takeIf { n -> n in 1..31 } ?: return null }
        val dTo = dayTo.trim().takeIf { it.isNotEmpty() }?.let { (it.toIntOrNull() ?: return null).takeIf { n -> n in 1..31 } ?: return null }
        if (dTo != null && d == null) return null
        val m = month.trim().takeIf { it.isNotEmpty() }?.let { (it.toIntOrNull() ?: return null).takeIf { n -> n in 1..12 } ?: return null }
        val y = year.trim().takeIf { it.isNotEmpty() }?.let { (it.toIntOrNull() ?: return null).takeIf { n -> n in 2000..2100 } ?: return null }
        if (d == null && m == null && y == null && weekdays.isEmpty()) return null
        return DateMatch(d, m, y, weekdays.sorted(), dTo?.takeIf { it != d })
    }

    // Call this only when whenError() and thenError() are null.
    fun toRule(newId: String): Rule = Rule(
        id = id ?: newId,
        name = name.trim(),
        enabled = enabled,
        conditions = RuleConditions(
            text = if (textOn) TextMatch(textMode, text.trim()) else null,
            amount = if (amountOn) amountRange() else null,
            time = if (timeOn) timeRange() else null,
            date = if (dateOn) dateMatch() else null,
            accounts = if (accountOn) accounts.toList() else emptyList(),
            type = if (typeOn) type else null,
        ),
        action = if (ignore) RuleAction(ignore = true) else RuleAction(
            category = category,
            subcategory = if (subOn) subcategory else null,
            description = if (descOn) description.trim() else null,
        ),
    )

    companion object {
        fun from(rule: Rule): RuleDraft {
            val c = rule.conditions
            val a = rule.action
            return RuleDraft(
                id = rule.id, enabled = rule.enabled, name = rule.name,
                textOn = c.text != null, textMode = c.text?.mode ?: TextMode.CONTAINS, text = c.text?.value.orEmpty(),
                amountOn = c.amount != null, amountMin = c.amount?.min?.let(::plain).orEmpty(), amountMax = c.amount?.max?.let(::plain).orEmpty(),
                timeOn = c.time != null, timeFrom = c.time?.let { formatMinutes(it.fromMinutes) }.orEmpty(), timeTo = c.time?.let { formatMinutes(it.toMinutes) }.orEmpty(),
                dateOn = c.date != null, day = c.date?.day?.toString().orEmpty(), dayTo = c.date?.dayTo?.toString().orEmpty(), month = c.date?.month?.toString().orEmpty(),
                year = c.date?.year?.toString().orEmpty(), weekdays = c.date?.weekdays.orEmpty().toSet(),
                accountOn = c.accounts.isNotEmpty(), accounts = c.accounts.toSet(),
                typeOn = c.type != null, type = c.type ?: "expense",
                category = a.category, subOn = a.subcategory != null, subcategory = a.subcategory,
                descOn = a.description != null, description = a.description.orEmpty(), ignore = a.ignore,
            )
        }

        private fun plain(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
    }
}

// "12:30" or "9:05" as minutes after midnight, or null when the text is not a time.
fun parseMinutes(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59 && parts[1].length == 2) h * 60 + m else null
}

fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
