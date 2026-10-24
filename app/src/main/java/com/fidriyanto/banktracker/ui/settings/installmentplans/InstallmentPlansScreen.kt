package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.settings.schedules.IconBadge
import com.fidriyanto.banktracker.ui.theme.Space

// ac: installment-plan-add, installment-plan-delete - every plan with its account, step and monthly amount
@Composable
fun InstallmentPlansScreen(onBack: () -> Unit, onAdd: () -> Unit, vmKey: String? = null, viewModel: InstallmentPlansViewModel = hiltViewModel(key = vmKey)) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Installment?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = Space.sm, end = Space.lg, top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.plans_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (s.loadFailed) {
                ErrorText(stringResource(R.string.plans_load_failed))
                OutlinedButton(onClick = viewModel::load) { Text(stringResource(R.string.plans_retry)) }
            }
            if (s.deleteFailed) ErrorText(stringResource(R.string.plans_delete_failed))
            if (s.plans.isEmpty() && !s.loading && !s.loadFailed) EmptyPlans()
            s.plans.forEach { plan ->
                val account = accounts.firstOrNull { it.id == plan.wallet }
                PlanRow(plan, account?.displayName ?: plan.wallet, account?.currency.orEmpty(), onDelete = { deleting = plan })
            }
        }
        Button(onClick = onAdd, Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md).testTag("plans_add")) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(R.string.plans_add), Modifier.padding(start = Space.sm))
        }
    }
    deleting?.let { plan ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.plans_delete_title)) },
            text = { Text(stringResource(R.string.plans_delete_body)) },
            confirmButton = { TextButton(onClick = { deleting = null; viewModel.delete(plan.id) }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun EmptyPlans() {
    Column(Modifier.fillMaxWidth().padding(vertical = Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
        IconBadge(Icons.Outlined.CreditCard, size = 64)
        Text(stringResource(R.string.plans_empty_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.plans_empty_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlanRow(plan: Installment, accountName: String, currency: String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = Space.md, top = Space.md, bottom = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            IconBadge(Icons.Outlined.CreditCard)
            Column(Modifier.weight(1f)) {
                Text(plan.merchant, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                val muted = MaterialTheme.colorScheme.onSurfaceVariant
                Text(accountName, style = MaterialTheme.typography.bodyMedium, color = muted)
                Text(stringResource(R.string.plans_row_step, plan.currentStep, plan.totalInstallments), style = MaterialTheme.typography.bodyMedium, color = muted)
                Text(stringResource(R.string.plans_row_monthly, formatMoney(currency, plan.installmentAmount)), style = MaterialTheme.typography.bodyMedium, color = muted)
                Text(stringResource(if (plan.isCompleted) R.string.plans_status_completed else R.string.plans_status_active), style = MaterialTheme.typography.bodyMedium, color = muted)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.plans_delete_desc), tint = MaterialTheme.colorScheme.error) }
        }
    }
}
