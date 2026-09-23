package com.fidriyanto.banktracker.ui.budgetglance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private fun money(currency: String, a: Double): String {
    val n = NumberFormat.getNumberInstance(Locale.US).format(kotlin.math.abs(a).roundToLong())
    return if (currency == "THB") "฿$n" else "Rp $n"
}

@Composable
fun BudgetGlanceScreen(viewModel: BudgetGlanceViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Currency toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("IDR", "THB").forEach { cur ->
                val on = s.currency == cur
                Surface(
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)),
                ) {
                    Text(
                        if (cur == "IDR") "Rp" else "฿",
                        color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.selectCurrency(cur) }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }
            }
        }

        when {
            s.loading -> Text(stringResource(R.string.home_budget), color = MaterialTheme.colorScheme.onSurfaceVariant)
            s.error != null -> Text(stringResource(R.string.budget_error), color = MaterialTheme.colorScheme.error)
            s.glance == null || s.glance!!.lines.isEmpty() ->
                Text(stringResource(R.string.budget_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> GlanceBody(s.glance!!)
        }
    }
}

@Composable
private fun GlanceBody(g: com.fidriyanto.banktracker.domain.usecase.BudgetGlance) {
    val app = LocalAppColors.current
    val over = g.totalSpent > g.totalTarget
    // ac: android-budget-daily-allowance — flex total/spent/remaining and a safe daily allowance
    // Ring + remaining
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.budget_remaining), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(money(g.currency, g.remaining), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.budget_of_total, money(g.currency, g.totalTarget)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Ring(fraction = if (g.totalTarget > 0) (g.totalSpent / g.totalTarget).toFloat() else 0f,
                over = over, color = MaterialTheme.colorScheme.primary, track = MaterialTheme.colorScheme.surfaceContainerHighest, red = app.red)
        }
    }
    // Safe-to-spend callout
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                if (over) stringResource(R.string.budget_over_total, money(g.currency, g.totalSpent - g.totalTarget))
                else stringResource(R.string.budget_safe, money(g.currency, g.dailyAllowance)),
                color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            )
            Text(stringResource(R.string.budget_days_left, g.daysLeft), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
    // Lines
    g.lines.forEach { LineRow(it, g.currency, g.daysLeft, app.red) }
}

@Composable
private fun Ring(fraction: Float, over: Boolean, color: Color, track: Color, red: Color) {
    val pct = (fraction * 100).roundToLong()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color = if (over) red else color, startAngle = -90f, sweepAngle = 360f * fraction.coerceIn(0f, 1f), useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.budget_percent, pct.toInt()), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (over) red else color)
    }
}

@Composable
private fun LineRow(ls: BudgetLineSpend, currency: String, daysLeft: Int, red: Color) {
    // ac: android-budget-daily-allowance — each line's remaining and per-day remaining
    val perDay = if (daysLeft > 0) ls.remaining / daysLeft else ls.remaining
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ls.line.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.budget_percent, (ls.progress * 100).roundToLong().toInt()), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { ls.progress },
                color = if (ls.isOver) red else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.budget_line_remaining, money(currency, ls.remaining)) + " · " +
                        stringResource(R.string.budget_line_perday, money(currency, perDay)),
                    fontSize = 12.sp, color = if (ls.isOver) red else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.budget_line_of, money(currency, ls.line.target)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

