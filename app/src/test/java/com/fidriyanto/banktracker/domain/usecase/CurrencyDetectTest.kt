package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyDetectTest {
    @Test fun any_iso_code_is_accepted() {
        assertEquals("EUR", CurrencyDetect.normalize("eur"))
        assertEquals("JPY", CurrencyDetect.normalize("JPY"))
        assertEquals("MYR", CurrencyDetect.normalize("myr"))
    }

    @Test fun symbols_and_words_map_to_codes() {
        assertEquals("IDR", CurrencyDetect.normalize("Rp"))
        assertEquals("IDR", CurrencyDetect.normalize("Rp."))
        assertEquals("THB", CurrencyDetect.normalize("฿"))
        assertEquals("THB", CurrencyDetect.normalize("บาท"))
        assertEquals("EUR", CurrencyDetect.normalize("€"))
        assertEquals("THB", CurrencyDetect.normalize("baht"))
        assertEquals("IDR", CurrencyDetect.normalize("rupiah"))
    }

    @Test fun an_unclear_word_prefers_the_users_currency() {
        assertEquals("", CurrencyDetect.normalize("dollar"))
        assertEquals("SGD", CurrencyDetect.normalize("dollar", listOf("SGD")))
        assertEquals("CNY", CurrencyDetect.normalize("¥", listOf("CNY")))
        assertEquals("JPY", CurrencyDetect.normalize("¥"))
    }

    @Test fun finds_the_mark_next_to_a_number() {
        assertEquals("THB", CurrencyDetect.detect("Paid THB 50 at Shop"))
        assertEquals("THB", CurrencyDetect.detect("ชำระเงิน 320.00 บาท ที่ TOPS"))
        assertEquals("IDR", CurrencyDetect.detect("Transfer masuk Rp 2.000.000 dari BUDI"))
        assertEquals("EUR", CurrencyDetect.detect("You paid €12,50 at Cafe"))
        assertEquals("JPY", CurrencyDetect.detect("Card used JPY 3,000 at Store"))
        assertEquals("MYR", CurrencyDetect.detect("Payment of RM 25.00 approved"))
        assertEquals("USD", CurrencyDetect.detect("Charged 40 USD today"))
    }

    @Test fun ignores_words_and_names_that_are_not_currencies() {
        assertEquals("", CurrencyDetect.detect("KFC 50 combo"))
        assertEquals("", CurrencyDetect.detect("See you at 7"))
        assertEquals("", CurrencyDetect.detect("Prices fell 3% today"))
        assertFalse(CurrencyDetect.hasMark("Order ABC123 shipped"))
        assertTrue(CurrencyDetect.hasMark("Total GBP 9.99"))
    }
}
