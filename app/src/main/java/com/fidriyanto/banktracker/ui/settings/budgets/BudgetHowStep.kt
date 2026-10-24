package com.fidriyanto.banktracker.ui.settings.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.OpenCard
import com.fidriyanto.banktracker.ui.settings.rules.OptionCard
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space

// Step 2: the monthly target, its currency, and for a fixed line an optional due day. The last line sums it up.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetHowStep(d: BudgetDraft, showErrors: Boolean, duplicate: BudgetLine?, accounts: List<Account>, onEdit: ((BudgetDraft) -> BudgetDraft) -> Unit) {
    val currencies = (accounts.map { it.currency } + d.currency).filter { it.isNotBlank() }.distinct()
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.budget_how_title), stringResource(R.string.budget_how_body))
        OutlinedTextField(
            value = d.target, onValueChange = { v -> onEdit { it.copy(target = v.filter { c -> c.isDigit() || c == '.' }) } },
            label = { Text(if (d.currency.isBlank()) stringResource(R.string.budget_target) else stringResource(R.string.budget_target_in, d.currency)) },
            textStyle = MaterialTheme.typography.headlineMedium, singleLine = true, isError = showErrors && d.howError() != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
        )
        if (showErrors && d.howError() != null) ErrorText(stringResource(R.string.budget_error_target))
        Text(stringResource(R.string.budget_currency), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            currencies.forEach { code -> FilterChip(selected = d.currency == code, onClick = { onEdit { it.copy(currency = code) } }, label = { Text(code) }) }
        }
        if (d.kind == KIND_FIXED) {
            if (d.dueOn) {
                OpenCard(Icons.Outlined.Event, stringResource(R.string.budget_due_title), onRemove = { onEdit { it.copy(dueOn = false) } }) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        (1..31).forEach { day -> FilterChip(selected = d.dueDay == day, onClick = { onEdit { it.copy(dueDay = day) } }, label = { Text(day.toString()) }) }
                    }
                }
            } else {
                OptionCard(Icons.Outlined.Event, stringResource(R.string.budget_due_title), stringResource(R.string.budget_due_hint), stringResource(R.string.budget_due_add)) { onEdit { it.copy(dueOn = true) } }
            }
        }
        if (duplicate != null) {
            Text(stringResource(R.string.budget_duplicate_warning, duplicate.name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (d.byRules) Text(stringResource(R.string.budget_rules_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(summary(d), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun summary(d: BudgetDraft): String {
    val value = d.targetValue ?: 0.0
    // With no account there is no currency yet, and formatMoney needs one.
    val amount = if (d.currency.isBlank()) value.toString().removeSuffix(".0") else formatMoney(d.currency, value)
    return if (d.kind == KIND_FIXED && d.dueOn) {
        stringResource(R.string.budget_summary_due, d.displayName, amount, d.dueDay)
    } else {
        stringResource(R.string.budget_summary, d.displayName, amount)
    }
}
