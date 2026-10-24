package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.SpendCompare
import com.fidriyanto.banktracker.domain.model.SpendPoint
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.common.LoadingIndicator
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import com.fidriyanto.banktracker.ui.feed.CategoryAvatar
import com.fidriyanto.banktracker.ui.feed.EditTransactionBottomSheet
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: home-cycle-spending-comparison — cumulative spending chart, shared by Home and the Budget tab: this cycle (pine line + translucent
// pine fill + dot on today) vs last cycle (straight dashed reference line to its total). Press and hold
// to scrub: a crosshair shows the amount and date for both cycles at the touched day.
@Composable
fun SpendCompareChart(sc: SpendCompare, currency: String) {
    val pine = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val appColors = LocalAppColors.current
    val overColor = appColors.red
    val underColor = appColors.green
    val thisLbl = stringResource(R.string.home_spend_this_cycle)
    val lastLbl = stringResource(R.string.home_spend_last_cycle)
    val diffLbl = stringResource(R.string.home_spend_difference)
    val twoAgoLbl = stringResource(R.string.home_spend_two_cycles_ago)
    val threeAgoLbl = stringResource(R.string.home_spend_three_cycles_ago)
    val avgLbl = stringResource(R.string.home_spend_average)
    // ac: spend-compare-more-cycles — 0 = last cycle, 1 = two cycles ago, 2 = the last three cycles together
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val view = if (mode == 2 && sc.older.size < 2 || mode == 1 && sc.older.isEmpty()) 0 else mode
    val refs: List<Pair<String, List<SpendPoint>>> = when (view) {
        1 -> listOf(twoAgoLbl to sc.older[0])
        2 -> listOf(lastLbl to sc.lastPoints, twoAgoLbl to sc.older[0], threeAgoLbl to sc.older[1])
        else -> listOf(lastLbl to sc.lastPoints)
    }
    // the value a reference curve had on a given day (its last value once that cycle has ended)
    fun List<SpendPoint>.at(day: Int) = (getOrNull(day) ?: lastOrNull())?.cumulative ?: 0.0
    val density = LocalDensity.current
    var selectedX by remember { mutableStateOf<Float?>(null) }
    val start = runCatching { LocalDate.parse(sc.startIso) }.getOrDefault(LocalDate.now())
    val maxDay = sc.maxDay.coerceAtLeast(1)

    Surface(color = surface, shape = AppShape.lg, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (sc.older.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    FilterChip(view == 0, { mode = 0 }, { Text(stringResource(R.string.home_spend_vs_last)) })
                    FilterChip(view == 1, { mode = 1 }, { Text(stringResource(R.string.home_spend_vs_two_ago)) })
                    if (sc.older.size >= 2) FilterChip(view == 2, { mode = 2 }, { Text(stringResource(R.string.home_spend_vs_three)) })
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                LegendItem(pine, thisLbl, dashed = false)
                refs.forEachIndexed { i, (label, _) -> LegendItem(muted.copy(alpha = 1f - 0.25f * i), label, dashed = true) }
            }
            Canvas(
                Modifier.fillMaxWidth().height(180.dp).pointerInput(sc) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        selectedX = down.position.x
                        while (true) {
                            val e = awaitPointerEvent()
                            val ch = e.changes.firstOrNull() ?: break
                            if (!ch.pressed) break
                            selectedX = ch.position.x
                            ch.consume()
                        }
                        selectedX = null
                    }
                }
            ) {
                val leftPad = 8.dp.toPx(); val rightPad = 8.dp.toPx()
                val topPad = 12.dp.toPx(); val bottomPad = 22.dp.toPx()
                val plotW = size.width - leftPad - rightPad
                val plotH = size.height - topPad - bottomPad
                val baseY = topPad + plotH
                val maxY = maxOf(refs.maxOf { (_, pts) -> pts.maxOfOrNull { it.cumulative } ?: 0.0 }, sc.points.maxOf { it.cumulative }, 1.0)
                fun xOf(day: Int) = leftPad + plotW * (day.toFloat() / maxDay)
                fun yOf(v: Double) = topPad + plotH * (1f - (v / maxY).toFloat())

                drawLine(outline, Offset(leftPad, baseY), Offset(leftPad + plotW, baseY), strokeWidth = 1f)

                // reference cycles: each one's actual cumulative curve across its whole cycle, dashed and muted
                refs.forEachIndexed { i, (_, pts) ->
                    if (pts.isEmpty()) return@forEachIndexed
                    val refPath = Path()
                    pts.forEachIndexed { j, p ->
                        val x = xOf(p.dayIndex); val y = yOf(p.cumulative)
                        if (j == 0) refPath.moveTo(x, y) else refPath.lineTo(x, y)
                    }
                    drawPath(
                        refPath, color = muted.copy(alpha = 0.8f - 0.2f * i),
                        style = Stroke(
                            width = 1.6.dp.toPx(), cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                        ),
                    )
                }

                // this cycle: line + translucent fill beneath
                val line = Path()
                sc.points.forEachIndexed { i, p ->
                    val x = xOf(p.dayIndex); val y = yOf(p.cumulative)
                    if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(xOf(sc.points.last().dayIndex), baseY)
                    lineTo(xOf(sc.points.first().dayIndex), baseY)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(pine.copy(alpha = 0.30f), pine.copy(alpha = 0f)), startY = topPad, endY = baseY))
                drawPath(line, color = pine, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

                // dot on today
                val lastPt = sc.points.last()
                val dot = Offset(xOf(lastPt.dayIndex), yOf(lastPt.cumulative))
                drawCircle(surface, radius = 5.dp.toPx(), center = dot)
                drawCircle(pine, radius = 3.5.dp.toPx(), center = dot)

                // x axis date labels, thinned to 5 ticks
                val axisPaint = android.graphics.Paint().apply {
                    color = muted.toArgb(); isAntiAlias = true
                    textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER
                }
                val ticks = 4
                for (t in 0..ticks) {
                    val day = maxDay * t / ticks
                    val label = start.plusDays(day.toLong()).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
                    val tx = xOf(day).coerceIn(leftPad + 12.dp.toPx(), leftPad + plotW - 12.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(label, tx, size.height - 4.dp.toPx(), axisPaint)
                }

                // press-and-hold crosshair + tooltip
                val sx = selectedX
                if (sx != null) {
                    val day = (((sx - leftPad) / plotW) * maxDay).roundToInt().coerceIn(0, sc.todayIndex)
                    val cur = sc.points[day].cumulative
                    // each reference cycle's cumulative at the same day-of-cycle (clamped to its last day)
                    val refValues = refs.map { (label, pts) -> label to pts.at(day) }
                    val lst = refValues.map { it.second }.average()
                    val cx = xOf(day)
                    drawLine(outline, Offset(cx, topPad), Offset(cx, baseY), strokeWidth = 1.dp.toPx())
                    drawCircle(pine, radius = 4.dp.toPx(), center = Offset(cx, yOf(cur)))
                    refValues.forEach { (_, v) -> drawCircle(muted, radius = 3.dp.toPx(), center = Offset(cx, yOf(v))) }

                    val dateStr = start.plusDays(day.toLong()).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
                    // ac: home-cycle-spending-comparison — difference = this cycle minus the reference at this
                    // day. Positive means spending faster (red); negative means slower (green). With three
                    // reference cycles the reference is their average.
                    val diff = cur - lst
                    val diffColor = if (diff > 0) overColor else if (diff < 0) underColor else muted
                    val lines = buildList {
                        add(dateStr)
                        add("$thisLbl  ${formatMoney(currency, cur)}")
                        refValues.forEach { (label, v) -> add("$label  ${formatMoney(currency, v)}") }
                        val diffName = if (refs.size > 1) "$diffLbl ($avgLbl)" else diffLbl
                        add("$diffName  ${formatMoney(currency, diff, showSign = true)}")
                    }
                    val tPaint = android.graphics.Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx() }
                    val padX = 8.dp.toPx(); val padY = 6.dp.toPx(); val lh = 15.sp.toPx()
                    val boxW = lines.maxOf { tPaint.measureText(it) } + padX * 2
                    val boxH = lh * lines.size + padY * 2
                    var bx = cx + 8.dp.toPx()
                    if (bx + boxW > size.width) bx = cx - 8.dp.toPx() - boxW
                    bx = bx.coerceIn(0f, (size.width - boxW).coerceAtLeast(0f))
                    val by = topPad
                    drawContext.canvas.nativeCanvas.apply {
                        val bg = android.graphics.Paint().apply { isAntiAlias = true; color = surface.toArgb() }
                        val br = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1f; color = outline.toArgb() }
                        drawRoundRect(bx, by, bx + boxW, by + boxH, 8f, 8f, bg)
                        drawRoundRect(bx, by, bx + boxW, by + boxH, 8f, 8f, br)
                        lines.forEachIndexed { i, text ->
                            val paint = tPaint.apply {
                                when (i) {
                                    0 -> { color = onSurface.toArgb(); typeface = android.graphics.Typeface.DEFAULT_BOLD }
                                    1 -> { color = pine.toArgb(); typeface = android.graphics.Typeface.DEFAULT }
                                    lines.lastIndex -> { color = diffColor.toArgb(); typeface = android.graphics.Typeface.DEFAULT_BOLD }
                                    else -> { color = muted.toArgb(); typeface = android.graphics.Typeface.DEFAULT }
                                }
                            }
                            drawText(text, bx + padX, by + padY + lh * (i + 1) - 4.dp.toPx(), paint)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun LegendItem(color: Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        Canvas(Modifier.size(width = 16.dp, height = 8.dp)) {
            val y = size.height / 2
            drawLine(
                color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
