package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
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
    "MANDIRI_CC" to "Mandiri Credit Card",
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

// ac: filter-transactions-by-category — all known expense and income categories in alphabetical order
private val CATEGORY_OPTIONS: List<String> = listOf(
    "Bills", "Bonus", "Business", "Dividends", "Entertainment", "Food & Drink",
    "Freelance", "Gift", "Gifts", "Groceries", "Health & Wellbeing", "Investment",
    "Other", "Rental", "Salary", "Shopping", "Subscriptions", "Transfer Out",
    "Transport", "Travel",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val monthFilter     by viewModel.monthFilter.collectAsStateWithLifecycle()
    val walletFilter    by viewModel.walletFilter.collectAsStateWithLifecycle()
    val typeFilter      by viewModel.typeFilter.collectAsStateWithLifecycle()
    val categoryFilter  by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val searchQuery     by viewModel.searchQuery.collectAsStateWithLifecycle()
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
    // ac: batch-select-and-delete-transactions — selection mode state
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val inSelectionMode = selectedIds.isNotEmpty()
    var pendingBatchDelete by remember { mutableStateOf(false) }
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

            // ac: search-transactions-by-text — a search text field is shown above the transaction list filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.feed_search_placeholder), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp)) } }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(48.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            )

            Spacer(Modifier.height(6.dp))

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

            Spacer(Modifier.height(6.dp))

            // ac: filter-transactions-by-category — category selector shown in the filter row
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { viewModel.setCategory(null) },
                    label = { Text(stringResource(R.string.feed_filter_all), fontSize = 12.sp) }
                )
                CATEGORY_OPTIONS.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ac: batch-select-and-delete-transactions — toolbar shows count and Delete button
            if (inSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${selectedIds.size} selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(onClick = { pendingBatchDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete_background_label))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

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
                            if (inSelectionMode) {
                                // ac: batch-select-and-delete-transactions — in selection mode tapping toggles selection
                                val isSelected = tx.id in selectedIds
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedIds = if (isSelected) selectedIds - tx.id else selectedIds + tx.id
                                        },
                                    )
                                    Box(Modifier.weight(1f)) {
                                        TransactionCard(
                                            transaction = tx,
                                            onRetry = {},
                                            onConfirm = { _, _ -> },
                                            onLongClick = {
                                                selectedIds = if (isSelected) selectedIds - tx.id else selectedIds + tx.id
                                            },
                                        )
                                    }
                                }
                            } else {
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
                                        // ac: edit-transaction-from-feed — tap opens the edit bottom sheet
                                        onClick = { pendingEdit = tx },
                                        // ac: batch-select-and-delete-transactions — long press enters selection mode
                                        onLongClick = { selectedIds = setOf(tx.id) },
                                    )
                                }
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

    // ac: batch-select-and-delete-transactions — confirmation dialog with count
    if (pendingBatchDelete) {
        AlertDialog(
            onDismissRequest = { pendingBatchDelete = false },
            title = { Text(stringResource(R.string.batch_delete_title)) },
            text = { Text(stringResource(R.string.batch_delete_message, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    // ac: batch-select-and-delete-transactions — confirming deletes all and exits selection mode
                    viewModel.deleteMultiple(selectedIds)
                    selectedIds = emptySet()
                    pendingBatchDelete = false
                }) {
                    Text(stringResource(R.string.delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBatchDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
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
