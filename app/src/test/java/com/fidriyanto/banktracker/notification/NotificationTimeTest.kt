package com.fidriyanto.banktracker.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class NotificationTimeTest {
    private val post = 1_800_000_000_000L

    @Test fun theNotificationTimeIsUsedWhenItIsSensible() {
        val sent = post - TimeUnit.MINUTES.toMillis(5)
        assertEquals(sent, notificationTime(sent, post))
    }

    @Test fun aRepostWithANewPostTimeKeepsTheSameTime() {
        val sent = post - TimeUnit.MINUTES.toMillis(20)
        val firstPost = post
        val repost = post + TimeUnit.MINUTES.toMillis(30)
        assertEquals(notificationTime(sent, firstPost), notificationTime(sent, repost))
    }

    @Test fun aMissingTimeFallsBackToThePostTime() {
        assertEquals(post, notificationTime(0L, post))
    }

    @Test fun aTimeFarInThePastFallsBackToThePostTime() {
        assertEquals(post, notificationTime(post - TimeUnit.DAYS.toMillis(30), post))
    }

    @Test fun aTimeInTheFutureFallsBackToThePostTime() {
        assertEquals(post, notificationTime(post + TimeUnit.HOURS.toMillis(3), post))
    }

    @Test fun aLateMessageFromAFewDaysAgoKeepsItsOwnTime() {
        val sent = post - TimeUnit.DAYS.toMillis(2)
        assertEquals(sent, notificationTime(sent, post))
    }
}
