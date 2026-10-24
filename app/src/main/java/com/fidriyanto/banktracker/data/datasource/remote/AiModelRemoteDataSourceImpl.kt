package com.fidriyanto.banktracker.data.datasource.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val BUFFER_BYTES = 64 * 1024
private const val PROGRESS_STEP_BYTES = 512 * 1024L

@Singleton
class AiModelRemoteDataSourceImpl @Inject constructor(
    private val service: AiModelDownloadService,
) : AiModelRemoteDataSource {
    override suspend fun download(url: String, part: File, onProgress: (Long, Long) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val have = part.length()
                val response = service.fetch(url, if (have > 0) "bytes=$have-" else null)
                // 416 means the part file already holds the whole file.
                if (response.code() == 416) return@withContext Result.success(Unit)
                val body = response.body()
                if (!response.isSuccessful || body == null) error("HTTP ${response.code()}")
                // 200 means the server ignored the Range header, so start again from zero.
                val append = response.code() == 206 && have > 0
                val start = if (append) have else 0L
                val total = start + body.contentLength()
                var done = start
                var reported = start
                body.byteStream().use { input ->
                    FileOutputStream(part, append).use { out ->
                        val buf = ByteArray(BUFFER_BYTES)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (done - reported >= PROGRESS_STEP_BYTES) {
                                reported = done
                                onProgress(done, total)
                            }
                        }
                    }
                }
                onProgress(done, total)
                Result.success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
