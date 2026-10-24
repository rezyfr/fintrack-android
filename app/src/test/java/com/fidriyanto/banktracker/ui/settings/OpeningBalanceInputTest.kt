package com.fidriyanto.banktracker.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningBalanceInputTest {
    // ac: account-opening-balance — an empty field means the account started from zero
    @Test fun empty_means_zero() {
        assertEquals(0.0, parseOpeningBalance("")!!, 0.0)
        assertEquals(0.0, parseOpeningBalance("   ")!!, 0.0)
    }

    @Test fun reads_a_number_and_rejects_text() {
        assertEquals(12400.5, parseOpeningBalance("12400.50")!!, 0.0)
        assertEquals(300.0, parseOpeningBalance(" 300 ")!!, 0.0)
        assertNull(parseOpeningBalance("12,400"))
        assertNull(parseOpeningBalance("abc"))
    }

    @Test fun a_stored_balance_shows_without_trailing_zeros() {
        assertEquals("", openingText(0.0))
        assertEquals("12400", openingText(12400.0))
        assertEquals("12400.5", openingText(12400.5))
        assertEquals("1500000", openingText(1_500_000.0))
    }
}
