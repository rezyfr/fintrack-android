package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CustomBankDataSourceImplTest {
    @get:Rule val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After fun stop() = scope.cancel()

    private fun source(): CustomBankDataSourceImpl {
        val store = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "prefs.preferences_pb") }
        return CustomBankDataSourceImpl(store)
    }

    private fun pattern(id: String, name: String = id) =
        CustomBankPattern(id, name, "title", "Paid {amount} to {merchant}", "A", "expense", true, "Example title", "Paid 5 to Cafe")

    @Test fun startsEmpty() = runBlocking {
        assertTrue(source().all().isEmpty())
    }

    @Test fun savedPatternIsReadBackWithAllFields() = runBlocking {
        val ds = source()
        ds.upsert(pattern("a"))
        assertEquals(listOf(pattern("a")), ds.all())
    }

    @Test fun upsertReplacesThePatternWithTheSameIdAndKeepsTheOrder() = runBlocking {
        val ds = source()
        ds.upsert(pattern("a"))
        ds.upsert(pattern("b"))
        ds.upsert(pattern("a", name = "Renamed").copy(enabled = false))
        assertEquals(listOf("a", "b"), ds.all().map { it.id })
        assertEquals("Renamed", ds.all().first().name)
        assertEquals(false, ds.all().first().enabled)
    }

    @Test fun deleteRemovesOnlyThatPattern() = runBlocking {
        val ds = source()
        ds.upsert(pattern("a"))
        ds.upsert(pattern("b"))
        ds.delete("a")
        assertEquals(listOf("b"), ds.all().map { it.id })
    }
}
