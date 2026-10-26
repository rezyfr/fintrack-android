package com.fidriyanto.banktracker.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NotificationKeysTest {
    private val day = LocalDate.of(2026, 10, 26)

    @Test fun theKeyKeepsTheFormatOlderVersionsUsed() {
        assertEquals(listOf("Shop|65.0|2026-10-26|1000"), notificationKeys("Shop", 65.0, day, 1000L))
    }

    @Test fun twoDifferentTimesGiveTwoKeys() {
        assertEquals(2, notificationKeys("Shop", 65.0, day, 1000L, 2000L).size)
    }

    @Test fun theSameTimeTwiceGivesOneKey() {
        assertEquals(1, notificationKeys("Shop", 65.0, day, 1000L, 1000L).size)
    }

    @Test fun aMissingTimeIsLeftOut() {
        assertEquals(1, notificationKeys("Shop", 65.0, day, 1000L, null).size)
    }

    @Test fun twoPaymentsOfTheSameAmountAtDifferentTimesShareNoKey() {
        val first = notificationKeys("Shop", 65.0, day, 1000L, 1005L).toSet()
        val second = notificationKeys("Shop", 65.0, day, 4_600_000L, 4_600_005L).toSet()
        assertTrue(first.intersect(second).isEmpty())
    }
}
