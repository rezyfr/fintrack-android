package com.fidriyanto.banktracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.AutoAddedItem
import com.fidriyanto.banktracker.domain.usecase.ManageAutoAddedUseCase
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.holdsWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ac: auto-added-notice - the automatic transactions the user has not dismissed, for the book on screen
@HiltViewModel
class AutoAddedNoticeViewModel @Inject constructor(
    private val manage: ManageAutoAddedUseCase,
) : ViewModel() {
    val items: StateFlow<List<AutoAddedItem>> = combine(manage.observe(), BookStore.book) { list, book ->
        list.filter { book.holdsWallet(it.wallet) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Dismiss hides every note, in every book, so the notice does not come back when the book changes.
    fun dismiss() = viewModelScope.launch { manage.dismissAll() }
}
