package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeRateTest {
    @Test fun medianOfAnOddCount() = assertEquals(530.0, medianRate(listOf(540.0, 520.0, 530.0))!!, 0.0)

    // The upper middle value, the same index web Budget picks
    @Test fun medianOfAnEvenCount() = assertEquals(530.0, medianRate(listOf(520.0, 530.0, 510.0, 540.0))!!, 0.0)

    @Test fun noRatesGivesNull() = assertNull(medianRate(emptyList()))

    @Test fun fallbackIs535ForRupiahAndBahtOnly() {
        val legacy = CurrencySettings()
        assertEquals(535.0, fallbackRate(legacy, "THB"), 0.0)
        assertEquals(1.0, fallbackRate(legacy, "USD"), 0.0)
        assertEquals(1.0, fallbackRate(CurrencySettings("SGD", "SGD", overrides = emptyList()), "THB"), 0.0)
    }

    @Test fun aTypedRateWinsForItsCurrencyOnly() {
        val s = CurrencySettings("USD", "USD", 1, listOf(CurrencyOverride("EUR", "EUR", 1.1), CurrencyOverride("JPY", "JPY", 0.0)))
        assertEquals(1.1, fallbackRate(s, "EUR"), 0.0)
        assertEquals(1.0, fallbackRate(s, "JPY"), 0.0)
        assertEquals(1.0, fallbackRate(s, "GBP"), 0.0)
    }

    @Test fun theRateTableConvertsBetweenAnyTwoCurrencies() {
        val t = RateTable("USD", mapOf("EUR" to 1.1, "JPY" to 0.0067))
        assertEquals(1.0, t.homePer("USD"), 0.0)
        assertEquals(110.0, t.toHome("EUR", 100.0), 1e-9)
        assertEquals(100.0, t.convert(110.0, "USD", "EUR"), 1e-9)
        assertEquals(1.1 / 0.0067 * 10, t.convert(10.0, "EUR", "JPY"), 1e-6)
        assertEquals(5.0, t.convert(5.0, "GBP", "GBP"), 0.0)
        assertEquals(7.0, t.toHome("GBP", 7.0), 0.0)
    }

    @Test fun currenciesInUseListHomeFirstWithoutRepeats() {
        val accounts = listOf(Account("A", "A", "EUR", false), Account("B", "B", "USD", false), Account("C", "C", "JPY", false), Account("D", "D", "EUR", false))
        assertEquals(listOf("USD", "EUR", "JPY"), currenciesInUse("USD", accounts))
        assertEquals(listOf("USD"), currenciesInUse("USD", emptyList()))
    }

    @Test fun aLabelIsTheChosenNameOrTheCode() {
        val s = CurrencySettings("USD", "United States", 1, listOf(CurrencyOverride("EUR", "Europe"), CurrencyOverride("JPY", "")))
        assertEquals("United States", s.labelFor("USD"))
        assertEquals("Europe", s.labelFor("EUR"))
        assertEquals("JPY", s.labelFor("JPY"))
        assertEquals("GBP", s.labelFor("GBP"))
    }
}
