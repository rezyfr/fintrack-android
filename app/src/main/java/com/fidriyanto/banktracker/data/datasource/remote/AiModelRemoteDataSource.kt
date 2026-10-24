package com.fidriyanto.banktracker.data.datasource.remote

import java.io.File

interface AiModelRemoteDataSource {
    // Downloads [url] into [part]. A part file left by an earlier try is resumed with an HTTP Range request.
    suspend fun download(url: String, part: File, onProgress: (done: Long, total: Long) -> Unit): Result<Unit>
}
