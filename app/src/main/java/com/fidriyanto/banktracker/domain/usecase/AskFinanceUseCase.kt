package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.LlmRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.domain.model.currenciesInUse
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

private const val MAX_HISTORY = 6
private const val CONTEXT_CHARS = 160

class AskFinanceUseCase @Inject constructor(
    private val llm: LlmRepository,
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val currencySettings: CurrencySettingsRepository,
    private val budgetLines: BudgetLineRepository,
    private val walletBalances: WalletBalanceRepository,
    private val categories: CategoryRepository,
) {
    // The on-device model picks a tool and a period word. The tool counts the rows, so the amounts are exact.
    suspend operator fun invoke(question: String, history: List<AskMessage>): Result<AskMessage> {
        val today = LocalDate.now()
        val settings = currencySettings.observe().first()
        PayCycle.startDay = settings.cycleStartDay
        val cats = categories.observeAll().first()
        val box = FinanceToolBox(transactions, accounts, currencySettings, budgetLines, walletBalances, today, QuestionHints.limit(question))
        val system = AiPrompts.ask(today, PayCycle.current(today), currenciesInUse(settings.homeCode, accounts.observeAll().first()), cats, history.lastOrNull { !it.fromUser }?.context)
        return llm.generate(system, question.trim(), history.takeLast(MAX_HISTORY), box.tools()).mapCatching { reply ->
            val results = box.results.joinToString("\n")
            val tables = box.tables.toList()
            // The last call and the first line of what it found: the category and the dates a follow-up can reuse.
            val context = box.calls.lastOrNull()?.let { call -> "$call, which found: ${box.results.lastOrNull()?.lines()?.firstOrNull().orEmpty().take(CONTEXT_CHARS)}" }
            when {
                // A model that calls a tool may end with no text. The tables, or the tool's sentence, are then the answer.
                reply.isBlank() -> if (tables.isNotEmpty()) AskMessage(false, "", tables = tables, context = context) else AskMessage(false, box.results.lastOrNull().orEmpty(), results.ifEmpty { null }, context = context)
                // Numbers without a tool cannot be checked.
                box.results.isEmpty() && AnswerGuard.numbers(reply).isNotEmpty() -> error("The model gave numbers without a tool")
                // The model copied a number wrongly. What the tools found is exact, so that is the answer.
                !AnswerGuard.accept(reply, box.results) -> if (tables.isNotEmpty()) AskMessage(false, "", tables = tables, context = context) else AskMessage(false, results, context = context)
                else -> AskMessage(false, reply, source = results.takeIf { tables.isEmpty() && it.isNotEmpty() }, tables = tables, context = context)
            }
        }
    }
}
