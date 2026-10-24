package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.domain.model.BudgetLine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageBudgetLinesUseCaseTest {
    private fun line(id: Long, order: Int = 0, category: String? = "Food", sub: String? = null, currency: String = "AAA") =
        BudgetLine(id, "L$id", "flex", currency, 100.0, null, null, null, order, category, sub)

    private val repo = mockk<BudgetLineRepository>()
    private val useCase = ManageBudgetLinesUseCase(repo)

    @Test fun aNewLineGetsTheHighestSortOrderPlusOne() = runBlocking {
        val created = slot<BudgetLine>()
        coEvery { repo.getAll() } returns Result.success(listOf(line(1, 3), line(2, 7), line(3, 5)))
        coEvery { repo.create(capture(created)) } returns Result.success(Unit)
        assertTrue(useCase.save(line(0)).isSuccess)
        assertEquals(8, created.captured.sortOrder)
    }

    @Test fun aNewLineWithNoLinesGetsSortOrderOne() = runBlocking {
        val created = slot<BudgetLine>()
        coEvery { repo.getAll() } returns Result.success(emptyList())
        coEvery { repo.create(capture(created)) } returns Result.success(Unit)
        useCase.save(line(0))
        assertEquals(1, created.captured.sortOrder)
    }

    @Test fun anExistingLineIsUpdatedWithItsOwnSortOrder() = runBlocking {
        val updated = slot<BudgetLine>()
        coEvery { repo.update(capture(updated)) } returns Result.success(Unit)
        useCase.save(line(4, order = 2))
        assertEquals(2, updated.captured.sortOrder)
        coVerify(exactly = 0) { repo.create(any()) }
        coVerify(exactly = 0) { repo.getAll() }
    }

    @Test fun aFailedListStopsTheCreate() = runBlocking {
        coEvery { repo.getAll() } returns Result.failure(IllegalStateException("offline"))
        assertTrue(useCase.save(line(0)).isFailure)
        coVerify(exactly = 0) { repo.create(any()) }
    }

    @Test fun deleteAndListGoToTheRepository() = runBlocking {
        val lines = listOf(line(1))
        coEvery { repo.getAll() } returns Result.success(lines)
        coEvery { repo.delete(1) } returns Result.success(Unit)
        assertSame(lines, useCase.list().getOrThrow())
        assertTrue(useCase.delete(1).isSuccess)
    }

    @Test fun nextSortOrderIsOneAfterTheHighest() {
        assertEquals(1, BudgetLineRules.nextSortOrder(emptyList()))
        assertEquals(10, BudgetLineRules.nextSortOrder(listOf(line(1, 9), line(2, 2))))
    }

    @Test fun aLineWithTheSameCategorySubcategoryAndCurrencyIsADuplicate() {
        val others = listOf(line(1, sub = "Cafe"), line(2, sub = null), line(3, currency = "BBB", sub = null))
        assertEquals(2L, BudgetLineRules.duplicateOf(line(9, sub = null), others)?.id)
        assertEquals(1L, BudgetLineRules.duplicateOf(line(9, sub = "Cafe"), others)?.id)
    }

    @Test fun aDifferentCurrencyOrSubcategoryIsNotADuplicate() {
        val others = listOf(line(1, sub = "Cafe"))
        assertNull(BudgetLineRules.duplicateOf(line(9, sub = "Bar"), others))
        assertNull(BudgetLineRules.duplicateOf(line(9, sub = "Cafe", currency = "BBB"), others))
    }

    @Test fun aLineDoesNotDuplicateItselfAndARuleLineHasNoTwin() {
        assertNull(BudgetLineRules.duplicateOf(line(1), listOf(line(1))))
        assertNull(BudgetLineRules.duplicateOf(line(9, category = null), listOf(line(1, category = null))))
    }
}
