package com.fidriyanto.banktracker.ui.installments

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

private val IDR_FMT = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun fmtIdr(amount: Double) = "Rp ${IDR_FMT.format(amount.toLong())}"
private fun fmtDate(date: LocalDate) = date.format(DATE_FMT)

// ac: installment-name-edit — wallet display names
private fun walletLabel(wallet: String) = when (wallet) {
    "MANDIRI_CC" -> "Mandiri Credit Card"
    "BCA_CC"     -> "BCA Credit Card"
    else         -> wallet
}

// ac: installment-overview — Installments screen accessible from the navigation
@Composable
fun InstallmentsScreen(
    viewModel: InstallmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
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

    val nonExcludedActive = state.active.filter { !it.excluded }
    val totalMonthly = nonExcludedActive.sumOf { it.installmentAmount }
    // ac: installment-step-controls
    val totalRemaining = nonExcludedActive.sumOf { it.remaining * it.installmentAmount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SummaryCard(
                totalMonthly = totalMonthly,
                totalRemaining = totalRemaining,
                activeCount = state.active.size,
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            SectionHeader(title = stringResource(R.string.installments_active), count = state.active.size)
        }

        if (state.active.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.installments_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            // ac: installment-overview — each card shows merchant, wallet, amount/month, progress, next due, estimated finish
            items(state.active, key = { it.id }) { installment ->
                InstallmentCard(
                    installment = installment,
                    onEditClick = { viewModel.startEdit(installment.id, installment.merchant) },
                    onIncrementStep = { viewModel.onIncrementStep(installment) },
                    onExcludeToggle = { viewModel.onSetExcluded(installment.id, !installment.excluded) },
                )
            }
        }

        if (state.completed.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                // ac: installment-overview — completed installments in a separate section
                SectionHeader(title = stringResource(R.string.installments_completed), count = state.completed.size)
            }
            items(state.completed, key = { it.id }) { installment ->
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.installments_monthly_total), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fmtIdr(totalMonthly), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.installments_active_plans), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(activeCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.installments_total_remaining), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtIdr(totalRemaining), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(count.toString(), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row: merchant name + edit icon + amount
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            installment.merchant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        // ac: installment-name-edit — edit icon next to merchant name
                        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.installments_edit_name_title), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                        // ac: installment-name-edit — wallet shown as display name
                        Text(walletLabel(installment.wallet), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmtIdr(installment.installmentAmount), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.installments_per_month), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Progress row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.installments_step_of, installment.currentStep, installment.totalInstallments), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // ac: installment-step-controls
                    Text(stringResource(R.string.installments_remaining_count, installment.remaining), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { installment.progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else appColors.blue,
                )
            }

            // Dates row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.installments_next_due_date, fmtDate(installment.nextDueDate())), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.installments_done_by_date, fmtDate(installment.estimatedFinish())), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            // Controls row: +1 step button + exclude toggle
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // ac: installment-step-controls — +1 step button
                OutlinedButton(
                    onClick = onIncrementStep,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(stringResource(R.string.installments_increment_step), fontSize = 12.sp)
                }

                // ac: installment-step-controls — exclude toggle and dimmed visual for excluded installments
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isExcluded) stringResource(R.string.installments_excluded) else stringResource(R.string.installments_include),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = !isExcluded,
                        onCheckedChange = { onExcludeToggle() },
                        modifier = Modifier.height(24.dp).padding(0.dp),
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
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(installment.merchant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(stringResource(R.string.installments_wallet_payments, walletLabel(installment.wallet), installment.totalInstallments), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.installments_paid_off), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
