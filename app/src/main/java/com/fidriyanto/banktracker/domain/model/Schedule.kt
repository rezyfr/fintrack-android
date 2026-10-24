package com.fidriyanto.banktracker.domain.model

enum class ScheduleRepeat { MONTHLY, WEEKLY, EVERY_N_DAYS }

// ac: scheduled-transactions - a transaction the app adds on a set day. [every] depends on [repeat]:
// the day of the month for MONTHLY, the weekday (1 is Monday, 7 is Sunday) for WEEKLY, and the number of
// days between two dates for EVERY_N_DAYS. [postedThrough] is the last date the app has handled. Nothing
// on or before it is posted again, so a schedule that was off does not post its past dates when it is turned on.
data class Schedule(
    val id: String,
    val name: String,
    val amount: Double,
    val txType: String,
    val walletId: String,
    val toWalletId: String? = null,
    val category: String,
    val subcategory: String? = null,
    val repeat: ScheduleRepeat,
    val every: Int,
    val startDate: String,
    val endDate: String? = null,
    val autoPost: Boolean = false,
    val postedThrough: String? = null,
    // ac: schedule-installment-link - the installment plan each posted transaction pays a step of. Null means none.
    val installmentId: Long? = null,
)
