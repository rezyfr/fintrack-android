package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Installment

// ac: installment-link-edit — the installment section of the edit sheet. A linked transaction shows its
// plan and step with an Unlink button. An unlinked card expense can be linked to one of the plans on
// its wallet by hand. [candidates] are the plans that fit the wallet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentLinkSection(
    linked: Installment?,
    linkedUnknown: Boolean,
    candidates: List<Installment>,
    onLink: (Installment) -> Unit,
    onUnlink: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    if (linked != null || linkedUnknown) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (linked != null) stringResource(R.string.installment_linked, linked.merchant, linked.currentStep, linked.totalInstallments)
                else stringResource(R.string.installment_linked_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onUnlink) { Text(stringResource(R.string.installment_unlink)) }
        }
    } else if (candidates.isNotEmpty()) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(R.string.installment_none_option),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.installment_link_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                candidates.forEach { plan ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.installment_option, plan.merchant, plan.currentStep, plan.totalInstallments)) },
                        onClick = { onLink(plan); expanded = false },
                    )
                }
            }
        }
    }
}
