package com.fidriyanto.banktracker.ui.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.AiModelState
import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.ui.ai.AiModelCard
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: on-device-ai — ask about your spending in plain words. The model runs on this phone.
@Composable
fun AskScreen(onClose: () -> Unit, viewModel: AskViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val examples = listOf(R.string.ask_example_food, R.string.ask_example_budget, R.string.ask_example_top, R.string.ask_example_compare)

    LaunchedEffect(state.messages.size, state.loading) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size)
    }

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding().padding(horizontal = Space.lg)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.ask_title), style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close)) }
        }
        if (modelState != AiModelState.Ready) {
            Text(stringResource(R.string.ask_needs_model), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = Space.sm))
            AiModelCard(modelState, viewModel::download, viewModel::cancelDownload, viewModel::deleteModel)
            return@Column
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(Space.md)) {
            if (state.messages.isEmpty()) item {
                Text(stringResource(R.string.ask_privacy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    examples.forEach { res ->
                        val text = stringResource(res)
                        AssistChip(onClick = { viewModel.send(text) }, label = { Text(text) })
                    }
                }
            }
            items(state.messages) { Bubble(it) }
            item {
                if (state.loading) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.ask_thinking), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.failed) Text(stringResource(R.string.ask_error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Space.sm), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.ask_hint)) }, maxLines = 3, shape = AppShape.xl,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.send(input); input = "" }),
            )
            IconButton(onClick = { viewModel.send(input); input = "" }, enabled = input.isNotBlank() && !state.loading) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.ask_send))
            }
        }
    }
}

@Composable
private fun Bubble(message: AskMessage) {
    val mine = message.fromUser
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = if (message.tables.isEmpty()) 320.dp else 340.dp).background(
                if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                AppShape.xl,
            ).padding(horizontal = Space.lg, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            if (message.text.isNotBlank()) Text(message.text, color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            message.tables.forEach { AskTableView(it) }
            message.source?.let {
                Text(stringResource(R.string.ask_source, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
