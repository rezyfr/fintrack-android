package com.fidriyanto.banktracker.domain.model

// ac: home-cycle-spending-comparison — one plotted day: index in the cycle (0 = the 26th), its ISO
// date, and cumulative combined spend (IDR + THB->IDR) up to that day.
data class SpendPoint(val dayIndex: Int, val dateIso: String, val cumulative: Double)

// ac: home-cycle-spending-comparison — chart data. points: this cycle day 0..today. lastPoints: the
// previous cycle's full cumulative curve, same day-of-cycle index. maxDay: the longer cycle's last day.
data class SpendCompare(
    val startIso: String,
    val cycleDays: Int,
    val maxDay: Int,
    val todayIndex: Int,
    val points: List<SpendPoint>,
    val lastPoints: List<SpendPoint>,
    // ac: spend-compare-more-cycles — the curves of the cycles before the previous one: index 0 is two
    // cycles back, index 1 is three cycles back. A cycle with no spend at all is left out.
    val older: List<List<SpendPoint>> = emptyList(),
)
