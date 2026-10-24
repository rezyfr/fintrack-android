package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.CURRENCY_SYMBOLS
import java.util.Currency
import java.util.Locale

// Finds and normalizes currencies without a fixed list. Any ISO 4217 code works, and so does every
// symbol in the app's symbol table. [known] holds the user's own currencies. They win when a symbol
// or a word fits more than one currency, such as "$" or "dollar".
object CurrencyDetect {
    private val isoCodes: Set<String> by lazy { Currency.getAvailableCurrencies().map { it.currencyCode }.toSet() }

    // Symbols and words that are not ISO codes, mapped to the codes they can mean.
    private val marks: Map<String, List<String>> by lazy {
        val fromTable = CURRENCY_SYMBOLS.entries.groupBy({ it.value.trim().lowercase() }, { it.key })
        fromTable + mapOf("บาท" to listOf("THB"), "rp." to listOf("IDR"))
    }

    private val markPattern = Regex(
        "(?:(?<![A-Za-z])(Rp\\.?|S\\$|A\\$|RM|[฿$€£¥₫₱]|บาท)\\s?\\d)|(?:\\d\\s?(บาท|Rp|[฿€£¥₫₱]))",
        RegexOption.IGNORE_CASE,
    )
    private val codeBeforeNumber = Regex("(?<![A-Za-z])([A-Z]{3})\\s?\\d")
    private val codeAfterNumber = Regex("\\d\\s?([A-Z]{3})(?![A-Za-z])")

    // The code a currency word or symbol stands for, or an empty text when it is unclear.
    fun normalize(raw: String, known: Collection<String> = emptyList()): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        if (t.uppercase() in isoCodes) return t.uppercase()
        pick(marks[t.lowercase()], known, firstWins = true)?.let { return it }
        if (t.length < 4) return ""
        val byName = Currency.getAvailableCurrencies()
            .filter { it.getDisplayName(Locale.ENGLISH).contains(t, ignoreCase = true) }
            .map { it.currencyCode }
        return pick(byName, known).orEmpty()
    }

    // The first currency mark that sits next to a number in [text], for example "THB 50", "50 บาท" or "$12".
    fun detect(text: String, known: Collection<String> = emptyList()): String {
        val candidates = buildList {
            markPattern.findAll(text).forEach { m -> add(m.range.first to normalize(m.groupValues[1].ifEmpty { m.groupValues[2] }, known)) }
            codeBeforeNumber.findAll(text).forEach { m -> add(m.range.first to normalize(m.groupValues[1], known)) }
            codeAfterNumber.findAll(text).forEach { m -> add(m.range.first to normalize(m.groupValues[1], known)) }
        }
        return candidates.filter { it.second.isNotEmpty() }.minByOrNull { it.first }?.second.orEmpty()
    }

    fun hasMark(text: String): Boolean = detect(text).isNotEmpty()

    // One code, or the user's own when several fit. A symbol shared by two currencies, such as ¥, takes
    // the first. A currency word that fits many, such as "dollar", stays unclear.
    private fun pick(codes: List<String>?, known: Collection<String>, firstWins: Boolean = false): String? {
        if (codes.isNullOrEmpty()) return null
        val own = codes.filter { it in known }
        return when {
            own.isNotEmpty() -> own.first()
            codes.size == 1 || firstWins -> codes.first()
            else -> null
        }
    }
}
