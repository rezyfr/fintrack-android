package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.AiModelFileDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AiModelRemoteDataSource
import com.fidriyanto.banktracker.domain.model.AiModelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_URL =
    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
private const val MODEL_BYTES = 2_588_147_712L

@Singleton
class AiModelRepositoryImpl @Inject constructor(
    private val files: AiModelFileDataSource,
    private val remote: AiModelRemoteDataSource,
) : AiModelRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(initialState())
    private var job: Job? = null

    override val state: StateFlow<AiModelState> = _state.asStateFlow()

    private fun initialState(): AiModelState = if (files.isInstalled()) AiModelState.Ready else AiModelState.NotInstalled

    override fun download() {
        if (job?.isActive == true || files.isInstalled()) return
        _state.value = AiModelState.Downloading(files.partFile().length(), MODEL_BYTES)
        job = scope.launch {
            val result = remote.download(MODEL_URL, files.partFile()) { done, total ->
                _state.value = AiModelState.Downloading(done, total)
            }
            _state.value = result.fold(
                onSuccess = {
                    files.partFile().renameTo(files.file())
                    initialState()
                },
                onFailure = { AiModelState.Failed(it.message ?: "Download failed") },
            )
        }
    }

    override fun cancelDownload() {
        job?.cancel()
        _state.value = initialState()
    }

    override fun delete() {
        job?.cancel()
        files.delete()
        _state.value = AiModelState.NotInstalled
    }
}
