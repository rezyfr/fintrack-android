package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.AiNotificationReply
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.CategorySuggestion

// Turns the one-line text a small model writes into checked values. The model is never trusted:
// amounts, currencies and categories are validated against what the app knows.
object AiReplyParser {
    // "85.00" and "1,500.50" use a decimal point. "125.000" and "1,500,000" use thousands separators.
    fun parseAmount(raw: String): Double? {
        val s = raw.filter { it.isDigit() || it == '.' || it == ',' }
        if (s.none { it.isDigit() }) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val decimalAt = when {
            lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
            lastDot >= 0 -> lastDot.takeIf { s.count { it == '.' } == 1 && s.length - it - 1 != 3 }
            lastComma >= 0 -> lastComma.takeIf { s.count { it == ',' } == 1 && s.length - it - 1 != 3 }
            else -> null
        }
        val whole = (if (decimalAt == null) s else s.substring(0, decimalAt)).filter { it.isDigit() }
        val fraction = decimalAt?.let { s.substring(it + 1).filter { c -> c.isDigit() } }.orEmpty()
        return "$whole${if (fraction.isEmpty()) "" else ".$fraction"}".toDoubleOrNull()
    }

    fun normalizeCurrency(raw: String, known: Collection<String> = emptyList()): String = CurrencyDetect.normalize(raw, known)

    // The first currency mark in the notification text, used when the model leaves the currency out.
    fun detectCurrency(text: String, known: Collection<String> = emptyList()): String = CurrencyDetect.detect(text, known)

    // Reply format: type|amount|currency|merchant|category|subcategory. A type of none means not a payment.
    fun parseNotification(
        reply: String,
        sourceText: String,
        categories: List<Category>,
        knownCurrencies: Collection<String> = emptyList(),
    ): AiNotificationReply? {
        val f = fields(reply) ?: return null
        val isIncome = when (f[0].lowercase()) { "expense" -> false; "income" -> true; else -> return null }
        val amount = parseAmount(f[1])?.takeIf { it > 0 } ?: return null
        val currency = normalizeCurrency(f[2], knownCurrencies).ifEmpty { detectCurrency(sourceText, knownCurrencies) }
        if (currency.isEmpty()) return null
        val (category, sub) = match(f[4], f[5], categories) ?: ("Other" to null)
        return AiNotificationReply(isIncome, amount, currency, f[3].trim(), category, sub)
    }

    // Reply format: category|subcategory. The category must exist in the user's list.
    fun parseSuggestion(reply: String, categories: List<Category>): CategorySuggestion? {
        val line = reply.lineSequence().firstOrNull { it.isNotBlank() } ?: return null
        val parts = line.split('|').map { it.trim() }
        return match(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, categories)
            ?.let { CategorySuggestion(it.first, it.second) }
    }

    private fun fields(reply: String): List<String>? {
        val line = reply.lineSequence().firstOrNull { it.count { c -> c == '|' } >= 2 } ?: return null
        return line.split('|').map { it.trim() }.let { it + List(maxOf(0, 6 - it.size)) { "" } }
    }

    private fun match(category: String, sub: String, categories: List<Category>): Pair<String, String?>? {
        val c = categories.firstOrNull { it.name.equals(category.trim(), ignoreCase = true) } ?: return null
        return c.name to c.subcategories.firstOrNull { it.equals(sub.trim(), ignoreCase = true) }
    }
}
