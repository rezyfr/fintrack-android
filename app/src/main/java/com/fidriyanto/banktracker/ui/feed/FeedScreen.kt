package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

private val WALLET_OPTIONS = listOf(
    null         to "All wallets",
    "BBL"        to "Bangkok Bank",
    "MANDIRI"    to "Mandiri",
    "MANDIRI_CC" to "Mandiri CC",
    "BCA"        to "BCA",
    "INVESTMENT" to "Investments",
)

private val TYPE_OPTIONS = listOf(
    null         to "All",
    "expense"    to "Expense",
    "income"     to "Income",
    "transfer"   to "Transfer",
    "investment" to "Investment",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val monthFilter  by viewModel.monthFilter.collectAsStateWithLifecycle()
    val walletFilter by viewModel.walletFilter.collectAsStateWithLifecycle()
    val typeFilter   by viewModel.typeFilter.collectAsStateWithLifecycle()

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) viewModel.refresh()
    }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && pullState.isRefreshing) pullState.endRefresh()
    }

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Transactions",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // Month chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = monthFilter == null,
                    onClick = { viewModel.setMonth(null) },
                    label = { Text(stringResource(R.string.feed_filter_all), fontSize = 12.sp) }
                )
                viewModel.recentMonths.forEach { ym ->
                    FilterChip(
                        selected = monthFilter == ym,
                        onClick = { viewModel.setMonth(ym) },
                        label = { Text(FeedViewModel.monthDisplayLabel(ym), fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Wallet chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WALLET_OPTIONS.forEach { (value, label) ->
                    FilterChip(
                        selected = walletFilter == value,
                        onClick = { viewModel.setWallet(value) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Type chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TYPE_OPTIONS.forEach { (value, label) ->
                    FilterChip(
                        selected = typeFilter == value,
                        onClick = { viewModel.setType(value) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Error banner
            uiState.error?.let { err ->
                Text(
                    "Couldn't load — showing cached data. $err",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Content
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No transactions found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { entity ->
                            TransactionCard(
                                entity = entity,
                                onRetry = { viewModel.retry(entity.id) },
                                onConfirm = { item, category ->
                                    viewModel.updateAndSync(entity.id, item, category)
                                }
                            )
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullState
        )
    }
}
