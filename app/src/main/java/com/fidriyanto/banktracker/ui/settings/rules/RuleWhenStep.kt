package com.fidriyanto.banktracker.ui.settings.rules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.TextMode
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.Space

private val TYPES = listOf("expense", "income", "transfer", "investment")

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, decimal: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

// The first step. Only the text shows at first. Each button turns on one more condition.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleWhenStep(d: RuleDraft, showErrors: Boolean, accounts: List<Account>, onEdit: ((RuleDraft) -> RuleDraft) -> Unit) {
    val error = if (showErrors) d.whenError() else null
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.rules_when_title), stringResource(R.string.rules_when_body))
        if (error == WhenError.NONE) ErrorText(stringResource(R.string.rules_error_none))
        if (d.textOn) OpenCard(Icons.Outlined.TextFields, stringResource(R.string.rules_text_title), onRemove = { onEdit { it.copy(textOn = false, text = "") } }) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                TextMode.entries.forEach { m ->
                    FilterChip(selected = d.textMode == m, onClick = { onEdit { it.copy(textMode = m) } }, label = {
                        Text(stringResource(when (m) { TextMode.CONTAINS -> R.string.rules_mode_contains; TextMode.STARTS_WITH -> R.string.rules_mode_starts; TextMode.EQUALS -> R.string.rules_mode_equals }))
                    })
                }
            }
            OutlinedTextField(value = d.text, onValueChange = { v -> onEdit { it.copy(text = v) } }, label = { Text(stringResource(R.string.rules_text_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (error == WhenError.TEXT) ErrorText(stringResource(R.string.rules_error_text))
        }
        if (d.amountOn) OpenCard(Icons.Outlined.Payments, stringResource(R.string.rules_amount_title), onRemove = { onEdit { it.copy(amountOn = false) } }) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                NumberField(stringResource(R.string.rules_amount_from), d.amountMin, { v -> onEdit { it.copy(amountMin = v) } }, Modifier.weight(1f), decimal = true)
                NumberField(stringResource(R.string.rules_amount_to), d.amountMax, { v -> onEdit { it.copy(amountMax = v) } }, Modifier.weight(1f), decimal = true)
            }
            if (error == WhenError.AMOUNT) ErrorText(stringResource(R.string.rules_error_amount))
        }
        if (d.timeOn) OpenCard(Icons.Outlined.Schedule, stringResource(R.string.rules_time_title), onRemove = { onEdit { it.copy(timeOn = false) } }) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedTextField(d.timeFrom, { v -> onEdit { it.copy(timeFrom = v) } }, Modifier.weight(1f), label = { Text(stringResource(R.string.rules_time_from)) }, placeholder = { Text("12:30") }, singleLine = true)
                OutlinedTextField(d.timeTo, { v -> onEdit { it.copy(timeTo = v) } }, Modifier.weight(1f), label = { Text(stringResource(R.string.rules_time_to)) }, placeholder = { Text("13:30") }, singleLine = true)
            }
            if (error == WhenError.TIME) ErrorText(stringResource(R.string.rules_error_time))
        }
        if (d.dateOn) OpenCard(Icons.Outlined.CalendarMonth, stringResource(R.string.rules_date_title), onRemove = { onEdit { it.copy(dateOn = false) } }) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                NumberField(stringResource(R.string.rules_date_day), d.day, { v -> onEdit { it.copy(day = v) } }, Modifier.weight(1f))
                NumberField(stringResource(R.string.rules_date_day_to), d.dayTo, { v -> onEdit { it.copy(dayTo = v) } }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                NumberField(stringResource(R.string.rules_date_month), d.month, { v -> onEdit { it.copy(month = v) } }, Modifier.weight(1f))
                NumberField(stringResource(R.string.rules_date_year), d.year, { v -> onEdit { it.copy(year = v) } }, Modifier.weight(1f))
            }
            Text(stringResource(R.string.rules_date_weekdays), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                (1..7).forEach { n ->
                    FilterChip(selected = n in d.weekdays, onClick = { onEdit { it.copy(weekdays = if (n in it.weekdays) it.weekdays - n else it.weekdays + n) } },
                        label = { Text(DayOfWeek.of(n).getDisplayName(TextStyle.SHORT, Locale.getDefault())) })
                }
            }
            if (error == WhenError.DATE) ErrorText(stringResource(R.string.rules_error_date))
        }
        if (d.accountOn) OpenCard(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.rules_account_title), onRemove = { onEdit { it.copy(accountOn = false, accounts = emptySet()) } }) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                accounts.forEach { a ->
                    FilterChip(selected = a.id in d.accounts, onClick = { onEdit { it.copy(accounts = if (a.id in it.accounts) it.accounts - a.id else it.accounts + a.id) } }, label = { Text(a.displayName) })
                }
            }
            if (error == WhenError.ACCOUNT) ErrorText(stringResource(R.string.rules_error_account))
        }
        if (d.typeOn) OpenCard(Icons.Outlined.SwapVert, stringResource(R.string.rules_type_title), onRemove = { onEdit { it.copy(typeOn = false) } }) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                TYPES.forEach { t -> FilterChip(selected = d.type == t, onClick = { onEdit { it.copy(type = t) } }, label = { Text(stringResource(typeLabelRes(t))) }) }
            }
        }
        if (!(d.textOn && d.amountOn && d.timeOn && d.dateOn && d.accountOn && d.typeOn)) {
            Text(stringResource(R.string.rules_more_precise), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.sm))
        }
        if (!d.textOn) OptionCard(Icons.Outlined.TextFields, stringResource(R.string.rules_text_title), stringResource(R.string.rules_text_hint), stringResource(R.string.rules_text_add)) { onEdit { it.copy(textOn = true) } }
        if (!d.amountOn) OptionCard(Icons.Outlined.Payments, stringResource(R.string.rules_amount_title), stringResource(R.string.rules_amount_hint), stringResource(R.string.rules_amount_add)) { onEdit { it.copy(amountOn = true) } }
        if (!d.timeOn) OptionCard(Icons.Outlined.Schedule, stringResource(R.string.rules_time_title), stringResource(R.string.rules_time_hint), stringResource(R.string.rules_time_add)) { onEdit { it.copy(timeOn = true) } }
        if (!d.dateOn) OptionCard(Icons.Outlined.CalendarMonth, stringResource(R.string.rules_date_title), stringResource(R.string.rules_date_hint), stringResource(R.string.rules_date_add)) { onEdit { it.copy(dateOn = true) } }
        if (!d.accountOn) OptionCard(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.rules_account_title), stringResource(R.string.rules_account_hint), stringResource(R.string.rules_account_add)) { onEdit { it.copy(accountOn = true) } }
        if (!d.typeOn) OptionCard(Icons.Outlined.SwapVert, stringResource(R.string.rules_type_title), stringResource(R.string.rules_type_hint), stringResource(R.string.rules_type_add)) { onEdit { it.copy(typeOn = true) } }
        DetailDots(d.detail)
    }
}
