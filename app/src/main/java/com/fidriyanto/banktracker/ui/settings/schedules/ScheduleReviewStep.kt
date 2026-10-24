package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.settings.rules.StepHeading
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.Space

// ac: scheduled-transactions - step 3: how each transaction will look in the feed, and the auto-post switch
@Composable
fun ScheduleReviewStep(d: ScheduleDraft, accounts: List<Account>, onAutoPost: (Boolean) -> Unit) {
    val account = accounts.firstOrNull { it.id == d.walletId }
    val next = d.nextDates(3)
    val sign = when (d.txType) {
        "income" -> 1.0
        "expense" -> -1.0
        else -> 0.0
    }
    val amount = d.amountValue ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
        StepHeading(stringResource(R.string.schedule_review_title), stringResource(R.string.schedule_review_body))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                IconBadge(Icons.Outlined.Event)
                Column(Modifier.weight(1f)) {
                    Text(d.name.trim().ifBlank { d.category }, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.schedule_review_when_where, next.firstOrNull()?.label().orEmpty(), account?.displayName.orEmpty()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    formatMoney(account?.currency.orEmpty(), amount * sign.let { if (it == 0.0) 1.0 else it }, showSign = sign != 0.0),
                    fontWeight = FontWeight.SemiBold, color = if (sign > 0) LocalAppColors.current.green else if (sign < 0) LocalAppColors.current.red else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (d.installmentId != null) Text(stringResource(R.string.schedule_review_plan), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.schedule_review_rules), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.clickable { onAutoPost(!d.autoPost) }.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.schedule_auto_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.schedule_auto_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = d.autoPost, onCheckedChange = onAutoPost)
            }
        }
        Text(stringResource(if (d.autoPost) R.string.schedule_auto_on_note else R.string.schedule_auto_off_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
