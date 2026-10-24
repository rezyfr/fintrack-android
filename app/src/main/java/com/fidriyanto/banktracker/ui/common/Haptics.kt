package com.fidriyanto.banktracker.ui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

// The app had no haptic feedback at all. These two give a switch and a commit different feels,
// which is most of what a finance app needs: a tick when you move between things, a firmer
// confirm when something is saved or removed.
//
// Compose's own HapticFeedbackType carries only LongPress and TextHandleMove on this Compose
// version, so the feedback goes through the host View, which has the full set.

// A light tick: tab change, chip select, cycle step, toggle.
@Composable
fun rememberTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }
    }
}

// A firmer confirm: save, delete, swipe commit. CONFIRM needs API 30, so older devices get the
// long-press feel, which is the closest thing available to them.
@Composable
fun rememberConfirm(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val effect =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
            view.performHapticFeedback(effect)
        }
    }
}
