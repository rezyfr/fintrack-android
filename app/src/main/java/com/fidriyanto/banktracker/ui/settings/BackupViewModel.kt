package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.BackupOutcome
import com.fidriyanto.banktracker.domain.usecase.BackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// What the backup rows tell the user after an export or an import
enum class BackupMessage { EXPORTED, IMPORTED, NOT_A_BACKUP, TOO_NEW, FAILED }

data class BackupState(
    val available: Boolean = false,
    val busy: Boolean = false,
    val message: BackupMessage? = null,
)

// ac: offline-backup - the Export and Import rows of Settings, shown only on an offline install
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backup: BackupUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(available = backup.available()) } }
    }

    fun fileName(): String = backup.fileName()

    fun export(uri: String) = run(BackupMessage.EXPORTED) { backup.export(uri) }

    fun restore(uri: String) = run(BackupMessage.IMPORTED) { backup.restore(uri) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun run(done: BackupMessage, block: suspend () -> BackupOutcome) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val message = when (block()) {
            BackupOutcome.DONE -> done
            BackupOutcome.NOT_A_BACKUP -> BackupMessage.NOT_A_BACKUP
            BackupOutcome.TOO_NEW -> BackupMessage.TOO_NEW
            BackupOutcome.FAILED -> BackupMessage.FAILED
        }
        _state.update { it.copy(busy = false, message = message) }
    }
}
