package com.fidriyanto.banktracker.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

// Motion the whole app shares. Before this file the only animated screens were the setup pages,
// so the running app never moved: figures appeared at their final value and bars at their final
// width. These three helpers cover what a finance screen actually needs.

private const val ENTRANCE_STAGGER_MS = 70L
private const val ENTRANCE_MS = 380
private const val COUNT_UP_MS = 650
private const val GROW_MS = 700

// Content fades in and rises a little, one item after the other. index is the place in the page,
// and each place waits 70 ms longer than the one before.
@Composable
fun Entrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * ENTRANCE_STAGGER_MS)
        shown = true
    }
    AnimatedVisibility(
        shown,
        modifier,
        enter = fadeIn(tween(ENTRANCE_MS)) + slideInVertically(tween(ENTRANCE_MS)) { it / 5 },
    ) { content() }
}

// A money figure counts up to its value: from zero the first time, and from the value it was
// showing when the figure changes. The interpolation runs in Double, so large amounts stay exact
// at rest. Pass the result to formatMoney.
@Composable
fun countUp(target: Double, durationMillis: Int = COUNT_UP_MS): Double {
    var settled by remember { mutableStateOf(0.0) }
    val progress = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        progress.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
        settled = target
    }
    return settled + (target - settled) * progress.value
}

// A 0..1 fraction grows from zero, for progress bars and the budget ring.
@Composable
fun grownFraction(target: Float, durationMillis: Int = GROW_MS): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(durationMillis, easing = FastOutSlowInEasing)) }
    return anim.value
}
