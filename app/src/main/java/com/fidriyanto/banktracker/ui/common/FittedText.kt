package com.fidriyanto.banktracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

// Picks the largest style from a list that fits the text on one line in the width given.
//
// A money figure beside a ring has little room, and the app's amounts run to eight digits. Guessing
// the size from the number of characters is not good enough: it clipped "Rp 4,821,852" down to
// "Rp". This measures instead, so the figure always fits.
//
// The candidates are roles from the type scale, in descending size, so a figure that has to shrink
// still lands on the scale rather than on some fraction of it.
//
// Pass the settled value, never a value that is counting up: the size is then stable for the whole
// animation instead of stepping while the digits grow.
@Composable
fun rememberFittedStyle(text: String, maxWidth: Dp, candidates: List<TextStyle>): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, maxWidth, candidates, density) {
        val maxPx = with(density) { maxWidth.roundToPx() }
        if (maxPx <= 0) {
            candidates.first()
        } else {
            candidates.firstOrNull { style ->
                measurer.measure(
                    text = AnnotatedString(text),
                    style = style,
                    softWrap = false,
                    maxLines = 1,
                ).size.width <= maxPx
            } ?: candidates.last()
        }
    }
}
