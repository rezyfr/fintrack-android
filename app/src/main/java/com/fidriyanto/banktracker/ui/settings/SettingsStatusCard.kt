package com.fidriyanto.banktracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.Space

// The top of Settings: whether the app can read bank notifications. When it cannot, the card says so and has the fix.
@Composable
fun SettingsStatusCard(active: Boolean, onEnable: () -> Unit) {
    val container = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(Modifier.fillMaxWidth().testTag("settings_status"), colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                StatusDot(if (active) LocalAppColors.current.green else MaterialTheme.colorScheme.error)
                Text(stringResource(if (active) R.string.settings_status_on_title else R.string.settings_status_off_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(stringResource(if (active) R.string.settings_status_on_body else R.string.settings_status_off_body), style = MaterialTheme.typography.bodyMedium)
            if (!active) OutlinedButton(onClick = onEnable, Modifier.testTag("settings_enable_access")) { Text(stringResource(R.string.settings_enable_access)) }
        }
    }
}
