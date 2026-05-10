package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface ProcessedRefDao {
    @Query("SELECT COUNT(*) FROM processed_refs WHERE referenceNo = :ref")
    suspend fun exists(ref: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ProcessedRefEntity)
}
