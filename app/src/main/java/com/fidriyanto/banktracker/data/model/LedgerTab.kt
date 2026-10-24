package com.fidriyanto.banktracker.data.model

enum class LedgerTab { EXPENSES, IDR_EXPENSES, INCOME, IDR_INCOME }

// ac: configurable-currency-pair - the ledger tab is a label kept from the first version. A wallet in the home
// currency uses the IDR tabs and a wallet in any other currency uses the plain tabs, which matches the rows
// already stored. Nothing reads the tab. Manual Add and captured notifications both use this, so they agree.
fun ledgerTabFor(walletCurrency: String, homeCode: String, isIncome: Boolean): LedgerTab = when {
    walletCurrency != homeCode -> if (isIncome) LedgerTab.INCOME else LedgerTab.EXPENSES
    else -> if (isIncome) LedgerTab.IDR_INCOME else LedgerTab.IDR_EXPENSES
}
