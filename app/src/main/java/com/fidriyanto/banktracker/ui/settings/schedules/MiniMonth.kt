package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.fidriyanto.banktracker.ui.theme.Space
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ac: scheduled-transactions - one month with the dates of the schedule filled in
@Composable
fun MiniMonth(month: LocalDate, highlighted: Set<LocalDate>) {
    val ym = YearMonth.from(month)
    val lead = ym.atDay(1).dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Row {
                DayOfWeek.entries.forEach { d -> Cell(Modifier.weight(1f), d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), false, muted = true) }
            }
            cells.chunked(7).forEach { week ->
                Row {
                    (0 until 7).forEach { i -> Cell(Modifier.weight(1f), week.getOrNull(i)?.dayOfMonth?.toString().orEmpty(), week.getOrNull(i) in highlighted) }
                }
            }
        }
    }
}

@Composable
private fun Cell(modifier: Modifier, text: String, on: Boolean, muted: Boolean = false) {
    Box(modifier.aspectRatio(1f).padding(Space.xs / 2).clip(CircleShape).background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        Text(
            text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            color = if (on) MaterialTheme.colorScheme.onPrimary else if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}
