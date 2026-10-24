package com.fidriyanto.banktracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.AutoSource
import com.fidriyanto.banktracker.domain.model.formatMoney
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: auto-added-notice - a small card at the top of Home. It shows the newest transaction the app added by itself and
// how many more there are. The close button dismisses all of them. A tap opens the Transactions tab.
@Composable
fun AutoAddedNotice(onView: () -> Unit, modifier: Modifier = Modifier, viewModel: AutoAddedNoticeViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val first = items.firstOrNull() ?: return
    val signed = when (first.txType) {
        "income" -> first.amount
        "expense" -> -first.amount
        else -> first.amount
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer, shape = AppShape.lg,
        modifier = modifier.fillMaxWidth().testTag("auto_added_notice").clickable { viewModel.dismiss(); onView() },
    ) {
        Row(Modifier.padding(start = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f).padding(vertical = Space.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(first.item, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(formatMoney(AccountStore.currencyOf(first.wallet), signed, showSign = first.txType != "transfer"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                val source = stringResource(if (first.source == AutoSource.SCHEDULED) R.string.auto_notice_scheduled else R.string.auto_notice_notification)
                Text(
                    if (items.size > 1) stringResource(R.string.auto_notice_source_more, source, pluralStringResource(R.plurals.auto_notice_more, items.size - 1, items.size - 1)) else source,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            IconButton(onClick = viewModel::dismiss, Modifier.testTag("auto_added_dismiss")) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.auto_notice_dismiss), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}
