package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyCompactTest {
    @Test fun smallAmountsUseTheFullFormat() {
        assertEquals(formatMoney("THB", 285.0), formatMoneyCompact("THB", 285.0))
    }

    @Test fun thousandsAndMillionsUseOneDecimalAtMost() {
        assertEquals("฿1.1k", formatMoneyCompact("THB", 1_080.06))
        assertEquals("฿2k", formatMoneyCompact("THB", 2_000.0))
        assertEquals("Rp 220k", formatMoneyCompact("IDR", 220_000.0))
        assertEquals("Rp 14.6M", formatMoneyCompact("IDR", 14_587_592.0))
        assertEquals("-Rp 14.6M", formatMoneyCompact("IDR", -14_587_592.0))
    }
}
