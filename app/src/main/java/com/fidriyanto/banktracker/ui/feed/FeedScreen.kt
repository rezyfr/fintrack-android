package com.fidriyanto.banktracker.ui.feed

import androidx.compose.runtime.saveable.rememberSaveable
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.domain.usecase.BudgetAttribution
import com.fidriyanto.banktracker.ui.common.holdsWallet
import com.fidriyanto.banktracker.ui.common.holdsTransaction
import com.fidriyanto.banktracker.ui.common.currencyCode
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.add.categoriesFor
import com.fidriyanto.banktracker.ui.add.TxType
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import java.time.LocalDate
import com.fidriyanto.banktracker.ui.common.ALL_SUBCATEGORIES
import com.fidriyanto.banktracker.ui.common.subcategoriesFor
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import androidx.compose.foundation.ExperimentalFoundationApi
import com.fidriyanto.banktracker.domain.model.formatMoney

// ac: filter-transactions-by-wallet — every account from the accounts table, so custom accounts and
// all cards are listed
private val walletOptions: List<Pair<String, String>>
    get() = AccountStore.all.filter { BookStore.book.value.holdsWallet(it.id) }.map { it.id to it.displayName }

private val TYPE_OPTIONS = listOf(
    null to "All",
    "expense" to "Expense",
    "income" to "Income",
    "transfer" to "Transfer",
    "investment" to "Investment",
)

