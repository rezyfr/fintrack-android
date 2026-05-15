package com.fidriyanto.banktracker.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.ui.theme.MutedText

@Composable
fun FeedScreen(viewModel: FeedViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Transactions", fontWeight = FontWeight.Bold, fontSize = 20.sp,
            color = Color.White, modifier = Modifier.padding(vertical = 16.dp)
        )
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No transactions yet.\nBangkok Bank notifications will appear here.",
                    color = MutedText, fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { entity ->
                    TransactionCard(
                        entity = entity,
                        onRetry = { viewModel.retry(entity.id) },
                        onConfirm = { item, category -> viewModel.updateAndSync(entity.id, item, category) }
                    )
                }
            }
        }
    }
}
