package com.fidriyanto.banktracker.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NotificationParserTest {
    private val bangkokZone = ZoneId.of("Asia/Bangkok")

    private fun parse(title: String?, text: String?, timestampMs: Long) =
        NotificationParser.parse(title, text, timestampMs, bangkokZone)
    private val date = LocalDate.of(2026, 8, 25)
    private val timestampMs = date.atTime(LocalTime.NOON)
        .atZone(bangkokZone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `parses myBCA spend posted under the Financial Diary title`() {
        val parsed = parse(
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
        val parsed = parse(
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
        val parsed = parse(
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
        val parsed = parse(
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
            parse(
                "myBCA",
                "Your OTP code is 123456",
                timestampMs
            )
        )
    }

    @Test
    fun `still parses a BBL notification`() {
        val parsed = parse(
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

    // ac: notification-listener-rebinds — a withdrawal notification that the app missed on the phone.
    // The pattern reads it, which is how we knew the miss was the listener being unbound and not the
    // parser. The balance at the end carries THB too, so the amount must come from the first mark.
    @Test
    fun `parses a BBL transfer or withdrawal with a trailing balance`() {
        val parsed = parse(
            "Bangkok Bank",
            "Transfer/Withdrawal from your account X5009 1,062.00THB via Mobile 15/10/26 18:08 " +
                "The available balance is 2,823.45THB",
            timestampMs
        )

        requireNotNull(parsed)
        assertEquals(1062.0, parsed.amount, 0.0)
        assertEquals("BBL", parsed.wallet)
        assertEquals("Transfer/Withdrawal from your account X5009", parsed.item)
    }

    @Test
    fun `ignores a non-bank notification`() {
        assertNull(
            parse(
                "WhatsApp",
                "You spent the day offline",
                timestampMs
            )
        )
    }

    // ac: on-device-ai — only notifications that look like money moving reach the on-device model
    @Test fun looks_like_payment_needs_a_number_and_a_money_signal() {
        assertTrue(NotificationParser.looksLikePayment("Bualuang", "ชำระเงิน 320.00 บาท ที่ TOPS"))
        assertTrue(NotificationParser.looksLikePayment("New bank", "Payment of THB 50 at Shop"))
        assertTrue(NotificationParser.looksLikePayment("BCA mobile", "Transfer masuk Rp 2.000.000 dari BUDI"))
        assertTrue(NotificationParser.looksLikePayment("Wallet", "You paid €12,50 at Cafe"))
        assertTrue(NotificationParser.looksLikePayment("Card", "Card used JPY 3,000 at Store"))
        assertFalse(NotificationParser.looksLikePayment("Messages", "See you at 7"))
        assertFalse(NotificationParser.looksLikePayment("News", "Prices fell 3% today"))
        assertFalse(NotificationParser.looksLikePayment("Shop", "Payment reminder"))
    }
}
