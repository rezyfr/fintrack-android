package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.data.datasource.AccountLocalDataSource
import com.fidriyanto.banktracker.data.db.AppDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

// The tests use the app's real Hilt modules. A fake module that copies AppModule by hand goes stale when
// AppModule changes, so add a @TestInstallIn module only for the one binding a test must replace.
@HiltAndroidTest
class HiltGraphTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var db: AppDatabase
    @Inject lateinit var accounts: AccountLocalDataSource

    @Before fun inject() = hiltRule.inject()

    @Test fun theRealGraphGivesOneOpenDatabase() = runBlocking {
        accounts.observeAll().first()
        assertTrue(db.isOpen)
    }
}
