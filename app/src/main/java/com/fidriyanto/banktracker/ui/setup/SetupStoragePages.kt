package com.fidriyanto.banktracker.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.BackupOutcome
import com.fidriyanto.banktracker.domain.model.ServerCheck
import com.fidriyanto.banktracker.domain.model.StorageMode
import com.fidriyanto.banktracker.ui.common.Entrance
import com.fidriyanto.banktracker.ui.theme.Space

// Some file apps report a JSON file as plain text or as bytes, so the picker accepts those too.
private val BACKUP_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

// ac: setup-storage-choice - the page that asks where to keep the data: on the phone, or on a server.
// With the phone chosen, the user can also start from a backup file.
@Composable
fun StoragePage(s: SetupFormState, vm: SetupViewModel, onDone: () -> Unit) {
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.restoreBackup(it.toString(), onDone) }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PageTitle(R.string.setup_storage_title, R.string.setup_storage_body)
        Entrance(2) {
            StorageOption(
                Icons.Outlined.PhoneAndroid, R.string.setup_storage_local_title, R.string.setup_storage_local_body,
                selected = s.storage == StorageMode.LOCAL,
            ) { vm.chooseStorage(StorageMode.LOCAL) }
        }
        Entrance(3) {
            StorageOption(
                Icons.Outlined.Dns, R.string.setup_storage_server_title, R.string.setup_storage_server_body,
                selected = s.storage == StorageMode.SERVER,
            ) { vm.chooseStorage(StorageMode.SERVER) }
        }
        if (s.storage == StorageMode.LOCAL) {
            TextButton(onClick = { restoreLauncher.launch(BACKUP_TYPES) }, enabled = !s.restoring, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (s.restoring) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.setup_storage_restore))
            }
        }
        s.restoreResult?.let { RestoreResultText(it) }
    }
}

@Composable
private fun RestoreResultText(result: BackupOutcome) {
    val text = when (result) {
        BackupOutcome.NOT_A_BACKUP -> R.string.backup_not_a_backup
        BackupOutcome.TOO_NEW -> R.string.backup_too_new
        BackupOutcome.DONE, BackupOutcome.FAILED -> R.string.backup_failed
    }
    Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun StorageOption(icon: ImageVector, title: Int, body: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant)
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), border = border) {
        Row(Modifier.padding(Space.lg), horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = if (selected) colors.primary else colors.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

// ac: setup-storage-choice - the page for the server address and token, with a test of the connection.
// Continue stays off until the test passes.
@Composable
fun ServerPage(s: SetupFormState, vm: SetupViewModel) {
    var showToken by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PageTitle(R.string.setup_server_title, R.string.setup_server_body)
        Entrance(2) {
            OutlinedTextField(
                value = s.serverAddress, onValueChange = vm::setServerAddress, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.setup_server_address)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }
        Entrance(3) {
            OutlinedTextField(
                value = s.serverToken, onValueChange = vm::setServerToken, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.setup_server_token)) },
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(if (showToken) R.string.setup_server_hide_token else R.string.setup_server_show_token),
                        )
                    }
                },
            )
        }
        Entrance(4) {
            OutlinedButton(
                onClick = vm::checkServer, modifier = Modifier.fillMaxWidth(),
                enabled = s.serverAddress.isNotBlank() && s.serverToken.isNotBlank() && !s.checking,
            ) {
                if (s.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.setup_server_test))
            }
        }
        s.serverCheck?.let { ServerCheckText(it) }
    }
}

@Composable
private fun ServerCheckText(check: ServerCheck) {
    val text = when (check) {
        is ServerCheck.Ok ->
            if (check.accounts == 0) stringResource(R.string.setup_server_ok_empty)
            else pluralStringResource(R.plurals.setup_server_ok_data, check.accounts, check.accounts)
        ServerCheck.BadToken -> stringResource(R.string.setup_server_bad_token)
        ServerCheck.NotFinTrack -> stringResource(R.string.setup_server_not_fintrack)
        ServerCheck.Unreachable -> stringResource(R.string.setup_server_unreachable)
        ServerCheck.BadAddress -> stringResource(R.string.setup_server_bad_address)
        ServerCheck.NeedsHttps -> stringResource(R.string.setup_server_needs_https)
    }
    val colors = MaterialTheme.colorScheme
    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (check is ServerCheck.Ok) colors.primary else colors.error)
}
