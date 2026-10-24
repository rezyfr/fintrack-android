package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.SetupError
import com.fidriyanto.banktracker.domain.model.SetupInput

// The first problem in the setup answers, or null when they can be saved. The setup screen and the
// use case both call it, so the button and the save agree.
object SetupValidator {
    fun validate(input: SetupInput): SetupError? = when {
        CurrencyDetect.normalize(input.homeCode).isEmpty() -> SetupError.HOME_CURRENCY
        input.cycleStartDay !in 1..28 -> SetupError.CYCLE_DAY
        input.accounts.isEmpty() -> SetupError.NO_ACCOUNT
        input.accounts.any { it.name.isBlank() || CurrencyDetect.normalize(it.currency).isEmpty() } -> SetupError.ACCOUNT_CURRENCY
        else -> null
    }
}
