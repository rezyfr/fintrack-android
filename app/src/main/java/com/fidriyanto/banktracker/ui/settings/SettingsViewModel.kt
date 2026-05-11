package com.fidriyanto.banktracker.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isSignedIn: Boolean = false,
    val accountEmail: String = "",
    val claudeApiKey: String = "",
    val isListenerActive: Boolean = false,
    val isSyncing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val prefs: SecurePrefs,
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = SettingsState(
            isSignedIn = authManager.isSignedIn(),
            accountEmail = authManager.getSignedInEmail() ?: "",
            claudeApiKey = prefs.claudeApiKey,
            isListenerActive = true
        )
    }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun saveClaudeKey(key: String) { prefs.claudeApiKey = key; refresh() }

    fun signOut() { authManager.signOut(); refresh() }

    fun retryPendingSyncs() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true)
        repository.retryFailedSyncs()
        _state.value = _state.value.copy(isSyncing = false)
    }
}
