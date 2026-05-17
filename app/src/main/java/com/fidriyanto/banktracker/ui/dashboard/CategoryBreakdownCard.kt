package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.MutedText
import com.fidriyanto.banktracker.ui.theme.Surface

private val ProgressTrackColor = Color(0xFF2A2A2A)

@Composable
fun CategoryBreakdownCard(
    summary: CurrencySummary,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Spending Breakdown",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    formatAmount(summary.totalExpenses, currencySymbol),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (summary.categoryBreakdown.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("No spending recorded", color = MutedText, fontSize = 14.sp)
            } else {
                Spacer(Modifier.height(12.dp))
                summary.categoryBreakdown.forEachIndexed { index, row ->
                    CategoryRowItem(row, currencySymbol)
                    if (index < summary.categoryBreakdown.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRowItem(row: CategoryRow, currencySymbol: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.category,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatAmount(row.amount, currencySymbol),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { row.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .semantics {
                        contentDescription = "${row.category}: ${(row.percentage * 100).toInt()} percent of spending"
                    },
                color = Accent,
                trackColor = ProgressTrackColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${(row.percentage * 100).toInt()}%",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
