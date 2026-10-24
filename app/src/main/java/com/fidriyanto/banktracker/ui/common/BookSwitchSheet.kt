package com.fidriyanto.banktracker.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.domain.model.currencySymbol
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SLIDE_MS = 280
private const val SHEET_DELAY_MS = 250L
private const val MIN_SPIN_MS = 700L
private const val HOLD_MS = 450L
private const val MAX_WAIT_MS = 5000L
private const val SCRIM_ALPHA = 0.4f

// ac: book-switch-sheet — switching the book changes it at once. If the page behind is still loading
// after a short moment, a sheet rises that says which book it switched to, with a ring turning around
// the badge. When the data is there the ring closes, the badge turns to a check, and the sheet leaves,
// so the page is already loaded when the sheet is gone. A page that is ready at once, such as one that
// filters data the app already holds, gets no sheet.
@Composable
fun BookSwitchSheet(routeKey: String?) {
    val requested by BookTransition.requested.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    // The book on the sheet stays the one that was picked, even if the sheet is still leaving.
    var shown by remember { mutableStateOf(requested) }
    val route by rememberUpdatedState(routeKey)
    val confirm = rememberConfirm()

    LaunchedEffect(requested) {
        if (requested == BookStore.book.value) {
            visible = false
            return@LaunchedEffect
        }
        BookStore.select(requested)
        // One frame lets the pages see the new book and report that they are loading.
        withFrameNanos { }
        val key = route
        // A page that is ready within a moment needs no sheet. The switch is already done.
        if (key == null || BookLoad.awaitIdle(key, SHEET_DELAY_MS)) {
            visible = false
            return@LaunchedEffect
        }
        // A real wait. The sheet covers the page until the data is there, and stays long enough to read.
        shown = requested
        ready = false
        visible = true
        val raised = System.currentTimeMillis()
        BookLoad.awaitIdle(key, MAX_WAIT_MS)
        val shownFor = System.currentTimeMillis() - raised
        val least = SLIDE_MS + MIN_SPIN_MS
        if (shownFor < least) delay(least - shownFor)
        ready = true
        confirm()
        delay(HOLD_MS)
        visible = false
    }

    // The sheet cannot be dismissed early, and the page behind it ignores touches until it is gone.
    BackHandler(enabled = visible) { }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(SLIDE_MS)), exit = fadeOut(tween(SLIDE_MS))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it },
            exit = slideOutVertically(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it },
        ) {
            SheetBody(shown, ready)
        }
    }
}

@Composable
private fun SheetBody(book: Book, ready: Boolean) {
    val label = book.code?.let { CurrencySettingsStore.current.labelFor(it) } ?: stringResource(R.string.currency_combined)
    val symbol = book.code?.let { currencySymbol(it).trim() }
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = AppShape.sheet,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            // The sheet fills the screen, so the area around the badge carries a faint pattern of the
            // book's own currency mark. Combined has no mark of its own and uses the generic sign.
            SymbolPattern(symbol ?: GENERIC_SIGN)
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Space.xl).padding(bottom = Space.xxl * 2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SwitchEmblem(symbol = symbol, ready = ready)
                Spacer(Space.xxl)
                // A screen reader announces the switch when the sheet appears.
                Column(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    Text(
                        stringResource(R.string.book_switched_to, label),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    AnimatedContent(targetState = ready, transitionSpec = { swap() }, label = "status") { done ->
                        Text(
                            stringResource(if (done) R.string.book_switch_ready else R.string.book_switch_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private const val GENERIC_SIGN = "¤"

// The old content leaves before the new content arrives, so the two never overlap on screen.
private fun swap(): ContentTransform = fadeIn(tween(160, delayMillis = 120)) togetherWith fadeOut(tween(120))

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) = androidx.compose.foundation.layout.Spacer(Modifier.size(height))

// Rows of the currency mark, every other row shifted half a step, drawn very faintly.
@Composable
private fun SymbolPattern(symbol: String) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.primary.copy(alpha = PATTERN_ALPHA))
    Canvas(Modifier.fillMaxSize()) {
        val step = 76.dp.toPx()
        val mark = measurer.measure(AnnotatedString(symbol), style = style, maxLines = 1)
        var row = 0
        var y = -step / 2
        while (y < size.height + step) {
            var x = -step + if (row % 2 == 0) 0f else step / 2
            while (x < size.width + step) {
                drawText(mark, topLeft = Offset(x - mark.size.width / 2f, y - mark.size.height / 2f))
                x += step
            }
            y += step * 0.8f
            row++
        }
    }
}

private const val PATTERN_ALPHA = 0.07f

// A badge with the currency mark. While the data loads, an arc turns around it and rings spread out
// across the whole sheet. When the data is there the arc grows into a full ring, one last ring bursts
// outward, and the mark gives way to a check.
@Composable
private fun SwitchEmblem(symbol: String?, ready: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val track = primary.copy(alpha = 0.16f)
    val infinite = rememberInfiniteTransition(label = "emblem")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin")
    val pulse by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)), label = "pulse")
    val close by animateFloatAsState(if (ready) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "close")
    // The badge pops, and a last ring bursts, when the check arrives.
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(ready) {
        if (ready) {
            burst.snapTo(0f)
            launch { burst.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
            pop.snapTo(0.8f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            burst.snapTo(1f)
            pop.snapTo(1f)
        }
    }
    val size = 168.dp
    Box(
        Modifier
            .size(size)
            .drawBehind {
                // Drawn behind the badge and past its edge, so the rings reach across the screen.
                val from = this.size.minDimension / 2
                val reach = RING_REACH.toPx()
                if (!ready) {
                    repeat(RING_COUNT) { i ->
                        val p = (pulse + i / RING_COUNT.toFloat()) % 1f
                        drawCircle(primary.copy(alpha = (1f - p) * 0.16f), radius = from + p * (reach - from), style = Stroke(2.dp.toPx()))
                    }
                }
                if (burst.value < 1f) {
                    drawCircle(primary.copy(alpha = (1f - burst.value) * 0.24f), radius = from + burst.value * (reach - from), style = Stroke(3.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.toPx() / 22
            val inset = stroke / 2 + size.toPx() * 0.045f
            val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = 110f + (360f - 110f) * close
            val start = -90f + spin * (1f - close)
            drawArc(primary, start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Box(
            Modifier.size(size * 0.64f).scale(pop.value).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(targetState = ready, transitionSpec = { swap() }, label = "badge") { done ->
                when {
                    done -> Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(52.dp))
                    symbol != null -> Text(symbol, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                    else -> Icon(Icons.Outlined.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

private val RING_REACH = 900.dp
private const val RING_COUNT = 4
