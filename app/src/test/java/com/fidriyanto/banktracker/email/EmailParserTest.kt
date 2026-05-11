package com.fidriyanto.banktracker.email

import org.junit.Assert.*
import org.junit.Test

class EmailParserTest {
    private val parser = EmailParser()

    private val billPaymentHtml = """
        <table>
          <tr><td>Service name / Payee name</td><td>TRUE MONEY CO., LTD.</td></tr>
          <tr><td>Amount (Baht)</td><td>500.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Reference no.</td><td>417486</td></tr>
          <tr><td>Date</td><td>10 May 2026 at 17:52:34 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    private val eWalletHtml = """
        <table>
          <tr><td>e-wallet number</td><td>004xx-xxx-xxx-7802</td></tr>
          <tr><td>e-wallet owner</td><td>MR. NUTTAWUT KOSANPRAPAI</td></tr>
          <tr><td>e-wallet provider name</td><td>K Plus Wallet</td></tr>
          <tr><td>Amount (Baht)</td><td>100.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Bank Reference No.</td><td>599120</td></tr>
          <tr><td>Date</td><td>10 May 2026 at 10:05:26 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    private val promptPayHtml = """
        <table>
          <tr><td>Receiving method</td><td>Deposit to recipient's account registered with PromptPay</td></tr>
          <tr><td>Account name</td><td>MR MONGKON JUNSINGKORN</td></tr>
          <tr><td>Amount (Baht)</td><td>226.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Reference no.</td><td>464325</td></tr>
          <tr><td>Date</td><td>10 May 2026 at 13:46:01 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    private val bankTransferHtml = """
        <table>
          <tr><td>Account name</td><td>NITTRA PATTAR</td></tr>
          <tr><td>Bank</td><td>TTB</td></tr>
          <tr><td>Amount (Baht)</td><td>55.00</td></tr>
          <tr><td>Fee (Baht)</td><td>0.00</td></tr>
          <tr><td>Bank reference no.</td><td>445720</td></tr>
          <tr><td>Date</td><td>9 May 2026 at 14:49:06 (Thailand time)</td></tr>
        </table>
    """.trimIndent()

    @Test fun `parses bill payment format`() {
        val result = parser.parse(billPaymentHtml)!!
        assertEquals("TRUE MONEY CO., LTD.", result.merchant)
        assertEquals(500.0, result.amount, 0.01)
        assertEquals("BillPayment", result.channel)
        assertEquals("417486", result.referenceNo)
        assertEquals(10, result.date.dayOfMonth)
        assertEquals(5, result.date.monthValue)
    }

    @Test fun `parses e-wallet format`() {
        val result = parser.parse(eWalletHtml)!!
        assertEquals("K Plus Wallet", result.merchant)
        assertEquals(100.0, result.amount, 0.01)
        assertEquals("eWallet", result.channel)
        assertEquals("599120", result.referenceNo)
    }

    @Test fun `parses PromptPay format`() {
        val result = parser.parse(promptPayHtml)!!
        assertTrue(result.merchant.contains("MR MONGKON"))
        assertEquals(226.0, result.amount, 0.01)
        assertEquals("PromptPay", result.channel)
    }

    @Test fun `parses bank transfer format`() {
        val result = parser.parse(bankTransferHtml)!!
        assertTrue(result.merchant.contains("TTB"))
        assertEquals(55.0, result.amount, 0.01)
        assertEquals("BankTransfer", result.channel)
    }

    @Test fun `adds fee to amount`() {
        val html = """
            <table>
              <tr><td>Service name / Payee name</td><td>SomeService</td></tr>
              <tr><td>Amount (Baht)</td><td>100.00</td></tr>
              <tr><td>Fee (Baht)</td><td>5.00</td></tr>
              <tr><td>Reference no.</td><td>123</td></tr>
              <tr><td>Date</td><td>10 May 2026 at 10:00:00 (Thailand time)</td></tr>
            </table>
        """.trimIndent()
        val result = parser.parse(html)!!
        assertEquals(105.0, result.amount, 0.01)
    }

    @Test fun `returns null when amount missing`() {
        assertNull(parser.parse("<table></table>"))
    }
}
