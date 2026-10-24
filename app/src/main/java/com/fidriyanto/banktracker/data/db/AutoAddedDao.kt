package com.fidriyanto.banktracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoAddedDao {
    @Insert
    suspend fun insert(row: AutoAddedEntity): Long

    @Query("SELECT * FROM auto_added WHERE dismissed = 0 ORDER BY addedAtMs DESC, id DESC")
    fun observeActive(): Flow<List<AutoAddedEntity>>

    @Query("UPDATE auto_added SET dismissed = 1 WHERE dismissed = 0")
    suspend fun dismissAll()

    @Query("DELETE FROM auto_added WHERE addedAtMs < :cutoffMs")
    suspend fun prune(cutoffMs: Long)
}
