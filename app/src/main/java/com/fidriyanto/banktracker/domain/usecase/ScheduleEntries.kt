package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.domain.model.RuleOutcome
import com.fidriyanto.banktracker.domain.model.Schedule
import java.time.LocalDate

// ac: scheduled-transactions - the transaction a schedule adds on [date], with the result of the custom rules applied
internal fun scheduleEntry(s: Schedule, date: LocalDate, out: RuleOutcome, tab: LedgerTab) = TransactionEntry(
    tab = tab, date = date, item = out.description ?: s.name, amount = s.amount,
    category = out.category ?: s.category, wallet = s.walletId, txType = s.txType,
    toWallet = if (s.txType == "transfer") s.toWalletId else null,
    subcategory = if (out.category != null) out.subcategory else s.subcategory,
    // ac: schedule-installment-link - the link is kept, so the plan moves up one step when the transaction is saved
    installmentId = s.installmentId,
)
