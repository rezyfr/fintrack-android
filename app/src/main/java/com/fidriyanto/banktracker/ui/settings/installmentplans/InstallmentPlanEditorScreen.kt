package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.settings.rules.ErrorText
import com.fidriyanto.banktracker.ui.theme.Space

// ac: installment-plan-add - a plan in two steps: which plan, then its payments. The main button stays at the bottom.
@Composable
fun InstallmentPlanEditorScreen(onClose: () -> Unit, vmKey: String? = null, viewModel: InstallmentPlanEditorViewModel = hiltViewModel(key = vmKey)) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    // Read the current value, not the one this composition captured, so an old closed state cannot close a new editor.
    LaunchedEffect(s.closed) { if (viewModel.state.value.closed) onClose() }
    BackHandler { if (!viewModel.back()) onClose() }

    // The keyboard pushes the page up, so the main button stays in view while the user types
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = Space.sm, end = Space.lg, top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = { if (!viewModel.back()) onClose() }) {
                Icon(if (s.step == 0) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(stringResource(R.string.plans_editor_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.rules_step, s.step + 1, PLAN_LAST_STEP + 1), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (s.step == 0) {
                PlanWhichStep(s.draft, s.showErrors, accounts, viewModel::edit, viewModel::openPicker)
            } else {
                PlanPaymentsStep(s.draft, s.showErrors, accounts, plans, viewModel::edit)
            }
            if (s.saveFailed) ErrorText(stringResource(R.string.plans_save_failed))
        }
        Button(
            onClick = { if (s.step == PLAN_LAST_STEP) viewModel.save() else viewModel.next() },
            enabled = !s.saving, modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md),
        ) {
            Text(stringResource(if (s.step == PLAN_LAST_STEP) R.string.plans_save else R.string.rules_continue))
        }
    }
    s.picker?.let { PaymentPickerDialog(it, accounts.firstOrNull { a -> a.id == s.draft.walletId }?.currency.orEmpty(), onPick = viewModel::pick, onDismiss = viewModel::closePicker) }
}
