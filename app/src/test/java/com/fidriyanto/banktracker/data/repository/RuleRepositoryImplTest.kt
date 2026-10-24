package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.RuleDataSource
import com.fidriyanto.banktracker.data.datasource.remote.RuleRemoteDataSource
import com.fidriyanto.banktracker.data.model.RuleDocument
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeLocal(var doc: RuleDocument = RuleDocument(emptyList(), 0L), var dirty: Boolean = false) : RuleDataSource {
    private val flow = MutableStateFlow(doc.rules)
    override fun observe(): Flow<List<Rule>> = flow
    override suspend fun document() = doc
    override suspend fun isDirty() = dirty
    override suspend fun save(document: RuleDocument, dirty: Boolean) { doc = document; this.dirty = dirty; flow.value = document.rules }
    override suspend fun markSynced() { dirty = false }
}

private class FakeRemote(var doc: RuleDocument? = null, var fail: Boolean = false) : RuleRemoteDataSource {
    var saves = 0
    override suspend fun get(): RuleDocument? = if (fail) error("offline") else doc
    override suspend fun save(document: RuleDocument) { if (fail) error("offline"); doc = document; saves++ }
}

class RuleRepositoryImplTest {
    private fun rule(id: String) = Rule(id, id, true, RuleConditions(), RuleAction(category = "X"))

    @Test fun an_edit_is_saved_on_the_phone_and_sent() = runBlocking {
        val local = FakeLocal()
        val remote = FakeRemote()
        RuleRepositoryImpl(local, remote).replaceAll(listOf(rule("a")))
        assertEquals(listOf("a"), local.doc.rules.map { it.id })
        assertFalse(local.dirty)
        assertEquals(listOf("a"), remote.doc?.rules?.map { it.id })
        assertTrue(local.doc.updatedAtMs > 0)
    }

    @Test fun a_failed_send_leaves_the_edit_marked_and_refresh_sends_it() = runBlocking {
        val local = FakeLocal()
        val remote = FakeRemote(fail = true)
        val repo = RuleRepositoryImpl(local, remote)
        repo.replaceAll(listOf(rule("a")))
        assertTrue(local.dirty)
        assertTrue(repo.refresh().isFailure)
        remote.fail = false
        assertTrue(repo.refresh().isSuccess)
        assertFalse(local.dirty)
        assertEquals(listOf("a"), remote.doc?.rules?.map { it.id })
    }

    @Test fun a_newer_shared_list_replaces_the_phone_list() = runBlocking {
        val local = FakeLocal(RuleDocument(listOf(rule("old")), 100L))
        val remote = FakeRemote(RuleDocument(listOf(rule("new")), 200L))
        RuleRepositoryImpl(local, remote).refresh()
        assertEquals(listOf("new"), local.doc.rules.map { it.id })
        assertEquals(200L, local.doc.updatedAtMs)
        assertEquals(0, remote.saves)
    }

    @Test fun a_newer_phone_edit_replaces_the_shared_list() = runBlocking {
        val local = FakeLocal(RuleDocument(listOf(rule("mine")), 300L), dirty = true)
        val remote = FakeRemote(RuleDocument(listOf(rule("theirs")), 200L))
        RuleRepositoryImpl(local, remote).refresh()
        assertEquals(listOf("mine"), remote.doc?.rules?.map { it.id })
        assertFalse(local.dirty)
    }

    @Test fun an_older_phone_edit_loses_to_a_newer_shared_list() = runBlocking {
        val local = FakeLocal(RuleDocument(listOf(rule("mine")), 100L), dirty = true)
        val remote = FakeRemote(RuleDocument(listOf(rule("theirs")), 200L))
        RuleRepositoryImpl(local, remote).refresh()
        assertEquals(listOf("theirs"), local.doc.rules.map { it.id })
        assertFalse(local.dirty)
    }

    @Test fun rules_made_before_sync_existed_are_sent_once() = runBlocking {
        val local = FakeLocal(RuleDocument(listOf(rule("early")), 0L))
        val remote = FakeRemote()
        RuleRepositoryImpl(local, remote).refresh()
        assertEquals(listOf("early"), remote.doc?.rules?.map { it.id })
        assertTrue(local.doc.updatedAtMs > 0)
    }

    @Test fun nothing_is_sent_when_both_sides_are_empty_or_equal() = runBlocking {
        val remote = FakeRemote()
        RuleRepositoryImpl(FakeLocal(), remote).refresh()
        assertEquals(0, remote.saves)
        val same = RuleDocument(listOf(rule("a")), 50L)
        val remote2 = FakeRemote(same)
        RuleRepositoryImpl(FakeLocal(same), remote2).refresh()
        assertEquals(0, remote2.saves)
    }

    @Test fun a_refresh_that_cannot_reach_the_server_keeps_the_phone_list() = runBlocking {
        val local = FakeLocal(RuleDocument(listOf(rule("a")), 10L))
        val result = RuleRepositoryImpl(local, FakeRemote(fail = true)).refresh()
        assertTrue(result.isFailure)
        assertEquals(listOf("a"), local.doc.rules.map { it.id })
    }
}