// ac: filter-transactions-by-category — the categories of every type in alphabetical order: the live
// expense categories plus the income, transfer and investment ones the Add form offers
private val categoryOptions: List<String>
    get() = TxType.entries.flatMap { categoriesFor(it) }.distinct().sorted()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val monthFilter by viewModel.monthFilter.collectAsStateWithLifecycle()
    val walletFilter by viewModel.walletFilter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val subcategoryFilter by viewModel.subcategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    // ac: advanced-transaction-filters
    val amountMin by viewModel.amountMin.collectAsStateWithLifecycle()
    val amountMax by viewModel.amountMax.collectAsStateWithLifecycle()
    val dateFrom by viewModel.dateFrom.collectAsStateWithLifecycle()
    val dateTo by viewModel.dateTo.collectAsStateWithLifecycle()
    // ac: currency-books — the list shows the book picked at the top of the app. A transfer between the
    // two currencies shows in both books. A wallet chip from another book is cleared.
    val book by BookStore.book.collectAsStateWithLifecycle()
    // ac: budget-by-category — expenses that count toward no budget get a mark, and a chip shows only them
    val budgetLines by viewModel.budgetLines.collectAsStateWithLifecycle()
    var noBudgetOnly by rememberSaveable { mutableStateOf(false) }
    val noBudgetIds = remember(uiState.items, budgetLines, AccountStore.all) {
        if (budgetLines.isEmpty()) emptySet()
        else {
            val currencyByWallet = AccountStore.all.associate { it.id to it.currency }
            val homeCode = CurrencySettingsStore.current.homeCode
            uiState.items.filter { it.txType == "expense" && BudgetAttribution.lineFor(it, budgetLines, currencyByWallet, homeCode) == null }
                .map { it.id }.toSet()
        }
    }
    val bookItems = remember(uiState.items, book, noBudgetOnly, noBudgetIds) {
        uiState.items.filter { book.holdsTransaction(it.wallet, it.toWallet) && (!noBudgetOnly || it.id in noBudgetIds) }
    }
    LaunchedEffect(book) { if (!book.holdsWallet(walletFilter)) viewModel.setWallet(null) }
    var amountMinText by remember { mutableStateOf("") }
    var amountMaxText by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("list") }
    var showFilters by remember { mutableStateOf(false) }
    val hasAdvancedFilters = amountMin != null || amountMax != null || dateFrom != null || dateTo != null
    val activeFilterCount = listOfNotNull(monthFilter, walletFilter, typeFilter, categoryFilter, subcategoryFilter).size + (if (searchQuery.isNotEmpty()) 1 else 0) + (if (hasAdvancedFilters) 1 else 0) + (if (noBudgetOnly) 1 else 0)

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
    // ac: batch-edit-transaction-category — pending category picker state
    var pendingBatchCategory by remember { mutableStateOf<String?>(null) }
    var batchCategoryMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.snackbar_delete_failed)
    val editFailedMessage = stringResource(R.string.snackbar_edit_failed)
    val batchCategoryFailedMessage = stringResource(R.string.snackbar_batch_category_failed)
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
    LaunchedEffect(viewModel) {
        // ac: batch-edit-transaction-category — if the batch update fails, an error is shown
        viewModel.batchCategoryFailures.collect {
            snackbarHostState.showSnackbar(message = batchCategoryFailedMessage, duration = SnackbarDuration.Long)
        }
    }

    Box(Modifier.nestedScroll(pullState.nestedScrollConnection)) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Transactions",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = Space.lg, end = Space.lg, top = Space.lg, bottom = Space.md)
            )

            // ac: transactions-list-calendar-toggle — List/Calendar segmented toggle + a Filters button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.clip(AppShape.pill).background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    listOf("list" to stringResource(R.string.feed_tab_list), "calendar" to stringResource(R.string.feed_tab_calendar)).forEach { (m, label) ->
                        val on = mode == m
                        Text(
                            label,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.clip(AppShape.pill)
                                .background(if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { mode = m }
                                .padding(horizontal = Space.lg, vertical = Space.sm),
                        )
                    }
                }
                val filterLabel = if (activeFilterCount > 0) stringResource(R.string.feed_filters) + " ($activeFilterCount)" else stringResource(R.string.feed_filters)
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = AppShape.pill) {
                    Text(
                        filterLabel,
                        color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clip(AppShape.pill).clickable { showFilters = true }
                            .padding(horizontal = Space.lg, vertical = Space.sm),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // ac: transactions-list-calendar-toggle — filters open in a bottom sheet, not stacked above the list
            if (showFilters) {
                // ac: feed-filter-apply — the sheet edits a draft, started from the filters in use
                var draft by remember {
                    mutableStateOf(
                        FeedFilters(
                            monthFilter, walletFilter, typeFilter, categoryFilter, subcategoryFilter, searchQuery,
                            amountMinText, amountMaxText, dateFrom, dateTo, noBudgetOnly,
                        )
                    )
                }
                val draftHasAdvanced = draft.amountMin.isNotEmpty() || draft.amountMax.isNotEmpty() || draft.dateFrom != null || draft.dateTo != null
                // ac: feed-filter-apply — the sheet opens fully, so Apply is always in view below the filters
                ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                    Column {
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                            // ac: search-transactions-by-text — a search text field is shown above the transaction list filters
                            OutlinedTextField(
                                value = draft.search,
                                onValueChange = { draft = draft.copy(search = it) },
                                placeholder = { Text(stringResource(R.string.feed_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = if (draft.search.isNotEmpty()) {
                                    { IconButton(onClick = { draft = draft.copy(search = "") }) { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp)) } }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md).height(48.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(Modifier.height(6.dp))

                            // Month chips
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Space.md),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                FilterChip(
                                    selected = draft.month == null,
                                    onClick = { draft = draft.copy(month = null, dateFrom = null, dateTo = null) },
                                    label = { Text(stringResource(R.string.feed_filter_all), style = MaterialTheme.typography.bodySmall) }
                                )
                                viewModel.recentMonths.forEach { ym ->
                                    FilterChip(
                                        selected = draft.month == ym,
                                        onClick = { draft = draft.copy(month = ym, dateFrom = null, dateTo = null) },
                                        label = { Text(monthDisplayLabel(ym), style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Wallet chips
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Space.md),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                (listOf(null to stringResource(R.string.insights_all_wallets)) + walletOptions).forEach { (value, label) ->
                                    FilterChip(
                                        selected = draft.wallet == value,
                                        onClick = { draft = draft.copy(wallet = value) },
                                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Type chips
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Space.md),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                TYPE_OPTIONS.forEach { (value, label) ->
                                    FilterChip(
                                        selected = draft.type == value,
                                        onClick = { draft = draft.copy(type = value) },
                                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                                FilterChip(
                                    selected = draft.noBudgetOnly,
                                    onClick = { draft = draft.copy(noBudgetOnly = !draft.noBudgetOnly) },
                                    label = { Text(stringResource(R.string.feed_no_budget), style = MaterialTheme.typography.bodySmall) }
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            // ac: filter-transactions-by-category — category selector shown in the filter row
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Space.md),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                FilterChip(
                                    selected = draft.category == null,
                                    onClick = { draft = draft.copy(category = null, subcategory = null) },
                                    label = { Text(stringResource(R.string.feed_filter_all), style = MaterialTheme.typography.bodySmall) }
                                )
                                categoryOptions.forEach { cat ->
                                    FilterChip(
                                        selected = draft.category == cat,
                                        onClick = { draft = draft.copy(category = cat, subcategory = null) },
                                        label = { Text(cat, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }

                            // ac: filter-transactions-by-subcategory — subcategory chips; scoped to the chosen
                            // category's subcategories when one is selected, otherwise all subcategories.
                            val subcategoryOptions = draft.category?.let { subcategoriesFor(it) }?.takeIf { it.isNotEmpty() } ?: ALL_SUBCATEGORIES
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.feed_filter_subcategory),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Space.md),
                            )
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Space.md),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                FilterChip(
                                    selected = draft.subcategory == null,
                                    onClick = { draft = draft.copy(subcategory = null) },
                                    label = { Text(stringResource(R.string.feed_filter_all), style = MaterialTheme.typography.bodySmall) }
                                )
                                subcategoryOptions.forEach { sub ->
                                    FilterChip(
                                        selected = draft.subcategory == sub,
                                        onClick = { draft = draft.copy(subcategory = sub) },
                                        label = { Text(sub, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // ac: advanced-transaction-filters — toggle and inputs for amount range and date range
                            Row(
                                modifier = Modifier.padding(horizontal = Space.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilterChip(
                                    selected = showAdvanced || draftHasAdvanced,
                                    onClick = { showAdvanced = !showAdvanced },
                                    label = { Text(stringResource(R.string.feed_advanced_filters), style = MaterialTheme.typography.bodySmall) },
                                )
                                if (draftHasAdvanced) {
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = {
                                        draft = draft.copy(amountMin = "", amountMax = "", dateFrom = null, dateTo = null)
                                    }) {
                                        Text(stringResource(R.string.feed_clear_filters), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            if (showAdvanced) {
                                Column(Modifier.padding(horizontal = Space.md)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                                        OutlinedTextField(
                                            value = draft.amountMin,
                                            onValueChange = { v -> draft = draft.copy(amountMin = v) },
                                            placeholder = { Text(stringResource(R.string.feed_amount_min), style = MaterialTheme.typography.bodySmall) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                        )
                                        OutlinedTextField(
                                            value = draft.amountMax,
                                            onValueChange = { v -> draft = draft.copy(amountMax = v) },
                                            placeholder = { Text(stringResource(R.string.feed_amount_max), style = MaterialTheme.typography.bodySmall) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                                        DateFilterChip(
                                            label = draft.dateFrom?.let { stringResource(R.string.feed_date_from_value, it) }
                                                ?: stringResource(R.string.feed_date_from),
                                            selected = draft.dateFrom != null,
                                            onPick = { draft = draft.copy(dateFrom = it, month = null) },
                                            onClear = { draft = draft.copy(dateFrom = null) },
                                            modifier = Modifier.weight(1f),
                                        )
                                        DateFilterChip(
                                            label = draft.dateTo?.let { stringResource(R.string.feed_date_to_value, it) }
                                                ?: stringResource(R.string.feed_date_to),
                                            selected = draft.dateTo != null,
                                            onPick = { draft = draft.copy(dateTo = it, month = null) },
                                            onClear = { draft = draft.copy(dateTo = null) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                        // ac: feed-filter-apply — Reset clears the draft; Apply sets every filter at once
                        Row(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = Space.md, end = Space.md, top = Space.md, bottom = Space.md),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { draft = FeedFilters() }) {
                                Text(stringResource(R.string.feed_filter_reset))
                            }
                            Button(
                                onClick = {
                                    val d = draft
                                    viewModel.setMonth(d.month)
                                    viewModel.setWallet(d.wallet)
                                    viewModel.setType(d.type)
                                    viewModel.setCategory(d.category)
                                    viewModel.setSubcategory(d.subcategory)
                                    viewModel.setSearchQuery(d.search)
                                    viewModel.setAmountMin(d.amountMin.toDoubleOrNull())
                                    viewModel.setAmountMax(d.amountMax.toDoubleOrNull())
                                    viewModel.setDateFrom(d.dateFrom)
                                    viewModel.setDateTo(d.dateTo)
                                    amountMinText = d.amountMin
                                    amountMaxText = d.amountMax
                                    noBudgetOnly = d.noBudgetOnly
                                    showFilters = false
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text(stringResource(R.string.feed_filter_apply), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ac: batch-select-and-delete-transactions — toolbar shows count and Delete button
            if (inSelectionMode) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(onClick = { selectedIds = emptySet(); pendingBatchCategory = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    ) {
                        // ac: batch-edit-transaction-category — category picker alongside Delete
                        ExposedDropdownMenuBox(
                            expanded = batchCategoryMenuExpanded,
                            onExpandedChange = { batchCategoryMenuExpanded = it },
                            modifier = Modifier.weight(1f),
                        ) {
                            OutlinedTextField(
                                value = pendingBatchCategory ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text(stringResource(R.string.batch_category_label), style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(batchCategoryMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )
                            ExposedDropdownMenu(
                                expanded = batchCategoryMenuExpanded,
                                onDismissRequest = { batchCategoryMenuExpanded = false },
                            ) {
                                categoryOptions.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = { pendingBatchCategory = cat; batchCategoryMenuExpanded = false },
                                    )
                                }
                            }
                        }
                        // ac: batch-edit-transaction-category — Apply button updates every selected transaction
                        Button(
                            enabled = pendingBatchCategory != null,
                            onClick = {
                                pendingBatchCategory?.let { viewModel.updateCategoryMultiple(selectedIds, it) }
                                selectedIds = emptySet()
                                pendingBatchCategory = null
                            },
                        ) {
                            Text(stringResource(R.string.batch_category_apply))
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
                    stringResource(R.string.feed_load_error, err),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs)
                )
            }

            // Content
            if (mode == "calendar") {
                // ac: loading-states-for-fetches — the calendar waits for the first fetch like the list
                if (uiState.isLoading && bookItems.isEmpty()) LoadingIndicator() else FeedCalendar(bookItems, fixedCurrency = book.currencyCode())
            } else {
                when {
                    uiState.isLoading && bookItems.isEmpty() -> LoadingIndicator()
                    bookItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No transactions found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    else -> {
                        val groupedByDate = remember(bookItems) { groupByDate(bookItems, LocalDate.now()) }
                        LazyColumn(
                            contentPadding = PaddingValues(start = Space.lg, top = Space.xs, end = Space.lg, bottom = Space.xs + LocalFloatingToolbarInset.current),
                            verticalArrangement = Arrangement.spacedBy(Space.sm)
                        ) {
                            // ac: transaction-list-date-groups — the list is split into Today, Yesterday, Last 7 days, and Older sections
                            groupedByDate.forEach { (group, rows) ->
                                // ac: feed-sticky-date-header — the date stays on screen while its rows scroll
                                // under it, set in the display font, with that group's outgoing total beside it
                                stickyHeader(key = "header-${group.name}") {
                                    val total = groupOutgoing(rows, CurrencySettingsStore.current.noWalletCurrency)
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(top = Space.sm, bottom = Space.xs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom,
                                    ) {
                                        Text(
                                            stringResource(group.labelRes),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        if (total != null) {
                                            Text(
                                                formatMoney(total.first, -total.second, showSign = true),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                items(rows, key = { it.id }) { tx ->
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
                                                    noBudget = tx.id in noBudgetIds,
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
                                                noBudget = tx.id in noBudgetIds,
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
            }
        }

        // Draw the refresh indicator only during a pull or a refresh. At rest it parks above this page,
        // over the book switch.
        if (pullState.isRefreshing || pullState.progress > 0f) {
            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullState
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = Space.sm, top = Space.sm, end = Space.sm, bottom = Space.sm + LocalFloatingToolbarInset.current),
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
        LaunchedEffect(tx.id) { viewModel.loadInstallments() }
        val installments by viewModel.installments.collectAsStateWithLifecycle()
        EditTransactionBottomSheet(
            transaction = tx,
            installments = installments,
            onDismiss = { pendingEdit = null },
            onSave = { edit ->
                viewModel.edit(tx.id, edit)
                pendingEdit = null
            },
            budgetLines = budgetLines,
            // ac: transaction-form-redesign — Delete in the sheet header opens the same confirmation dialog
            onDelete = {
                pendingEdit = null
                pendingDelete = tx
            },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterChip(
    label: String,
    selected: Boolean,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected,
        onClick = { if (selected) onClear() else showPicker = true },
        label = { Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
        modifier = modifier,
    )
    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val ld = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onPick(ld.toString())
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DeleteSwipeBackground() {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.red, AppShape.md),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            stringResource(R.string.delete_background_label),
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = Space.lg),
        )
    }
}
