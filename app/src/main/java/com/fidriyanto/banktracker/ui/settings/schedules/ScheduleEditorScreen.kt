package com.fidriyanto.banktracker.ui.settings.schedules

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.Space

// ac: scheduled-transactions - a schedule in three steps: what, when, and a check. The main button stays at the bottom.
@Composable
fun ScheduleEditorScreen(scheduleId: String?, template: ScheduleTemplate?, onClose: () -> Unit, vmKey: String? = null, viewModel: ScheduleEditorViewModel = hiltViewModel(key = vmKey)) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val frequent by viewModel.frequentByType.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var warn by remember { mutableStateOf(false) }
    LaunchedEffect(scheduleId, template, accounts.isEmpty()) { if (s.loading && (accounts.isNotEmpty() || scheduleId != null)) viewModel.load(scheduleId, template) }
    // Read the current value, not the one this composition captured, so an old closed state cannot close a new editor.
    LaunchedEffect(s.closed) { if (viewModel.state.value.closed) onClose() }
    BackHandler { if (!viewModel.back()) onClose() }

    // The keyboard pushes the page up, so the main button stays in view while the user types
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = Space.sm, end = Space.lg, top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            IconButton(onClick = { if (!viewModel.back()) onClose() }) {
                Icon(if (s.step == 0) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(stringResource(if (s.draft.id == null) R.string.schedule_editor_new else R.string.schedule_editor_edit), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.rules_step, s.step + 1, LAST_STEP + 1), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (!s.loading) when (s.step) {
                0 -> ScheduleWhatStep(s.draft, s.showErrors, accounts, categories, plans, frequent, viewModel::edit)
                1 -> ScheduleWhenStep(s.draft, s.showErrors, viewModel::edit)
                else -> {
                    ScheduleReviewStep(s.draft, accounts) { on -> if (on) warn = true else viewModel.edit { it.copy(autoPost = false) } }
                    if (s.draft.id != null) OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.schedule_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        Button(onClick = { if (s.step == LAST_STEP) viewModel.save() else viewModel.next() }, Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md)) {
            Text(stringResource(if (s.step == LAST_STEP) R.string.schedule_save else R.string.rules_continue))
        }
    }
    if (warn) AutoPostWarning(onConfirm = { viewModel.edit { it.copy(autoPost = true) }; warn = false }, onDismiss = { warn = false })
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.schedule_delete_title)) },
            text = { Text(stringResource(R.string.schedule_delete_body)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
