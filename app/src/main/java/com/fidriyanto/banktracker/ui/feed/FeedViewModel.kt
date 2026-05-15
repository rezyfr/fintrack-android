package com.fidriyanto.banktracker.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun retry(id: Long) = viewModelScope.launch { repository.syncTransaction(id) }

    fun updateAndSync(id: Long, item: String, category: String) =
        viewModelScope.launch { repository.updateAndSync(id, item, category) }
}
