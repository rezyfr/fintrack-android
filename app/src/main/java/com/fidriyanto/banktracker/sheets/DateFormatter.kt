package com.fidriyanto.banktracker.sheets

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    private val emailPattern = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    private val incomePattern = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun toExpensesDate(date: LocalDate): String =
        "${date.dayOfMonth}/${date.monthValue}/${date.year}"

    fun toIncomeDate(date: LocalDate): String =
        date.format(incomePattern)

    fun parseEmailDate(raw: String): LocalDate? = try {
        val datePart = raw.substringBefore(" at").trim()
        LocalDate.parse(datePart, emailPattern)
    } catch (e: Exception) { null }
}
