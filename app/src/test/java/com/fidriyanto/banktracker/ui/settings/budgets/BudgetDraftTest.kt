package com.fidriyanto.banktracker.ui.settings.budgets

import com.fidriyanto.banktracker.domain.model.BudgetLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetDraftTest {
    private val base = BudgetDraft(category = "Food", target = "5000", currency = "AAA")

    @Test fun aLineThatFollowsACategoryNeedsOne() {
        assertEquals(BudgetError.CATEGORY, base.copy(category = "").whatError())
        assertNull(base.whatError())
    }

    @Test fun theTargetMustBeANumberOfZeroOrMore() {
        assertNull(base.copy(target = "0").howError())
        assertEquals(BudgetError.TARGET, base.copy(target = "").howError())
        assertEquals(BudgetError.TARGET, base.copy(target = "-5").howError())
        assertEquals(BudgetError.TARGET, base.copy(target = ".").howError())
    }

    @Test fun aNewLineHasIdZeroAndTheDraftValues() {
        val line = base.copy(kind = KIND_FIXED, subcategory = "Cafe", subOn = true, dueOn = true, dueDay = 12).toBudgetLine()
        assertEquals(0L, line.id)
        assertEquals("fixed", line.kind)
        assertEquals("Food", line.category)
        assertEquals("Cafe", line.subcategory)
        assertEquals(5000.0, line.target, 0.0)
        assertEquals("AAA", line.currency)
        assertEquals(12, line.dueDay)
    }

    @Test fun theNameFallsBackToTheSubcategoryThenTheCategory() {
        assertEquals("Food", base.toBudgetLine().name)
        assertEquals("Cafe", base.copy(subcategory = "Cafe", subOn = true).toBudgetLine().name)
        assertEquals("Eating out", base.copy(name = "  Eating out ").toBudgetLine().name)
    }

    @Test fun aSubcategoryThatIsSwitchedOffIsDropped() {
        assertNull(base.copy(subcategory = "Cafe", subOn = false).toBudgetLine().subcategory)
    }

    @Test fun aDueDayOnlyCountsForAFixedLine() {
        assertNull(base.copy(kind = KIND_FLEX, dueOn = true, dueDay = 5).toBudgetLine().dueDay)
        assertNull(base.copy(kind = KIND_FIXED, dueOn = false, dueDay = 5).toBudgetLine().dueDay)
    }

    private val ruleLine = BudgetLine(
        7, "Rent", "fixed", "AAA", 11500.0, listOf("W1"), "Rent|Lease", listOf("Housing"), 2, null, null, 10,
    )

    @Test fun aLineWithMatchRulesKeepsItsRuleAndHasNoCategory() {
        val d = BudgetDraft.from(ruleLine)
        assertTrue(d.byRules)
        assertNull(d.whatError())
        val saved = d.copy(name = "Home rent", target = "12000", kind = KIND_FLEX, currency = "BBB").toBudgetLine()
        assertEquals(7L, saved.id)
        assertEquals("Rent|Lease", saved.matchPattern)
        assertEquals(listOf("W1"), saved.matchWallets)
        assertEquals(listOf("Housing"), saved.matchCategories)
        assertEquals(2, saved.sortOrder)
        assertNull(saved.category)
        assertEquals("Home rent", saved.name)
        assertEquals(12000.0, saved.target, 0.0)
        assertEquals("flex", saved.kind)
        assertEquals("BBB", saved.currency)
    }

    @Test fun aBlankNameOnARuleLineKeepsTheOldName() {
        assertEquals("Rent", BudgetDraft.from(ruleLine).copy(name = "").toBudgetLine().name)
    }

    @Test fun anExistingCategoryLineOpensWithItsValues() {
        val line = BudgetLine(3, "Cafes", "flex", "AAA", 800.5, null, null, null, 1, "Food", "Cafe", null)
        val d = BudgetDraft.from(line)
        assertFalse(d.byRules)
        assertFalse(d.isNew)
        assertTrue(d.subOn)
        assertEquals("800.5", d.target)
        assertEquals(line, d.toBudgetLine())
    }
}
