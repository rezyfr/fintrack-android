package com.fidriyanto.banktracker.ui.feed

import com.fidriyanto.banktracker.domain.model.currenciesInUse
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import com.fidriyanto.banktracker.domain.model.formatMoney
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.domain.usecase.CalendarGrid
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

private val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// ac: configurable-currency-pair — the symbol comes from a currency-code lookup instead of a
// hardcoded "THB"/else branch
private fun compact(currency: String, a: Double): String {
    val sym = currencySymbol(currency)
    val v = abs(a)
    return when {
        v >= 1_000_000 -> "$sym${(a / 1_000_000).let { if (abs(it) >= 10) it.roundToLong().toString() else String.format(Locale.US, "%.1f", it) }}M"
        v >= 1_000 -> "$sym${(a / 1_000).roundToLong()}k"
        v > 0 -> "$sym${a.roundToLong()}"
        else -> ""
    }
}

private fun dayLabel(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
// ac: currency-books — a book fixes the currency through fixedCurrency and hides the currency toggle.
// COMBINED passes null and keeps the toggle.
fun FeedCalendar(items: List<TransactionUiModel>, fixedCurrency: String? = null) {
    val app = LocalAppColors.current
    val currencySettings = CurrencySettingsStore.current
    var picked by remember { mutableStateOf(currencySettings.homeCode) }
    val currency = fixedCurrency ?: picked
    var selected by remember { mutableStateOf<String?>(null) }

    val cycle = remember { PayCycle.current() }
    val weeks = remember { CalendarGrid.weeksForCycle(cycle) }
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    // ac: configurable-currency-pair — a transaction's currency comes from its account instead of
    // a hardcoded "BBL" wallet id check
    fun inCurrency(t: TransactionUiModel) =
        (AccountStore.all.firstOrNull { it.id == t.wallet }?.currency ?: currencySettings.homeCode) == currency

    val expenseByDay = remember(items, currency) {
        items.filter { it.txType == "expense" && inCurrency(it) }
            .groupBy { it.dateIso }
            .mapValues { e -> e.value.sumOf { it.amount } }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = Space.md)) {
        // currency toggle, only when no book fixes the currency
        if (fixedCurrency == null) Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = Space.sm)) {
            currenciesInUse(currencySettings.homeCode, AccountStore.all).forEach { cur ->
                val on = currency == cur
                Surface(
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    shape = AppShape.pill,
                ) {
                    Text(
                        currencySymbol(cur),
                        color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clip(AppShape.pill).clickable { picked = cur; selected = null }
                            .padding(horizontal = Space.lg, vertical = Space.sm),
                    )
                }
            }
        }
        Text(stringResource(R.string.feed_cycle_range, dayLabel(cycle.fromIso), dayLabel(cycle.toIso)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Space.sm))

        // weekday header
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { w -> Text(w, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(4.dp))

        // ac: transactions-list-calendar-toggle — weekday-aligned grid with per-day spending totals
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
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
            Surface(color = MaterialTheme.colorScheme.surface, shape = AppShape.lg, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(dayLabel(day), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(Space.lg))
                    if (rows.isEmpty()) {
                        Text(stringResource(R.string.feed_calendar_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = Space.lg, end = Space.lg, bottom = Space.lg))
                    } else rows.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.item, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            val inc = r.txType == "income"
                            Text(formatMoney(currency, if (inc) r.amount else -r.amount, showSign = true), style = MaterialTheme.typography.titleSmall, color = if (inc) app.green else MaterialTheme.colorScheme.onSurface)
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
        shape = AppShape.sm,
        tonalElevation = if (cell.inCycle && !selectedDay) 1.dp else 0.dp,
        modifier = modifier.height(56.dp)
            .then(if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, AppShape.sm) else Modifier)
            .clip(AppShape.sm).clickable(enabled = cell.inCycle, onClick = onClick),
    ) {
        Column(Modifier.padding(Space.xs), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (cell.inCycle) dayNum.toString() else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (cell.inCycle && expense != null && expense > 0) {
                Text(compact(currency, expense), style = MaterialTheme.typography.labelSmall, color = red, maxLines = 1)
            }
        }
    }
}
