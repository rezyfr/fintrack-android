package com.fidriyanto.banktracker.ui.feed

import com.fidriyanto.banktracker.domain.model.formatMoney
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.domain.model.isZeroDecimalCurrency
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.categoryIcon
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import com.fidriyanto.banktracker.ui.theme.LocalIsDarkTheme
import com.fidriyanto.banktracker.ui.theme.categoryColor
import com.fidriyanto.banktracker.ui.theme.categoryInk

// ac: transaction-category-icons — each row shows its category icon in a tinted rounded square
// ac: category-colour-everywhere — the tint is the category's own chart colour, so a row, its donut
// arc and its budget bar agree. The icon uses the darker companion, which carries contrast on a
// light surface. The old palette picked a colour from the name's hash and matched nothing.
@Composable
internal fun CategoryAvatar(category: String) {
    val onDark = LocalIsDarkTheme.current
    val tint = categoryColor(category)
    val ink = categoryInk(category, onDark)
    Box(
        modifier = Modifier.size(38.dp).clip(AppShape.md).background(tint.copy(alpha = if (onDark) 0.22f else 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoryIcon(category), contentDescription = category, tint = ink, modifier = Modifier.size(20.dp))
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
    noBudget: Boolean = false,
) {
    val appColors = LocalAppColors.current
    val borderColor = when (transaction.status) {
        TransactionStatus.PENDING_EDIT -> appColors.blue
        TransactionStatus.PENDING_SYNC -> appColors.warning
        TransactionStatus.SYNC_FAILED -> MaterialTheme.colorScheme.error
        TransactionStatus.SYNCED -> Color.Transparent
    }
    val badgeText = when (transaction.status) {
        TransactionStatus.PENDING_EDIT -> "Pending"
        TransactionStatus.SYNCED -> "Synced"
        TransactionStatus.PENDING_SYNC -> "Queued"
        TransactionStatus.SYNC_FAILED -> "Failed"
    }
    val badgeColor = when (transaction.status) {
        TransactionStatus.PENDING_EDIT -> appColors.blue
        TransactionStatus.SYNCED -> appColors.green
        TransactionStatus.PENDING_SYNC -> appColors.warning
        TransactionStatus.SYNC_FAILED -> MaterialTheme.colorScheme.error
    }
    val date = runCatching { LocalDate.parse(transaction.dateIso) }.getOrNull()
    val dateStr = date?.format(DateTimeFormatter.ofPattern("d MMM")) ?: ""

    // ac: configurable-currency-pair — currency comes from the transaction's account instead of a
    // hardcoded "BBL" wallet id check; a null wallet keeps its prior fallback to the foreign currency
    val currencySettings = CurrencySettingsStore.current
    val txCurrency = AccountStore.currencyOf(transaction.wallet, if (transaction.wallet == null) currencySettings.noWalletCurrency else currencySettings.homeCode)
    val amountDisplay = formatMoney(
        txCurrency,
        if (transaction.txType in setOf("income", "transfer", "investment")) transaction.amount else -transaction.amount,
        showSign = transaction.txType == "income",
    )

    var expanded by remember(transaction.id) { mutableStateOf(false) }
    var itemInput by remember(transaction.id) { mutableStateOf(transaction.item) }
    var categoryInput by remember(transaction.id) { mutableStateOf(transaction.category) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (borderColor != Color.Transparent) 1.dp else 0.dp, borderColor, AppShape.md)
            .combinedClickable(
                onClick = {
                    if (transaction.status == TransactionStatus.PENDING_EDIT) expanded = !expanded
                    else onClick()
                },
                onLongClick = onLongClick,
            ),
        shape = AppShape.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(transaction.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.item, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                // ac: add-transaction-subcategory — show the subcategory next to the category when set
                val categoryLabel = transaction.subcategory?.takeIf { it.isNotBlank() }
                    ?.let { "${transaction.category} · $it" } ?: transaction.category
                Text(stringResource(R.string.feed_category_date, categoryLabel, dateStr), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // ac: budget-by-category — an expense whose category has no budget in its book
                if (noBudget) {
                    Text(stringResource(R.string.feed_no_budget), style = MaterialTheme.typography.labelSmall, color = appColors.warning)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                // ac: red-means-a-problem — an expense is the normal case and stays neutral, with the
                // minus sign carrying the direction. Red is reserved for over budget, overdue and owed.
                val amountColor = when (transaction.txType) {
                    "income" -> appColors.green
                    "transfer", "investment" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(amountDisplay, style = MaterialTheme.typography.titleMedium, color = amountColor)
                if (transaction.status != TransactionStatus.SYNCED) {
                    Text(badgeText, style = MaterialTheme.typography.bodySmall, color = badgeColor)
                }
                transaction.wallet?.let { w ->
                    Text(
                        AccountStore.nameOf(w),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = Space.md, top = Space.xs, end = Space.md, bottom = Space.md)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
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
                Text(stringResource(R.string.action_retry), color = appColors.blue, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
