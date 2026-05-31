package com.fidriyanto.banktracker.notification

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

object NotificationParser {

    private val bblAmountRegex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)(?:THB|฿)""", RegexOption.IGNORE_CASE)
    // ac: bca-expense-notification — myBCA notifications whose body matches 'You spent IDR x at [Category]' are parsed
    private val bcaAmountRegex = Regex("""You spent IDR\s+([\d,]+(?:\.\d{1,2})?)\s+at\s+(.+)""", RegexOption.IGNORE_CASE)
    private val bangkokZone = ZoneId.of("Asia/Bangkok")

    // ac: bca-expense-notification — the BCA category text is mapped to the nearest app category; unrecognised categories default to Other
    private val bcaCategoryMap = listOf(
        "food"         to "Food & Drink",
        "beverage"     to "Food & Drink",
        "dining"       to "Food & Drink",
        "restaurant"   to "Food & Drink",
        "grocery"      to "Groceries",
        "groceries"    to "Groceries",
        "supermarket"  to "Groceries",
        "shop"         to "Shopping",
        "retail"       to "Shopping",
        "fashion"      to "Shopping",
        "transport"    to "Transport",
        "taxi"         to "Transport",
        "commute"      to "Transport",
        "travel"       to "Travel",
        "hotel"        to "Travel",
        "health"       to "Health & Wellbeing",
        "medical"      to "Health & Wellbeing",
        "pharmacy"     to "Health & Wellbeing",
        "entertainment" to "Entertainment",
        "bill"         to "Bills",
        "utilit"       to "Bills",
        "subscription" to "Subscriptions",
        "business"     to "Business",
        "gift"         to "Gifts",
    )

    private fun mapBcaCategory(raw: String): String {
        val lower = raw.lowercase()
        return bcaCategoryMap.firstOrNull { lower.contains(it.first) }?.second ?: "Other"
    }

    fun parse(title: String?, text: String?, timestampMs: Long): ParsedTransaction? {
        title ?: return null
        text  ?: return null
        if (!isBankNotification(title, text)) return null
        val date: LocalDate = Instant.ofEpochMilli(timestampMs)
            .atZone(bangkokZone)
            .toLocalDate()

        // ac: bca-expense-notification — myBCA notifications whose body matches 'You spent IDR x at [Category]' are parsed
        if ("mybca" in title.lowercase() || "my bca" in title.lowercase()) {
            val bcaMatch = bcaAmountRegex.find(text) ?: return null
            val amount = bcaMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val rawCategory = bcaMatch.groupValues[2].trim()
            // ac: bca-expense-notification — the merchant field is set to the BCA category text
            return ParsedTransaction(
                merchant = rawCategory,
                amount   = amount,
                date     = date,
                referenceNo = "",
                wallet   = "BCA",
                category = mapBcaCategory(rawCategory),
            )
        }

        // BBL / Bangkok Bank path
        val amountMatch = bblAmountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val merchant = text.substring(0, amountMatch.range.first).trim()
        if (merchant.isEmpty()) return null
        return ParsedTransaction(
            merchant = merchant,
            amount   = amount,
            date     = date,
            referenceNo = "",
            wallet   = "BBL",
            category = "Other",
        )
    }

    private fun isBankNotification(title: String, text: String): Boolean {
        val t = title.lowercase()
        val b = text.lowercase()
        return "mybca" in t || "my bca" in t ||
               "you spent idr" in b ||
               "bill payment" in t || "ชำระบิล" in t ||
               "e-wallet" in t || "ewallet" in t ||
               "promptpay" in t || "พร้อมเพย์" in t ||
               "transfer" in t || "โอนเงิน" in t ||
               "bangkok bank" in t || "bbl" in t || "bualuang" in t || "ธนาคารกรุงเทพ" in t
    }
}
