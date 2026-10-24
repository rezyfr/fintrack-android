package com.fidriyanto.banktracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingInstallmentStepDao {
    @Query("SELECT * FROM pending_installment_steps")
    suspend fun getAll(): List<PendingInstallmentStepEntity>

    @Query("SELECT * FROM pending_installment_steps WHERE installmentId = :id")
    suspend fun get(id: Long): PendingInstallmentStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: PendingInstallmentStepEntity)

    @Query("DELETE FROM pending_installment_steps WHERE installmentId = :id")
    suspend fun delete(id: Long)
}
