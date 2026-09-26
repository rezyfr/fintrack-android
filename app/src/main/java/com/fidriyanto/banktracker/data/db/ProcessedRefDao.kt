package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface ProcessedRefDao {
    @Query("SELECT COUNT(*) FROM processed_refs WHERE compositeKey = :key")
    suspend fun exists(key: String): Int

    // Returns the new rowId, or -1 when the key already exists (conflict ignored). This makes the
    // dedup atomic: only the first insert of a compositeKey wins, even under concurrent calls.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ProcessedRefEntity): Long
}
