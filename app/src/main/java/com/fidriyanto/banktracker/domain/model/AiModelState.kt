package com.fidriyanto.banktracker.domain.model

sealed interface AiModelState {
    data object NotInstalled : AiModelState
    data class Downloading(val doneBytes: Long, val totalBytes: Long) : AiModelState {
        val percent: Int get() = if (totalBytes > 0) (doneBytes * 100 / totalBytes).toInt().coerceIn(0, 100) else 0
    }
    data object Ready : AiModelState
    data class Failed(val message: String) : AiModelState
}
