package com.fidriyanto.banktracker.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val avatarPalette = listOf(
    0xFF0F5D4C, 0xFF2155C4, 0xFF9A6B12, 0xFFB23A2E, 0xFF6D4BC4, 0xFF0F9B76, 0xFFC43062,
)

@Composable
private fun CategoryAvatar(category: String) {
    val idx = (category.hashCode().let { if (it < 0) -it else it }) % avatarPalette.size
    val base = Color(avatarPalette[idx])
    val initial = category.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier.size(38.dp).clip(CircleShape).background(base.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = base, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    transaction: TransactionUiModel,
    onRetry: () -> Unit,
    onConfirm: (item: String, category: String) -> Unit,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
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
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (transaction.amount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }.format(transaction.amount)
    } else {
        NumberFormat.getNumberInstance(Locale.US).format(transaction.amount.toLong())
    }
    val amountDisplay = "$sign$symbol$amountStr"

    var expanded by remember(transaction.id) { mutableStateOf(false) }
    var itemInput by remember(transaction.id) { mutableStateOf(transaction.item) }
    var categoryInput by remember(transaction.id) { mutableStateOf(transaction.category) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (borderColor != Color.Transparent) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (transaction.status == TransactionStatus.PENDING_EDIT) expanded = !expanded
                    else onClick()
                },
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(transaction.category)
            Spacer(Modifier.width(12.dp))
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
                if (transaction.status != TransactionStatus.SYNCED) {
                    Text(badgeText, fontSize = 11.sp, color = badgeColor)
                }
                transaction.wallet?.let { w ->
                    Text(
                        when (w) {
                            "MANDIRI_CC" -> "Mandiri Credit Card"
                            "BCA_CC"     -> "BCA Credit Card"
                            else         -> w
                        },
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
                        Text(stringResource(R.string.feed_card_confirm))
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
