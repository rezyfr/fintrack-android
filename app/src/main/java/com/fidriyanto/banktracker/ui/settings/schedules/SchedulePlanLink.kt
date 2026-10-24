package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.ui.settings.rules.OpenCard
import com.fidriyanto.banktracker.ui.settings.rules.OptionCard
import com.fidriyanto.banktracker.ui.theme.Space

// ac: schedule-installment-link - an expense can pay a step of an installment plan. The choice is only offered when the
// account has an active plan. A linked schedule moves the plan up one step each time it posts, and it ends with the plan.
@Composable
fun SchedulePlanLink(d: ScheduleDraft, plans: List<Installment>, onEdit: ((ScheduleDraft) -> ScheduleDraft) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val candidates = plans.filter { it.wallet == d.walletId }
    val linked = plans.firstOrNull { it.id == d.installmentId }
    when {
        d.installmentId != null -> OpenCard(Icons.Outlined.CreditCard, stringResource(R.string.schedule_plan_title), onRemove = { onEdit { it.copy(installmentId = null) } }) {
            Text(
                if (linked != null) stringResource(R.string.schedule_plan_linked, linked.merchant, linked.currentStep, linked.totalInstallments, linked.remaining)
                else stringResource(R.string.schedule_plan_gone),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        candidates.isNotEmpty() -> OptionCard(Icons.Outlined.CreditCard, stringResource(R.string.schedule_plan_title), stringResource(R.string.schedule_plan_hint), stringResource(R.string.schedule_plan_add)) { picking = true }
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.schedule_plan_pick)) },
            text = {
                Column {
                    candidates.forEach { plan ->
                        Text(
                            stringResource(R.string.installment_option, plan.merchant, plan.currentStep, plan.totalInstallments),
                            Modifier.fillMaxWidth().clickable { onEdit { it.withPlan(plan) }; picking = false }.padding(vertical = Space.md),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
