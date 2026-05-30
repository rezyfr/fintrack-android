package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isListenerActive: Boolean = false,
    val isSyncing: Boolean = false,
    val isClearing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val useCase: SettingsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState(isListenerActive = true))
    val state = _state.asStateFlow()

    fun retryPendingSyncs() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true)
        useCase.retryFailedSyncs()
        _state.value = _state.value.copy(isSyncing = false)
    }

    fun markAllSynced() = viewModelScope.launch {
        _state.value = _state.value.copy(isClearing = true)
        useCase.markAllSynced()
        _state.value = _state.value.copy(isClearing = false)
    }
}
