package com.fidriyanto.banktracker.domain.model

// The words the user marked in an example notification.
data class CustomBankMarks(val amountIndex: Int? = null, val merchantIndexes: Set<Int> = emptySet())

// ac: custom-bank-patterns - builds a template from the words the user marked, and reads the marks back
// from a template. A word is a run of characters without white space.
object CustomBankTemplate {
    private val number = Regex("""\d+(?:[.,]\d+)*""")
    private val spaces = Regex("""\s+""")

    fun words(text: String): List<String> = text.trim().split(spaces).filter { it.isNotEmpty() }

    fun hasDigit(word: String): Boolean = word.any { it.isDigit() }

    // The merchant words must be next to each other. An empty merchant is fine.
    fun merchantIsConnected(indexes: Set<Int>): Boolean =
        indexes.isEmpty() || indexes.max() - indexes.min() == indexes.size - 1

    // Null when no amount is marked or the merchant words are not next to each other.
    // Only the number part of the amount word becomes {amount}. The rest of the word, for example
    // a currency code, stays in the template as it is.
    fun build(words: List<String>, marks: CustomBankMarks): String? {
        val at = marks.amountIndex?.takeIf { it in words.indices && hasDigit(words[it]) } ?: return null
        if (!merchantIsConnected(marks.merchantIndexes) || at in marks.merchantIndexes) return null
        val parts = mutableListOf<String>()
        words.forEachIndexed { i, word ->
            when {
                i == at -> parts += word.replaceFirst(number, AMOUNT_BLANK)
                i in marks.merchantIndexes -> if (i == marks.merchantIndexes.min()) parts += MERCHANT_BLANK
                else -> parts += word
            }
        }
        return parts.joinToString(" ")
    }

    // The reverse of build. Each word that is not in the merchant is one template part, so the merchant
    // length is the difference. Returns no marks when the template does not fit the words.
    fun marksOf(words: List<String>, template: String): CustomBankMarks {
        val parts = words(template)
        val amountPart = parts.indexOfFirst { AMOUNT_BLANK in it }
        val merchantPart = parts.indexOf(MERCHANT_BLANK)
        val merchantLength = if (merchantPart < 0) 0 else words.size - (parts.size - 1)
        if (amountPart < 0 || (merchantPart >= 0 && merchantLength < 1) || (merchantPart < 0 && parts.size != words.size)) return CustomBankMarks()
        val amountAt = if (merchantPart in 0 until amountPart) amountPart + merchantLength - 1 else amountPart
        val merchant = if (merchantPart < 0) emptySet() else (merchantPart until merchantPart + merchantLength).toSet()
        return CustomBankMarks(amountAt, merchant)
    }
}
