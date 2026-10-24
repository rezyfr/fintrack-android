package com.fidriyanto.banktracker.ui.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.domain.model.SetupStatus

// ac: first-run-setup - shows the setup screen to a user with no accounts. Everyone else goes to the app.
@Composable
fun SetupGate(viewModel: SetupGateViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    when (status) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        SetupStatus.NEEDED -> SetupScreen(onDone = viewModel::onSetupFinished)
        SetupStatus.DONE -> content()
    }
}
