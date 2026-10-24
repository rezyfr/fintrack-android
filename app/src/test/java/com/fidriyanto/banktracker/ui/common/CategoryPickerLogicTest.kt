package com.fidriyanto.banktracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryPickerLogicTest {
    private val all = listOf(
        PickerCategory("Bills", listOf("Rent", "Internet")),
        PickerCategory("Food & Drink", listOf("Restaurant", "Cafe / coffee")),
        PickerCategory("Transport", listOf("Fuel", "Toll")),
        PickerCategory("Groceries"),
        PickerCategory("Travel", listOf("Flights")),
        PickerCategory("Health"),
        PickerCategory("Gifts"),
        PickerCategory("Other"),
    )

    @Test fun categoryMatchKeepsAllSubcategories() {
        assertEquals(listOf(all[1]), filterCategories(all, "food"))
    }

    @Test fun subcategoryMatchKeepsOnlyTheMatchingOnes() {
        assertEquals(listOf(PickerCategory("Food & Drink", listOf("Cafe / coffee"))), filterCategories(all, "cafe"))
    }

    @Test fun searchIgnoresCaseAndUsesContains() {
        assertEquals(listOf("Bills"), filterCategories(all, "ILL").map { it.name })
        assertEquals(listOf("Transport"), filterCategories(all, "fUEL").map { it.name })
    }

    @Test fun emptyQueryReturnsAll() {
        assertEquals(all, filterCategories(all, ""))
        assertEquals(all, filterCategories(all, "   "))
    }

    @Test fun noMatchReturnsNothing() {
        assertEquals(emptyList<PickerCategory>(), filterCategories(all, "zzz"))
    }

    @Test fun chipsStartWithTheMostUsedThenTheNormalOrder() {
        assertEquals(
            listOf("Transport", "Bills", "Food & Drink", "Groceries", "Travel", "Health"),
            chipNames(all, listOf("Transport"), selected = "Transport"),
        )
    }

    @Test fun chipsWithNoHistoryUseTheFirstSix() {
        assertEquals(all.take(6).map { it.name }, chipNames(all, emptyList(), selected = "Bills"))
    }

    @Test fun selectedCategoryOutsideTheTopIsShownFirst() {
        val chips = chipNames(all, listOf("Bills"), selected = "Other")
        assertEquals("Other", chips.first())
        assertEquals(7, chips.size)
    }

    @Test fun selectedCategoryThatIsNotInTheListIsStillShown() {
        assertEquals("Removed", chipNames(all, emptyList(), selected = "Removed").first())
    }

    @Test fun blankSelectionAddsNoChip() {
        assertEquals(6, chipNames(all, emptyList(), selected = "").size)
    }

    @Test fun frequentNamesThatAreNotInTheListAreIgnored() {
        assertEquals(all.take(6).map { it.name }, chipNames(all, listOf("Gone"), selected = "Bills"))
    }
}
