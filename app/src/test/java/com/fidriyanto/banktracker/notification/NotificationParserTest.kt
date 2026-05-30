package com.fidriyanto.banktracker.notification

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class NotificationParserTest {

    // 2026-05-24T00:00:00Z = 2026-05-24 07:00 Asia/Bangkok — still May 24
    private val may24EpochMs = 1779580800000L

    @Test
    fun `parses bill payment notification`() {
        val result = NotificationParser.parse(
            title = "ชำระบิล / Bill Payment",
            text = "BTS TIM TVM 65.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("BTS TIM TVM", result.merchant)
        assertEquals(65.0, result.amount, 0.01)
        assertEquals("BillPayment", result.channel)
        assertEquals(LocalDate.of(2026, 5, 24), result.date)
        assertEquals("", result.referenceNo)
    }

    @Test
    fun `parses ewallet notification`() {
        val result = NotificationParser.parse(
            title = "E-Wallet Transfer",
            text = "K Plus Wallet 40.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("K Plus Wallet", result.merchant)
        assertEquals(40.0, result.amount, 0.01)
        assertEquals("eWallet", result.channel)
    }

    @Test
    fun `parses promptpay notification`() {
        val result = NotificationParser.parse(
            title = "โอนเงิน PromptPay",
            text = "MR MONGKON 226.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("MR MONGKON", result.merchant)
        assertEquals(226.0, result.amount, 0.01)
        assertEquals("PromptPay", result.channel)
    }

    @Test
    fun `parses bank transfer notification`() {
        val result = NotificationParser.parse(
            title = "โอนเงิน / Transfer",
            text = "TTB NITTRA PATTAR 55.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("TTB NITTRA PATTAR", result.merchant)
        assertEquals(55.0, result.amount, 0.01)
        assertEquals("BankTransfer", result.channel)
    }

    @Test
    fun `returns null when title has no recognized channel`() {
        assertNull(NotificationParser.parse(
            title = "Unknown App",
            text = "Something 100.00THB",
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `parses BBL mobile transfer notification`() {
        val result = NotificationParser.parse(
            title = "Bangkok Bank",
            text = "Transfer/Withdrawal from your account 12345 533.93THB via Mobile 28/5/26 14:23 The available balance is 15,234.56THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals(533.93, result.amount, 0.01)
        assertEquals("BankTransfer", result.channel)
        assertEquals("Transfer/Withdrawal from your account 12345", result.merchant)
    }

    @Test
    fun `parses BBL card alert with baht symbol`() {
        val result = NotificationParser.parse(
            title = "Bangkok Bank",
            text = "A transaction of 90.00฿ was made on 28/05/2026 at 08:27 hrs. If you did not make the transaction, please call back.",
            timestampMs = may24EpochMs
        )!!
        assertEquals(90.0, result.amount, 0.01)
        assertEquals("BankTransfer", result.channel)
    }

    @Test
    fun `parses baht symbol amount`() {
        val result = NotificationParser.parse(
            title = "Bill Payment",
            text = "Some Merchant 1,250.50฿",
            timestampMs = may24EpochMs
        )!!
        assertEquals(1250.5, result.amount, 0.01)
        assertEquals("Some Merchant", result.merchant)
    }

    @Test
    fun `returns null when text has no THB amount`() {
        assertNull(NotificationParser.parse(
            title = "Bill Payment",
            text = "Some merchant without amount",
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `returns null when text is null`() {
        assertNull(NotificationParser.parse(
            title = "Bill Payment",
            text = null,
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `returns null when title is null`() {
        assertNull(NotificationParser.parse(
            title = null,
            text = "BTS TIM TVM 65.00THB",
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `handles comma-separated amounts`() {
        val result = NotificationParser.parse(
            title = "Bill Payment",
            text = "TRUE MONEY CO LTD 1,500.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals(1500.0, result.amount, 0.01)
        assertEquals("TRUE MONEY CO LTD", result.merchant)
    }

    @Test
    fun `returns null when merchant is empty`() {
        assertNull(NotificationParser.parse(
            title = "Bill Payment",
            text = "65.00THB",
            timestampMs = may24EpochMs
        ))
    }
}
