package com.fidriyanto.banktracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.fidriyanto.banktracker.data.db.TransactionDao
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.fake.FakeSheetsSyncer
import com.fidriyanto.banktracker.ui.feed.FeedScreen
import com.fidriyanto.banktracker.TestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class FeedScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var transactionDao: TransactionDao
    @Inject lateinit var fakeSheetsSyncer: FakeSheetsSyncer

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeSheetsSyncer.reset()
    }

    private fun entity(item: String = "Coffee", status: TransactionStatus = TransactionStatus.PENDING_EDIT) =
        TransactionEntity(
            item = item, amount = 80.0, category = "Food & Drink",
            dateIso = "2026-05-16", referenceNo = "REF001",
            tab = com.fidriyanto.banktracker.data.model.LedgerTab.EXPENSES, status = status
        )

    @Test
    fun emptyFeed_showsEmptyStateMessage() {
        composeRule.setContent { FeedScreen() }
        composeRule.onNodeWithText("No transactions yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun pendingEditCard_tap_expandsEditFields() {
        runBlocking { transactionDao.insert(entity()) }
        composeRule.setContent { FeedScreen() }

        composeRule.onNodeWithText("Coffee").performClick()

        composeRule.onNodeWithText("Confirm & Sync").assertIsDisplayed()
        composeRule.onNodeWithText("Dismiss").assertIsDisplayed()
    }

    @Test
    fun expandedCard_dismiss_collapsesEditFields() {
        runBlocking { transactionDao.insert(entity()) }
        composeRule.setContent { FeedScreen() }

        composeRule.onNodeWithText("Coffee").performClick()
        composeRule.onNodeWithText("Dismiss").performClick()

        composeRule.onNodeWithText("Confirm & Sync").assertDoesNotExist()
    }

    @Test
    fun expandedCard_confirmSync_triggersSyncAndCollapses() {
        runBlocking { transactionDao.insert(entity()) }
        composeRule.setContent { FeedScreen() }

        composeRule.onNodeWithText("Coffee").performClick()
        composeRule.onNodeWithText("Confirm & Sync").performClick()

        composeRule.waitUntil(3_000) { fakeSheetsSyncer.syncCalled }
        composeRule.onNodeWithText("Confirm & Sync").assertDoesNotExist()
    }

    @Test
    fun syncFailedCard_showsRetryButton() {
        runBlocking { transactionDao.insert(entity(status = TransactionStatus.SYNC_FAILED)) }
        composeRule.setContent { FeedScreen() }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun syncedCard_tap_doesNotExpand() {
        runBlocking { transactionDao.insert(entity(status = TransactionStatus.SYNCED)) }
        composeRule.setContent { FeedScreen() }

        composeRule.onNodeWithText("Coffee").performClick()

        composeRule.onNodeWithText("Confirm & Sync").assertDoesNotExist()
    }
}
