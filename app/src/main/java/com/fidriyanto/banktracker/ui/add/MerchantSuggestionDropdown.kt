package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.LocalAppColors

// ac: merchant-autocomplete — focusing the merchant field shows a dropdown of up to 10 previously saved merchants
// ac: merchant-autocomplete — typing filters the list to merchants containing the typed string (case-insensitive)
// ac: merchant-autocomplete — selecting a suggestion fills the merchant field and closes the dropdown
// ac: merchant-autocomplete — if no history exists or no suggestions match the current input, no dropdown is shown
// ac: merchant-autocomplete — a merchant name is saved to history only when a transaction is successfully submitted
// ac: merchant-autocomplete — history is deduplicated — the same merchant appears at most once, ordered most-recent first
// ac: merchant-autocomplete — history is capped at 10 entries; adding an 11th drops the oldest
@Composable
fun MerchantSuggestionDropdown(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    val appColors = LocalAppColors.current
    val surface   = MaterialTheme.colorScheme.surface
    val outline   = MaterialTheme.colorScheme.outline

    DropdownMenu(
        expanded = suggestions.isNotEmpty(),
        onDismissRequest = {},
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surface),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = appColors.gold,
                )
                Text(
                    text = stringResource(R.string.merchant_suggestions_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.gold,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = outline,
            )

            suggestions.forEach { merchant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(merchant) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
