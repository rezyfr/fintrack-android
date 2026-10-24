package com.fidriyanto.banktracker.ui.settings.custombanks

import com.fidriyanto.banktracker.domain.model.CustomBankMarks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomBankDraftTest {
    private val draft = CustomBankDraft(title = "Pay alert", text = "You paid 120.50THB to Corner Cafe", walletId = "A")

    @Test fun amountModeOnlyMarksAWordWithADigit() {
        assertNull(draft.tap(0).marks.amountIndex)
        assertEquals(2, draft.tap(2).marks.amountIndex)
    }

    @Test fun tappingTheAmountAgainClearsIt() {
        assertNull(draft.tap(2).tap(2).marks.amountIndex)
    }

    @Test fun merchantModeTogglesWordsAndSkipsTheAmountWord() {
        val d = draft.tap(2).copy(mode = MarkMode.MERCHANT).tap(4).tap(5).tap(2)
        assertEquals(setOf(4, 5), d.marks.merchantIndexes)
        assertEquals("You paid {amount}THB to {merchant}", d.template)
        assertEquals(setOf(5), d.tap(4).marks.merchantIndexes)
    }

    @Test fun markingAWordAsAmountRemovesItFromTheMerchant() {
        val d = draft.copy(mode = MarkMode.MERCHANT).tap(2).copy(mode = MarkMode.AMOUNT).tap(2)
        assertEquals(emptySet<Int>(), d.marks.merchantIndexes)
        assertEquals(2, d.marks.amountIndex)
    }

    @Test fun errorsForNoAmountAndSplitMerchant() {
        assertEquals(MarkError.NO_AMOUNT, draft.markError())
        val split = draft.tap(2).copy(mode = MarkMode.MERCHANT).tap(1).tap(5)
        assertEquals(MarkError.MERCHANT_SPLIT, split.markError())
        assertNull(split.tap(1).tap(5).markError())
    }

    @Test fun newTextClearsTheMarks() {
        assertEquals(CustomBankMarks(), draft.tap(2).withText("Other 5").marks)
    }

    @Test fun defaultsUseTheTitleOnce() {
        val d = draft.tap(2).withDefaults()
        assertEquals("Pay alert", d.name)
        assertEquals("Pay alert", d.titleContains)
        val cleared = d.copy(titleContains = "").withDefaults()
        assertEquals("", cleared.titleContains)
    }

    @Test fun tryItShowsTheReadingOrNoMatch() {
        val d = draft.copy(mode = MarkMode.AMOUNT).tap(2).copy(mode = MarkMode.MERCHANT).tap(4).tap(5).withDefaults()
        assertEquals(120.5, d.tryResult()!!.amount, 0.0)
        assertEquals("Corner Cafe", d.tryResult()!!.merchant)
        assertNull(d.copy(tryText = "Nothing here").tryResult())
        assertNull(d.copy(titleContains = "other").tryResult())
    }

    @Test fun savedPatternOpensWithItsMarks() {
        val d = draft.tap(2).copy(mode = MarkMode.MERCHANT).tap(4).tap(5).withDefaults()
        val pattern = d.toPattern("new")!!
        assertEquals("new", pattern.id)
        val reopened = CustomBankDraft.from(pattern)
        assertEquals(d.marks, reopened.marks)
        assertEquals(pattern.template, reopened.template)
        assertNotNull(reopened.toPattern("x"))
    }
}
