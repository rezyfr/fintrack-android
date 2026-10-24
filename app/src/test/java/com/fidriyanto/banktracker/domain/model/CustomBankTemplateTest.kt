package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomBankTemplateTest {
    private val words = CustomBankTemplate.words("You paid 120.50THB to Corner Cafe today")

    @Test fun splitsTextIntoWords() {
        assertEquals(listOf("You", "paid", "120.50THB", "to", "Corner", "Cafe", "today"), words)
        assertEquals(listOf("a", "b"), CustomBankTemplate.words("  a \n b  "))
    }

    @Test fun onlyTheNumberPartOfTheAmountWordIsTheBlank() {
        val t = CustomBankTemplate.build(words, CustomBankMarks(amountIndex = 2))
        assertEquals("You paid {amount}THB to Corner Cafe today", t)
    }

    @Test fun merchantWordsBecomeOneBlank() {
        val t = CustomBankTemplate.build(words, CustomBankMarks(2, setOf(4, 5)))
        assertEquals("You paid {amount}THB to {merchant} today", t)
    }

    @Test fun merchantWordsThatAreNotNextToEachOtherGiveNoTemplate() {
        assertNull(CustomBankTemplate.build(words, CustomBankMarks(2, setOf(1, 4))))
        assertFalse(CustomBankTemplate.merchantIsConnected(setOf(1, 4)))
        assertTrue(CustomBankTemplate.merchantIsConnected(setOf(4, 5, 6)))
        assertTrue(CustomBankTemplate.merchantIsConnected(emptySet()))
    }

    @Test fun noAmountGivesNoTemplate() {
        assertNull(CustomBankTemplate.build(words, CustomBankMarks()))
        assertNull(CustomBankTemplate.build(words, CustomBankMarks(amountIndex = 1)))
    }

    @Test fun amountWordCannotBeInTheMerchant() {
        assertNull(CustomBankTemplate.build(words, CustomBankMarks(2, setOf(2, 3))))
    }

    @Test fun builtTemplateMatchesItsExample() {
        val text = "You paid 1,234.50THB to Corner Cafe today"
        val w = CustomBankTemplate.words(text)
        val template = CustomBankTemplate.build(w, CustomBankMarks(2, setOf(4, 5)))!!
        val p = CustomBankPattern("1", "Bank", null, template, "A")
        assertEquals(CustomBankMatch(1234.5, "Corner Cafe"), CustomBankMatcher.match(p, null, text))
    }

    @Test fun marksAreReadBackFromATemplate() {
        val marks = CustomBankMarks(2, setOf(4, 5))
        val template = CustomBankTemplate.build(words, marks)!!
        assertEquals(marks, CustomBankTemplate.marksOf(words, template))
    }

    @Test fun marksAreReadBackWhenTheMerchantComesFirst() {
        val w = CustomBankTemplate.words("Corner Cafe charged you 9.99 today")
        val marks = CustomBankMarks(4, setOf(0, 1))
        assertEquals(marks, CustomBankTemplate.marksOf(w, CustomBankTemplate.build(w, marks)!!))
    }

    @Test fun marksWithoutMerchantAreReadBack() {
        val marks = CustomBankMarks(amountIndex = 2)
        assertEquals(marks, CustomBankTemplate.marksOf(words, CustomBankTemplate.build(words, marks)!!))
    }

    @Test fun aTemplateThatDoesNotFitTheWordsGivesNoMarks() {
        assertEquals(CustomBankMarks(), CustomBankTemplate.marksOf(words, "Paid {amount}"))
    }
}
