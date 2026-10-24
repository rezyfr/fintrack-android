package com.fidriyanto.banktracker.ui.settings.rules

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.TextMode
import com.fidriyanto.banktracker.ui.common.AccountStore
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

fun plain(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

fun typeLabelRes(id: String): Int = when (id) {
    "income" -> R.string.tx_type_income
    "transfer" -> R.string.tx_type_transfer
    "investment" -> R.string.tx_type_investment
    else -> R.string.tx_type_expense
}

// Parts joined with commas and a last "and". One part stays as it is.
@Composable
fun joinNatural(parts: List<String>): String {
    if (parts.size < 2) return parts.firstOrNull().orEmpty()
    return parts.dropLast(1).joinToString(", ") + " " + stringResource(R.string.rules_and) + " " + parts.last()
}

// The conditions of a rule as short phrases, such as "the amount is 20 to 80".
@Composable
fun conditionParts(c: RuleConditions): List<String> = buildList {
    c.text?.let {
        add(stringResource(when (it.mode) {
            TextMode.CONTAINS -> R.string.rules_sum_text_contains
            TextMode.STARTS_WITH -> R.string.rules_sum_text_starts
            TextMode.EQUALS -> R.string.rules_sum_text_equals
        }, it.value))
    }
    c.amount?.let { a ->
        add(when {
            a.min != null && a.min == a.max -> stringResource(R.string.rules_sum_amount_exact, plain(a.min))
            a.min != null && a.max != null -> stringResource(R.string.rules_sum_amount_between, plain(a.min), plain(a.max))
            a.min != null -> stringResource(R.string.rules_sum_amount_least, plain(a.min))
            else -> stringResource(R.string.rules_sum_amount_most, plain(a.max ?: 0.0))
        })
    }
    c.time?.let { add(stringResource(R.string.rules_sum_time, formatMinutes(it.fromMinutes), formatMinutes(it.toMinutes))) }
    c.date?.let { d ->
        val days = d.day?.let { from -> d.dayTo?.let { stringResource(R.string.rules_sum_day_range, from, it) } ?: from.toString() }
        val day = listOfNotNull(days, d.month?.let { Month.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }, d.year?.toString()).joinToString(" ")
        if (day.isNotEmpty()) add(stringResource(R.string.rules_sum_date, day))
        if (d.weekdays.isNotEmpty()) add(stringResource(R.string.rules_sum_weekdays, d.weekdays.joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }))
    }
    if (c.accounts.isNotEmpty()) add(stringResource(R.string.rules_sum_account, c.accounts.joinToString(", ") { id -> AccountStore.all.firstOrNull { it.id == id }?.displayName ?: id }))
    c.type?.let { add(stringResource(R.string.rules_sum_type, stringResource(typeLabelRes(it)))) }
}

// What the rule does, such as "set the category to Transport and the description to Late taxi".
@Composable
fun actionSentence(a: RuleAction): String {
    if (a.ignore) return stringResource(R.string.rules_sum_ignore)
    val parts = buildList {
        a.category?.let { add(stringResource(R.string.rules_sum_category, it)) }
        a.subcategory?.let { add(stringResource(R.string.rules_sum_subcategory, it)) }
        a.description?.let { add(stringResource(R.string.rules_sum_description, it)) }
    }
    return joinNatural(parts)
}

@Composable
fun ruleSentence(rule: Rule): String = stringResource(R.string.rules_sum_when, joinNatural(conditionParts(rule.conditions)), actionSentence(rule.action))
