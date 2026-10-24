package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerAddressTest {
    @Test fun anAddressWithoutASchemeGetsHttps() =
        assertEquals("https://fintrack.example.com", normalizeServerAddress("  fintrack.example.com/ "))

    @Test fun theSchemePortAndPathStay() {
        assertEquals("http://10.0.2.2:8080", normalizeServerAddress("http://10.0.2.2:8080"))
        assertEquals("https://example.com/fintrack", normalizeServerAddress("https://example.com/fintrack//"))
    }

    @Test fun textThatIsNotAnAddressIsRejected() {
        assertNull(normalizeServerAddress(""))
        assertNull(normalizeServerAddress("   "))
        assertNull(normalizeServerAddress("my server"))
        assertNull(normalizeServerAddress("ftp://example.com"))
        assertNull(normalizeServerAddress("https://"))
    }
}
