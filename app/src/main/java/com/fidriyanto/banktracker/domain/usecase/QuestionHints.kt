package com.fidriyanto.banktracker.domain.usecase

// Facts the code can read from the question, so the model does not have to pass them on.
object QuestionHints {
    private val before = Regex("""\b(?:top|biggest|largest|highest|most expensive|costliest)\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val after = Regex("""\b(\d{1,2})\s+(?:biggest|largest|highest|most expensive|costliest|top)\b""", RegexOption.IGNORE_CASE)

    // "my 3 biggest expenses" and "top 5 shops" both give the number. No number gives null.
    fun limit(question: String): Int? =
        (after.find(question) ?: before.find(question))?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
}
