package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Category
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
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.INCOME_CATEGORIES
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.ui.common.CategoryPicker
import com.fidriyanto.banktracker.ui.common.PickerCategory
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.OpenCard
import com.fidriyanto.banktracker.ui.settings.rules.OptionCard
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space

private val types = listOf("expense" to R.string.schedule_type_expense, "income" to R.string.schedule_type_income, "transfer" to R.string.schedule_type_transfer)

// ac: scheduled-transactions - step 1: how much, and for what. The subcategory turns on with its own button.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleWhatStep(d: ScheduleDraft, showErrors: Boolean, accounts: List<Account>, categories: List<Category>, plans: List<Installment>, frequentByType: Map<String, List<String>>, onEdit: ((ScheduleDraft) -> ScheduleDraft) -> Unit) {
    val error = if (showErrors) d.whatError() else null
    val from = accounts.firstOrNull { it.id == d.walletId }
    val chosen = categories.firstOrNull { it.name == d.category }
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.schedule_what_title), stringResource(R.string.schedule_what_body))
        OutlinedTextField(
            value = d.amount, onValueChange = { v -> onEdit { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) } },
            label = { Text(from?.currency?.let { stringResource(R.string.schedule_amount_in, it) } ?: stringResource(R.string.schedule_amount)) },
            textStyle = MaterialTheme.typography.headlineMedium, singleLine = true, isError = error == WhatError.AMOUNT,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
        )
        if (error == WhatError.AMOUNT) ErrorText(stringResource(R.string.schedule_error_amount))
        OutlinedTextField(
            value = d.name, onValueChange = { v -> onEdit { it.copy(name = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.schedule_name)) }, supportingText = { Text(stringResource(R.string.schedule_name_hint)) },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            types.forEach { (id, label) ->
                FilterChip(selected = d.txType == id, onClick = { onEdit { it.copy(txType = id, toWalletId = null, category = "", subcategory = null, subOn = false, installmentId = null) } }, label = { Text(stringResource(label)) })
            }
        }
        val fromLabel = when (d.txType) {
            "income" -> R.string.schedule_paid_into
            "transfer" -> R.string.schedule_move_from
            else -> R.string.schedule_paid_from
        }
        AccountRow(Icons.Outlined.AccountBalance, stringResource(fromLabel), from, accounts) { a -> onEdit { it.copy(walletId = a.id, toWalletId = null, installmentId = null) } }
        if (error == WhatError.ACCOUNT) ErrorText(stringResource(R.string.schedule_error_account))
        if (d.txType == "expense") SchedulePlanLink(d, plans, onEdit)
        if (d.txType == "transfer") {
            // The received amount is the same as the sent one, so only an account in the same currency fits.
            val targets = accounts.filter { it.id != d.walletId && it.currency == from?.currency }
            AccountRow(Icons.Outlined.AccountBalance, stringResource(R.string.schedule_move_to), targets.firstOrNull { it.id == d.toWalletId }, targets) { a -> onEdit { it.copy(toWalletId = a.id) } }
            Text(stringResource(R.string.schedule_transfer_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (error == WhatError.TO_ACCOUNT) ErrorText(stringResource(R.string.schedule_error_to_account))
        } else {
            Text(stringResource(R.string.schedule_category), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            // Income has its own list of categories, the same as the Add form.
            val options = if (d.txType == "income") INCOME_CATEGORIES.map { PickerCategory(it) } else categories.map { PickerCategory(it.name, it.subcategories) }
            CategoryPicker(options, frequentByType[d.txType].orEmpty(), d.category) { name, s -> onEdit { it.copy(category = name, subOn = s != null, subcategory = s) } }
            if (error == WhatError.CATEGORY) ErrorText(stringResource(R.string.schedule_error_category))
            if (chosen != null && chosen.subcategories.isNotEmpty()) {
                if (d.subOn) {
                    OpenCard(Icons.Outlined.Category, stringResource(R.string.schedule_sub_title), onRemove = { onEdit { it.copy(subOn = false, subcategory = null) } }) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.padding(top = Space.xs)) {
                            chosen.subcategories.forEach { s -> FilterChip(selected = d.subcategory == s, onClick = { onEdit { it.copy(subcategory = s) } }, label = { Text(s) }) }
                        }
                    }
                } else {
                    OptionCard(Icons.Outlined.Category, stringResource(R.string.schedule_sub_title), stringResource(R.string.schedule_sub_hint, chosen.name), stringResource(R.string.schedule_sub_add)) { onEdit { it.copy(subOn = true) } }
                }
            }
        }
    }
}
