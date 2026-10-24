package com.fidriyanto.banktracker.ui.balances

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.domain.usecase.GrowthPoint
import com.fidriyanto.banktracker.domain.usecase.GrowthRange
import com.fidriyanto.banktracker.domain.usecase.GrowthSeries
import com.fidriyanto.banktracker.ui.common.rememberFittedStyle
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.Space
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// ac: account-growth-chart — the balance of checking and investment accounts at the end of each pay
// cycle, as two stacked bands. The last point is today. A credit card is not part of it. Investment
// is the money put in, because the app records transfers and not market value.
@Composable
internal fun GrowthCard(
    seriesFor: (currency: String, range: GrowthRange, includeInvestment: Boolean) -> GrowthSeries?,
    currenciesFor: (includeInvestment: Boolean) -> List<String>,
    hasInvestment: Boolean,
    modifier: Modifier = Modifier,
) {
    if (currenciesFor(true).isEmpty()) return
    val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    // ac: account-growth-toggle — hide investment from the chart. The choice is kept on the phone.
    var showInvestment by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_INVESTMENT, true)) }
    val withInvestment = showInvestment || !hasInvestment
    var range by rememberSaveable { mutableStateOf(GrowthRange.TWELVE_MONTHS) }
    val currencies = currenciesFor(withInvestment)
    var currency by rememberSaveable { mutableStateOf(currencies.firstOrNull().orEmpty()) }
    val chosen = if (currency in currencies) currency else currencies.firstOrNull()
    val series = chosen?.let { seriesFor(it, range, withInvestment) }
    // With nothing to draw the card is hidden. The exception is a chart emptied by the switch itself,
    // which keeps the switch so the investment can be shown again.
    if (series == null && withInvestment) return

    Column(
        modifier
            .fillMaxWidth()
            .clip(AppShape.xl)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.growth_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            if (chosen != null && currencies.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    currencies.forEach { code ->
                        FilterChip(selected = code == chosen, onClick = { currency = code }, label = { Text(code) })
                    }
                }
            } else if (chosen != null) {
                Text(chosen, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (series != null) {
            GrowthBody(series, showInvestment = withInvestment, range = range, onRange = { range = it })
        }

        if (hasInvestment) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = showInvestment, role = Role.Switch) {
                        showInvestment = it
                        prefs.edit { putBoolean(KEY_SHOW_INVESTMENT, it) }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.growth_show_investment), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = showInvestment, onCheckedChange = null)
            }
        }
    }
}

private const val PREFS = "growth_card"
private const val KEY_SHOW_INVESTMENT = "show_investment"

