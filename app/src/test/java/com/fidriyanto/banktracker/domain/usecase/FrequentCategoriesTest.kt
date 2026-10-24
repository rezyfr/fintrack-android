package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FrequentCategoriesTest {
    private val names = listOf("Bills", "Food", "Transport", "Other")

    @Test fun mostUsedComesFirst() {
        val uses = listOf("Food" to "2026-10-01", "Food" to "2026-10-02", "Bills" to "2026-10-03")
        assertEquals(listOf("Food", "Bills"), rankCategories(uses, names, 6))
    }

    @Test fun tieGoesToTheMostRecentUse() {
        val uses = listOf("Bills" to "2026-10-01", "Food" to "2026-10-05", "Transport" to "2026-10-03")
        assertEquals(listOf("Food", "Transport", "Bills"), rankCategories(uses, names, 6))
    }

    @Test fun tieOnTheSameDayGoesToTheOrderOfNames() {
        val uses = listOf("Transport" to "2026-10-05", "Food" to "2026-10-05")
        assertEquals(listOf("Food", "Transport"), rankCategories(uses, names, 6))
    }

    @Test fun dateWithATimeIsComparedByItsDay() {
        val uses = listOf("Bills" to "2026-10-05T08:00:00", "Food" to "2026-10-04T23:00:00")
        assertEquals(listOf("Bills", "Food"), rankCategories(uses, names, 6))
    }

    @Test fun emptyHistoryGivesNothing() {
        assertEquals(emptyList<String>(), rankCategories(emptyList(), names, 6))
    }

    @Test fun unknownCategoriesAreIgnored() {
        val uses = listOf("Gone" to "2026-10-01", "Gone" to "2026-10-02", "Bills" to "2026-10-03")
        assertEquals(listOf("Bills"), rankCategories(uses, names, 6))
    }

    @Test fun limitCutsTheResult() {
        val uses = listOf("Food" to "2026-10-01", "Food" to "2026-10-02", "Bills" to "2026-10-03", "Other" to "2026-10-04")
        assertEquals(listOf("Food", "Other"), rankCategories(uses, names, 2))
        assertEquals(emptyList<String>(), rankCategories(uses, names, 0))
    }

    private val today = LocalDate.of(2026, 10, 18)
    private val repo = mockk<TransactionRepository>()
    private val useCase = GetFrequentCategoriesUseCase(repo)

    private fun row(category: String, date: String) = TransactionUiModel(
        id = 1, item = "x", category = category, amount = 1.0, dateIso = date,
        wallet = "A", txType = "expense", status = TransactionStatus.SYNCED,
    )

    @Test fun useCaseReadsTheLast90DaysOfOneType() = runBlocking {
        coEvery { repo.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Result.success(listOf(row("Food", "2026-10-01"), row("Food", "2026-10-02"), row("Bills", "2026-10-03")))
        val result = useCase("expense", names, today = today)
        assertEquals(listOf("Food", "Bills"), result)
        coVerify { repo.fetch(null, null, "expense", null, null, null, null, "2026-07-20", "2026-10-18", null) }
    }

    @Test fun useCaseGivesAnEmptyListWhenTheFetchFails() = runBlocking {
        coEvery { repo.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(RuntimeException("offline"))
        assertEquals(emptyList<String>(), useCase("expense", names, today = today))
    }

    @Test fun byTypeRanksEachTypeOnItsOwnNames() = runBlocking {
        coEvery { repo.fetch(any(), any(), "income", any(), any(), any(), any(), any(), any(), any()) } returns Result.success(listOf(row("Salary", "2026-10-01")))
        coEvery { repo.fetch(any(), any(), "expense", any(), any(), any(), any(), any(), any(), any()) } returns Result.success(listOf(row("Food", "2026-10-01")))
        val result = useCase.byType(mapOf("expense" to names, "income" to listOf("Salary", "Other")))
        assertEquals(mapOf("expense" to listOf("Food"), "income" to listOf("Salary")), result)
    }
}
