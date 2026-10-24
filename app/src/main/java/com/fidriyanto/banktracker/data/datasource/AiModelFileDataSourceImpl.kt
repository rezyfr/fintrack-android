package com.fidriyanto.banktracker.data.datasource

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val MIN_MODEL_BYTES = 1_000_000L

@Singleton
class AiModelFileDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AiModelFileDataSource {
    private val dir get() = File(context.filesDir, "ai").also { it.mkdirs() }

    override fun file() = File(dir, "model.litertlm")
    override fun partFile() = File(dir, "model.litertlm.part")
    override fun isInstalled() = file().length() > MIN_MODEL_BYTES

    override fun delete() {
        file().delete()
        partFile().delete()
    }
}