@Composable
private fun GrowthBody(series: GrowthSeries, showInvestment: Boolean, range: GrowthRange, onRange: (GrowthRange) -> Unit) {
    // A new series starts on its last point, which is today.
    var selected by remember(series) { mutableIntStateOf(series.points.lastIndex) }
    val point = series.points[selected.coerceIn(0, series.points.lastIndex)]
    val index = series.points.indexOf(point)
    val app = LocalAppColors.current

    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        Readout(series, point, index)

        GrowthChart(series, selected = index, onSelect = { selected = it })

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            LegendItem(app.growthChecking, stringResource(R.string.growth_checking), formatMoney(series.currency, point.checking), Modifier.weight(1f))
            if (showInvestment) {
                LegendItem(app.growthInvestment, stringResource(R.string.growth_investment), formatMoney(series.currency, point.investment), Modifier.weight(1f))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(
                GrowthRange.SIX_MONTHS to R.string.growth_range_6m,
                GrowthRange.TWELVE_MONTHS to R.string.growth_range_12m,
                GrowthRange.ALL to R.string.growth_range_all,
            ).forEach { (r, label) ->
                FilterChip(
                    selected = r == range,
                    onClick = { onRange(r) },
                    label = { Text(stringResource(label), Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Readout(series: GrowthSeries, point: GrowthPoint, index: Int) {
    val first = series.points.first()
    Column(Modifier.fillMaxWidth()) {
        Text(
            if (point.isNow) stringResource(R.string.growth_now_so_far) else stringResource(R.string.growth_cycle_ending, point.date.format(DAY)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The figure takes the largest role that fits, so a long amount never wraps or clips.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val figure = formatMoney(series.currency, point.total)
            val style = rememberFittedStyle(
                text = figure,
                maxWidth = maxWidth,
                candidates = listOf(
                    MaterialTheme.typography.displaySmall,
                    MaterialTheme.typography.headlineLarge,
                    MaterialTheme.typography.headlineMedium,
                ),
            )
            Text(figure, style = style, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
        // Both changes sit under the figure, one to a line. Beside it, a long amount had no room.
        if (index > 0) {
            ChangeLine(series.currency, point.total - series.points[index - 1].total, stringResource(R.string.growth_last_cycle_end))
            ChangeLine(series.currency, point.total - first.total, monthLabel(first))
        }
    }
}

@Composable
private fun ChangeLine(currency: String, change: Double, since: String) {
    Text(
        stringResource(R.string.growth_change_since, formatMoney(currency, change, showSign = true), since),
        style = MaterialTheme.typography.labelMedium,
        color = changeColor(change),
        maxLines = 1,
    )
}

// Growth is the good case and gets the accent. A fall is not an error here, so it stays neutral and
// red keeps meaning a problem.
@Composable
private fun changeColor(change: Double): Color =
    if (change > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun LegendItem(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs + 2.dp)) {
            Box(Modifier.size(10.dp).clip(AppShape.xs).background(color))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GrowthChart(series: GrowthSeries, selected: Int, onSelect: (Int) -> Unit) {
    val app = LocalAppColors.current
    val points = series.points
    val grid = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val labelStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(color = muted)
    val nowStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(color = ink)
    val measurer = rememberTextMeasurer()
    val nowLabel = stringResource(R.string.growth_now)
    val description = stringResource(R.string.growth_chart_description, formatMoney(series.currency, points[selected].total), points[selected].date.format(DAY))
    val density = LocalDensity.current
    val inset = with(density) { 8.dp.toPx() }

    // Tapping or dragging picks the nearest point.
    fun pick(x: Float, width: Float): Int {
        val step = (width - 2 * inset) / max(1, points.lastIndex)
        return ((x - inset) / step).let { if (it.isNaN()) 0 else Math.round(it) }.coerceIn(0, points.lastIndex)
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = description }
            .pointerInput(points) { detectTapGestures { onSelect(pick(it.x, size.width.toFloat())) } }
            .pointerInput(points) { detectHorizontalDragGestures { change, _ -> onSelect(pick(change.position.x, size.width.toFloat())) } },
    ) {
        val labelRoom = with(density) { 20.dp.toPx() }
        val top = with(density) { 8.dp.toPx() }
        val base = size.height - labelRoom
        val yMax = max(points.maxOf { it.total }, 1.0)
        val yMin = min(0.0, points.minOf { min(it.checking, it.total) })
        val span = (yMax - yMin).coerceAtLeast(1.0)
        val stepX = (size.width - 2 * inset) / max(1, points.lastIndex)
        fun x(i: Int) = inset + i * stepX
        fun y(v: Double) = (base - (v - yMin) / span * (base - top)).toFloat()

        // light guides at a third and two thirds, the baseline at zero
        listOf(1.0 / 3, 2.0 / 3, 1.0).forEach { f ->
            val gy = (base - f * (base - top)).toFloat()
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        val zero = y(0.0)

        val checking = Path().apply {
            moveTo(x(0), zero)
            points.forEachIndexed { i, p -> lineTo(x(i), y(p.checking)) }
            lineTo(x(points.lastIndex), zero)
            close()
        }
        drawPath(checking, app.growthChecking)

        val investment = Path().apply {
            moveTo(x(0), y(points[0].total))
            points.forEachIndexed { i, p -> lineTo(x(i), y(p.total)) }
            for (i in points.lastIndex downTo 0) lineTo(x(i), y(points[i].checking))
            close()
        }
        drawPath(investment, app.growthInvestment)

        // the cycle in progress is lighter, so it does not read as a finished month
        if (points.size >= 2 && points.last().isNow) {
            drawRect(surface.copy(alpha = 0.4f), Offset(x(points.lastIndex - 1), 0f), Size(stepX, base))
        }
        drawLine(grid, Offset(0f, zero), Offset(size.width, zero), strokeWidth = 2f)

        // the chosen point
        val sx = x(selected)
        val sy = y(points[selected].total)
        drawLine(ink.copy(alpha = 0.4f), Offset(sx, sy), Offset(sx, zero), strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        drawCircle(surface, radius = 10f, center = Offset(sx, sy))
        drawCircle(ink, radius = 10f, center = Offset(sx, sy), style = Stroke(width = 4f))

        // axis labels, at most about six, and always the last one
        val step = if (points.size <= 7) 1 else ceil(points.size / 6.0).toInt()
        val long = points.size > 12
        points.forEachIndexed { i, p ->
            if ((points.lastIndex - i) % step != 0) return@forEachIndexed
            val text = if (p.isNow) nowLabel else monthLabel(p, withYear = long)
            val style = if (p.isNow) nowStyle else labelStyle
            val measured = measurer.measure(AnnotatedString(text), style = style, maxLines = 1)
            val left = (x(i) - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width)
            drawText(measured, topLeft = Offset(left, base + with(density) { 4.dp.toPx() }))
        }
    }
}

private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

private fun monthLabel(p: GrowthPoint, withYear: Boolean = false): String {
    val month = p.date.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
    return if (withYear) "$month ${p.date.year.toString().takeLast(2)}" else month
}
