package com.fidriyanto.banktracker.data.datasource

import android.content.Context
import android.net.Uri
import com.fidriyanto.banktracker.data.model.BackupDocument
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupFileDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) : BackupFileDataSource {
    // "wt" empties the file first, so a shorter backup does not keep the end of an older one.
    override suspend fun write(uri: String, document: BackupDocument) = withContext(Dispatchers.IO) {
        val stream = requireNotNull(context.contentResolver.openOutputStream(Uri.parse(uri), "wt")) { "Cannot open $uri" }
        stream.bufferedWriter().use { gson.toJson(document, it) }
    }

    override suspend fun read(uri: String): BackupDocument = withContext(Dispatchers.IO) {
        val stream = requireNotNull(context.contentResolver.openInputStream(Uri.parse(uri))) { "Cannot open $uri" }
        requireNotNull(stream.bufferedReader().use { gson.fromJson(it, BackupDocument::class.java) }) { "The file is empty" }
    }
}
