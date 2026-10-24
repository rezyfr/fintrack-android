package com.fidriyanto.banktracker.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.AiModelState
import java.util.Locale
import com.fidriyanto.banktracker.ui.theme.Space

private fun gb(bytes: Long) = String.format(Locale.US, "%.1f", bytes / 1_000_000_000.0)

// ac: on-device-ai — shows whether the model is on the phone, and downloads or removes it
@Composable
fun AiModelCard(
    state: AiModelState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(stringResource(R.string.ai_model_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            when (state) {
                AiModelState.NotInstalled -> {
                    Text(stringResource(R.string.ai_model_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_model_download)) }
                }
                is AiModelState.Downloading -> {
                    LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.ai_model_progress, state.percent, gb(state.doneBytes), gb(state.totalBytes)),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                    }
                }
                AiModelState.Ready -> {
                    Text(stringResource(R.string.ai_model_ready), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_model_delete)) }
                }
                is AiModelState.Failed -> {
                    Text(stringResource(R.string.ai_model_failed, state.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_model_retry)) }
                }
            }
        }
    }
}
