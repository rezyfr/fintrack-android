package com.fidriyanto.banktracker.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.DeleteTransactionUseCase
import com.fidriyanto.banktracker.domain.usecase.EditTransactionUseCase
import com.fidriyanto.banktracker.domain.usecase.FeedUseCase
import com.fidriyanto.banktracker.domain.usecase.GetRecentMerchantsUseCase
import com.fidriyanto.banktracker.domain.usecase.SaveMerchantUseCase
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
    private val useCase: FeedUseCase,
    private val deleteUseCase: DeleteTransactionUseCase,
    private val editUseCase: EditTransactionUseCase,
    private val getRecentMerchants: GetRecentMerchantsUseCase,
    private val saveMerchant: SaveMerchantUseCase,
) : ViewModel() {

    private val _monthFilter  = MutableStateFlow<String?>(currentMonthPrefix())
    private val _walletFilter = MutableStateFlow<String?>(null)
    private val _typeFilter   = MutableStateFlow<String?>(null)
    private val _remoteItems  = MutableStateFlow<List<TransactionUiModel>>(emptyList())
    private val _isLoading    = MutableStateFlow(false)
    private val _error        = MutableStateFlow<String?>(null)

    val monthFilter  = _monthFilter.asStateFlow()
    val walletFilter = _walletFilter.asStateFlow()
    val typeFilter   = _typeFilter.asStateFlow()

    val recentMonths: List<String> = (0..5).map { i ->
        val d = LocalDate.now().minusMonths(i.toLong())
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    private val localPending: Flow<List<TransactionUiModel>> = useCase.observePending()

    val uiState: StateFlow<FeedUiState> = combine(
        localPending, _remoteItems, _isLoading, _error
    ) { pending, remote, loading, error ->
        val pendingKeys = pending.map { "${it.merchant}|${it.amount}|${it.dateIso}" }.toSet()
        val deduped = remote.filter { r -> "${r.merchant}|${r.amount}|${r.dateIso}" !in pendingKeys }
        FeedUiState(items = pending + deduped, isLoading = loading, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState(isLoading = true))

    val merchantHistory: StateFlow<List<String>> = getRecentMerchants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        combine(_monthFilter, _walletFilter, _typeFilter) { m, w, t -> Triple(m, w, t) }
            .onEach { (m, w, t) -> fetchRemote(m, w, t) }
            .launchIn(viewModelScope)
    }

    fun setMonth(month: String?)   { _monthFilter.value = month }
    fun setWallet(wallet: String?) { _walletFilter.value = wallet }
    fun setType(type: String?)     { _typeFilter.value = type }

    fun refresh() {
        fetchRemote(_monthFilter.value, _walletFilter.value, _typeFilter.value)
    }

    private fun fetchRemote(month: String?, wallet: String?, txType: String?) =
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            useCase.fetchRemote(month, wallet, txType)
                .onSuccess { _remoteItems.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }

    fun retry(id: Long) = viewModelScope.launch { useCase.syncTransaction(id) }

    fun updateAndSync(id: Long, item: String, category: String) =
        viewModelScope.launch { useCase.updateAndSync(id, item, category) }

    private val _deleteFailures = MutableSharedFlow<DeleteFailureEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val deleteFailures: SharedFlow<DeleteFailureEvent> = _deleteFailures.asSharedFlow()

    fun delete(id: Long) = viewModelScope.launch {
        // ac: delete-transaction-from-feed — failure path emits an event for the snackbar with retry
        deleteUseCase(id).onFailure { _deleteFailures.emit(DeleteFailureEvent(id)) }
    }

    private val _editFailures = MutableSharedFlow<EditFailureEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val editFailures: SharedFlow<EditFailureEvent> = _editFailures.asSharedFlow()

    fun edit(id: Long, edit: TransactionEdit) = viewModelScope.launch {
        // ac: edit-transaction-from-feed — failure path emits an event so the snackbar can offer Retry that re-attempts the PATCH
        val result = editUseCase(id, edit)
        result.onSuccess { saveMerchant(edit.item) }
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
