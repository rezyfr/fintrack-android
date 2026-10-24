package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.settings.schedules.label
import com.fidriyanto.banktracker.ui.theme.Space

// ac: installment-plan-add - step 2: the monthly amount, the steps, the due day and the statement text
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanPaymentsStep(d: InstallmentPlanDraft, showErrors: Boolean, accounts: List<Account>, plans: List<Installment>, onEdit: ((InstallmentPlanDraft) -> InstallmentPlanDraft) -> Unit) {
    val error = if (showErrors) d.paymentsError() else null
    val currency = accounts.firstOrNull { it.id == d.walletId }?.currency
    val finish = d.estimatedFinish()
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.plans_pay_title), stringResource(R.string.plans_pay_body))
        OutlinedTextField(
            value = d.amount, onValueChange = { v -> onEdit { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) } },
            label = { Text(if (currency != null) stringResource(R.string.plans_amount_in, currency) else stringResource(R.string.plans_amount)) },
            textStyle = MaterialTheme.typography.headlineMedium, singleLine = true, isError = error == PlanError.AMOUNT,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
        )
        if (error == PlanError.AMOUNT) ErrorText(stringResource(R.string.plans_error_amount))
        NumberField(d.total, R.string.plans_total, error == PlanError.TOTAL) { v -> onEdit { it.copy(total = v) } }
        if (error == PlanError.TOTAL) ErrorText(stringResource(R.string.plans_error_total))
        NumberField(d.paid, R.string.plans_paid, error == PlanError.PAID) { v -> onEdit { it.copy(paid = v) } }
        if (error == PlanError.PAID) ErrorText(stringResource(R.string.plans_error_paid))
        Text(stringResource(R.string.plans_due_day), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            (1..31).forEach { day -> FilterChip(selected = d.dueDay == day, onClick = { onEdit { it.copy(dueDay = day) } }, label = { Text(day.toString()) }) }
        }
        OutlinedTextField(
            value = d.statement ?: d.name, onValueChange = { v -> onEdit { it.copy(statement = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.plans_statement)) }, isError = error == PlanError.STATEMENT,
            supportingText = { Text(stringResource(R.string.plans_statement_hint)) },
        )
        if (error == PlanError.STATEMENT) ErrorText(stringResource(R.string.plans_error_statement))
        if (finish != null) Text(stringResource(R.string.plans_finish, finish.label()), color = MaterialTheme.colorScheme.onSurface)
        if (d.paymentsError() == null && finish == null) Text(stringResource(R.string.plans_finish_done), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (d.collides(plans)) Text(stringResource(R.string.plans_duplicate), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.plans_locked), Modifier.padding(top = Space.sm), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NumberField(value: String, label: Int, isError: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(3)) }, singleLine = true, isError = isError,
        label = { Text(stringResource(label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
    )
}
