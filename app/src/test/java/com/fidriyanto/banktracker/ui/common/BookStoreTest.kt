package com.fidriyanto.banktracker.ui.common

import com.fidriyanto.banktracker.domain.model.Account
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Uses the default currency pair and three accounts: BBL is THB (foreign), BCA and MANDIRI are IDR (home).
class BookStoreTest {
    @Before fun accounts() = AccountStore.hydrate(
        listOf(Account("BBL", "Bangkok Bank", "THB", false), Account("BCA", "BCA", "IDR", false), Account("MANDIRI", "Mandiri", "IDR", false)),
    )

    @After fun reset() {
        BookStore.select(Book.COMBINED)
        AccountStore.hydrate(emptyList())
    }

    @Test fun theAppOpensOnCombined() = assertEquals(Book.COMBINED, BookStore.book.value)

    @Test fun combinedHoldsEveryWallet() {
        assertNull(Book.COMBINED.currencyCode())
        assertTrue(Book.COMBINED.holdsWallet("BBL"))
        assertTrue(Book.COMBINED.holdsWallet("BCA"))
    }

    private val thb = Book("THB")
    private val idr = Book("IDR")

    @Test fun aBookHoldsOnlyItsCurrency() {
        assertEquals("THB", thb.currencyCode())
        assertTrue(thb.holdsWallet("BBL"))
        assertFalse(thb.holdsWallet("BCA"))
        assertTrue(idr.holdsWallet("BCA"))
    }

    @Test fun aTransferBetweenCurrenciesShowsInBothBooks() {
        assertTrue(idr.holdsTransaction("BBL", "BCA"))
        assertTrue(thb.holdsTransaction("BBL", "BCA"))
        assertFalse(thb.holdsTransaction("BCA", "MANDIRI"))
    }

    @Test fun aThirdCurrencyHasItsOwnBook() {
        AccountStore.hydrate(AccountStore.all + Account("WISE", "Wise", "EUR", false))
        val eur = Book("EUR")
        assertTrue(eur.holdsWallet("WISE"))
        assertFalse(eur.holdsWallet("BBL"))
        assertFalse(thb.holdsWallet("WISE"))
        assertTrue(Book.COMBINED.holdsWallet("WISE"))
        assertTrue(eur.holdsTransaction("BCA", "WISE"))
    }
}
