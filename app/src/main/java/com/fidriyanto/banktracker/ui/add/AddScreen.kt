package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.categorization.ClaudeCategorizor
import com.fidriyanto.banktracker.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(viewModel: AddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Transaction", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)

        ToggleRow("Account", listOf("THB", "IDR"), state.account) {
            viewModel.update { copy(account = it) }
        }

        ToggleRow("Type", listOf("Expense", "Income"), state.type) {
            viewModel.update { copy(type = it) }
        }

        OutlinedTextField(
            value = state.amount, onValueChange = { viewModel.update { copy(amount = it) } },
            label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = state.description, onValueChange = { viewModel.update { copy(description = it) } },
            label = { Text("Description / Item") }, modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.category, onValueChange = {},
                readOnly = true, label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ClaudeCategorizor.CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { viewModel.update { copy(category = cat) }; expanded = false }
                    )
                }
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        state.successMessage?.let { Text(it, color = Accent, fontSize = 12.sp) }

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            else Text("Sync to Sheets", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt) })
            }
        }
    }
}
