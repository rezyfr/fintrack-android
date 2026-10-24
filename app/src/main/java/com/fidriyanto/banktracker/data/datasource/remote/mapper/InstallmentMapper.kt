package com.fidriyanto.banktracker.data.datasource.remote.mapper

import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import com.fidriyanto.banktracker.domain.model.Installment

fun InstallmentDto.toDomain() = Installment(
    id = id,
    merchant = merchant,
    merchantPattern = merchantPattern,
    wallet = wallet,
    installmentAmount = installmentAmount,
    totalInstallments = totalInstallments,
    currentStep = currentStep,
    dueDay = dueDay,
    status = status,
    excluded = excluded,
)
