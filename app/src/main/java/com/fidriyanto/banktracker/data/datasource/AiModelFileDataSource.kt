package com.fidriyanto.banktracker.data.datasource

import java.io.File

interface AiModelFileDataSource {
    fun file(): File
    fun partFile(): File
    fun isInstalled(): Boolean
    fun delete()
}
