package com.fidriyanto.banktracker.ui.dashboard

import com.fidriyanto.banktracker.domain.model.formatMoney
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.Space

// ac: multicurrency-money-formatting — Insights sections format with their currency code
@Composable
fun BalanceCard(summary: CurrencySummary, currency: String, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.xl, horizontal = Space.lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BalanceColumn(stringResource(R.string.home_income), formatMoney(currency, summary.moneyIn), MaterialTheme.colorScheme.onSurface)
            BalanceColumn(stringResource(R.string.home_spent), formatMoney(currency, summary.moneyOut), MaterialTheme.colorScheme.onSurface)
            val netColor = if (summary.net >= 0) appColors.green else MaterialTheme.colorScheme.error
            BalanceColumn(stringResource(R.string.insights_net), formatMoney(currency, summary.net, showSign = true), netColor)
        }
    }
}

@Composable
private fun BalanceColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, style = MaterialTheme.typography.titleMedium)
    }
}
