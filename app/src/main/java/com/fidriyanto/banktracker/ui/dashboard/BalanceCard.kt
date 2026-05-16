package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.Destructive
import com.fidriyanto.banktracker.ui.theme.MutedText
import com.fidriyanto.banktracker.ui.theme.Surface
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceCard(summary: CurrencySummary, currencySymbol: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BalanceColumn("Income", formatAmount(summary.totalIncome, currencySymbol), Color.White)
            BalanceColumn("Expenses", formatAmount(summary.totalExpenses, currencySymbol), Color.White)
            val netColor = if (summary.net >= 0) Accent else Destructive
            val netText = formatAmount(summary.net, currencySymbol, showSign = true)
            BalanceColumn("Net", netText, netColor)
        }
    }
}

@Composable
private fun BalanceColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

internal fun formatAmount(amount: Double, symbol: String, showSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    val absFormatted = formatter.format(kotlin.math.abs(amount))
    return when {
        showSign && amount >= 0 -> "+$symbol$absFormatted"
        amount < 0 -> "-$symbol$absFormatted"
        else -> "$symbol$absFormatted"
    }
}
