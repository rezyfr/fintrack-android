package com.fidriyanto.banktracker.notification

import java.util.concurrent.TimeUnit

private val MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)
private val MAX_AHEAD_MS = TimeUnit.MINUTES.toMillis(1)

// ac: repost-duplicate - the time a notification is about. An app can post the same notification again, for example
// a chat app that updates its group when another chat gets a message. Android then gives the copy a new post time, but
// the time the notification carries ("when") stays the same. The duplicate check uses that time, so a repost is
// seen as the same payment. A missing time, one far in the past or one in the future falls back to the post time.
fun notificationTime(whenMs: Long, postTimeMs: Long): Long =
    if (whenMs in (postTimeMs - MAX_AGE_MS)..(postTimeMs + MAX_AHEAD_MS)) whenMs else postTimeMs
