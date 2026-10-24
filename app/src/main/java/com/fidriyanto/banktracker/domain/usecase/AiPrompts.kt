package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.Category

// The instructions sent to the on-device model. The category list is the user's own, read at run time.
object AiPrompts {
    fun categoryList(categories: List<Category>): String = categories.joinToString("; ") {
        if (it.subcategories.isEmpty()) it.name else "${it.name} (${it.subcategories.joinToString(", ")})"
    }

    fun notification(categories: List<Category>): String =
        "You read one bank notification. Reply with one line only: type|amount|currency|merchant|category|subcategory. " +
            // ac: ai-amount-copied-verbatim — the model copies the amount and AiReplyParser.parseAmount
            // normalises it. Asking a small model to drop separators made it guess which mark was the
            // decimal one, and an Indonesian amount, IDR134.199,00, came back as 134. parseAmount
            // already reads both conventions and is tested on them.
            "type is expense, income or none. amount is copied exactly as the notification writes it, keeping its separators. currency is a 3 letter code. " +
            "merchant is the shop or the person. Money the user receives is income. Money the user sends out by transfer is none. category and subcategory come from this list, and subcategory may be empty: " +
            "${categoryList(categories)}. For one-time codes, ads and anything that is not a payment reply none|0|none|||."

    fun suggestion(categories: List<Category>): String =
        "You file one personal expense. Reply with one line only: category|subcategory. " +
            "Use only this list, and leave subcategory empty when none fits: ${categoryList(categories)}."

    fun ask(today: java.time.LocalDate, cycle: PayCycle.Range, currencies: List<String>, categories: List<Category> = emptyList(), previous: String? = null): String =
        "You answer questions about the user's own spending. Today is $today, a ${today.dayOfWeek}. " +
            "The pay cycle starts on day ${PayCycle.startDay} of each month. The cycle in progress is ${cycle.fromIso} to ${cycle.toIso}. " +
            "The currencies are ${currencies.joinToString(" and ")}. Call a tool for every number. Pass the period exactly as the user said it, or use one of the period words. Never write dates yourself. " +
            "Example: for 'Food in the first week of August 2026' call spending_total with category Food and period 'first week of August 2026'. If the question names a kind of spending, pass it as category. " +
            "Use largest_expenses for the biggest single expenses, spending_by_category for categories, top_merchants for shops, and find_transactions to list transactions. Break it down, list them and show the transactions mean find_transactions with the category and period of the last answer. Never ask the user a question. If the meaning is unclear, pick the most likely one and call the tool. " +
            "Answer in the language of the question, in one or two short sentences, and copy amounts exactly from the tool. " +
            "If the question is not about the user's money, say you can only answer questions about their spending." +
            (if (categories.isEmpty()) "" else " The categories, with their subcategories in brackets, are: ${categoryList(categories)}.") +
            (if (previous == null) "" else " The last answer came from: $previous. For a follow-up such as break it down, and last month, or what about food, keep its category and period unless the user names new ones.")
}
