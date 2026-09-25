package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CategoryRow
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.ui.theme.categoryColor
import kotlin.math.atan2
import kotlin.math.sqrt

private const val GAP_DEG = 2f
private const val CHART_DP = 220
private const val STROKE_DP = 38

@Composable
fun CategoryBreakdownCard(
    summary: CurrencySummary,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    // ac: category-spending-donut — chart hidden when no expense transactions in the period
    if (summary.categoryBreakdown.isEmpty()) return

    val categories = summary.categoryBreakdown
    var selectedIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(categories) { selectedIndex = -1 }

    // ac: category-spending-donut — animated donut chart with one arc per spending category
    val progress = remember(categories) { Animatable(0f) }
    LaunchedEffect(categories) {
        progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    val density = LocalDensity.current
    val strokePx = with(density) { STROKE_DP.dp.toPx() }

    val targetSweeps = remember(categories) {
        categories.map { (it.percentage * 360f).coerceAtLeast(0f) }
    }
    val targetSweepsRef = rememberUpdatedState(targetSweeps)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.breakdown_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    formatAmount(summary.totalExpenses, currencySymbol),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ac: category-spending-donut — total or tapped category name+amount displayed in centre
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(CHART_DP.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val sweeps = targetSweepsRef.value
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val r = sqrt(dx * dx + dy * dy)
                                val outerR = size.width / 2f
                                val innerR = outerR - strokePx
                                if (r >= innerR * 0.8f && r <= outerR * 1.1f) {
                                    var angleDeg =
                                        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                                            .toFloat() + 90f
                                    if (angleDeg < 0f) angleDeg += 360f
                                    if (angleDeg >= 360f) angleDeg -= 360f
                                    var start = 0f
                                    var hit = -1
                                    for (i in sweeps.indices) {
                                        val segSweep = (sweeps[i] - GAP_DEG).coerceAtLeast(0f)
                                        val segStart = start + GAP_DEG / 2f
                                        if (angleDeg >= segStart && angleDeg < segStart + segSweep) {
                                            hit = i; break
                                        }
                                        start += sweeps[i]
                                    }
                                    selectedIndex = if (selectedIndex == hit) -1 else hit
                                }
                            }
                        }
                ) {
                    val anim = progress.value
                    var startAngle = -90f
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    val arcTopLeft = Offset(strokePx / 2f, strokePx / 2f)
                    categories.forEachIndexed { i, row ->
                        val isSelected = selectedIndex == i
                        val allocSweep = (row.percentage * 360f).coerceAtLeast(0f) * anim
                        val segSweep = (allocSweep - GAP_DEG).coerceAtLeast(0f)
                        drawArc(
                            color = categoryColor(row.category),
                            startAngle = startAngle + GAP_DEG / 2f,
                            sweepAngle = segSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokePx * 1.2f else strokePx,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += allocSweep
                    }
                }

                val selectedRow = categories.getOrNull(selectedIndex)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width((CHART_DP - STROKE_DP * 2 - 8).dp)
                ) {
                    if (selectedRow != null) {
                        // ac: category-spending-donut — tapping a segment shows category name+amount in centre
                        Text(
                            selectedRow.category,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatAmount(selectedRow.amount, currencySymbol),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${(selectedRow.percentage * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            stringResource(R.string.breakdown_total_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        // ac: category-spending-donut — total expense amount displayed in centre of donut
                        Text(
                            formatAmount(summary.totalExpenses, currencySymbol),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ac: category-spending-donut — scrollable list with colour dot, name, amount and percentage bar
            categories.forEachIndexed { index, row ->
                DonutCategoryRow(
                    row = row,
                    currencySymbol = currencySymbol,
                    isSelected = selectedIndex == index
                )
                // ac: insights-subcategory-breakdown — the selected category expands into its subcategories
                if (selectedIndex == index) {
                    val subs = summary.subcategoryBreakdown[row.category].orEmpty()
                    if (subs.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        subs.forEach { sub ->
                            SubcategoryRow(sub, currencySymbol)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
                if (index < categories.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ac: insights-subcategory-breakdown — one indented row per subcategory with amount and percentage
@Composable
private fun SubcategoryRow(row: CategoryRow, currencySymbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val meta = "${formatAmount(row.amount, currencySymbol)} · ${(row.percentage * 100).toInt()}%"
        Text(
            row.category,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Text(
            meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun DonutCategoryRow(
    row: CategoryRow,
    currencySymbol: String,
    isSelected: Boolean,
) {
    val color = categoryColor(row.category)
    // ac: category-spending-donut — each category as a card with dot, name, percentage top-right, bold amount below
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "${row.category}: ${formatAmount(row.amount, currencySymbol)}, ${(row.percentage * 100).toInt()} percent of spending"
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // ac: category-spending-donut — each arc uses a distinct colour consistent per category
                    Canvas(Modifier.size(9.dp)) { drawCircle(color) }
                    Text(
                        row.category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${(row.percentage * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                formatAmount(row.amount, currencySymbol),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
