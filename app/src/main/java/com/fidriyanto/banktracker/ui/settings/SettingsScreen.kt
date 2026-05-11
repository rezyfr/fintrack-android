package com.fidriyanto.banktracker.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.Destructive

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var claudeKeyInput by remember(state.claudeApiKey) { mutableStateOf(state.claudeApiKey) }
    var showKey by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Google Account", fontWeight = FontWeight.SemiBold, color = Color.White)
                if (state.isSignedIn) {
                    Text(state.accountEmail, fontSize = 13.sp, color = Color.Gray)
                    OutlinedButton(onClick = { viewModel.signOut() }) { Text("Sign Out") }
                } else {
                    Text("Not signed in", fontSize = 13.sp, color = Color.Gray)
                    Button(onClick = {
                        signInLauncher.launch(viewModel.getSignInIntent())
                    }) { Text("Sign in with Google") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Claude API Key", fontWeight = FontWeight.SemiBold, color = Color.White)
                OutlinedTextField(
                    value = claudeKeyInput,
                    onValueChange = { claudeKeyInput = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show", fontSize = 12.sp)
                        }
                    }
                )
                Button(onClick = { viewModel.saveClaudeKey(claudeKeyInput) }) { Text("Save") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notification Listener", fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dotColor = if (state.isListenerActive) Accent else Destructive
                    Text("●", color = dotColor, fontSize = 18.sp)
                    Text(if (state.isListenerActive) "Listening" else "Inactive", color = Color.White)
                }
                if (!state.isListenerActive) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text("Enable Access") }
                }
            }
        }

        Button(
            onClick = { viewModel.retryPendingSyncs() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSyncing
        ) {
            if (state.isSyncing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
            else Text("Retry Pending Syncs")
        }
    }
}
