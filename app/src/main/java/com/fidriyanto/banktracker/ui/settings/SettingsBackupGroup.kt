package com.fidriyanto.banktracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R

// Some file apps report a JSON file as plain text or as bytes, so the picker accepts those too.
private val BACKUP_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

// ac: offline-backup - the Backup group of Settings. It shows only on an offline install. The import asks
// first, because it replaces the data on the phone.
@Composable
fun SettingsBackupGroup(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmImport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.export(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restore(it.toString()) }
    }
    if (!state.available) return

    SettingsGroup(stringResource(R.string.settings_group_backup)) {
        SettingsRow(
            Icons.Outlined.FileUpload, stringResource(R.string.settings_backup_export), "settings_backup_export",
            { exportLauncher.launch(viewModel.fileName()) }, subtitle = stringResource(R.string.settings_backup_export_hint), busy = state.busy,
        )
        SettingsDivider()
        SettingsRow(
            Icons.Outlined.FileDownload, stringResource(R.string.settings_backup_import), "settings_backup_import",
            { confirmImport = true }, subtitle = stringResource(R.string.settings_backup_import_hint), tone = DiscTone.SECONDARY, busy = state.busy,
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmImport = false; importLauncher.launch(BACKUP_TYPES) }) {
                    Text(stringResource(R.string.backup_import_choose_file))
                }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            text = { Text(stringResource(message.text())) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

private fun BackupMessage.text(): Int = when (this) {
    BackupMessage.EXPORTED -> R.string.backup_exported
    BackupMessage.IMPORTED -> R.string.backup_imported
    BackupMessage.NOT_A_BACKUP -> R.string.backup_not_a_backup
    BackupMessage.TOO_NEW -> R.string.backup_too_new
    BackupMessage.FAILED -> R.string.backup_failed
}
