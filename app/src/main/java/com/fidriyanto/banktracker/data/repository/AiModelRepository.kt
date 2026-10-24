package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.AiModelState
import kotlinx.coroutines.flow.StateFlow

interface AiModelRepository {
    val state: StateFlow<AiModelState>
    // Starts the download in the background. It continues when the screen is closed.
    fun download()
    fun cancelDownload()
    fun delete()
}
