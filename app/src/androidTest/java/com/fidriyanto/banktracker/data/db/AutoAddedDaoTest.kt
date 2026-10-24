package com.fidriyanto.banktracker.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoAddedDaoTest {
    private val db = inMemoryDatabase()
    private val dao = db.autoAddedDao()

    @After fun close() = db.close()

    private fun row(item: String, at: Long) = AutoAddedEntity(addedAtMs = at, item = item, amount = 1.0, txType = "expense", wallet = "A", source = "schedule")

    @Test fun theNewestRowComesFirstAndDismissHidesAll() = runBlocking {
        dao.insert(row("old", 1))
        dao.insert(row("new", 2))
        assertEquals(listOf("new", "old"), dao.observeActive().first().map { it.item })
        dao.dismissAll()
        assertEquals(0, dao.observeActive().first().size)
    }

    @Test fun pruneDeletesRowsOlderThanTheCutoff() = runBlocking {
        dao.insert(row("old", 1))
        dao.insert(row("kept", 10))
        dao.prune(cutoffMs = 5)
        assertEquals(listOf("kept"), dao.observeActive().first().map { it.item })
    }
}
