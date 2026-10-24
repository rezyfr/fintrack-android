package com.fidriyanto.banktracker.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// A screen that saves a transaction calls notifyChanged(). A page that is already open reloads when it sees the new count.
object DataChanges {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun notifyChanged() {
        _count.value += 1
    }
}
