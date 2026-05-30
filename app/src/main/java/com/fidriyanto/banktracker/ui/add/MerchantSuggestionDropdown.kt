package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    DropdownMenu(
        expanded = suggestions.isNotEmpty(),
        onDismissRequest = {},
        modifier = Modifier.fillMaxWidth(),
    ) {
        suggestions.forEach { merchant ->
            DropdownMenuItem(
                text = { Text(merchant) },
                onClick = { onSelect(merchant) },
            )
        }
    }
}
