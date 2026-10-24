package com.fidriyanto.banktracker.ui.common

import com.fidriyanto.banktracker.ui.navigation.Tab
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTransitionTest {
    // The tracked pages are told apart by the tab route, so the two must stay the same string.
    @Test fun load_keys_are_the_tab_routes() {
        assertEquals(Tab.Home.route, BookLoad.HOME)
        assertEquals(Tab.Budget.route, BookLoad.BUDGET)
    }

    @Test fun a_page_is_busy_until_it_reports_done_and_pages_do_not_affect_each_other() {
        BookLoad.report(BookLoad.HOME, true)
        assertTrue(BookLoad.HOME in BookLoad.busy.value)
        assertFalse(BookLoad.BUDGET in BookLoad.busy.value)
        BookLoad.report(BookLoad.HOME, false)
        assertFalse(BookLoad.HOME in BookLoad.busy.value)
    }

    // The sheet stays away when the page is ready within a moment, so this must answer true at once.
    @Test fun waiting_returns_true_at_once_when_the_page_is_idle() = runBlocking {
        BookLoad.report(BookLoad.BUDGET, false)
        assertTrue(BookLoad.awaitIdle(BookLoad.BUDGET, 1000))
    }

    // A page that gets idle while the sheet is deciding counts as quick.
    @Test fun waiting_returns_true_when_the_page_gets_idle_in_time() = runBlocking {
        BookLoad.report(BookLoad.HOME, true)
        val waiter = async { BookLoad.awaitIdle(BookLoad.HOME, 2000) }
        delay(50)
        BookLoad.report(BookLoad.HOME, false)
        assertTrue(waiter.await())
    }

    // A page that never finishes must not hold the screen for ever.
    @Test fun waiting_gives_up_after_the_timeout() = runBlocking {
        BookLoad.report(BookLoad.HOME, true)
        try {
            val start = System.currentTimeMillis()
            // A false answer is what raises the sheet.
            assertFalse(BookLoad.awaitIdle(BookLoad.HOME, 120))
            assertTrue(System.currentTimeMillis() - start < 2000)
        } finally {
            BookLoad.report(BookLoad.HOME, false)
        }
    }
}
