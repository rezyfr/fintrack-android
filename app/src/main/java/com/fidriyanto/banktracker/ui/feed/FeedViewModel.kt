package com.fidriyanto.banktracker.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.repository.MerchantHistoryRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantHistoryRepository: MerchantHistoryRepository,
) : ViewModel() {

    private val _monthFilter    = MutableStateFlow<String?>(currentMonthPrefix())
    private val _walletFilter   = MutableStateFlow<String?>(null)
    private val _typeFilter     = MutableStateFlow<String?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    // ac: filter-transactions-by-subcategory — subcategory filter applied client-side
    private val _subcategoryFilter = MutableStateFlow<String?>(null)
    // ac: search-transactions-by-text — search query state
    private val _searchQuery    = MutableStateFlow("")
    // ac: advanced-transaction-filters — amount range and date range state
    private val _amountMin      = MutableStateFlow<Double?>(null)
    private val _amountMax      = MutableStateFlow<Double?>(null)
    private val _dateFrom       = MutableStateFlow<String?>(null)
    private val _dateTo         = MutableStateFlow<String?>(null)
    private val _remoteItems    = MutableStateFlow<List<TransactionUiModel>>(emptyList())
    private val _isLoading      = MutableStateFlow(false)
    private val _error          = MutableStateFlow<String?>(null)

    val monthFilter    = _monthFilter.asStateFlow()
    val walletFilter   = _walletFilter.asStateFlow()
    val typeFilter     = _typeFilter.asStateFlow()
    val categoryFilter = _categoryFilter.asStateFlow()
    val subcategoryFilter = _subcategoryFilter.asStateFlow()
    val searchQuery    = _searchQuery.asStateFlow()
    val amountMin      = _amountMin.asStateFlow()
    val amountMax      = _amountMax.asStateFlow()
    val dateFrom       = _dateFrom.asStateFlow()
    val dateTo         = _dateTo.asStateFlow()

    val recentMonths: List<String> = (0..5).map { i ->
        val d = java.time.LocalDate.now().minusMonths(i.toLong())
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    // ac: search-transactions-by-text — search filter combines with other filters
    val uiState: StateFlow<FeedUiState> = combine(
        transactionRepository.observePending(), _remoteItems, _isLoading, _error,
        combine(_searchQuery, _subcategoryFilter) { q, s -> q to s }
    ) { pending, remote, loading, error, filters ->
        val (query, subcategory) = filters
        val pendingKeys = pending.map { "${it.item}|${it.amount}|${it.dateIso}" }.toSet()
        val deduped = remote.filter { r -> "${r.item}|${r.amount}|${r.dateIso}" !in pendingKeys }
        val merged = pending + deduped
        // ac: search-transactions-by-text — filters whose item contains the query
        val bySearch = if (query.isBlank()) merged else merged.filter { tx -> tx.item.contains(query, ignoreCase = true) }
        // ac: filter-transactions-by-subcategory — keep only transactions with the chosen subcategory
        val filtered = if (subcategory == null) bySearch else bySearch.filter { it.subcategory == subcategory }
        FeedUiState(items = filtered, isLoading = loading, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState(isLoading = true))

    val merchantHistory: StateFlow<List<String>> = merchantHistoryRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // ac: advanced-transaction-filters — all filters combined
        combine(
            _monthFilter, _walletFilter, _typeFilter, _categoryFilter,
            combine(_searchQuery, _amountMin, _amountMax, _dateFrom, _dateTo, ::AdvancedParams)
        ) { m, w, t, c, adv ->
            FilterParams(m, w, t, c, adv.search, adv.amountMin, adv.amountMax, adv.dateFrom, adv.dateTo)
        }.onEach { fetchRemote(it) }.launchIn(viewModelScope)
    }

    fun setMonth(month: String?)       { _monthFilter.value = month; _dateFrom.value = null; _dateTo.value = null }
    fun setWallet(wallet: String?)     { _walletFilter.value = wallet }
    fun setType(type: String?)         { _typeFilter.value = type }
    // ac: filter-transactions-by-category — null clears the filter
    fun setCategory(category: String?) { _categoryFilter.value = category }
    // ac: filter-transactions-by-subcategory — null clears the subcategory filter
    fun setSubcategory(subcategory: String?) { _subcategoryFilter.value = subcategory }
    // ac: search-transactions-by-text — clearing the search field restores the unfiltered list
    fun setSearchQuery(query: String)  { _searchQuery.value = query }
    // ac: advanced-transaction-filters
    fun setAmountMin(value: Double?)   { _amountMin.value = value }
    fun setAmountMax(value: Double?)   { _amountMax.value = value }
    fun setDateFrom(value: String?)    { _dateFrom.value = value }
    fun setDateTo(value: String?)      { _dateTo.value = value }
    fun clearAdvancedFilters() {
        _searchQuery.value = ""; _amountMin.value = null; _amountMax.value = null
        _dateFrom.value = null; _dateTo.value = null
    }

    fun refresh() = fetchRemote(FilterParams(
        _monthFilter.value, _walletFilter.value, _typeFilter.value, _categoryFilter.value,
        _searchQuery.value, _amountMin.value, _amountMax.value, _dateFrom.value, _dateTo.value,
    ))

    private fun fetchRemote(p: FilterParams) = viewModelScope.launch {
        _isLoading.value = true; _error.value = null
        val month = if (p.dateFrom != null || p.dateTo != null) null else p.month
        transactionRepository.fetch(month, p.wallet, p.txType, p.category, p.search.ifBlank { null }, p.amountMin, p.amountMax, p.dateFrom, p.dateTo)
            .onSuccess { _remoteItems.value = it }.onFailure { _error.value = it.message }
        _isLoading.value = false
    }

    fun retry(id: Long) = viewModelScope.launch { transactionRepository.syncTransaction(id) }
    fun updateAndSync(id: Long, item: String, category: String) = viewModelScope.launch { transactionRepository.updateAndSync(id, item, category) }

    private val _deleteFailures = MutableSharedFlow<DeleteFailureEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val deleteFailures: SharedFlow<DeleteFailureEvent> = _deleteFailures.asSharedFlow()

    fun delete(id: Long) = viewModelScope.launch {
        // ac: delete-transaction-from-feed — failure path emits an event for the snackbar with retry
        transactionRepository.deleteTransaction(id)
            .onSuccess { _remoteItems.value = _remoteItems.value.filter { it.id != id } }
            .onFailure { _deleteFailures.emit(DeleteFailureEvent(id)) }
    }

    // ac: batch-select-and-delete-transactions — deletes all selected transactions
    fun deleteMultiple(ids: Set<Long>) = viewModelScope.launch {
        ids.forEach { id ->
            transactionRepository.deleteTransaction(id)
                .onSuccess { _remoteItems.value = _remoteItems.value.filter { it.id != id } }
                .onFailure { _deleteFailures.emit(DeleteFailureEvent(id)) }
        }
    }

    private val _batchCategoryFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val batchCategoryFailures: SharedFlow<Unit> = _batchCategoryFailures.asSharedFlow()

    // ac: batch-edit-transaction-category — updates the category of every selected transaction
    fun updateCategoryMultiple(ids: Set<Long>, category: String) = viewModelScope.launch {
        transactionRepository.updateCategoryMultiple(ids, category)
            .onSuccess {
                _remoteItems.value = _remoteItems.value.map { tx ->
                    if (tx.id in ids) tx.copy(category = category) else tx
                }
            }
            .onFailure { _batchCategoryFailures.emit(Unit) }
    }

    private val _editFailures = MutableSharedFlow<EditFailureEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val editFailures: SharedFlow<EditFailureEvent> = _editFailures.asSharedFlow()

    fun edit(id: Long, edit: TransactionEdit) = viewModelScope.launch {
        // ac: edit-transaction-from-feed — failure path emits an event so the snackbar can offer Retry that re-attempts the PATCH
        val result = transactionRepository.editTransaction(id, edit)
        result.onSuccess {
            merchantHistoryRepository.save(edit.item)
            _remoteItems.value = _remoteItems.value.map { tx ->
                if (tx.id == id) tx.copy(item = edit.item, category = edit.category, amount = edit.amount, dateIso = edit.dateIso, wallet = edit.wallet, txType = edit.txType, subcategory = edit.subcategory, toWallet = edit.toWallet, toAmount = edit.toAmount) else tx
            }
        }
        result.onFailure { _editFailures.emit(EditFailureEvent(id, edit)) }
    }

    private data class FilterParams(
        val month: String?, val wallet: String?, val txType: String?, val category: String?,
        val search: String = "", val amountMin: Double? = null, val amountMax: Double? = null,
        val dateFrom: String? = null, val dateTo: String? = null,
    )
    private data class AdvancedParams(val search: String, val amountMin: Double?, val amountMax: Double?, val dateFrom: String?, val dateTo: String?)
}
