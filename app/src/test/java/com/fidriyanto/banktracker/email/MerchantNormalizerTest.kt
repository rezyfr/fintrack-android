package com.fidriyanto.banktracker.email

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {
    @Test fun `strips CO LTD suffix`() {
        assertEquals("TRUE MONEY", MerchantNormalizer.normalize("TRUE MONEY CO., LTD."))
    }
    @Test fun `strips PCL suffix`() {
        assertEquals("CENTRAL RESTAURANTS GROUP", MerchantNormalizer.normalize("CENTRAL RESTAURANTS GROUP PCL"))
    }
    @Test fun `uppercases and trims`() {
        assertEquals("SUSHIRO", MerchantNormalizer.normalize("  Sushiro  "))
    }
    @Test fun `removes punctuation`() {
        assertEquals("MCDONALDS SILOM 64", MerchantNormalizer.normalize("McDonald's Silom 64"))
    }
    @Test fun `handles already clean input`() {
        assertEquals("BTS TIM TVM", MerchantNormalizer.normalize("BTS TIM TVM"))
    }
}
