package com.fidriyanto.banktracker.ui.balances

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private fun formatBalance(currency: String, amount: Double): String = when (currency) {
    "THB" -> "฿${NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(amount)}"
    else  -> "Rp ${NumberFormat.getNumberInstance(Locale.US).format(amount.roundToLong())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(viewModel: BalancesViewModel = hiltViewModel()) {
    val state      by viewModel.state.collectAsStateWithLifecycle()
    val reconMonth by viewModel.reconMonth.collectAsStateWithLifecycle()

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) { if (pullState.isRefreshing) viewModel.refresh() }
    LaunchedEffect(state.isLoading) { if (!state.isLoading && pullState.isRefreshing) pullState.endRefresh() }

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Balances",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            when {
                state.isLoading && state.balances.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.balances.isEmpty() -> {
                    Text(
                        "Failed to load: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                    )
                }
                else -> {
                    // ac: view-wallet-balances — each wallet is displayed as a card
                    state.balances.forEach { wallet ->
                        WalletBalanceCard(wallet)
                    }

                    // ac: view-wallet-balances — net worth row shows THB Net and IDR Net excluding credit cards
                    NetWorthRow(state.balances)

                    Spacer(Modifier.height(16.dp))

                    // ac: reconcile-wallet-monthly — Reconciliation section below wallet balance cards
                    ReconciliationSection(
                        rows = state.reconciliation,
                        month = reconMonth,
                        isLoading = state.reconLoading,
                        availableMonths = viewModel.availableMonths,
                        monthLabel = viewModel::monthDisplayLabel,
                        onMonthChange = viewModel::setReconMonth,
                        onSaveBalance = viewModel::saveOpeningBalance,
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        PullToRefreshContainer(pullState, Modifier.align(Alignment.TopCenter))
    }
}

// ac: view-wallet-balances — credit card wallets visually distinguished and labeled as owed
@Composable
private fun WalletBalanceCard(wallet: WalletBalanceDto) {
    val appColors = LocalAppColors.current
    val isCredit = wallet.type == "credit"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCredit) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(wallet.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                formatBalance(wallet.currency, wallet.balance),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (isCredit) {
                Text(stringResource(R.string.balances_owed), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NetWorthRow(balances: List<WalletBalanceDto>) {
    val thbNet = balances.filter { it.currency == "THB" }.sumOf { it.balance }
    val idrAssets = balances.filter { it.currency == "IDR" && it.type != "credit" }.sumOf { it.balance }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.balances_thb_net), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatBalance("THB", thbNet), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.balances_idr_net), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatBalance("IDR", idrAssets), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ac: reconcile-wallet-monthly — a month navigator lets the user select which month to reconcile
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReconciliationSection(
    rows: List<ReconciliationRowDto>,
    month: String,
    isLoading: Boolean,
    availableMonths: List<String>,
    monthLabel: (String) -> String,
    onMonthChange: (String) -> Unit,
    onSaveBalance: (String, Double) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        "Reconciliation",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = monthLabel(month),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableMonths.forEach { ym ->
                DropdownMenuItem(
                    text = { Text(monthLabel(ym), fontSize = 13.sp) },
                    onClick = { onMonthChange(ym); expanded = false },
                )
            }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp))
        }
    } else {
        rows.forEach { row ->
            // ac: reconcile-wallet-monthly — each wallet row shows opening balance input, net change, closing
            ReconciliationRow(row = row, onSave = onSaveBalance)
        }
    }
}

@Composable
private fun ReconciliationRow(row: ReconciliationRowDto, onSave: (String, Double) -> Unit) {
    var input by remember(row.walletId, row.openingBalance) {
        mutableStateOf(row.openingBalance?.let { formatAmountPlain(it) } ?: "")
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.walletName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(row.currency, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.balances_statement_opening), fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                singleLine = true,
            )

            // ac: reconcile-wallet-monthly — saving an opening balance persists to Supabase
            val parsedInput = input.toDoubleOrNull()
            if (parsedInput != null && parsedInput != row.openingBalance) {
                TextButton(
                    onClick = { onSave(row.walletId, parsedInput) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.action_save), fontSize = 12.sp)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.balances_net_label, formatBalance(row.currency, row.netChange)), fontSize = 12.sp)
                row.calculatedClosing?.let {
                    Text(stringResource(R.string.balances_closing_label, formatBalance(row.currency, it)), fontSize = 12.sp)
                }
            }

            // ac: reconcile-wallet-monthly — reconciliation status indicator
            ReconciliationStatus(row)
        }
    }
}

@Composable
private fun ReconciliationStatus(row: ReconciliationRowDto) {
    val appColors = LocalAppColors.current
    when {
        row.openingBalance == null -> {
            Text(stringResource(R.string.balances_no_opening), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        row.nextOpening == null -> {
            Text(stringResource(R.string.balances_no_next_month), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> {
            val diff = row.difference ?: 0.0
            if (abs(diff) < 0.01) {
                Text(
                    stringResource(R.string.balances_reconciled),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.green,
                )
            } else {
                Text(
                    stringResource(R.string.balances_off_by, formatBalance(row.currency, abs(diff))),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.red,
                )
            }
        }
    }
}

private fun formatAmountPlain(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
