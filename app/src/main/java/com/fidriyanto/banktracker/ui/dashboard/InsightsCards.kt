package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.MonthTrendPoint
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.WalletRow
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.domain.usecase.CalendarGrid
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.feed.CategoryAvatar
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize

private const val HEATMAP_LEVELS = 5
private const val HEATMAP_MAX_DAYS = 31
private val HEATMAP_ALPHAS = listOf(0.18f, 0.35f, 0.55f, 0.78f, 1f)

private fun dayLabel(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US))
}.getOrDefault(iso)

private fun monthLabel(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM", Locale.US))
}.getOrDefault(iso)

@Composable
private fun InsightsCard(title: String, trailing: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Space.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                trailing()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ac: insights-largest-expenses — the period's largest expenses with a link to the full list
@Composable
fun LargestExpensesCard(expenses: List<TransactionUiModel>, currency: String, onOpenTransactions: () -> Unit) {
    if (expenses.isEmpty()) return
    val app = LocalAppColors.current
    InsightsCard(stringResource(R.string.insights_largest_title)) {
        expenses.forEachIndexed { i, tx ->
            Row(Modifier.fillMaxWidth().padding(vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(tx.category)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tx.item, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val meta = tx.category + " · " + dayLabel(tx.dateIso.take(10))
                    Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Text(formatMoney(currency, -tx.amount), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            }
            if (i < expenses.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
        Text(
            stringResource(R.string.insights_all_transactions),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = Space.sm).clip(AppShape.sm).clickable(onClick = onOpenTransactions).padding(vertical = Space.sm)
        )
    }
}

// ac: insights-paid-with — expense total and share per wallet
@Composable
fun PaidWithCard(rows: List<WalletRow>, currency: String) {
    if (rows.isEmpty()) return
    InsightsCard(stringResource(R.string.insights_paid_with_title)) {
        rows.forEachIndexed { i, row ->
            val name = AccountStore.all.firstOrNull { it.id == row.wallet }?.displayName ?: row.wallet
            Column(Modifier.fillMaxWidth().padding(vertical = Space.sm)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formatMoney(currency, row.amount), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${(row.percentage * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { row.percentage },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
            if (i < rows.lastIndex) Spacer(Modifier.height(4.dp))
        }
    }
}

// ac: insights-daily-heatmap — one cell per day of the period, shaded by that day's spend in five
// levels. Tap a day to see its total. Shown only for periods of one month or less.
@Composable
fun DailyHeatmapCard(dailySpend: Map<String, Double>, fromIso: String, toIso: String, currency: String) {
    val start = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return
    val end = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return
    if (end.toEpochDay() - start.toEpochDay() + 1 > HEATMAP_MAX_DAYS) return
    if (dailySpend.isEmpty()) return

    val today = LocalDate.now()
    val weeks = remember(fromIso, toIso) { CalendarGrid.weeksForCycle(PayCycle.Range(fromIso, toIso, start, end)) }
    val lastDay = if (today.isBefore(end)) today else end
    var selected by remember(fromIso, toIso) { mutableStateOf(if (lastDay.isBefore(start)) fromIso else lastDay.toString()) }

    val inRange = dailySpend.filterKeys { it in fromIso..toIso }
    val max = inRange.values.maxOrNull() ?: 0.0
    val elapsedDays = (lastDay.toEpochDay() - start.toEpochDay() + 1).coerceAtLeast(1)
    val average = inRange.values.sum() / elapsedDays
    fun level(v: Double?): Int =
        if (v == null || v <= 0.0 || max <= 0.0) 0 else ceil(v / max * HEATMAP_LEVELS).toInt().coerceIn(1, HEATMAP_LEVELS)

    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val outline = MaterialTheme.colorScheme.outlineVariant
    // The grid from CalendarGrid starts on Sunday.
    val weekdays = remember { (0..6).map { DayOfWeek.SUNDAY.plus(it.toLong()).getDisplayName(TextStyle.SHORT, Locale.US) } }

    InsightsCard(stringResource(R.string.insights_daily_title)) {
        val amount = inRange[selected]
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayLabel(selected), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            Text(" · ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (amount != null && amount > 0.0) formatMoney(currency, amount) else stringResource(R.string.insights_daily_no_spend),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { w ->
                Text(w, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                week.forEach { cell ->
                    val date = LocalDate.parse(cell.iso)
                    val future = date.isAfter(today)
                    val lvl = level(inRange[cell.iso])
                    val shape = AppShape.sm
                    Box(
                        Modifier.weight(1f).height(38.dp).clip(shape)
                            .then(
                                when {
                                    !cell.inCycle -> Modifier
                                    future -> Modifier.border(1.dp, outline, shape)
                                    lvl == 0 -> Modifier.background(empty)
                                    else -> Modifier.background(primary.copy(alpha = HEATMAP_ALPHAS[lvl - 1]))
                                }
                            )
                            .then(if (cell.inCycle && cell.iso == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                            .clickable(enabled = cell.inCycle && !future) { selected = cell.iso }
                            .padding(Space.xs)
                    ) {
                        if (cell.inCycle) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    future -> MaterialTheme.colorScheme.outline
                                    lvl >= 4 -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.insights_daily_less), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(6.dp))
            HEATMAP_ALPHAS.forEach { a ->
                Box(Modifier.size(12.dp).clip(AppShape.xs).background(primary.copy(alpha = a)))
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.insights_daily_more), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.insights_daily_average, formatMoney(currency, average)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ac: insights-six-month-trend — money in (green) and money out (red) for the last six pay cycles. Tap a month to
// see its totals. The latest month is selected first.
@Composable
fun MonthlyTrendCard(points: List<MonthTrendPoint>, currency: String) {
    if (points.size < 2 || points.all { it.moneyIn == 0.0 && it.moneyOut == 0.0 }) return
    val app = LocalAppColors.current
    val inColor = app.green
    val outColor = app.red
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }

    InsightsCard(stringResource(R.string.insights_trend_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
            TrendLegend(inColor, stringResource(R.string.insights_trend_in))
            TrendLegend(outColor, stringResource(R.string.insights_trend_out))
        }
        Spacer(Modifier.height(8.dp))
        val p = points[selected]
        val month = monthLabel(p.endIso)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (p.inProgress) stringResource(R.string.insights_trend_in_progress, month) else month,
                color = onSurface, style = MaterialTheme.typography.titleSmall)
            Text(" · ", color = muted, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.insights_trend_summary, formatMoney(currency, p.moneyIn), formatMoney(currency, p.moneyOut)),
                color = muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier.fillMaxWidth().height(170.dp).pointerInput(points) {
                detectTapGestures { offset ->
                    val pad = 16.dp.toPx()
                    val step = (size.width - pad * 2) / (points.size - 1)
                    selected = ((offset.x - pad) / step).let { kotlin.math.round(it).toInt() }.coerceIn(0, points.lastIndex)
                }
            }
        ) {
            val pad = 16.dp.toPx()
            val topPad = 12.dp.toPx()
            val bottomPad = 22.dp.toPx()
            val plotW = size.width - pad * 2
            val plotH = size.height - topPad - bottomPad
            val baseY = topPad + plotH
            val maxY = maxOf(points.maxOf { maxOf(it.moneyIn, it.moneyOut) }, 1.0)
            fun xOf(i: Int) = pad + plotW * i / (points.size - 1)
            fun yOf(v: Double) = topPad + plotH * (1f - (v / maxY).toFloat())

            drawLine(outline, Offset(pad, baseY), Offset(pad + plotW, baseY), strokeWidth = 1f)
            drawLine(outline, Offset(xOf(selected), topPad), Offset(xOf(selected), baseY), strokeWidth = 1.dp.toPx())

            fun series(color: Color, value: (MonthTrendPoint) -> Double) {
                val path = Path()
                points.forEachIndexed { i, pt -> if (i == 0) path.moveTo(xOf(i), yOf(value(pt))) else path.lineTo(xOf(i), yOf(value(pt))) }
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { i, pt ->
                    val c = Offset(xOf(i), yOf(value(pt)))
                    if (i == selected) {
                        drawCircle(color, radius = 6.dp.toPx(), center = c)
                    } else {
                        drawCircle(surface, radius = 4.5.dp.toPx(), center = c)
                        drawCircle(color, radius = 4.5.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
            series(inColor) { it.moneyIn }
            series(outColor) { it.moneyOut }

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true; textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER
            }
            points.forEachIndexed { i, pt ->
                paint.color = (if (i == selected) onSurface else muted).toArgb()
                paint.typeface = if (i == selected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                drawContext.canvas.nativeCanvas.drawText(monthLabel(pt.endIso), xOf(i), size.height - 4.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun TrendLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Canvas(Modifier.size(width = 16.dp, height = 8.dp)) {
            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ac: insights-bento — two compact tiles side by side above the full-width cards, so the page is a
// grid and not one column of identical blocks. Both read from the daily spend the heatmap already
// uses, so no new data is fetched.
@Composable
fun SpendStatsRow(dailySpend: Map<String, Double>, fromIso: String, toIso: String, currency: String) {
    val start = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return
    val end = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return
    val inRange = dailySpend.filterKeys { it in fromIso..toIso }.filterValues { it > 0.0 }
    if (inRange.isEmpty()) return

    val today = LocalDate.now()
    val lastDay = if (today.isBefore(end)) today else end
    val elapsed = (lastDay.toEpochDay() - start.toEpochDay() + 1).coerceAtLeast(1)
    val average = inRange.values.sum() / elapsed
    val biggest = inRange.maxByOrNull { it.value }

    // The two tiles sit side by side, so they take the height of the taller one. Without this the
    // tile without a caption is shorter and the row looks ragged.
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        StatTile(
            label = stringResource(R.string.insights_avg_per_day),
            value = formatMoney(currency, average),
            caption = null,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            label = stringResource(R.string.insights_biggest_day),
            value = formatMoney(currency, biggest?.value ?: 0.0),
            caption = biggest?.key?.let { dayLabel(it) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, caption: String?, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    ) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
