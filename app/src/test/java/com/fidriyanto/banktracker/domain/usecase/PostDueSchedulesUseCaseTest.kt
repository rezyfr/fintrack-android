package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.AutoAddedRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.data.repository.ScheduleRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AutoSource
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.Schedule
import com.fidriyanto.banktracker.domain.model.ScheduleRepeat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PostDueSchedulesUseCaseTest {
    private val schedules = mockk<ScheduleRepository>(relaxed = true)
    private val accounts = mockk<AccountRepository>()
    private val currency = mockk<CurrencySettingsRepository>()
    private val rules = mockk<RuleRepository>()
    private val transactions = mockk<TransactionRepository>()
    private val installments = mockk<InstallmentRepository>()
    private val autoAdded = mockk<AutoAddedRepository>(relaxed = true)
    private val posted = mutableListOf<TransactionEntry>()

    private fun sched(autoPost: Boolean = true, through: String? = "2026-10-31") = Schedule(
        id = "1", name = "Rent", amount = 100.0, txType = "expense", walletId = "A", category = "Bills",
        repeat = ScheduleRepeat.MONTHLY, every = 1, startDate = "2026-09-01", autoPost = autoPost, postedThrough = through,
    )

    private fun useCase(list: List<Schedule>): PostDueSchedulesUseCase {
        coEvery { schedules.all() } returns list
        every { accounts.observeAll() } returns flowOf(listOf(Account("A", "A", "THB", true)))
        every { currency.observe() } returns flowOf(CurrencySettings())
        every { rules.observe() } returns flowOf(emptyList())
        coEvery { transactions.insertManual(any()) } coAnswers { posted.add(firstArg()); Result.success(Unit) }
        return PostDueSchedulesUseCase(schedules, accounts, currency, rules, transactions, installments, autoAdded)
    }

    @Test fun missedDatesArePostedOnceEach() = runBlocking {
        val count = useCase(listOf(sched()))(LocalDate.of(2026, 12, 3))
        assertEquals(2, count)
        assertEquals(listOf("2026-11-01", "2026-12-01"), posted.map { it.date.toString() })
        coVerify { schedules.markPosted("1", "2026-12-01") }
    }

    @Test fun aScheduleWithAutoPostOffPostsNothing() = runBlocking {
        assertEquals(0, useCase(listOf(sched(autoPost = false)))(LocalDate.of(2026, 12, 3)))
        assertEquals(0, posted.size)
    }

    @Test fun nothingIsPostedBeforeTheDueDate() = runBlocking {
        assertEquals(0, useCase(listOf(sched()))(LocalDate.of(2026, 10, 20)))
    }

    private fun plan(step: Int, status: String = "active") = Installment(
        id = 9, merchant = "Laptop", wallet = "A", installmentAmount = 100.0, totalInstallments = 6, currentStep = step, dueDay = 1, status = status,
    )

    @Test fun aLinkedScheduleCarriesThePlanId() = runBlocking {
        coEvery { installments.getAll() } returns Result.success(listOf(plan(2)))
        useCase(listOf(sched().copy(installmentId = 9)))(LocalDate.of(2026, 11, 3))
        assertEquals(listOf(9L), posted.map { it.installmentId })
    }

    @Test fun aLinkedScheduleStopsWhenThePlanHasNoStepsLeft() = runBlocking {
        coEvery { installments.getAll() } returns Result.success(listOf(plan(5)))
        val count = useCase(listOf(sched().copy(installmentId = 9)))(LocalDate.of(2027, 3, 3))
        assertEquals(1, count)
        coVerify { schedules.markPosted("1", "2027-03-01") }
    }

    @Test fun aFinishedPlanPostsNothing() = runBlocking {
        coEvery { installments.getAll() } returns Result.success(listOf(plan(6, "completed")))
        assertEquals(0, useCase(listOf(sched().copy(installmentId = 9)))(LocalDate.of(2026, 12, 3)))
    }

    @Test fun plansThatCannotBeReadMeanNothingIsPostedOrMarked() = runBlocking {
        coEvery { installments.getAll() } returns Result.failure(Exception("offline"))
        assertEquals(0, useCase(listOf(sched().copy(installmentId = 9)))(LocalDate.of(2026, 12, 3)))
        coVerify(exactly = 0) { schedules.markPosted(any(), any()) }
    }

    @Test fun eachPostedTransactionIsNotedForTheHomeNotice() = runBlocking {
        useCase(listOf(sched()))(LocalDate.of(2026, 12, 3))
        coVerify(exactly = 2) { autoAdded.add("Rent", 100.0, "expense", "A", AutoSource.SCHEDULED) }
    }

    @Test fun nothingIsNotedWhenNothingIsPosted() = runBlocking {
        useCase(listOf(sched(autoPost = false)))(LocalDate.of(2026, 12, 3))
        coVerify(exactly = 0) { autoAdded.add(any(), any(), any(), any(), any()) }
    }
}
