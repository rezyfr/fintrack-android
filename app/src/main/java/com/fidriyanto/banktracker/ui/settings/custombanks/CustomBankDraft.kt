package com.fidriyanto.banktracker.ui.settings.custombanks

import com.fidriyanto.banktracker.domain.model.CustomBankMarks
import com.fidriyanto.banktracker.domain.model.CustomBankMatch
import com.fidriyanto.banktracker.domain.model.CustomBankMatcher
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import com.fidriyanto.banktracker.domain.model.CustomBankTemplate

enum class MarkMode { AMOUNT, MERCHANT }

enum class MarkError { NO_AMOUNT, MERCHANT_SPLIT }

// ac: custom-bank-patterns - what the editor holds while the user teaches a bank. The marks are word
// positions in the example text. The template is built from them, so it always fits the example.
data class CustomBankDraft(
    val id: String? = null,
    val title: String = "",
    val text: String = "",
    val mode: MarkMode = MarkMode.AMOUNT,
    val marks: CustomBankMarks = CustomBankMarks(),
    val name: String = "",
    val titleContains: String = "",
    val walletId: String = "",
    val txType: String = "expense",
    val enabled: Boolean = true,
    // The name and the title filter start as the notification title once, when step 3 opens first.
    val defaultsApplied: Boolean = false,
    val tryText: String = "",
) {
    val words: List<String> get() = CustomBankTemplate.words(text)
    val template: String? get() = CustomBankTemplate.build(words, marks)

    fun markError(): MarkError? = when {
        marks.amountIndex == null -> MarkError.NO_AMOUNT
        !CustomBankTemplate.merchantIsConnected(marks.merchantIndexes) -> MarkError.MERCHANT_SPLIT
        else -> null
    }

    fun nameError(): Boolean = name.isBlank()

    // A new text clears the marks, because they point at words of the old text.
    fun withText(newText: String) = copy(text = newText, marks = CustomBankMarks(), tryText = newText, defaultsApplied = defaultsApplied && id != null)

    // A word with a digit is the amount. Tapping it again clears the mark.
    fun tapAmount(index: Int): CustomBankDraft {
        val word = words.getOrNull(index) ?: return this
        if (!CustomBankTemplate.hasDigit(word)) return this
        return if (marks.amountIndex == index) copy(marks = marks.copy(amountIndex = null))
        else copy(marks = CustomBankMarks(index, marks.merchantIndexes - index))
    }

    fun tapMerchant(index: Int): CustomBankDraft {
        if (index == marks.amountIndex || index !in words.indices) return this
        val next = if (index in marks.merchantIndexes) marks.merchantIndexes - index else marks.merchantIndexes + index
        return copy(marks = marks.copy(merchantIndexes = next))
    }

    fun tap(index: Int) = if (mode == MarkMode.AMOUNT) tapAmount(index) else tapMerchant(index)

    fun withDefaults(): CustomBankDraft =
        if (defaultsApplied) this else copy(name = name.ifBlank { title.trim() }, titleContains = title.trim(), defaultsApplied = true, tryText = text)

    fun toPattern(newId: String): CustomBankPattern? {
        val built = template ?: return null
        return CustomBankPattern(
            id = id ?: newId, name = name.trim(), titleContains = titleContains.trim().ifEmpty { null },
            template = built, walletId = walletId, txType = txType, enabled = enabled,
            exampleTitle = title, exampleText = text,
        )
    }

    // What the pattern reads from the text in the Try it box. Null means it does not match.
    fun tryResult(): CustomBankMatch? = toPattern("preview")?.let { CustomBankMatcher.match(it, title, tryText) }

    companion object {
        fun from(p: CustomBankPattern) = CustomBankDraft(
            id = p.id, title = p.exampleTitle, text = p.exampleText,
            marks = CustomBankTemplate.marksOf(CustomBankTemplate.words(p.exampleText), p.template),
            name = p.name, titleContains = p.titleContains.orEmpty(), walletId = p.walletId, txType = p.txType,
            enabled = p.enabled, defaultsApplied = true, tryText = p.exampleText,
        )
    }
}
