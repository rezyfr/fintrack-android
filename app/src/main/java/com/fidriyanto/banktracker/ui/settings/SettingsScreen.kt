package com.fidriyanto.banktracker.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.LocalAppColors
import com.fidriyanto.banktracker.ui.theme.LocalIsDarkTheme
import com.fidriyanto.banktracker.ui.theme.LocalThemeToggle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val toggleTheme = LocalThemeToggle.current
    val appColors = LocalAppColors.current

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_dark_theme), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = isDark, onCheckedChange = { toggleTheme() })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_notification_listener), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dotColor = if (state.isListenerActive) appColors.green else MaterialTheme.colorScheme.error
                    Text("●", color = dotColor, fontSize = 18.sp)
                    Text(if (state.isListenerActive) "Listening" else "Inactive", color = MaterialTheme.colorScheme.onSurface)
                }
                if (!state.isListenerActive) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text(stringResource(R.string.settings_enable_access)) }
                }
            }
        }

        Button(
            onClick = { viewModel.retryPendingSyncs() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSyncing && !state.isClearing
        ) {
            if (state.isSyncing) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.settings_retry_pending_syncs))
        }

        OutlinedButton(
            onClick = { viewModel.markAllSynced() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSyncing && !state.isClearing
        ) {
            if (state.isClearing) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary)
            else Text(stringResource(R.string.settings_mark_all_synced))
        }
    }
}
