package com.fidriyanto.banktracker.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The soft highlight in the top-right corner of a pine hero card.
//
// This used to be a 150dp child Box pushed out of view with an offset. An offset does not change
// what the parent measured, so the circle set the card's minimum height and left dead space under
// short content. Drawing it instead keeps it out of the layout entirely.
fun Modifier.heroGlow(color: Color): Modifier = drawBehind {
    val radius = 75.dp.toPx()
    val inset = 22.dp.toPx()
    drawCircle(
        color = color.copy(alpha = 0.07f),
        radius = radius,
        center = Offset(size.width + inset, -inset),
    )
}
