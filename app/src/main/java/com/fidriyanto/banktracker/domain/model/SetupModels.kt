package com.fidriyanto.banktracker.domain.model

// One account the user adds during first-run setup.
data class NewAccount(val name: String, val currency: String, val type: AccountType)

// Everything first-run setup asks for. The currencies in use are the home currency and the currency of
// each account. rates holds a starting rate (home units per unit) for the currencies the user typed one for.
data class SetupInput(
    val homeCode: String,
    val cycleStartDay: Int,
    val accounts: List<NewAccount>,
    val rates: Map<String, Double> = emptyMap(),
)

enum class SetupStatus { NEEDED, DONE }

enum class SetupError { HOME_CURRENCY, CYCLE_DAY, NO_ACCOUNT, ACCOUNT_CURRENCY }
