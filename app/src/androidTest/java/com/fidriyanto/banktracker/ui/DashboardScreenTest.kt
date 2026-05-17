package com.fidriyanto.banktracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.fidriyanto.banktracker.TestActivity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewDao
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.fake.FakeGoogleAuthManager
import com.fidriyanto.banktracker.fake.FakeMonthlyOverviewFetcher
import com.fidriyanto.banktracker.ui.dashboard.DashboardScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DashboardScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var fakeGoogleAuthManager: FakeGoogleAuthManager
    @Inject lateinit var fakeFetcher: FakeMonthlyOverviewFetcher
    @Inject lateinit var dao: MonthlyOverviewDao

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeGoogleAuthManager.reset()
        fakeFetcher.reset()
    }

    private fun currentMonthLabel(): String {
        val now = java.time.LocalDate.now()
        val name = now.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        return "$name ${now.year}"
    }

    private fun thbEntity(month: String) = MonthlyOverviewEntity(
        month = month, currency = "THB",
        bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
        foodDrink = 12400.0, groceries = 0.0, healthWellbeing = 0.0,
        other = 0.0, shopping = 0.0, transport = 6800.0,
        travel = 0.0, business = 0.0, gifts = 0.0,
        totalExpenditure = 19200.0, income = 45200.0, grossSavings = 26000.0
    )

    private fun idrEntity(month: String) = MonthlyOverviewEntity(
        month = month, currency = "IDR",
        bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
        foodDrink = 500000.0, groceries = 0.0, healthWellbeing = 0.0,
        other = 0.0, shopping = 0.0, transport = 0.0,
        travel = 0.0, business = 0.0, gifts = 0.0,
        totalExpenditure = 500000.0, income = 2000000.0, grossSavings = 1500000.0
    )

    @Test
    fun whenNotSignedIn_showsSignInPrompt() {
        fakeGoogleAuthManager.signedIn = false
        composeRule.setContent { DashboardScreen() }

        composeRule.onNodeWithText("Sign in with Google to load your dashboard").assertIsDisplayed()
    }

    @Test
    fun whenSignedIn_noCache_doesNotShowCurrencySection() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        composeRule.setContent { DashboardScreen() }

        composeRule.onNodeWithText("── THB ──────────────────────────").assertDoesNotExist()
    }

    @Test
    fun whenCacheHasData_showsTHBAndIDRSections() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val month = currentMonthLabel()
        runBlocking { dao.upsertAll(listOf(thbEntity(month), idrEntity(month))) }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("── THB ──────────────────────────")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("── THB ──────────────────────────").assertIsDisplayed()
        composeRule.onNodeWithText("── IDR ──────────────────────────").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsBalanceCardLabels() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val month = currentMonthLabel()
        runBlocking { dao.upsertAll(listOf(thbEntity(month))) }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Income").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Income").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Expenses").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Net").onFirst().assertIsDisplayed()
    }

    @Test
    fun periodChips_areDisplayed() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val month = currentMonthLabel()
        runBlocking { dao.upsertAll(listOf(thbEntity(month))) }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("This Month").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("This Month").assertIsDisplayed()
        composeRule.onNodeWithText("Last Month").assertIsDisplayed()
        composeRule.onNodeWithText("Last 3 Months").assertIsDisplayed()
    }

    @Test
    fun noSpending_showsNoSpendingRecorded() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val month = currentMonthLabel()
        runBlocking {
            dao.upsertAll(listOf(
                MonthlyOverviewEntity(
                    month = month, currency = "THB",
                    bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
                    foodDrink = 0.0, groceries = 0.0, healthWellbeing = 0.0,
                    other = 0.0, shopping = 0.0, transport = 0.0,
                    travel = 0.0, business = 0.0, gifts = 0.0,
                    totalExpenditure = 0.0, income = 0.0, grossSavings = 0.0
                )
            ))
        }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("No spending recorded").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("No spending recorded").onFirst().assertIsDisplayed()
    }
}
