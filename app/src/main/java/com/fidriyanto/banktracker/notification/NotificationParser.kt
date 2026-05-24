package com.fidriyanto.banktracker.notification

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

object NotificationParser {

    private val amountRegex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)THB""", RegexOption.IGNORE_CASE)
    private val bangkokZone = ZoneId.of("Asia/Bangkok")

    fun parse(title: String?, text: String?, timestampMs: Long): ParsedTransaction? {
        title ?: return null
        text ?: return null
        val channel = detectChannel(title) ?: return null
        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val merchant = text.substring(0, amountMatch.range.first).trim()
        if (merchant.isEmpty()) return null
        val date: LocalDate = Instant.ofEpochMilli(timestampMs)
            .atZone(bangkokZone)
            .toLocalDate()
        return ParsedTransaction(
            merchant = merchant,
            amount = amount,
            date = date,
            channel = channel,
            referenceNo = ""
        )
    }

    private fun detectChannel(title: String): String? {
        val t = title.lowercase()
        return when {
            "bill payment" in t || "ชำระบิล" in t -> "BillPayment"
            "e-wallet" in t || "ewallet" in t -> "eWallet"
            "promptpay" in t || "พร้อมเพย์" in t -> "PromptPay"
            "transfer" in t || "โอนเงิน" in t -> "BankTransfer"
            else -> null
        }
    }
}
