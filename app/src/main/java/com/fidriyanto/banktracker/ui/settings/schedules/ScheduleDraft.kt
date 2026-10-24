package com.fidriyanto.banktracker.ui.settings.schedules

import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.Schedule
import com.fidriyanto.banktracker.domain.model.TRANSFER_CATEGORY
import com.fidriyanto.banktracker.domain.model.ScheduleDates
import com.fidriyanto.banktracker.domain.model.ScheduleRepeat
import java.time.LocalDate

enum class WhatError { NAME, AMOUNT, ACCOUNT, TO_ACCOUNT, CATEGORY }

// ac: scheduled-transactions - the idea the user starts from. It only sets the type of transaction.
enum class ScheduleTemplate(val txType: String) { BILLS("expense"), SUBSCRIPTION("expense"), INSTALLMENT("expense"), INCOME("income"), TRANSFER("transfer"), OTHER("expense") }

// What the editor holds while the user fills a schedule in. The amount is text so a half-typed number is kept.
data class ScheduleDraft(
    val id: String? = null,
    val name: String = "",
    val amount: String = "",
    val txType: String = "expense",
    val walletId: String = "",
    val toWalletId: String? = null,
    val category: String = "",
    val subcategory: String? = null,
    val subOn: Boolean = false,
    val repeat: ScheduleRepeat = ScheduleRepeat.MONTHLY,
    val every: Int = 1,
    val startDate: LocalDate = LocalDate.now(),
    val endOn: Boolean = false,
    val endDate: LocalDate = LocalDate.now().plusMonths(12),
    val autoPost: Boolean = false,
    // ac: schedule-installment-link - the plan each posted transaction pays a step of
    val installmentId: Long? = null,
) {
    val amountValue: Double? get() = amount.replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }

    fun whatError(): WhatError? = when {
        amountValue == null -> WhatError.AMOUNT
        walletId.isBlank() -> WhatError.ACCOUNT
        txType == "transfer" && (toWalletId.isNullOrBlank() || toWalletId == walletId) -> WhatError.TO_ACCOUNT
        txType != "transfer" && category.isBlank() -> WhatError.CATEGORY
        else -> null
    }

    fun endError(): Boolean = endOn && endDate.isBefore(startDate)

    // The name is the description in the feed. A blank name falls back to the category.
    fun toSchedule(newId: String) = Schedule(
        id = id ?: newId,
        name = name.trim().ifBlank { category.ifBlank { walletId } },
        amount = amountValue ?: 0.0,
        txType = txType,
        walletId = walletId,
        toWalletId = toWalletId.takeIf { txType == "transfer" },
        category = if (txType == "transfer") TRANSFER_CATEGORY else category,
        subcategory = subcategory.takeIf { subOn && txType != "transfer" },
        repeat = repeat,
        every = every,
        startDate = startDate.toString(),
        endDate = endDate.toString().takeIf { endOn },
        autoPost = autoPost,
        installmentId = installmentId.takeIf { txType == "expense" },
    )

    // ac: schedule-installment-link - picking a plan fills in the rest. It is monthly on the plan's due day, from its
    // next due date to its last step, so the schedule ends when the plan does. A name the user typed stays.
    fun withPlan(plan: Installment): ScheduleDraft = copy(
        installmentId = plan.id, walletId = plan.wallet, txType = "expense", toWalletId = null,
        amount = plan.installmentAmount.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() },
        name = name.ifBlank { plan.merchant }, repeat = ScheduleRepeat.MONTHLY, every = plan.dueDay,
        startDate = plan.nextDueDate(), endOn = true, endDate = plan.estimatedFinish(),
    )

    // The next dates the schedule falls on, for the preview
    fun nextDates(count: Int): List<LocalDate> =
        ScheduleDates.between(toSchedule("preview"), startDate, startDate.plusYears(2)).take(count)

    companion object {
        fun from(s: Schedule) = ScheduleDraft(
            id = s.id, name = s.name, amount = s.amount.toString().removeSuffix(".0"), txType = s.txType,
            walletId = s.walletId, toWalletId = s.toWalletId, category = s.category, subcategory = s.subcategory,
            subOn = s.subcategory != null, repeat = s.repeat, every = s.every, startDate = LocalDate.parse(s.startDate),
            endOn = s.endDate != null, endDate = s.endDate?.let(LocalDate::parse) ?: LocalDate.now().plusMonths(12),
            autoPost = s.autoPost, installmentId = s.installmentId,
        )

        fun from(template: ScheduleTemplate) = ScheduleDraft(txType = template.txType)
    }
}
