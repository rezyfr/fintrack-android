package com.fidriyanto.banktracker.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ac: floating-bottom-toolbar — height the floating toolbar covers at the bottom of every tab page.
// Scrolling content adds it as end padding so the last item can be scrolled clear of the toolbar.
val LocalFloatingToolbarInset = staticCompositionLocalOf<Dp> { 0.dp }
