package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fidriyanto.banktracker.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatIso(iso: String, pattern: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern(pattern, Locale.US))
}.getOrDefault(iso)

// ac: insights-cycle-stepper — shared by Budget and Insights: the selected pay cycle with arrows to
// the previous and next cycle. The cycle is named after the month it ends in. The next arrow is
// disabled on the cycle in progress.
@Composable
fun CycleStepper(fromIso: String, toIso: String, canGoNext: Boolean, onStep: (Int) -> Unit) {
    // ac: haptic-feedback — a light tick when the cycle moves
    val tick = rememberTick()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { tick(); onStep(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.insights_cycle_previous))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatIso(toIso, "MMMM yyyy"), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.feed_cycle_range, formatIso(fromIso, "d MMM"), formatIso(toIso, "d MMM")),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { tick(); onStep(-1) }, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.insights_cycle_next))
        }
    }
}
