package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface ProcessedRefDao {
    @Query("SELECT COUNT(*) FROM processed_refs WHERE compositeKey = :key")
    suspend fun exists(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ProcessedRefEntity)
}
