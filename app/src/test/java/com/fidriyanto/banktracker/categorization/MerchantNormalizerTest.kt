package com.fidriyanto.banktracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {
    @Test fun `strips CO LTD suffix`() =
        assertEquals("TRUE MONEY", MerchantNormalizer.normalize("TRUE MONEY CO., LTD."))

    @Test fun `strips PCL suffix`() =
        assertEquals("SOME CORP", MerchantNormalizer.normalize("SOME CORP PCL"))

    @Test fun `strips PLC suffix`() =
        assertEquals("SOME CORP", MerchantNormalizer.normalize("SOME CORP PLC."))

    @Test fun `uppercases and trims`() =
        assertEquals("BTS", MerchantNormalizer.normalize("  bts  "))

    @Test fun `removes dots`() =
        assertEquals("MR JOHN", MerchantNormalizer.normalize("MR. JOHN"))
}
