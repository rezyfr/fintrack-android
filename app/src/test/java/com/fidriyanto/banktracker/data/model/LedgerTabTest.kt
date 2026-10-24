package com.fidriyanto.banktracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

// The tab is a label kept from the first version. A home wallet uses the IDR tabs. Any other currency uses
// the plain tabs, which matches the rows already stored.
class LedgerTabTest {
    @Test fun otherCurrencyWalletUsesThePlainTabs() {
        assertEquals(LedgerTab.EXPENSES, ledgerTabFor("THB", "IDR", isIncome = false))
        assertEquals(LedgerTab.INCOME, ledgerTabFor("THB", "IDR", isIncome = true))
    }

    @Test fun homeWalletUsesTheHomeTabs() {
        assertEquals(LedgerTab.IDR_EXPENSES, ledgerTabFor("IDR", "IDR", isIncome = false))
        assertEquals(LedgerTab.IDR_INCOME, ledgerTabFor("IDR", "IDR", isIncome = true))
    }

    @Test fun theHomeCurrencyComesFromSettings() {
        assertEquals(LedgerTab.IDR_EXPENSES, ledgerTabFor("USD", "USD", isIncome = false))
        assertEquals(LedgerTab.EXPENSES, ledgerTabFor("EUR", "USD", isIncome = false))
        assertEquals(LedgerTab.INCOME, ledgerTabFor("JPY", "USD", isIncome = true))
    }
}
