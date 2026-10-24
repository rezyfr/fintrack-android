package com.fidriyanto.banktracker.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType

// ac: manage-accounts — the account list read by plain Composables (no ViewModel of their own),
// backed by the accounts table. It is empty until a screen that depends on GetAccountsUseCase has
// loaded, and stays empty on a new install until the user sets up accounts.
object AccountStore {
    var all by mutableStateOf<List<Account>>(emptyList())
        private set

    fun hydrate(accounts: List<Account>) {
        all = accounts
    }

    // ac: multicurrency-money-formatting — the currency an amount is held in, read from its account.
    // fallback is used when the wallet is null or unknown.
    fun currencyOf(wallet: String?, fallback: String = CurrencySettingsStore.current.homeCode): String =
        all.firstOrNull { it.id == wallet }?.currency ?: fallback

    // The name to show for a wallet id. An id with no account shows as itself.
    fun nameOf(wallet: String): String = all.firstOrNull { it.id == wallet }?.displayName ?: wallet

    // Investment accounts are a money flow zone of their own.
    fun isInvestment(wallet: String?): Boolean = all.firstOrNull { it.id == wallet }?.type == AccountType.INVESTMENT
}
