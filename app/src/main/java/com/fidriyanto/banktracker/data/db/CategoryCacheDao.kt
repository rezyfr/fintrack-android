package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface CategoryCacheDao {
    @Query("SELECT * FROM category_cache WHERE merchantKey = :key")
    suspend fun getByMerchant(key: String): CategoryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CategoryCacheEntity)
}
