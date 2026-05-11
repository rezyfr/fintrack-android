package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TransactionCard(entity: TransactionEntity, onRetry: () -> Unit) {
    val borderColor = when (entity.status) {
        TransactionStatus.PENDING_EDIT -> Primary
        TransactionStatus.PENDING_SYNC -> Warning
        TransactionStatus.SYNC_FAILED -> Destructive
        TransactionStatus.SYNCED -> Color.Transparent
    }
    val badgeText = when (entity.status) {
        TransactionStatus.PENDING_EDIT -> "Pending"
        TransactionStatus.SYNCED -> "Synced"
        TransactionStatus.PENDING_SYNC -> "Queued"
        TransactionStatus.SYNC_FAILED -> "Failed"
    }
    val badgeColor = when (entity.status) {
        TransactionStatus.PENDING_EDIT -> Secondary
        TransactionStatus.SYNCED -> Accent
        TransactionStatus.PENDING_SYNC -> Warning
        TransactionStatus.SYNC_FAILED -> Destructive
    }
    val date = runCatching { LocalDate.parse(entity.dateIso) }.getOrNull()
    val dateStr = date?.format(DateTimeFormatter.ofPattern("d MMM")) ?: ""
    val amountStr = if (entity.amount % 1.0 == 0.0) entity.amount.toInt().toString() else entity.amount.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (borderColor != Color.Transparent) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entity.item, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                Text("${entity.category} · $dateStr", fontSize = 12.sp, color = MutedText)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("-฿$amountStr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmountRed)
                Text(badgeText, fontSize = 11.sp, color = badgeColor)
            }
        }
        if (entity.status == TransactionStatus.SYNC_FAILED) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("Retry", color = Secondary, fontSize = 12.sp)
            }
        }
    }
}
