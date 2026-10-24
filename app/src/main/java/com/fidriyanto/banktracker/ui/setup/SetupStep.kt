package com.fidriyanto.banktracker.ui.setup

// The pages of the first-run setup, one question each. The server page only shows when the user keeps the
// data on a server. The rates page only shows when an account uses a currency other than the home currency.
enum class SetupStep {
    WELCOME, STORAGE, SERVER, CURRENCY, CYCLE, ACCOUNTS, RATES, DONE;

    // The pages the user will see, in order.
    companion object {
        fun path(hasOtherCurrencies: Boolean, useServer: Boolean = false): List<SetupStep> =
            entries.filter { (it != RATES || hasOtherCurrencies) && (it != SERVER || useServer) }

        fun next(from: SetupStep, hasOtherCurrencies: Boolean, useServer: Boolean = false): SetupStep =
            path(hasOtherCurrencies, useServer).let { it.getOrElse(it.indexOf(from) + 1) { from } }

        fun previous(from: SetupStep, hasOtherCurrencies: Boolean, useServer: Boolean = false): SetupStep =
            path(hasOtherCurrencies, useServer).let { it.getOrElse(it.indexOf(from) - 1) { from } }
    }
}
