package com.fidriyanto.banktracker.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TransactionCard(
    transaction: TransactionUiModel,
    onRetry: () -> Unit,
    onConfirm: (item: String, category: String) -> Unit
) {
    val appColors = LocalAppColors.current
    val borderColor = when (transaction.status) {
        TransactionStatus.PENDING_EDIT -> appColors.blue
        TransactionStatus.PENDING_SYNC -> appColors.warning
        TransactionStatus.SYNC_FAILED  -> MaterialTheme.colorScheme.error
        TransactionStatus.SYNCED       -> Color.Transparent
    }
    val badgeText = when (transaction.status) {
        TransactionStatus.PENDING_EDIT -> "Pending"
        TransactionStatus.SYNCED       -> "Synced"
        TransactionStatus.PENDING_SYNC -> "Queued"
        TransactionStatus.SYNC_FAILED  -> "Failed"
    }
    val badgeColor = when (transaction.status) {
        TransactionStatus.PENDING_EDIT -> appColors.blue
        TransactionStatus.SYNCED       -> appColors.green
        TransactionStatus.PENDING_SYNC -> appColors.warning
        TransactionStatus.SYNC_FAILED  -> MaterialTheme.colorScheme.error
    }
    val date = runCatching { LocalDate.parse(transaction.dateIso) }.getOrNull()
    val dateStr = date?.format(DateTimeFormatter.ofPattern("d MMM")) ?: ""
    val isThb         = transaction.wallet == null || transaction.wallet == "BBL"
    val symbol        = if (isThb) "฿" else "Rp "
    val sign          = when (transaction.txType) {
        "income"                 -> "+"
        "transfer", "investment" -> ""
        else                     -> "-"
    }
    val amountStr     = if (isThb) {
        if (transaction.amount % 1.0 == 0.0) transaction.amount.toInt().toString() else transaction.amount.toString()
    } else {
        transaction.amount.toLong().toString()
    }
    val amountDisplay = "$sign$symbol$amountStr"

    var expanded by remember(transaction.id) { mutableStateOf(false) }
    var itemInput by remember(transaction.id) { mutableStateOf(transaction.item) }
    var categoryInput by remember(transaction.id) { mutableStateOf(transaction.category) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (borderColor != Color.Transparent) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (transaction.status == TransactionStatus.PENDING_EDIT)
                    Modifier.clickable { expanded = !expanded }
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(transaction.item, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.feed_category_date, transaction.category, dateStr), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                val amountColor = when (transaction.txType) {
                    "income"                 -> appColors.green
                    "transfer", "investment" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else                     -> appColors.red
                }
                Text(amountDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = amountColor)
                Text(badgeText, fontSize = 11.sp, color = badgeColor)
                transaction.wallet?.let { w ->
                    Text(
                        if (w == "MANDIRI_CC") "CC" else w,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp)) {
                OutlinedTextField(
                    value = itemInput,
                    onValueChange = { itemInput = it },
                    label = { Text(stringResource(R.string.feed_card_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text(stringResource(R.string.feed_card_category_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.action_dismiss), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            onConfirm(itemInput, categoryInput)
                            expanded = false
                        },
                        enabled = itemInput.isNotBlank() && categoryInput.isNotBlank()
                    ) {
                        Text(stringResource(R.string.feed_card_confirm_sync))
                    }
                }
            }
        }

        if (transaction.status == TransactionStatus.SYNC_FAILED) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_retry), color = appColors.blue, fontSize = 12.sp)
            }
        }
    }
}
