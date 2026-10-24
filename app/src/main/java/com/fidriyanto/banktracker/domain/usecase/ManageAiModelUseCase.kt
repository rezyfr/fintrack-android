package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AiModelRepository
import com.fidriyanto.banktracker.domain.model.AiModelState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ManageAiModelUseCase @Inject constructor(
    private val repository: AiModelRepository,
) {
    val state: StateFlow<AiModelState> get() = repository.state
    fun download() = repository.download()
    fun cancel() = repository.cancelDownload()
    fun delete() = repository.delete()
}
