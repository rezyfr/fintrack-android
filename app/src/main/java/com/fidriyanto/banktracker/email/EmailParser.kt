package com.fidriyanto.banktracker.email

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.sheets.DateFormatter
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailParser @Inject constructor() {

    fun parse(htmlBody: String): ParsedTransaction? {
        val fields = extractFields(htmlBody)
        val amount = fields["Amount (Baht)"]?.toDoubleOrNull() ?: return null
        val fee = fields["Fee (Baht)"]?.toDoubleOrNull() ?: 0.0
        val dateRaw = fields["Date"] ?: return null
        val date = DateFormatter.parseEmailDate(dateRaw) ?: return null
        val refNo = fields["Reference no."]
            ?: fields["Bank Reference No."]
            ?: fields["Bank reference no."]
            ?: fields["Reference no. 1"]
            ?: ""

        val (merchant, channel) = resolveMerchantAndChannel(fields)

        return ParsedTransaction(
            merchant = merchant,
            amount = amount + fee,
            date = date,
            channel = channel,
            referenceNo = refNo,
            rawFields = fields
        )
    }

    private fun extractFields(html: String): Map<String, String> {
        val doc = Jsoup.parse(html)
        val result = mutableMapOf<String, String>()
        val cells = doc.select("td")
        var i = 0
        while (i < cells.size - 1) {
            val label = cells[i].text().trim()
            val value = cells[i + 1].text().trim()
            if (label.isNotEmpty() && value.isNotEmpty()) {
                result[label] = value
            }
            i += 2
        }
        return result
    }

    private fun resolveMerchantAndChannel(fields: Map<String, String>): Pair<String, String> {
        return when {
            fields.containsKey("Service name / Payee name") ->
                Pair(fields["Service name / Payee name"] ?: "Unknown", "BillPayment")
            fields.containsKey("e-wallet provider name") -> {
                val provider = fields["e-wallet provider name"] ?: "eWallet"
                Pair(provider, "eWallet")
            }
            fields["Receiving method"]?.contains("PromptPay", ignoreCase = true) == true -> {
                val name = fields["Account name"]?.take(30) ?: "Unknown"
                Pair("PromptPay – $name", "PromptPay")
            }
            fields.containsKey("Bank") -> {
                val bank = fields["Bank"] ?: "Bank"
                val name = fields["Account name"]?.take(30) ?: "Unknown"
                Pair("$bank – $name", "BankTransfer")
            }
            else -> Pair("Unknown", "Unknown")
        }
    }
}
