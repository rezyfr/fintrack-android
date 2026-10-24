package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AutoAddedRepository
import com.fidriyanto.banktracker.domain.model.AutoSource
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AutoAddedUseCasesTest {
    private val repo = mockk<AutoAddedRepository>(relaxed = true)

    @Test fun recordingNotesTheTransactionAndRemovesOldNotesFirst() = runBlocking {
        RecordAutoAddedUseCase(repo)("Cafe", 4.0, "expense", "A", AutoSource.NOTIFICATION)
        coVerifyOrder {
            repo.prune(any())
            repo.add("Cafe", 4.0, "expense", "A", AutoSource.NOTIFICATION)
        }
    }

    @Test fun dismissingClearsEveryNote() = runBlocking {
        ManageAutoAddedUseCase(repo).dismissAll()
        coVerify { repo.dismissAll() }
    }
}
