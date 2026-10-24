package com.fidriyanto.banktracker.ui.settings.custombanks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CustomBankTemplate
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space

// ac: custom-bank-patterns - step 2: tap the words of the example to mark the amount and the merchant
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomBankMarkStep(d: CustomBankDraft, showErrors: Boolean, onEdit: ((CustomBankDraft) -> CustomBankDraft) -> Unit) {
    val error = if (showErrors) d.markError() else null
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.custom_bank_mark_title), stringResource(R.string.custom_bank_mark_body))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            FilterChip(selected = d.mode == MarkMode.AMOUNT, onClick = { onEdit { it.copy(mode = MarkMode.AMOUNT) } }, label = { Text(stringResource(R.string.custom_bank_mode_amount)) })
            FilterChip(selected = d.mode == MarkMode.MERCHANT, onClick = { onEdit { it.copy(mode = MarkMode.MERCHANT) } }, label = { Text(stringResource(R.string.custom_bank_mode_merchant)) })
        }
        Text(
            stringResource(if (d.mode == MarkMode.AMOUNT) R.string.custom_bank_mark_hint_amount else R.string.custom_bank_mark_hint_merchant),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            d.words.forEachIndexed { i, word ->
                val marked = i == d.marks.amountIndex || i in d.marks.merchantIndexes
                val canTap = if (d.mode == MarkMode.AMOUNT) CustomBankTemplate.hasDigit(word) else i != d.marks.amountIndex
                FilterChip(selected = marked, enabled = canTap || marked, onClick = { if (canTap) onEdit { it.tap(i) } }, label = { Text(word) })
            }
        }
        if (error == MarkError.NO_AMOUNT) ErrorText(stringResource(R.string.custom_bank_error_no_amount))
        if (error == MarkError.MERCHANT_SPLIT) ErrorText(stringResource(R.string.custom_bank_error_merchant))
        Text(stringResource(R.string.custom_bank_template_label), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(d.template ?: stringResource(R.string.custom_bank_template_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
