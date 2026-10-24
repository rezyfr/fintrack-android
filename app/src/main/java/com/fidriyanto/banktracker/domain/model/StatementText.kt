package com.fidriyanto.banktracker.domain.model

// A card statement line split into the plan text and the step the bank printed at its end.
data class StatementText(val text: String, val step: Int? = null, val total: Int? = null)

private val trailingStep = Regex(
    """[\s\-]*(?:KE[\s\-]*(\d{1,3})[\s\-]*(?:DARI|/)[\s\-]*(\d{1,3})|(\d{1,3})\s*/\s*(\d{1,3}))\s*$""",
    RegexOption.IGNORE_CASE,
)

// ac: installment-plan-add - cuts a step such as "005/012" or "ke-30/36" off the end of a statement line.
// A step that is not at the end, or that is above its total, stays in the text.
fun splitStatementStep(item: String): StatementText {
    val clean = item.trim()
    val m = trailingStep.find(clean) ?: return StatementText(clean)
    val step = (m.groupValues[1].ifEmpty { m.groupValues[3] }).toIntOrNull()
    val total = (m.groupValues[2].ifEmpty { m.groupValues[4] }).toIntOrNull()
    if (step == null || total == null || step !in 1..total) return StatementText(clean)
    val text = clean.substring(0, m.range.first).trim()
    return if (text.isEmpty()) StatementText(clean) else StatementText(text, step, total)
}
