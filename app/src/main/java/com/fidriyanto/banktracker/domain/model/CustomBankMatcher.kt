package com.fidriyanto.banktracker.domain.model

// What a custom pattern read from one notification. A null merchant means the notification has no merchant text.
data class CustomBankMatch(val amount: Double, val merchant: String?)

const val AMOUNT_BLANK = "{amount}"
const val MERCHANT_BLANK = "{merchant}"

// ac: custom-bank-patterns - turns a template into a regex and reads a notification with it. No Android code.
object CustomBankMatcher {
    private val blanks = Regex("""\{amount\}|\{merchant\}""")
    private const val AMOUNT_GROUP = """(\d+(?:[.,]\d+)*)"""

    // Null when the template is not valid: {amount} must appear once and {merchant} at most once.
    fun compile(template: String): Regex? {
        val text = template.trim()
        val found = blanks.findAll(text).toList()
        if (found.count { it.value == AMOUNT_BLANK } != 1 || found.count { it.value == MERCHANT_BLANK } > 1) return null
        val out = StringBuilder()
        var from = 0
        for (m in found) {
            out.append(literal(text.substring(from, m.range.first)))
            val atEnd = m.range.last + 1 >= text.length
            // A merchant at the end takes the rest of the text. Elsewhere it takes as little as it can.
            out.append(if (m.value == AMOUNT_BLANK) AMOUNT_GROUP else if (atEnd) "(.+)" else "(.+?)")
            from = m.range.last + 1
        }
        out.append(literal(text.substring(from)))
        return runCatching { Regex(out.toString(), setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)) }.getOrNull()
    }

    // A space in the template matches any run of white space. All other characters match as they are.
    private fun literal(part: String): String = buildString {
        var i = 0
        while (i < part.length) {
            val space = part[i].isWhitespace()
            var j = i
            while (j < part.length && part[j].isWhitespace() == space) j++
            append(if (space) """\s+""" else Regex.escape(part.substring(i, j)))
            i = j
        }
    }

    fun match(pattern: CustomBankPattern, title: String?, text: String): CustomBankMatch? {
        if (!pattern.enabled) return null
        val filter = pattern.titleContains?.trim().orEmpty()
        if (filter.isNotEmpty() && title?.contains(filter, ignoreCase = true) != true) return null
        val found = compile(pattern.template)?.find(text) ?: return null
        val amountAt = pattern.template.indexOf(AMOUNT_BLANK)
        val merchantAt = pattern.template.indexOf(MERCHANT_BLANK)
        val amountGroup = if (merchantAt in 0 until amountAt) 2 else 1
        val amount = parseNumber(found.groupValues[amountGroup])?.takeIf { it > 0.0 } ?: return null
        val merchant = if (merchantAt < 0) null else found.groupValues[3 - amountGroup].trim().ifEmpty { null }
        return CustomBankMatch(amount, merchant)
    }

    // The first enabled pattern that reads the notification wins, in list order.
    fun firstMatch(patterns: List<CustomBankPattern>, title: String?, text: String): Pair<CustomBankPattern, CustomBankMatch>? {
        for (p in patterns) match(p, title, text)?.let { return p to it }
        return null
    }

    // Reads "1,234.56", "1.234,56", "120.50", "50.000" and "1,234,567". The last separator is the decimal
    // point when both kinds appear. One separator followed by exactly three digits, after one to three
    // digits that do not start with 0, is a thousands separator. Any other single separator is a decimal point.
    fun parseNumber(raw: String): Double? {
        val last = maxOf(raw.lastIndexOf('.'), raw.lastIndexOf(','))
        if (last < 0) return raw.toDoubleOrNull()
        val sep = raw[last]
        val other = if (sep == '.') ',' else '.'
        val head = raw.substring(0, last)
        val tail = raw.substring(last + 1)
        val several = raw.count { it == sep } > 1
        val thousands = several || (other !in head && tail.length == 3 && head.length in 1..3 && !head.startsWith("0"))
        val whole = head.replace(".", "").replace(",", "")
        return if (thousands) (whole + tail).toDoubleOrNull() else "$whole.$tail".toDoubleOrNull()
    }
}
