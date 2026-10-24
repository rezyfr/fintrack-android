package com.fidriyanto.banktracker.domain

import com.fidriyanto.banktracker.domain.model.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun zeroDecimalCurrencyHasNoDecimalsAndASpace() = assertEquals("Rp 1,234,568", formatMoney("IDR", 1_234_567.6))

    @Test fun foreignCurrencyDropsDecimalsForWholeAmounts() = assertEquals("฿4,874", formatMoney("THB", 4874.0))

    @Test fun foreignCurrencyKeepsTwoDecimals() = assertEquals("฿4,874.50", formatMoney("THB", 4874.5))

    @Test fun negativeAmountHasLeadingMinus() = assertEquals("-Rp 717,491", formatMoney("IDR", -717_491.0))

    @Test fun showSignAddsPlus() = assertEquals("+฿100", formatMoney("THB", 100.0, showSign = true))

    @Test fun unknownCurrencyUsesItsCode() = assertEquals("CHF 12", formatMoney("CHF", 12.0))
}
