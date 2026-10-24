package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.ui.theme.Space

val AccountType.labelRes: Int
    get() = when (this) {
        AccountType.CHECKING -> R.string.account_type_checking
        AccountType.CREDIT -> R.string.account_type_credit
        AccountType.INVESTMENT -> R.string.account_type_investment
    }

// The choice of what an account is. Shared by Manage Accounts and the first-run setup.
@Composable
fun AccountTypeChips(selected: AccountType, onSelect: (AccountType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        AccountType.entries.forEach { type ->
            FilterChip(selected = type == selected, onClick = { onSelect(type) }, label = { Text(stringResource(type.labelRes)) })
        }
    }
}
