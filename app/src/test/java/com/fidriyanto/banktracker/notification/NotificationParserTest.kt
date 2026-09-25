package com.fidriyanto.banktracker.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NotificationParserTest {

    private val bangkokZone = ZoneId.of("Asia/Bangkok")
    private val date = LocalDate.of(2026, 8, 25)
    private val timestampMs = date.atTime(LocalTime.NOON)
        .atZone(bangkokZone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `parses myBCA spend posted under the Financial Diary title`() {
        val parsed = NotificationParser.parse(
            "Financial Diary",
            "You spent IDR 51,000.00 at Food & Beverage",
            timestampMs
        )

        requireNotNull(parsed)
        assertEquals("Food & Beverage", parsed.item)
        assertEquals(51_000.0, parsed.amount, 0.0)
        assertEquals("Food & Drink", parsed.category)
        assertEquals("BCA", parsed.wallet)
        assertEquals(date, parsed.date)
    }

    @Test
    fun `parses myBCA spend posted under the myBCA title`() {
        val parsed = NotificationParser.parse(
            "myBCA",
            "You spent IDR 25,000 at Transport",
            timestampMs
        )

        requireNotNull(parsed)
        assertEquals("Transport", parsed.item)
        assertEquals(25_000.0, parsed.amount, 0.0)
        assertEquals("Transport", parsed.category)
        assertEquals("BCA", parsed.wallet)
    }

    @Test
    fun `parses myBCA received money as income on BCA`() {
        val parsed = NotificationParser.parse(
            "Financial Diary",
            "You received IDR 25,918,161.00 from PT Gaji Sejahtera at Salary",
            timestampMs
        )

        requireNotNull(parsed)
        assertEquals("PT Gaji Sejahtera", parsed.item)
        assertEquals(25_918_161.0, parsed.amount, 0.0)
        assertEquals("BCA", parsed.wallet)
        assertEquals("income", parsed.txType)
    }

    @Test
    fun `maps an unrecognised BCA category to Other`() {
        val parsed = NotificationParser.parse(
            "Financial Diary",
            "You spent IDR 10,000.00 at Miscellaneous",
            timestampMs
        )

        requireNotNull(parsed)
        assertEquals("Other", parsed.category)
    }

    @Test
    fun `ignores a myBCA notification with no spend line`() {
        assertNull(
            NotificationParser.parse(
                "myBCA",
                "Your OTP code is 123456",
                timestampMs
            )
        )
    }

    @Test
    fun `still parses a BBL notification`() {
        val parsed = NotificationParser.parse(
            "Bangkok Bank",
            "PMT FOR GOODS 250.00THB",
            timestampMs
        )

        requireNotNull(parsed)
        assertEquals("PMT FOR GOODS", parsed.item)
        assertEquals(250.0, parsed.amount, 0.0)
        assertEquals("BBL", parsed.wallet)
        assertEquals("Other", parsed.category)
    }

    @Test
    fun `ignores a non-bank notification`() {
        assertNull(
            NotificationParser.parse(
                "WhatsApp",
                "You spent the day offline",
                timestampMs
            )
        )
    }
}
