package com.fidriyanto.banktracker.domain.model

// The installment a card transaction belongs to, with the step and status it moves to.
data class InstallmentMatch(val installment: Installment, val newStep: Int, val newStatus: String)
