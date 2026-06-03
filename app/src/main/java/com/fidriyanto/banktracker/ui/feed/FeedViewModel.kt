package com.fidriyanto.banktracker.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.repository.MerchantHistoryRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class FeedUiState(
    val items: List<TransactionUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class DeleteFailureEvent(val id: Long)
data class EditFailureEvent(val id: Long, val edit: TransactionEdit)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantHistoryRepository: MerchantHistoryRepository,
) : ViewModel() {

    private val _monthFilter    = MutableStateFlow<String?>(currentMonthPrefix())
    private val _walletFilter   = MutableStateFlow<String?>(null)
    private val _typeFilter     = MutableStateFlow<String?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    // ac: search-transactions-by-text — search query state
    private val _searchQuery    = MutableStateFlow("")
    private val _remoteItems    = MutableStateFlow<List<TransactionUiModel>>(emptyList())
    private val _isLoading      = MutableStateFlow(false)
    private val _error          = MutableStateFlow<String?>(null)

    val monthFilter    = _monthFilter.asStateFlow()
    val walletFilter   = _walletFilter.asStateFlow()
    val typeFilter     = _typeFilter.asStateFlow()
    val categoryFilter = _categoryFilter.asStateFlow()
    val searchQuery    = _searchQuery.asStateFlow()

    val recentMonths: List<String> = (0..5).map { i ->
        val d = LocalDate.now().minusMonths(i.toLong())
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    private val localPending: Flow<List<TransactionUiModel>> = transactionRepository.observePending()

    // ac: search-transactions-by-text — search filter combines with other filters
    val uiState: StateFlow<FeedUiState> = combine(
        localPending, _remoteItems, _isLoading, _error, _searchQuery
    ) { pending, remote, loading, error, query ->
        val pendingKeys = pending.map { "${it.merchant}|${it.amount}|${it.dateIso}" }.toSet()
        val deduped = remote.filter { r -> "${r.merchant}|${r.amount}|${r.dateIso}" !in pendingKeys }
        val merged = pending + deduped
        // ac: search-transactions-by-text — filters whose merchant or item contains the query
        val filtered = if (query.isBlank()) merged else merged.filter { tx ->
            tx.merchant.contains(query, ignoreCase = true) || tx.item.contains(query, ignoreCase = true)
        }
        FeedUiState(items = filtered, isLoading = loading, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState(isLoading = true))

    val merchantHistory: StateFlow<List<String>> = merchantHistoryRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // ac: filter-transactions-by-category — category filter combined with month, wallet, and type
        combine(_monthFilter, _walletFilter, _typeFilter, _categoryFilter) { m, w, t, c ->
            FilterParams(m, w, t, c)
        }
            .onEach { fetchRemote(it.month, it.wallet, it.txType, it.category) }
            .launchIn(viewModelScope)
    }

    fun setMonth(month: String?)       { _monthFilter.value = month }
    fun setWallet(wallet: String?)     { _walletFilter.value = wallet }
    fun setType(type: String?)         { _typeFilter.value = type }
    // ac: filter-transactions-by-category — null clears the filter
    fun setCategory(category: String?) { _categoryFilter.value = category }
    // ac: search-transactions-by-text — clearing the search field restores the unfiltered list
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun refresh() {
        fetchRemote(_monthFilter.value, _walletFilter.value, _typeFilter.value, _categoryFilter.value)
    }

    private fun fetchRemote(month: String?, wallet: String?, txType: String?, category: String?) =
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            transactionRepository.fetch(month, wallet, txType, category)
                .onSuccess { _remoteItems.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }

    private data class FilterParams(val month: String?, val wallet: String?, val txType: String?, val category: String?)

    fun retry(id: Long) = viewModelScope.launch { transactionRepository.syncTransaction(id) }

    fun updateAndSync(id: Long, item: String, category: String) =
        viewModelScope.launch { transactionRepository.updateAndSync(id, item, category) }

    private val _deleteFailures = MutableSharedFlow<DeleteFailureEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val deleteFailures: SharedFlow<DeleteFailureEvent> = _deleteFailures.asSharedFlow()

    fun delete(id: Long) = viewModelScope.launch {
        // ac: delete-transaction-from-feed — failure path emits an event for the snackbar with retry
        transactionRepository.deleteTransaction(id).onFailure { _deleteFailures.emit(DeleteFailureEvent(id)) }
    }

    // ac: batch-select-and-delete-transactions — deletes all selected transactions
    fun deleteMultiple(ids: Set<Long>) = viewModelScope.launch {
        ids.forEach { id ->
            transactionRepository.deleteTransaction(id).onFailure { _deleteFailures.emit(DeleteFailureEvent(id)) }
        }
    }

    private val _editFailures = MutableSharedFlow<EditFailureEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val editFailures: SharedFlow<EditFailureEvent> = _editFailures.asSharedFlow()

    fun edit(id: Long, edit: TransactionEdit) = viewModelScope.launch {
        // ac: edit-transaction-from-feed — failure path emits an event so the snackbar can offer Retry that re-attempts the PATCH
        val result = transactionRepository.editTransaction(id, edit)
        result.onSuccess { merchantHistoryRepository.save(edit.item) }
        result.onFailure { _editFailures.emit(EditFailureEvent(id, edit)) }
    }

    companion object {
        fun currentMonthPrefix(): String {
            val d = LocalDate.now()
            return "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
        }

        fun monthDisplayLabel(ym: String): String {
            val (y, m) = ym.split("-").map { it.toInt() }
            val name = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            return "$name ${(y % 100).toString().padStart(2, '0')}"
        }
    }
}
