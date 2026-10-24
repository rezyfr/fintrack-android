package com.fidriyanto.banktracker.data.model

import com.fidriyanto.banktracker.domain.model.Rule

// The whole rule list and the time of its last edit. Two copies of the list are compared by that time.
data class RuleDocument(val rules: List<Rule>, val updatedAtMs: Long)
