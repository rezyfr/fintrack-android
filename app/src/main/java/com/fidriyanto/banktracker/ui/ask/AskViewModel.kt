package com.fidriyanto.banktracker.ui.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.domain.usecase.AskFinanceUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageAiModelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AskState(
    val messages: List<AskMessage> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

// ac: on-device-ai — the chat on the Ask screen. The answers come from the model on this phone.
@HiltViewModel
class AskViewModel @Inject constructor(
    private val ask: AskFinanceUseCase,
    private val model: ManageAiModelUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AskState())
    val state: StateFlow<AskState> = _state.asStateFlow()
    val modelState = model.state

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty() || _state.value.loading) return
        val history = _state.value.messages
        _state.update { it.copy(messages = it.messages + AskMessage(fromUser = true, text = question), loading = true, failed = false) }
        viewModelScope.launch {
            ask(question, history)
                .onSuccess { answer -> _state.update { it.copy(messages = it.messages + answer, loading = false) } }
                .onFailure { _state.update { it.copy(loading = false, failed = true) } }
        }
    }

    fun clear() = _state.update { AskState() }
    fun download() = model.download()
    fun cancelDownload() = model.cancel()
    fun deleteModel() = model.delete()
}
