package com.fidriyanto.banktracker.notification

import java.time.LocalDate

// ac: catch-up-on-connect - the keys that say "this notification was already saved". The key names the shop text, the
// amount, the date and a time. A notification can be known under two times: the time it carries ("when") and the time
// Android posted it. Older versions of the app keyed on the post time, so both are used. A notification that matches
// any of them is a copy.
fun notificationKeys(item: String, amount: Double, date: LocalDate, vararg times: Long?): List<String> =
    times.filterNotNull().distinct().map { "$item|$amount|$date|$it" }
