package com.fidriyanto.banktracker.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: first-run-setup, setup-storage-choice - a wizard of one question per page for a new user: where to keep
// the data, the server when the data lives on one, the home currency, the day the
// pay cycle starts, the accounts, the exchange rates when needed, and a last page that shows the answers.
// The pages slide in and out, the progress bar glides, and the content rises in one item after the other.
@Composable
fun SetupScreen(onDone: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(s.progress, tween(450), label = "progress")
    BackHandler(enabled = s.step != SetupStep.WELCOME) { viewModel.back() }
    val glow = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(glow, MaterialTheme.colorScheme.background)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = Space.xl)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(s.step != SetupStep.WELCOME) {
                    IconButton(onClick = { viewModel.back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(6.dp).clip(AppShape.pill))
                AnimatedVisibility(s.step != SetupStep.WELCOME) { Box(Modifier.width(12.dp)) }
            }
            AnimatedContent(
                targetState = s.step,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = Space.lg),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(380)) { dir * it / 3 } + fadeIn(tween(380))) togetherWith
                        (slideOutHorizontally(tween(380)) { -dir * it / 3 } + fadeOut(tween(220)))
                },
                label = "step",
            ) { step ->
                when (step) {
                    SetupStep.WELCOME -> WelcomePage()
                    SetupStep.STORAGE -> StoragePage(s, viewModel, onDone)
                    SetupStep.SERVER -> ServerPage(s, viewModel)
                    SetupStep.CURRENCY -> CurrencyPage(s, viewModel)
                    SetupStep.CYCLE -> CyclePage(s, viewModel)
                    SetupStep.ACCOUNTS -> AccountsPage(s, viewModel)
                    SetupStep.RATES -> RatesPage(s, viewModel)
                    SetupStep.DONE -> DonePage(s)
                }
            }
            if (s.failed) Text(stringResource(R.string.setup_error_save), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = Space.sm))
            if (s.step == SetupStep.RATES) {
                TextButton(onClick = { viewModel.next() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.setup_skip)) }
            }
            Button(
                onClick = { if (s.step == SetupStep.DONE || (s.step == SetupStep.SERVER && s.serverHasData)) viewModel.finish(onDone) else viewModel.next() },
                enabled = s.canContinue && !s.saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = AppShape.xxl,
            ) {
                if (s.saving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else AnimatedContent(s.step, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "cta") { step ->
                    Text(
                        stringResource(
                            when {
                                step == SetupStep.WELCOME -> R.string.setup_start
                                step == SetupStep.DONE || (step == SetupStep.SERVER && s.serverHasData) -> R.string.setup_open
                                else -> R.string.setup_continue
                            },
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Box(Modifier.height(16.dp))
        }
    }
}
