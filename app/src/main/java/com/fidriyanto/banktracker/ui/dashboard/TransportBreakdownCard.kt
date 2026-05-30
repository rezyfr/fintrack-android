package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.domain.model.MerchantRow
import com.fidriyanto.banktracker.ui.theme.LocalAppColors

// ac: transport-provider-breakdown: each row shows merchant name, amount, and percentage bar
// ac: transport-provider-breakdown: card title is Transport by Provider
@Composable
fun TransportBreakdownCard(
    breakdown: List<MerchantRow>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Transport by Provider",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(12.dp))
            breakdown.forEachIndexed { index, row ->
                MerchantRowItem(row, currencySymbol, appColors.green, appColors.progressTrack)
                if (index < breakdown.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MerchantRowItem(
    row: MerchantRow,
    currencySymbol: String,
    progressColor: androidx.compose.ui.graphics.Color,
    progressTrack: androidx.compose.ui.graphics.Color,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.merchant,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatAmount(row.amount, currencySymbol),
                color = MaterialTheme.colorScheme.onSurface,
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
                        contentDescription = "${row.merchant}: ${(row.percentage * 100).toInt()} percent of transport"
                    },
                color = progressColor,
                trackColor = progressTrack
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${(row.percentage * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
