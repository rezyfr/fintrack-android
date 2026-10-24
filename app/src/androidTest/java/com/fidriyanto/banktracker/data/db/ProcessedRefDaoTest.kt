package com.fidriyanto.banktracker.data.db

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The notification capture saves a payment only when its key is new. The DAO must report a repeat key with -1.
class ProcessedRefDaoTest {
    private val db = inMemoryDatabase()
    private val dao = db.processedRefDao()

    @After fun close() = db.close()

    @Test fun theFirstInsertOfAKeyWinsAndARepeatReturnsMinusOne() = runBlocking {
        assertTrue(dao.insert(ProcessedRefEntity("bank|100|lunch")) > 0)
        assertEquals(-1L, dao.insert(ProcessedRefEntity("bank|100|lunch")))
        assertEquals(1, dao.exists("bank|100|lunch"))
        assertEquals(0, dao.exists("bank|100|dinner"))
    }
}
