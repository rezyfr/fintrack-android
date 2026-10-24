package com.fidriyanto.banktracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space

// ac: insights-combined-currency — pill toggle shared by Budget and Insights. options holds a key and
// its label for each pill.
@Composable
fun <T> CurrencyToggle(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, tagFor: ((T) -> String)? = null) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        options.forEach { (key, label) ->
            val on = key == selected
            Surface(
                color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                shape = AppShape.pill,
                modifier = Modifier.clip(AppShape.pill),
            ) {
                Text(
                    label,
                    color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.then(tagFor?.let { Modifier.testTag(it(key)) } ?: Modifier)
                        .clickable { onSelect(key) }.padding(horizontal = Space.lg, vertical = Space.sm),
                )
            }
        }
    }
}
