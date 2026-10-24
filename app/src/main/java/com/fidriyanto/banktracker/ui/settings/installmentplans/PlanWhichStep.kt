package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.domain.usecase.RECENT_PAYMENT_DAYS
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.OptionCard
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.settings.schedules.AccountRow
import com.fidriyanto.banktracker.ui.theme.Space

// ac: installment-plan-add - step 1: the name, the card account, and an optional start from a recent payment
@Composable
fun PlanWhichStep(d: InstallmentPlanDraft, showErrors: Boolean, accounts: List<Account>, onEdit: ((InstallmentPlanDraft) -> InstallmentPlanDraft) -> Unit, onStartFromPayment: () -> Unit) {
    val error = if (showErrors) d.planError() else null
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.plans_which_title), stringResource(R.string.plans_which_body))
        OutlinedTextField(
            value = d.name, onValueChange = { v -> onEdit { it.copy(name = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.plans_name)) }, isError = error == PlanError.NAME,
        )
        if (error == PlanError.NAME) ErrorText(stringResource(R.string.plans_error_name))
        AccountRow(Icons.Outlined.AccountBalance, stringResource(R.string.plans_account), accounts.firstOrNull { it.id == d.walletId }, accounts) { a -> onEdit { it.copy(walletId = a.id) } }
        if (error == PlanError.ACCOUNT) ErrorText(stringResource(R.string.plans_error_account))
        OptionCard(
            Icons.Outlined.Receipt, stringResource(R.string.plans_start_title),
            stringResource(if (d.walletId.isBlank()) R.string.plans_start_need_account else R.string.plans_start_hint),
            stringResource(R.string.plans_start_button), onStartFromPayment,
        )
    }
}

// The expenses of the last days on the chosen account. A tap fills the plan from that payment.
@Composable
fun PaymentPickerDialog(picker: PaymentPicker, currency: String, onPick: (TransactionUiModel) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plans_picker_title, RECENT_PAYMENT_DAYS)) },
        text = {
            when {
                picker.loading -> CircularProgressIndicator()
                picker.failed -> Text(stringResource(R.string.plans_picker_failed), color = MaterialTheme.colorScheme.error)
                picker.rows.isEmpty() -> Text(stringResource(R.string.plans_picker_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    picker.rows.forEach { tx ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(tx) }.padding(vertical = Space.sm)) {
                            Text(tx.item, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(R.string.plans_picker_row, formatMoney(currency, tx.amount), tx.dateIso.take(10)),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
