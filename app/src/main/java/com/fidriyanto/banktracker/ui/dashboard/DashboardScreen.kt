package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.ui.theme.MutedText

private val Period.label: String
    get() = when (this) {
        Period.THIS_MONTH -> "This Month"
        Period.LAST_MONTH -> "Last Month"
        Period.LAST_3_MONTHS -> "Last 3 Months"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Dashboard",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        when (val s = state) {
            DashboardUiState.NotSignedIn -> NotSignedInContent()
            DashboardUiState.LoadingNoCache -> LoadingContent()
            is DashboardUiState.Loaded -> LoadedContent(
                state = s,
                period = period,
                onSelectPeriod = viewModel::selectPeriod,
                onRefresh = viewModel::refresh
            )
        }
    }
}

@Composable
private fun NotSignedInContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sign in with Google to load your dashboard",
                    color = MutedText,
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
    onSelectPeriod: (Period) -> Unit,
    onRefresh: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            onRefresh()
        }
    }

    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(period, onSelectPeriod)

            if (state.refreshError && state.lastUpdated != null) {
                Text(
                    "Last updated ${state.lastUpdated}",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }

            CurrencySection("THB", "฿", state.thb)
            CurrencySection("IDR", "Rp", state.idr)

            Spacer(Modifier.height(16.dp))
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState
        )
    }
}

@Composable
private fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Period.entries.forEach { p ->
            FilterChip(
                selected = selected == p,
                onClick = { onSelect(p) },
                label = { Text(p.label, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun CurrencySection(label: String, symbol: String, summary: CurrencySummary) {
    Text(
        "── $label ──────────────────────────",
        color = MutedText,
        fontSize = 13.sp
    )
    BalanceCard(summary = summary, currencySymbol = symbol)
    CategoryBreakdownCard(summary = summary, currencySymbol = symbol)
}
