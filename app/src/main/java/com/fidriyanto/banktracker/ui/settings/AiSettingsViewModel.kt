package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.AiSettings
import com.fidriyanto.banktracker.domain.usecase.AiSettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageAiModelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val model: ManageAiModelUseCase,
    private val settingsUseCase: AiSettingsUseCase,
) : ViewModel() {
    val modelState = model.state
    val settings: StateFlow<AiSettings> = settingsUseCase.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiSettings())

    fun download() = model.download()
    fun cancelDownload() = model.cancel()
    fun deleteModel() = model.delete()
    fun setNotifications(on: Boolean) = viewModelScope.launch { settingsUseCase.save(settings.value.copy(notificationsEnabled = on)) }
    fun setSuggestions(on: Boolean) = viewModelScope.launch { settingsUseCase.save(settings.value.copy(suggestionsEnabled = on)) }
}
