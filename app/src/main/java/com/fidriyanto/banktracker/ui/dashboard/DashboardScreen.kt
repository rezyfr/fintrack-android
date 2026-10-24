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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.common.CycleStepper
import com.fidriyanto.banktracker.ui.common.Book
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.Space
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import com.fidriyanto.banktracker.ui.common.EmptyState

@Composable
fun DashboardScreen(onOpenTransactions: () -> Unit = {}, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ac: currency-books — the page shows the book picked by the switch at the top of the app
    val book by BookStore.book.collectAsStateWithLifecycle()
    val walletFilter by viewModel.walletFilter.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.lg)
    ) {
        when (val s = state) {
            DashboardUiState.NotSignedIn -> NotSignedInContent()
            DashboardUiState.LoadingNoCache -> LoadingIndicator()
            is DashboardUiState.Loaded -> LoadedContent(
                state = s,
                book = book,
                walletFilter = walletFilter,
                onStepCycle = viewModel::stepCycle,
                onSetWallet = viewModel::setWallet,
                onRefresh = viewModel::refresh,
                onOpenTransactions = onOpenTransactions,
            )
        }
    }
}

@Composable
private fun NotSignedInContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(Space.lg)) {
            Column(Modifier.padding(Space.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No data available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    state: DashboardUiState.Loaded,
    book: Book,
    walletFilter: String?,
    onStepCycle: (Int) -> Unit,
    onSetWallet: (String?) -> Unit,
    onRefresh: () -> Unit,
    onOpenTransactions: () -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) { if (pullState.isRefreshing) onRefresh() }
    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing && pullState.isRefreshing) pullState.endRefresh()
    }
    // ac: configurable-currency-pair — toggle labels and section currency come from the configured pair
    val currencySettings = CurrencySettingsStore.current

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(Modifier.fillMaxSize()) {
            // The month switch stays at the top while the cards scroll under it.
            CycleStepper(
                fromIso = state.fromIso,
                toIso = state.toIso,
                canGoNext = state.cycleOffset > 0,
                onStep = onStepCycle,
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = LocalFloatingToolbarInset.current),
                verticalArrangement = Arrangement.spacedBy(Space.lg)
            ) {
                val currency = book.code ?: currencySettings.homeCode
                val summary = if (book.isCombined) state.combined else state.byCurrency[currency] ?: CurrencySummary(0.0, 0.0, 0.0, emptyList())

                // ac: insights-filter-by-wallet — a wallet selector is shown in the Insights filter bar
                // ac: insights-wallet-flow — it lists the accounts of the selected currency. Combined has no
                // wallet chips, because it covers all accounts.
                if (!book.isCombined) {
                    val wallets = listOf(null to stringResource(R.string.insights_all_wallets)) +
                        AccountStore.all.filter { it.currency == currency }.map { it.id to it.displayName }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        wallets.forEach { (value, label) ->
                            FilterChip(
                                selected = walletFilter == value,
                                onClick = { onSetWallet(value) },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
                if (!summary.isEmpty()) {
                    CurrencySection(currency, summary, state.fromIso, state.toIso, onOpenTransactions)
                } else if (!state.isRefreshing) {
                    // ac: empty-states — an empty screen says what it is for and what to do next
                    EmptyState(
                        icon = Icons.Outlined.TrendingUp,
                        title = stringResource(if (walletFilter != null) R.string.dashboard_no_wallet_data else R.string.insights_no_cycle_data),
                        body = stringResource(R.string.insights_no_cycle_data_body),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Draw the refresh indicator only during a pull or a refresh. At rest it parks above this page,
        // over the tab switch.
        if (pullState.isRefreshing || pullState.progress > 0f) {
            PullToRefreshContainer(pullState, Modifier.align(Alignment.TopCenter))
        }
    }
}

private fun LocalDate.monthName(): String = month.getDisplayName(TextStyle.FULL, Locale.US)

@Composable
private fun CurrencySection(
    currency: String,
    summary: CurrencySummary,
    fromIso: String,
    toIso: String,
    onOpenTransactions: () -> Unit,
) {
    BalanceCard(summary = summary, currency = currency)
    // ac: insights-bento — a two-up row of compact tiles breaks the single column
    SpendStatsRow(summary.dailySpend, fromIso, toIso, currency)
    DailyHeatmapCard(summary.dailySpend, fromIso, toIso, currency)
    // ac: insights-compact-breakdown — the list names the cycle and the previous cycle by month
    val month = runCatching { LocalDate.parse(toIso) }.getOrNull()
    CategoryBreakdownCard(
        summary = summary,
        currency = currency,
        listTitle = stringResource(R.string.breakdown_list_title, month?.monthName().orEmpty()),
        compareLabel = stringResource(R.string.breakdown_vs_month, month?.minusMonths(1)?.monthName().orEmpty()),
    )
    LargestExpensesCard(summary.topExpenses, currency, onOpenTransactions)
    PaidWithCard(summary.walletBreakdown, currency)
    MonthlyTrendCard(summary.monthlyTrend, currency)
}
