package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun LocalDate.label(): String = format(shortDate)

@Composable
fun IconBadge(icon: ImageVector, size: Int = 34) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size((size / 2).dp))
    }
}

// A tappable row that opens a menu, such as the account a payment is taken from
@Composable
fun AccountRow(icon: ImageVector, label: String, selected: Account?, accounts: List<Account>, onPick: (Account) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Card(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                IconBadge(icon)
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(selected?.displayName ?: stringResource(R.string.schedule_pick_account), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            accounts.forEach { a -> DropdownMenuItem(text = { Text(stringResource(R.string.schedule_account_option, a.displayName, a.currency)) }, onClick = { onPick(a); open = false }) }
        }
    }
}

// The warning shown when auto-post is turned on. The app does not compare a scheduled payment with the
// bank notifications it captures, so the same payment can be recorded twice.
@Composable
fun AutoPostWarning(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LocalAppColors.current.red) },
        title = { Text(stringResource(R.string.schedule_warn_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(stringResource(R.string.schedule_warn_body))
                Text(stringResource(R.string.schedule_warn_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.schedule_warn_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.schedule_warn_cancel)) } },
    )
}
