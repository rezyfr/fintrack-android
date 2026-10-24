package com.fidriyanto.banktracker.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDaoTest {
    private val db = inMemoryDatabase()
    private val dao = db.accountDao()

    @After fun close() = db.close()

    private fun account(id: String, name: String = id, sortOrder: Int = 0) = AccountEntity(id, name, "USD", sortOrder, isDefault = false)

    @Test fun accountsComeInSortOrderThenByName() = runBlocking {
        dao.upsertAll(listOf(account("C", "Cash", 1), account("B", "Bank", 1), account("A", "Main", 0)))
        assertEquals(listOf("A", "B", "C"), dao.observeAll().first().map { it.id })
    }

    @Test fun anUpsertReplacesTheRowWithTheSameId() = runBlocking {
        dao.upsertAll(listOf(account("A", "Old")))
        dao.upsertAll(listOf(account("A", "New")))
        assertEquals(listOf("New"), dao.observeAll().first().map { it.name })
    }

    @Test fun deleteMissingKeepsOnlyTheListedIds() = runBlocking {
        dao.upsertAll(listOf(account("A"), account("B"), account("C")))
        dao.deleteMissing(listOf("A", "C"))
        assertEquals(listOf("A", "C"), dao.observeAll().first().map { it.id })
    }

    // The server can answer with no accounts. Then the phone must keep none either.
    @Test fun deleteMissingWithAnEmptyListDeletesEveryRow() = runBlocking {
        dao.upsertAll(listOf(account("A"), account("B")))
        dao.deleteMissing(emptyList())
        assertEquals(emptyList<AccountEntity>(), dao.observeAll().first())
    }
}
