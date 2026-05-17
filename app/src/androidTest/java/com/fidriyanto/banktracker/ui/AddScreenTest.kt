package com.fidriyanto.banktracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.fidriyanto.banktracker.fake.FakeSheetsSyncer
import com.fidriyanto.banktracker.ui.add.AddScreen
import com.fidriyanto.banktracker.TestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class AddScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var fakeSheetsSyncer: FakeSheetsSyncer

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeSheetsSyncer.reset()
    }

    private fun fillForm(amount: String = "150", description: String = "Lunch") {
        composeRule.onNode(hasSetTextAction() and hasText("Amount", substring = true))
            .performTextInput(amount)
        composeRule.onNode(hasSetTextAction() and hasText("Description", substring = true))
            .performTextInput(description)
    }

    @Test
    fun submitWithNoAmount_showsError() {
        composeRule.setContent { AddScreen() }

        composeRule.onNodeWithText("Sync to Sheets").performClick()

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Enter a valid amount").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Enter a valid amount").assertIsDisplayed()
    }

    @Test
    fun validEntry_syncSuccess_showsSuccessMessage() {
        fakeSheetsSyncer.shouldSucceed = true
        composeRule.setContent { AddScreen() }

        fillForm()
        composeRule.onNodeWithText("Sync to Sheets").performClick()

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Synced to Sheets!").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Synced to Sheets!").assertIsDisplayed()
    }

    @Test
    fun validEntry_syncFailure_showsOfflineMessage() {
        fakeSheetsSyncer.shouldSucceed = false
        composeRule.setContent { AddScreen() }

        fillForm()
        composeRule.onNodeWithText("Sync to Sheets").performClick()

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Sync failed", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Sync failed — saved offline").assertIsDisplayed()
    }

    @Test
    fun idrChip_selectsIDRAccount() {
        composeRule.setContent { AddScreen() }

        composeRule.onNodeWithText("IDR").performClick()

        composeRule.onNode(hasText("IDR") and isSelected()).assertIsDisplayed()
    }

    @Test
    fun incomeChip_selectsIncomeType() {
        composeRule.setContent { AddScreen() }

        composeRule.onNodeWithText("Income").performClick()

        composeRule.onNode(hasText("Income") and isSelected()).assertIsDisplayed()
    }
}
