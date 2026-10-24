package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomBankMatcherTest {
    private fun pattern(template: String, title: String? = null, enabled: Boolean = true, name: String = "Bank") =
        CustomBankPattern(id = name, name = name, titleContains = title, template = template, walletId = "A", enabled = enabled)

    @Test fun readsAmountAndMerchant() {
        val m = CustomBankMatcher.match(pattern("You paid {amount} to {merchant}"), "Pay", "You paid 120.50 to Corner Cafe")
        assertEquals(CustomBankMatch(120.5, "Corner Cafe"), m)
    }

    @Test fun readsDotThousandsAndCommaDecimal() {
        assertEquals(1234.56, CustomBankMatcher.match(pattern("Paid {amount} at {merchant}"), null, "Paid 1.234,56 at Shop")!!.amount, 0.0)
    }

    @Test fun readsCommaThousandsAndDotDecimal() {
        assertEquals(1234.56, CustomBankMatcher.match(pattern("Paid {amount} at {merchant}"), null, "Paid 1,234.56 at Shop")!!.amount, 0.0)
    }

    @Test fun readsAmountGluedToLetters() {
        val m = CustomBankMatcher.match(pattern("{merchant} {amount}THB"), null, "Corner Cafe 120.50THB")
        assertEquals(CustomBankMatch(120.5, "Corner Cafe"), m)
    }

    @Test fun readsAmountWithLettersBefore() {
        assertEquals(75.0, CustomBankMatcher.match(pattern("Spent USD{amount} today"), null, "Spent USD75 today")!!.amount, 0.0)
    }

    @Test fun merchantAtTheEndTakesTheRestOfTheText() {
        assertEquals("Big Shop Mall", CustomBankMatcher.match(pattern("Paid {amount} at {merchant}"), null, "Paid 10 at Big Shop Mall")!!.merchant)
    }

    @Test fun merchantInTheMiddleStopsAtTheNextWord() {
        val m = CustomBankMatcher.match(pattern("Paid {amount} at {merchant} on card"), null, "Paid 10 at Big Shop on card 1234")!!
        assertEquals("Big Shop", m.merchant)
    }

    @Test fun merchantBeforeAmountKeepsTheRightGroups() {
        val m = CustomBankMatcher.match(pattern("At {merchant} you paid {amount}"), null, "At Big Shop you paid 99.90")!!
        assertEquals(99.9, m.amount, 0.0)
        assertEquals("Big Shop", m.merchant)
    }

    @Test fun missingMerchantBlankGivesNullMerchant() {
        val m = CustomBankMatcher.match(pattern("Card payment of {amount}"), null, "Card payment of 55.00")!!
        assertNull(m.merchant)
    }

    @Test fun whitespaceInTheTextCanDiffer() {
        assertNotNull(CustomBankMatcher.match(pattern("You paid {amount} to {merchant}"), null, "You  paid\n5.00   to Cafe"))
    }

    @Test fun literalPartIsMatchedAsPlainText() {
        assertNull(CustomBankMatcher.match(pattern("Paid (ref.1) {amount}"), null, "Paid Xref.1 5.00"))
        assertNotNull(CustomBankMatcher.match(pattern("Paid (ref.1) {amount}"), null, "Paid (ref.1) 5.00"))
    }

    @Test fun textThatDoesNotFitGivesNull() {
        assertNull(CustomBankMatcher.match(pattern("You paid {amount} to {merchant}"), null, "Your statement is ready"))
    }

    @Test fun titleFilterIsCaseInsensitive() {
        val p = pattern("Paid {amount}", title = "my bank")
        assertNotNull(CustomBankMatcher.match(p, "My Bank Alert", "Paid 5"))
        assertNull(CustomBankMatcher.match(p, "Other", "Paid 5"))
        assertNull(CustomBankMatcher.match(p, null, "Paid 5"))
    }

    @Test fun emptyTitleFilterReadsAnyTitle() {
        assertNotNull(CustomBankMatcher.match(pattern("Paid {amount}", title = "  "), "Anything", "Paid 5"))
    }

    @Test fun disabledPatternDoesNotMatch() {
        assertNull(CustomBankMatcher.match(pattern("Paid {amount}", enabled = false), null, "Paid 5"))
    }

    @Test fun firstMatchInListOrderWins() {
        val first = pattern("Paid {amount}", name = "First")
        val second = pattern("Paid {amount} at {merchant}", name = "Second")
        val hit = CustomBankMatcher.firstMatch(listOf(first, second), null, "Paid 5 at Shop")!!
        assertEquals("First", hit.first.name)
    }

    @Test fun firstMatchSkipsPatternsThatDoNotMatch() {
        val a = pattern("Spent {amount}", name = "A")
        val b = pattern("Paid {amount}", name = "B", enabled = false)
        val c = pattern("Paid {amount}", name = "C")
        assertEquals("C", CustomBankMatcher.firstMatch(listOf(a, b, c), null, "Paid 5")!!.first.name)
    }

    @Test fun invalidTemplatesDoNotCompile() {
        assertNull(CustomBankMatcher.compile("No blank here"))
        assertNull(CustomBankMatcher.compile("{amount} and {amount}"))
        assertNull(CustomBankMatcher.compile("{amount} {merchant} {merchant}"))
        assertNotNull(CustomBankMatcher.compile("{amount}"))
    }

    @Test fun zeroAmountDoesNotMatch() {
        assertNull(CustomBankMatcher.match(pattern("Paid {amount}"), null, "Paid 0.00"))
    }

    @Test fun parseNumberFormats() {
        assertEquals(1234.56, CustomBankMatcher.parseNumber("1,234.56")!!, 0.0)
        assertEquals(1234.56, CustomBankMatcher.parseNumber("1.234,56")!!, 0.0)
        assertEquals(120.5, CustomBankMatcher.parseNumber("120.50")!!, 0.0)
        assertEquals(120.5, CustomBankMatcher.parseNumber("120,50")!!, 0.0)
        assertEquals(1234567.0, CustomBankMatcher.parseNumber("1,234,567")!!, 0.0)
        assertEquals(1234567.89, CustomBankMatcher.parseNumber("1.234.567,89")!!, 0.0)
        assertEquals(50000.0, CustomBankMatcher.parseNumber("50.000")!!, 0.0)
        assertEquals(1234.0, CustomBankMatcher.parseNumber("1,234")!!, 0.0)
        assertEquals(0.5, CustomBankMatcher.parseNumber("0.500")!!, 0.0)
        assertEquals(75.0, CustomBankMatcher.parseNumber("75")!!, 0.0)
    }
}
