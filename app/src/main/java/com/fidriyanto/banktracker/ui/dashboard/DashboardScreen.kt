package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.domain.model.Period

private val Period.label: String
    get() = when (this) {
        Period.THIS_MONTH    -> "This Month"
        Period.LAST_MONTH    -> "Last Month"
        Period.LAST_3_MONTHS -> "Last 3 Months"
    }

// ac: insights-filter-by-wallet — a wallet selector is shown in the Insights filter bar
private val WALLET_OPTIONS = listOf(
    null         to "All wallets",
    "BBL"        to "Bangkok Bank",
    "MANDIRI"    to "Mandiri",
    "BCA"        to "BCA",
    "MANDIRI_CC" to "Mandiri Credit Card",
    "BCA_CC"     to "BCA Credit Card",
    "INVESTMENT" to "Investments",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val period       by viewModel.period.collectAsStateWithLifecycle()
    val isCustom     by viewModel.isCustom.collectAsStateWithLifecycle()
    val customFrom   by viewModel.customFrom.collectAsStateWithLifecycle()
    val customTo     by viewModel.customTo.collectAsStateWithLifecycle()
    val walletFilter by viewModel.walletFilter.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Dashboard",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        when (val s = state) {
            DashboardUiState.NotSignedIn    -> NotSignedInContent()
            DashboardUiState.LoadingNoCache -> LoadingContent()
            is DashboardUiState.Loaded      -> LoadedContent(
                state          = s,
                period         = period,
                isCustom       = isCustom,
                customFrom     = customFrom,
                customTo       = customTo,
                walletFilter   = walletFilter,
                availableMonths = viewModel.availableMonths,
                onSelectPeriod = viewModel::selectPeriod,
                onSetCustom    = viewModel::setCustomRange,
                onSetWallet    = viewModel::setWallet,
                onRefresh      = viewModel::refresh,
                monthLabel     = viewModel::monthDisplayLabel,
            )
        }
    }
}

@Composable
private fun NotSignedInContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No data available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    state: DashboardUiState.Loaded,
    period: Period,
    isCustom: Boolean,
    customFrom: String?,
    customTo: String?,
    walletFilter: String?,
    availableMonths: List<String>,
    onSelectPeriod: (Period) -> Unit,
    onSetCustom: (String, String) -> Unit,
    onSetWallet: (String?) -> Unit,
    onRefresh: () -> Unit,
    monthLabel: (String) -> String,
) {
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) { if (pullState.isRefreshing) onRefresh() }
    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing && pullState.isRefreshing) pullState.endRefresh()
    }

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(
                selected        = period,
                isCustom        = isCustom,
                customFrom      = customFrom ?: availableMonths.first(),
                customTo        = customTo   ?: availableMonths.first(),
                availableMonths = availableMonths,
                onSelect        = onSelectPeriod,
                onSetCustom     = onSetCustom,
                monthLabel      = monthLabel,
            )

            // ac: insights-filter-by-wallet — a wallet selector is shown in the Insights filter bar alongside the period tabs
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WALLET_OPTIONS.forEach { (value, label) ->
                    FilterChip(
                        selected = walletFilter == value,
                        onClick = { onSetWallet(value) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            if (state.refreshError) {
                Text(
                    if (state.lastUpdated != null) "Last updated ${state.lastUpdated}"
                    else "Couldn't load — pull down to retry",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // ac: insights-filter-by-wallet — when a single wallet is selected only the relevant currency section is shown
            if (!state.thb.isEmpty()) CurrencySection(stringResource(R.string.dashboard_section_thb), "฿", state.thb)
            if (!state.idr.isEmpty()) CurrencySection(stringResource(R.string.dashboard_section_idr), "Rp", state.idr)
            if (walletFilter != null && state.thb.isEmpty() && state.idr.isEmpty() && !state.isRefreshing) {
                Text(
                    stringResource(R.string.dashboard_no_wallet_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        PullToRefreshContainer(pullState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: Period,
    isCustom: Boolean,
    customFrom: String,
    customTo: String,
    availableMonths: List<String>,
    onSelect: (Period) -> Unit,
    onSetCustom: (String, String) -> Unit,
    monthLabel: (String) -> String,
) {
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded   by remember { mutableStateOf(false) }
    var localFrom    by remember(customFrom) { mutableStateOf(customFrom) }
    var localTo      by remember(customTo)   { mutableStateOf(customTo) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Period.entries.forEach { p ->
                FilterChip(
                    selected = !isCustom && selected == p,
                    onClick  = { onSelect(p) },
                    label    = { Text(p.label, fontSize = 13.sp) }
                )
            }
            FilterChip(
                selected = isCustom,
                onClick  = { onSetCustom(localFrom, localTo) },
                label    = { Text(stringResource(R.string.dashboard_custom_period), fontSize = 13.sp) }
            )
        }

        if (isCustom) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthDropdown(
                    label     = "From",
                    selected  = localFrom,
                    months    = availableMonths,
                    expanded  = fromExpanded,
                    monthLabel = monthLabel,
                    onExpand  = { fromExpanded = it },
                    onPick    = { localFrom = it; onSetCustom(it, localTo) },
                    modifier  = Modifier.weight(1f)
                )
                MonthDropdown(
                    label     = "To",
                    selected  = localTo,
                    months    = availableMonths.filter { it >= localFrom },
                    expanded  = toExpanded,
                    monthLabel = monthLabel,
                    onExpand  = { toExpanded = it },
                    onPick    = { localTo = it; onSetCustom(localFrom, it) },
                    modifier  = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDropdown(
    label: String,
    selected: String,
    months: List<String>,
    expanded: Boolean,
    monthLabel: (String) -> String,
    onExpand: (Boolean) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpand, modifier = modifier) {
        OutlinedTextField(
            value = monthLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
            months.forEach { ym ->
                DropdownMenuItem(
                    text = { Text(monthLabel(ym), fontSize = 13.sp) },
                    onClick = { onPick(ym); onExpand(false) }
                )
            }
        }
    }
}

@Composable
private fun CurrencySection(label: String, symbol: String, summary: CurrencySummary) {
    // ac: dashboard-currency-section-header — bold headline text instead of divider-style line
    // ac: dashboard-currency-section-header — headline text is visibly larger than adjacent body text
    Text(
        label,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    BalanceCard(summary = summary, currencySymbol = symbol)
    CategoryBreakdownCard(summary = summary, currencySymbol = symbol)
    // ac: transport-provider-breakdown: card appears below category breakdown; hidden when no transport
    if (summary.transportBreakdown.isNotEmpty()) {
        TransportBreakdownCard(breakdown = summary.transportBreakdown, currencySymbol = symbol)
    }
}
