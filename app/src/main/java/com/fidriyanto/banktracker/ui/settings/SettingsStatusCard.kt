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

// The top of Settings: whether the app is reading bank notifications. It says so when the access is off, and when the
// access is on but Android has stopped the listener. Each of those has its own button.
@Composable
fun SettingsStatusCard(status: ListenerStatus, onEnable: () -> Unit, onReconnect: () -> Unit) {
    val ok = status == ListenerStatus.RUNNING
    val container = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    val (title, body) = when (status) {
        ListenerStatus.RUNNING -> R.string.settings_status_on_title to R.string.settings_status_on_body
        ListenerStatus.STOPPED -> R.string.settings_status_stopped_title to R.string.settings_status_stopped_body
        ListenerStatus.NO_ACCESS -> R.string.settings_status_off_title to R.string.settings_status_off_body
    }
    Card(Modifier.fillMaxWidth().testTag("settings_status"), colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                StatusDot(if (ok) LocalAppColors.current.green else MaterialTheme.colorScheme.error)
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            when (status) {
                ListenerStatus.NO_ACCESS -> OutlinedButton(onClick = onEnable, Modifier.testTag("settings_enable_access")) { Text(stringResource(R.string.settings_enable_access)) }
                ListenerStatus.STOPPED -> OutlinedButton(onClick = onReconnect, Modifier.testTag("settings_reconnect")) { Text(stringResource(R.string.settings_reconnect)) }
                ListenerStatus.RUNNING -> Unit
            }
        }
    }
}
