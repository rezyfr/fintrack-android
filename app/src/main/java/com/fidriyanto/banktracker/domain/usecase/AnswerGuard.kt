package com.fidriyanto.banktracker.domain.usecase

// Checks the sentence the model wrote against what the tools returned. A small model copies digits badly,
// for example "Rp1,1,850" for "Rp 1,137,850". An amount that no tool returned means the sentence cannot be trusted.
object AnswerGuard {
    private val number = Regex("""\d[\d,]*(?:\.\d+)?""")
    private const val MIN_DIGITS = 3

    // The numbers of three or more digits in a text, without thousands separators.
    fun numbers(text: String): Set<String> =
        number.findAll(text).map { it.value.replace(",", "").trimEnd('.') }.filter { it.count(Char::isDigit) >= MIN_DIGITS }.toSet()

    // True when every number in the reply was in a tool result.
    fun accept(reply: String, results: List<String>): Boolean = (numbers(reply) - numbers(results.joinToString("\n"))).isEmpty()
}
