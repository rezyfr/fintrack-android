package com.fidriyanto.banktracker.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.animation.core.animateFloat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val orbitSymbols = listOf("$", "€", "£", "¥")

// The welcome picture: a pulsing round badge with four currency symbols that circle around it.
@Composable
fun PulsingEmblem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "emblem")
    val pulse by transition.animateFloat(
        1f, 1.1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "turn")
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(210.dp).scale(pulse).clip(CircleShape).background(primary.copy(alpha = 0.09f)))
        Box(Modifier.size(150.dp).scale(2.1f - pulse).clip(CircleShape).background(primary.copy(alpha = 0.15f)))
        Box(Modifier.size(88.dp).clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp).rotate(turn / 2))
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val radius = with(density) { 96.dp.toPx() }
        orbitSymbols.forEachIndexed { i, symbol ->
            val angle = Math.toRadians((turn + i * 90.0))
            Box(
                Modifier
                    .offset { IntOffset((cos(angle) * radius).roundToInt(), (sin(angle) * radius).roundToInt()) }
                    .size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(symbol, color = primary, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

// The finish picture: a round badge that springs in, then a check mark that draws itself.
@Composable
fun AnimatedCheck(modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.5f) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        draw.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val fill = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier.size(128.dp).scale(scale.value)) {
        drawCircle(fill)
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.68f)
            lineTo(size.width * 0.73f, size.height * 0.35f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * draw.value, part, true)
        drawPath(part, mark, style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
