package com.fidriyanto.banktracker.ui.settings.custombanks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.Space

// ac: custom-bank-patterns - step 1: one example notification, pasted
@Composable
fun CustomBankExampleStep(
    d: CustomBankDraft,
    showErrors: Boolean,
    onEdit: ((CustomBankDraft) -> CustomBankDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.custom_bank_example_title), stringResource(R.string.custom_bank_example_body))
        OutlinedTextField(
            value = d.title, onValueChange = { v -> onEdit { it.copy(title = v) } }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_bank_field_title)) },
        )
        OutlinedTextField(
            value = d.text, onValueChange = { v -> onEdit { it.withText(v) } }, minLines = 3, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_bank_field_text)) }, isError = showErrors && d.text.isBlank(),
        )
        if (showErrors && d.text.isBlank()) ErrorText(stringResource(R.string.custom_bank_error_text))
    }
}
