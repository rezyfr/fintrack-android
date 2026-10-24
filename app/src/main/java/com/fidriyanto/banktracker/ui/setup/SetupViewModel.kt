package com.fidriyanto.banktracker.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.model.NewAccount
import com.fidriyanto.banktracker.domain.model.SetupError
import com.fidriyanto.banktracker.domain.model.SetupInput
import com.fidriyanto.banktracker.domain.usecase.CompleteSetupUseCase
import com.fidriyanto.banktracker.domain.usecase.CurrencyDetect
import com.fidriyanto.banktracker.domain.usecase.SetupValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import com.fidriyanto.banktracker.domain.model.BackupOutcome
import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.ServerConfig
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.domain.model.normalizeServerAddress
import com.fidriyanto.banktracker.domain.usecase.BackupUseCase
import com.fidriyanto.banktracker.domain.usecase.StorageUseCase

data class SetupFormState(
    val homeCode: String = deviceCurrency(),
    val cycleDay: String = "1",
    val accounts: List<NewAccount> = emptyList(),
    val rates: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val failed: Boolean = false,
    val step: SetupStep = SetupStep.WELCOME,
    // True when the user moved to a later page, so the page slides in from the right.
    val forward: Boolean = true,
    // ac: setup-storage-choice - where the data lives, and the server the user typed when it is a server.
    val storage: StorageMode? = null,
    val serverAddress: String = "",
    val serverToken: String = "",
    val serverCheck: ServerCheck? = null,
    val checking: Boolean = false,
    // ac: offline-backup - a backup import from the storage page, and its result when it did not work
    val restoring: Boolean = false,
    val restoreResult: BackupOutcome? = null,
) {
    val useServer: Boolean get() = storage == StorageMode.SERVER

    // True when the tested server already has accounts. Setup then ends on the server page.
    val serverHasData: Boolean get() = useServer && (serverCheck as? ServerCheck.Ok)?.let { it.accounts > 0 } == true

    // The server the user typed, ready to store.
    val server: ServerConfig? get() = normalizeServerAddress(serverAddress)?.let { ServerConfig(it, serverToken.trim()) }

    val input: SetupInput
        get() = SetupInput(homeCode, cycleDay.toIntOrNull() ?: 0, accounts, rates.mapNotNull { (c, r) -> r.toDoubleOrNull()?.let { c to it } }.toMap())
    val error: SetupError? get() = SetupValidator.validate(input)

    // The currency codes an account can use: the home currency first, then the ones accounts already use.
    val currencies: List<String>
        get() = (listOf(homeCode) + accounts.map { it.currency }).map { CurrencyDetect.normalize(it) }.filter { it.isNotEmpty() }.distinct()

    // The currencies that need an exchange rate to the home currency.
    val otherCurrencies: List<String> get() = currencies.drop(1)

    // Whether the page in front can be left with Continue.
    val canContinue: Boolean
        get() = when (step) {
            SetupStep.STORAGE -> storage != null
            SetupStep.SERVER -> serverCheck is ServerCheck.Ok && !checking
            SetupStep.CURRENCY -> CurrencyDetect.normalize(homeCode).isNotEmpty()
            SetupStep.CYCLE -> cycleDay.toIntOrNull() in 1..28
            SetupStep.ACCOUNTS -> accounts.isNotEmpty()
            SetupStep.DONE -> error == null
            else -> true
        }

    // Progress through the pages the user will see, from 0 to 1.
    val progress: Float
        get() = SetupStep.path(otherCurrencies.isNotEmpty(), useServer).let { (it.indexOf(step) + 1f) / it.size }
}

private fun deviceCurrency(): String = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("")

// ac: first-run-setup - the answers a new user gives before the app opens: currencies, the day the
// pay cycle starts, and the accounts. Nothing is preset.
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val completeSetup: CompleteSetupUseCase,
    private val storage: StorageUseCase,
    private val backup: BackupUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SetupFormState())
    val state: StateFlow<SetupFormState> = _state.asStateFlow()

    init {
        // A build with a built-in server suggests it. The user can still keep the data on the phone.
        viewModelScope.launch {
            val current = storage.get()
            val server = current.server
            if (current.mode == StorageMode.SERVER && server != null) {
                update { copy(storage = StorageMode.SERVER, serverAddress = server.baseUrl, serverToken = server.token) }
            } else if (current.chosen) {
                update { copy(storage = current.mode) }
            }
        }
    }

    fun chooseStorage(mode: StorageMode) = update { copy(storage = mode) }

    fun setServerAddress(value: String) = update { copy(serverAddress = value, serverCheck = null) }

    fun setServerToken(value: String) = update { copy(serverToken = value, serverCheck = null) }

    fun checkServer() {
        val s = _state.value
        if (s.checking) return
        _state.update { it.copy(checking = true, serverCheck = null) }
        viewModelScope.launch {
            val result = storage.check(s.serverAddress, s.serverToken)
            _state.update { it.copy(checking = false, serverCheck = result) }
        }
    }

    // ac: offline-backup - an offline user on a new phone starts from a backup file. The file brings the
    // accounts and the settings, so setup ends without new answers.
    fun restoreBackup(uri: String, onDone: () -> Unit) {
        if (_state.value.restoring) return
        _state.update { it.copy(restoring = true, restoreResult = null) }
        viewModelScope.launch {
            val result = runCatching { storage.choose(StorageMode.LOCAL, null) }.fold({ backup.restore(uri) }, { BackupOutcome.FAILED })
            if (result == BackupOutcome.DONE) onDone() else _state.update { it.copy(restoring = false, restoreResult = result) }
        }
    }

    fun update(change: SetupFormState.() -> SetupFormState) = _state.update { it.change().copy(failed = false) }

    fun addAccount(name: String, currency: String, type: AccountType) =
        update { copy(accounts = accounts + NewAccount(name.trim(), CurrencyDetect.normalize(currency), type)) }

    fun removeAccount(account: NewAccount) = update { copy(accounts = accounts - account) }

    fun next() = update {
        if (canContinue) copy(step = SetupStep.next(step, otherCurrencies.isNotEmpty(), useServer), forward = true) else this
    }

    // Returns false on the first page, so the system can leave the app.
    fun back(): Boolean {
        if (_state.value.step == SetupStep.WELCOME) return false
        update { copy(step = SetupStep.previous(step, otherCurrencies.isNotEmpty(), useServer), forward = false) }
        return true
    }

    // Stores the storage choice first, so the answers go to the chosen place. A server that already has
    // accounts keeps them, and setup ends without new answers.
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving || (!s.serverHasData && s.error != null)) return
        _state.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            runCatching { storage.choose(s.storage ?: StorageMode.LOCAL, s.server) }
                .mapCatching { if (!s.serverHasData) completeSetup(s.input).getOrThrow() }
                .onSuccess { onDone() }
                .onFailure { _state.update { it.copy(saving = false, failed = true) } }
        }
    }
}
