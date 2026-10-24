package com.fidriyanto.banktracker.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.domain.model.AskTable
import com.fidriyanto.banktracker.ui.theme.Space

// What a tool found, as a list: a title, the period, and one line per row with the value on the right.
@Composable
fun AskTableView(table: AskTable) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(table.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        table.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        table.sections.forEach { section ->
            section.heading?.let { Text(it, Modifier.padding(top = Space.sm), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            section.rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(vertical = Space.xs), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Column(Modifier.weight(1f)) {
                        Text(row.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        row.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text(row.value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
