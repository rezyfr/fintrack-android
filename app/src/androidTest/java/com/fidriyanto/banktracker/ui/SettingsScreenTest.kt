package com.fidriyanto.banktracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.fidriyanto.banktracker.fake.FakeGoogleAuthManager
import com.fidriyanto.banktracker.ui.settings.SettingsScreen
import com.fidriyanto.banktracker.TestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SettingsScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var fakeGoogleAuthManager: FakeGoogleAuthManager

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeGoogleAuthManager.reset()
    }

    @Test
    fun whenNotSignedIn_showsSignInButton() {
        fakeGoogleAuthManager.signedIn = false
        composeRule.setContent { SettingsScreen() }

        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun whenSignedIn_showsEmailAndSignOutButton() {
        fakeGoogleAuthManager.signedIn = true
        fakeGoogleAuthManager.email = "test@example.com"
        composeRule.setContent { SettingsScreen() }

        composeRule.onNodeWithText("test@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Sign Out").assertIsDisplayed()
    }

    @Test
    fun clickingSignOut_updatesAuthState() {
        fakeGoogleAuthManager.signedIn = true
        fakeGoogleAuthManager.email = "test@example.com"
        composeRule.setContent { SettingsScreen() }

        composeRule.onNodeWithText("Sign Out").performClick()

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Not signed in").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
    }

    @Test
    fun retryPendingSyncs_buttonIsDisplayed() {
        composeRule.setContent { SettingsScreen() }
        composeRule.onNodeWithText("Retry Pending Syncs").assertIsDisplayed()
    }
}
