package com.fidriyanto.banktracker.ui.settings.custombanks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.settings.schedules.AccountRow
import com.fidriyanto.banktracker.ui.theme.Space

private val types = listOf("expense" to R.string.custom_bank_type_expense, "income" to R.string.custom_bank_type_income)

// ac: custom-bank-patterns - step 3: name, account, type, title filter, and a test of the pattern
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomBankCheckStep(d: CustomBankDraft, showErrors: Boolean, accounts: List<Account>, onEdit: ((CustomBankDraft) -> CustomBankDraft) -> Unit) {
    val account = accounts.firstOrNull { it.id == d.walletId }
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.custom_bank_check_title), stringResource(R.string.custom_bank_check_body))
        OutlinedTextField(
            value = d.name, onValueChange = { v -> onEdit { it.copy(name = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_bank_name)) }, isError = showErrors && d.nameError(),
        )
        if (showErrors && d.nameError()) ErrorText(stringResource(R.string.custom_bank_error_name))
        AccountRow(Icons.Outlined.AccountBalance, stringResource(R.string.custom_bank_account), account, accounts) { a -> onEdit { it.copy(walletId = a.id) } }
        if (showErrors && d.walletId.isBlank()) ErrorText(stringResource(R.string.custom_bank_error_account))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            types.forEach { (id, label) ->
                FilterChip(selected = d.txType == id, onClick = { onEdit { it.copy(txType = id) } }, label = { Text(stringResource(label)) })
            }
        }
        OutlinedTextField(
            value = d.titleContains, onValueChange = { v -> onEdit { it.copy(titleContains = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_bank_title_filter)) }, supportingText = { Text(stringResource(R.string.custom_bank_title_filter_hint)) },
        )
        TryItCard(d, account, onEdit)
    }
}

@Composable
private fun TryItCard(d: CustomBankDraft, account: Account?, onEdit: ((CustomBankDraft) -> CustomBankDraft) -> Unit) {
    val result = d.tryResult()
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(stringResource(R.string.custom_bank_try_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = d.tryText, onValueChange = { v -> onEdit { it.copy(tryText = v) } }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.custom_bank_field_text)) },
            )
            if (result == null) {
                Text(stringResource(R.string.custom_bank_try_no_match), color = MaterialTheme.colorScheme.error)
            } else {
                val line = MaterialTheme.typography.bodyMedium
                Text(stringResource(R.string.custom_bank_try_amount, formatMoney(account?.currency.orEmpty(), result.amount)), style = line, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.custom_bank_try_merchant, result.merchant ?: d.name.trim()), style = line, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.custom_bank_try_account, account?.displayName.orEmpty()), style = line, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
