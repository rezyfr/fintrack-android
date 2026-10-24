package com.fidriyanto.banktracker.ui.dashboard

import com.fidriyanto.banktracker.domain.model.formatMoney
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import com.fidriyanto.banktracker.domain.model.formatMoneyCompact
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
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.CategoryRow
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.categoryColor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

private const val GAP_DEG = 2f
private const val CHART_DP = 220
private const val STROKE_DP = 38

@Composable
fun CategoryBreakdownCard(
    summary: CurrencySummary,
    currency: String,
    listTitle: String,
    compareLabel: String,
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
        Column(modifier = Modifier.padding(Space.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.breakdown_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMoney(currency, summary.categorySpend),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium)
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
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatMoney(currency, selectedRow.amount),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${(selectedRow.percentage * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            stringResource(R.string.breakdown_total_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        // ac: category-spending-donut — total expense amount displayed in centre of donut
                        Text(
                            formatMoney(currency, summary.categorySpend),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.breakdown_tap_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // ac: insights-compact-breakdown — one line per category under a header that names the
            // period and the comparison period
            Row(Modifier.fillMaxWidth().padding(bottom = Space.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(listTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (categories.any { it.previousAmount != null }) {
                    Text(compareLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            categories.forEachIndexed { index, row ->
                val subs = summary.subcategoryBreakdown[row.category].orEmpty()
                val expanded = selectedIndex == index
                CompactCategoryRow(
                    row = row,
                    currency = currency,
                    isSelected = expanded,
                    hasSubcategories = subs.isNotEmpty(),
                    onClick = { selectedIndex = if (expanded) -1 else index }
                )
                // ac: insights-subcategory-breakdown — the selected category expands into its subcategories
                if (expanded && subs.isNotEmpty()) {
                    subs.forEach { sub -> SubcategoryRow(sub, currency) }
                    Spacer(Modifier.height(6.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ac: insights-subcategory-breakdown — one indented row per subcategory with amount and percentage
@Composable
private fun SubcategoryRow(row: CategoryRow, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.xl, end = Space.xs, top = Space.xs, bottom = Space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.category, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        val meta = formatMoney(currency, row.amount) + " · " + (row.percentage * 100).toInt() + "%"
        Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

// ac: insights-compact-breakdown — colour square, name, amount, change against the previous cycle,
// and a chevron when the category has subcategories to expand
@Composable
private fun CompactCategoryRow(
    row: CategoryRow,
    currency: String,
    isSelected: Boolean,
    hasSubcategories: Boolean,
    onClick: () -> Unit,
) {
    val color = categoryColor(row.category)
    val chevronAngle by animateFloatAsState(if (isSelected) 90f else 0f, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space.md)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "${row.category}: ${formatMoney(currency, row.amount)}, ${(row.percentage * 100).toInt()} percent of spending"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(AppShape.xs).background(color))
        Spacer(Modifier.width(10.dp))
        Text(
            row.category,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatMoney(currency, row.amount),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium)
        Box(Modifier.width(92.dp), contentAlignment = Alignment.CenterEnd) {
            row.previousAmount?.let { prev -> CategoryDelta(row.amount - prev, currency) }
        }
        Box(Modifier.width(24.dp), contentAlignment = Alignment.CenterEnd) {
            if (hasSubcategories) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).rotate(chevronAngle)
                )
            }
        }
    }
}

// ac: insights-category-vs-previous — spend change against the previous period. More spend is red with
// an up arrow. Less spend is green with a down arrow.
@Composable
private fun CategoryDelta(delta: Double, currency: String) {
    if (delta == 0.0) return
    val app = LocalAppColors.current
    val up = delta > 0
    val amount = formatMoney(currency, abs(delta))
    val label = (if (up) "\u25B2 " else "\u25BC ") + formatMoneyCompact(currency, abs(delta))
    val description = stringResource(if (up) R.string.breakdown_delta_up else R.string.breakdown_delta_down, amount)
    Text(
        label,
        color = if (up) app.red else app.green,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier.semantics { contentDescription = description }
    )
}
