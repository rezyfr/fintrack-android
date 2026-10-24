package com.fidriyanto.banktracker.data.datasource

import android.content.Context
import com.fidriyanto.banktracker.domain.model.AskMessage
import com.fidriyanto.banktracker.domain.model.LlmTool
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_TOKENS = 4096
private const val IDLE_CLOSE_MS = 2 * 60 * 1000L

@Singleton
class LlmDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelFile: AiModelFileDataSource,
) : LlmDataSource {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: Engine? = null
    private var idleJob: Job? = null

    override suspend fun generate(
        system: String,
        prompt: String,
        history: List<AskMessage>,
        tools: List<LlmTool>,
    ): Result<String> {
        if (!modelFile.isInstalled()) return Result.failure(IllegalStateException("AI model is not installed"))
        idleJob?.cancel()
        return mutex.withLock {
            withContext(Dispatchers.Default) {
                try {
                    // Greedy sampling: the same question gives the same tool call, which parsing and tools need.
                    val config = ConversationConfig(
                        systemInstruction = Contents.of(system),
                        initialMessages = history.map { if (it.fromUser) Message.user(it.text) else Message.model(it.text) },
                        tools = tools.map { tool(LlmToolAdapter(it)) },
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.2, seed = 0),
                    )
                    val reply = engine().createConversation(config).use { it.sendMessage(prompt).toString().trim() }
                    Result.success(reply)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Result.failure(e)
                } finally {
                    scheduleClose()
                }
            }
        }
    }

    // The GPU is much faster than the CPU. A phone that cannot start it falls back to the CPU.
    private fun engine(): Engine = engine ?: build(Backend.GPU()).getOrElse { build(Backend.CPU()).getOrThrow() }.also { engine = it }

    private fun build(backend: Backend): Result<Engine> = runCatching {
        val config = EngineConfig(
            modelPath = modelFile.file().absolutePath,
            backend = backend,
            maxNumTokens = MAX_TOKENS,
            cacheDir = context.cacheDir.absolutePath,
        )
        Engine(config).also { e -> runCatching { e.initialize() }.onFailure { runCatching { e.close() }; throw it } }
    }

    // The loaded model holds about 400 MB of memory, so it is released when nobody asks for a while.
    private fun scheduleClose() {
        idleJob = scope.launch {
            delay(IDLE_CLOSE_MS)
            mutex.withLock {
                engine?.close()
                engine = null
            }
        }
    }
}
