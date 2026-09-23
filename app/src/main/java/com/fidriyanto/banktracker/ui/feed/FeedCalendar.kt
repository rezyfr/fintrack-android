package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.CalendarGrid
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private fun compact(currency: String, a: Double): String {
    val sym = if (currency == "THB") "฿" else "Rp"
    val v = abs(a)
    return when {
        v >= 1_000_000 -> "$sym${(a / 1_000_000).let { if (abs(it) >= 10) it.roundToLong().toString() else String.format(Locale.US, "%.1f", it) }}M"
        v >= 1_000 -> "$sym${(a / 1_000).roundToLong()}k"
        v > 0 -> "$sym${a.roundToLong()}"
        else -> ""
    }
}

private fun full(currency: String, a: Double): String {
    val sym = if (currency == "THB") "฿" else "Rp "
    return "$sym${NumberFormat.getNumberInstance(Locale.US).format(abs(a).roundToLong())}"
}

private fun dayLabel(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
fun FeedCalendar(items: List<TransactionUiModel>) {
    val app = LocalAppColors.current
    var currency by remember { mutableStateOf("IDR") }
    var selected by remember { mutableStateOf<String?>(null) }

    val cycle = remember { PayCycle.current() }
    val weeks = remember { CalendarGrid.weeksForCycle(cycle) }
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    val isThbCur = currency == "THB"
    fun inCurrency(t: TransactionUiModel) = (t.wallet == "BBL") == isThbCur

    val expenseByDay = remember(items, currency) {
        items.filter { it.txType == "expense" && inCurrency(it) }
            .groupBy { it.dateIso }
            .mapValues { e -> e.value.sumOf { it.amount } }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // currency toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("IDR", "THB").forEach { cur ->
                val on = currency == cur
                Surface(
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        if (cur == "IDR") "Rp" else "฿",
                        color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { currency = cur; selected = null }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Text(stringResource(R.string.feed_cycle_range, dayLabel(cycle.fromIso), dayLabel(cycle.toIso)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

        // weekday header
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { w -> Text(w, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(4.dp))

        // ac: transactions-list-calendar-toggle — weekday-aligned grid with per-day spending totals
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    DayCell(cell, currency, expenseByDay[cell.iso], selected == cell.iso, cell.iso == today, app.red,
                        onClick = { if (cell.inCycle) selected = if (selected == cell.iso) null else cell.iso },
                        modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // ac: transactions-list-calendar-toggle — selecting a day lists that day's transactions
        selected?.let { day ->
            Spacer(Modifier.height(8.dp))
            val rows = items.filter { it.dateIso == day && inCurrency(it) && (it.txType == "expense" || it.txType == "income") }
                .sortedByDescending { it.amount }
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(dayLabel(day), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(14.dp))
                    if (rows.isEmpty()) {
                        Text(stringResource(R.string.feed_calendar_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp))
                    } else rows.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.item, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            val inc = r.txType == "income"
                            Text((if (inc) "+" else "-") + full(currency, r.amount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (inc) app.green else app.red)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayCell(cell: CalendarGrid.Cell, currency: String, expense: Double?, selectedDay: Boolean, isToday: Boolean, red: Color, onClick: () -> Unit, modifier: Modifier) {
    val dayNum = cell.iso.substringAfterLast('-').toInt()
    Surface(
        color = when {
            !cell.inCycle -> Color.Transparent
            selectedDay -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = if (cell.inCycle && !selectedDay) 1.dp else 0.dp,
        modifier = modifier.height(56.dp)
            .then(if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            .clip(RoundedCornerShape(8.dp)).clickable(enabled = cell.inCycle, onClick = onClick),
    ) {
        Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (cell.inCycle) dayNum.toString() else "",
                fontSize = 11.sp,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (cell.inCycle && expense != null && expense > 0) {
                Text(compact(currency, expense), fontSize = 9.sp, color = red, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
