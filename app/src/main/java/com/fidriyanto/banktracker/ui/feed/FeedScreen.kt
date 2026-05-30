package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.theme.LocalAppColors

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
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val monthFilter    by viewModel.monthFilter.collectAsStateWithLifecycle()
    val walletFilter   by viewModel.walletFilter.collectAsStateWithLifecycle()
    val typeFilter     by viewModel.typeFilter.collectAsStateWithLifecycle()
    val merchantHistory by viewModel.merchantHistory.collectAsStateWithLifecycle()

    val pullState = rememberPullToRefreshState()
    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) viewModel.refresh()
    }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && pullState.isRefreshing) pullState.endRefresh()
    }

    var pendingDelete by remember { mutableStateOf<TransactionUiModel?>(null) }
    var pendingEdit by remember { mutableStateOf<TransactionUiModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.snackbar_delete_failed)
    val editFailedMessage = stringResource(R.string.snackbar_edit_failed)
    val retryLabel = stringResource(R.string.action_retry)

    LaunchedEffect(viewModel) {
        viewModel.deleteFailures.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = deleteFailedMessage,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                // ac: delete-transaction-from-feed — snackbar Retry re-attempts the DELETE
                viewModel.delete(event.id)
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.editFailures.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = editFailedMessage,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                // ac: edit-transaction-from-feed — snackbar Retry re-PATCHes with the same edit payload
                viewModel.edit(event.id, event.edit)
            }
        }
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
                        items(uiState.items, key = { it.id }) { tx ->
                            // ac: delete-transaction-from-feed — wrap each row in SwipeToDismissBox; left-swipe (EndToStart) reveals the delete background
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        pendingDelete = tx
                                    }
                                    false
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { DeleteSwipeBackground() },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                            ) {
                                TransactionCard(
                                    transaction = tx,
                                    onRetry = { viewModel.retry(tx.id) },
                                    onConfirm = { item, category ->
                                        viewModel.updateAndSync(tx.id, item, category)
                                    },
                                    onLongClick = { pendingEdit = tx },
                                )
                            }
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullState
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
        )
    }

    pendingDelete?.let { tx ->
        DeleteTransactionDialog(
            transaction = tx,
            // ac: delete-transaction-from-feed — Cancel clears the pending state; swipe was already vetoed so the card has reset itself
            onCancel = { pendingDelete = null },
            onConfirm = {
                viewModel.delete(tx.id)
                pendingDelete = null
            },
        )
    }

    pendingEdit?.let { tx ->
        EditTransactionBottomSheet(
            transaction = tx,
            onDismiss = { pendingEdit = null },
            onSave = { edit ->
                viewModel.edit(tx.id, edit)
                pendingEdit = null
            },
            merchantHistory = merchantHistory,
        )
    }
}

@Composable
private fun DeleteSwipeBackground() {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.red, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            stringResource(R.string.delete_background_label),
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
}
