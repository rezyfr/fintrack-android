package com.fidriyanto.banktracker.domain.usecase

// ac: insights-cycle-stepper — the [count] pay cycles that end with the cycle at [offset] (0 is the
// cycle in progress), oldest first.
internal fun trendCycles(offset: Int, count: Int = 6): List<PayCycle.Range> =
    (offset + count - 1 downTo offset).map { PayCycle.cycleRange(it) }
