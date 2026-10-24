package com.fidriyanto.banktracker.ui.installments

import com.fidriyanto.banktracker.ui.common.holdsWallet
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.Space
import androidx.compose.material.icons.outlined.Schedule
import com.fidriyanto.banktracker.ui.common.EmptyState

private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun fmtDate(date: LocalDate) = date.format(DATE_FMT)

// ac: installment-name-edit — wallet display names
private fun walletLabel(wallet: String) = AccountStore.nameOf(wallet)

// ac: multicurrency-money-formatting — installment amounts use the wallet currency
// ac: installment-overview — Installments screen accessible from the navigation
@Composable
fun InstallmentsScreen(
    viewModel: InstallmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ac: currency-books — only the plans on cards of the book picked at the top of the app
    val book by BookStore.book.collectAsStateWithLifecycle()
    val active = state.active.filter { book.holdsWallet(it.wallet) }
    val completed = state.completed.filter { book.holdsWallet(it.wallet) }

    if (state.isLoading) {
        LoadingIndicator()
        return
    }

    if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.load() }) { Text(stringResource(R.string.action_retry)) }
            }
        }
        return
    }

    val nonExcludedActive = active.filter { !it.excluded }
    val totalMonthly = nonExcludedActive.sumOf { it.installmentAmount }
    // ac: installment-step-controls
    val totalRemaining = nonExcludedActive.sumOf { it.remaining * it.installmentAmount }

    LazyColumn(
        contentPadding = PaddingValues(start = Space.lg, top = Space.lg, end = Space.lg, bottom = Space.lg + LocalFloatingToolbarInset.current),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        item {
            SummaryCard(
                totalMonthly = totalMonthly,
                totalRemaining = totalRemaining,
                activeCount = active.size,
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            SectionHeader(title = stringResource(R.string.installments_active), count = active.size)
        }

        if (active.isEmpty()) {
            item {
                // ac: empty-states — an empty screen says what it is for and what to do next
                EmptyState(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.installments_empty),
                    body = stringResource(R.string.installments_empty_body),
                )
            }
        } else {
            // ac: installment-overview — each card shows merchant, wallet, amount/month, progress, next due, estimated finish
            items(active, key = { it.id }) { installment ->
                InstallmentCard(
                    installment = installment,
                    onEditClick = { viewModel.startEdit(installment.id, installment.merchant) },
                    onIncrementStep = { viewModel.onIncrementStep(installment) },
                    onExcludeToggle = { viewModel.onSetExcluded(installment.id, !installment.excluded) },
                )
            }
        }

        if (completed.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                // ac: installment-overview — completed installments in a separate section
                SectionHeader(title = stringResource(R.string.installments_completed), count = completed.size)
            }
            items(completed, key = { it.id }) { installment ->
                CompletedCard(installment)
            }
        }
    }

    // ac: installment-name-edit — dialog with pre-filled field; saving PATCHes and updates card
    if (state.editingId != null) {
        EditNameDialog(
            initialName = state.editingInitialName,
            onSave = { name -> viewModel.saveEdit(name) },
            onDismiss = { viewModel.cancelEdit() },
        )
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.installments_edit_name_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.installments_edit_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ac: installment-step-controls — summary shows total monthly and total remaining for non-excluded installments
@Composable
private fun SummaryCard(totalMonthly: Double, totalRemaining: Double, activeCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.installments_monthly_total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(CurrencySettingsStore.current.homeCode, totalMonthly), style = MaterialTheme.typography.headlineSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.installments_active_plans), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(activeCount.toString(), style = MaterialTheme.typography.headlineSmall)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.installments_total_remaining), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(CurrencySettingsStore.current.homeCode, totalRemaining), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = Modifier.padding(vertical = Space.xs),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(count.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InstallmentCard(
    installment: Installment,
    onEditClick: () -> Unit,
    onIncrementStep: () -> Unit,
    onExcludeToggle: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val isExcluded = installment.excluded

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExcluded)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            // Top row: merchant name + edit icon + amount
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Text(
                            installment.merchant,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        // ac: installment-name-edit — edit icon next to merchant name
                        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp).testTag("inst_edit_${installment.id}")) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.installments_edit_name_title), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                        // ac: installment-name-edit — wallet shown as display name
                        Text(walletLabel(installment.wallet), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs), color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(AccountStore.currencyOf(installment.wallet), installment.installmentAmount), style = MaterialTheme.typography.headlineSmall, color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.installments_per_month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Progress row
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.installments_step_of, installment.currentStep, installment.totalInstallments), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // ac: installment-step-controls
                    Text(stringResource(R.string.installments_remaining_count, installment.remaining), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { installment.progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else appColors.blue,
                )
            }

            // Dates row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.installments_next_due_date, fmtDate(installment.nextDueDate())), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.installments_done_by_date, fmtDate(installment.estimatedFinish())), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            // Controls row: +1 step button + exclude toggle
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // ac: installment-step-controls — +1 step button
                OutlinedButton(
                    onClick = onIncrementStep,
                    contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.xs),
                    modifier = Modifier.height(32.dp).testTag("inst_step_${installment.id}"),
                ) {
                    Text(stringResource(R.string.installments_increment_step), style = MaterialTheme.typography.bodySmall)
                }

                // ac: installment-step-controls — exclude toggle and dimmed visual for excluded installments
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(
                        if (isExcluded) stringResource(R.string.installments_excluded) else stringResource(R.string.installments_include),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = !isExcluded,
                        onCheckedChange = { onExcludeToggle() },
                        modifier = Modifier.height(24.dp).padding(0.dp).testTag("inst_exclude_${installment.id}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedCard(installment: Installment) {
    // ac: installment-overview — when new matching transaction saved, step auto-increments via trigger
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Space.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(installment.merchant, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.installments_wallet_payments, walletLabel(installment.wallet), installment.totalInstallments), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.installments_paid_off), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
