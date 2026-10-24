package com.fidriyanto.banktracker.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.usecase.GetCardStatementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val statements: List<CardStatement> = emptyList(),
)

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val getCardStatements: GetCardStatementsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CardsUiState())
    val state: StateFlow<CardsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = CardsUiState(loading = true)
        viewModelScope.launch {
            getCardStatements.getStatements()
                .onSuccess { _state.value = CardsUiState(loading = false, statements = it) }
                .onFailure { _state.value = CardsUiState(loading = false, error = it.message ?: "Failed to load") }
        }
    }
}
