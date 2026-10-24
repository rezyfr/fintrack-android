package com.fidriyanto.banktracker.domain.model

// What an account is for. A credit account shows as owed, and net worth leaves out credit and
// investment accounts.
enum class AccountType(val id: String) {
    CHECKING("checking"),
    CREDIT("credit"),
    INVESTMENT("investment");

    companion object {
        fun from(id: String?): AccountType = entries.firstOrNull { it.id == id } ?: CHECKING
    }
}

data class Account(
    val id: String,
    val displayName: String,
    val currency: String,
    val isDefault: Boolean,
    val type: AccountType = AccountType.CHECKING,
    val sortOrder: Int = 0,
    // ac: account-opening-balance — what the account held before its first transaction
    val openingBalance: Double = 0.0,
)

// The wallet of a form nobody has picked a wallet for yet.
val NO_ACCOUNT = Account(id = "", displayName = "", currency = "", isDefault = false)
