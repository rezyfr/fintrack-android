package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.ledgerTabFor
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.AutoAddedRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.data.repository.ScheduleRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.AutoSource
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.ScheduleDates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

private const val MAX_PER_RUN = 62

// ac: scheduled-transactions - adds every transaction of an auto-post schedule that is due, including the
// dates missed while the phone was off. It returns how many it added. The custom rules run on each one.
class PostDueSchedulesUseCase @Inject constructor(
    private val schedules: ScheduleRepository,
    private val accounts: AccountRepository,
    private val currency: CurrencySettingsRepository,
    private val rules: RuleRepository,
    private val transactions: TransactionRepository,
    private val installments: InstallmentRepository,
    private val autoAdded: AutoAddedRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Int = lock.withLock {
        val wallets = accounts.observeAll().first()
        val homeCode = currency.observe().first().homeCode
        val rulesNow = rules.observe().first()
        var posted = 0
        val auto = schedules.all().filter { it.autoPost }
        // ac: schedule-installment-link - the steps each active plan has left. Null means the plans could not be read.
        val stepsLeft: MutableMap<Long, Int>? = if (auto.none { it.installmentId != null }) HashMap() else
            installments.getAll().getOrNull()?.filter { !it.isCompleted }?.associate { it.id to it.remaining }?.toMutableMap()
        for (s in auto) {
            val currencyOfWallet = wallets.firstOrNull { it.id == s.walletId }?.currency ?: continue
            // Plans that cannot be read are tried again at the next run. Nothing is skipped or marked.
            if (s.installmentId != null && stepsLeft == null) continue
            for (date in ScheduleDates.due(s, today).take(MAX_PER_RUN)) {
                // A plan that is finished or gone posts nothing more, so a schedule never goes past its last step.
                val planId = s.installmentId
                if (planId != null && (stepsLeft?.get(planId) ?: 0) <= 0) {
                    schedules.markPosted(s.id, date.toString())
                    continue
                }
                if (planId != null) stepsLeft?.put(planId, (stepsLeft[planId] ?: 1) - 1)
                val out = RuleEngine.apply(rulesNow, RuleInput(s.name, s.amount, s.txType, s.walletId, LocalDateTime.of(date, LocalTime.NOON)))
                if (!out.ignore) {
                    val entry = scheduleEntry(s, date, out, ledgerTabFor(currencyOfWallet, homeCode, s.txType == "income"))
                    transactions.insertManual(entry)
                    // ac: auto-added-notice - Home tells the user about the transaction until they dismiss it
                    autoAdded.add(entry.item, entry.amount, entry.txType, entry.wallet, AutoSource.SCHEDULED)
                    posted++
                }
                // Marked after each date, so a stop in the middle never posts a date twice.
                schedules.markPosted(s.id, date.toString())
            }
        }
        posted
    }

    private companion object {
        val lock = Mutex()
    }
}
