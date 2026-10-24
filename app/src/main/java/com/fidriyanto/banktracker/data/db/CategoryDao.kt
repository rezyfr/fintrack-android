package com.fidriyanto.banktracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM subcategories ORDER BY sortOrder, name")
    fun observeSubcategories(): Flow<List<SubcategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(rows: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubcategories(rows: List<SubcategoryEntity>)

    @Query("DELETE FROM categories WHERE name NOT IN (:keepNames)")
    suspend fun deleteMissingCategories(keepNames: List<String>)

    @Query("DELETE FROM subcategories")
    suspend fun clearSubcategories()

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    @Query("DELETE FROM subcategories WHERE category = :category AND name = :name")
    suspend fun deleteSubcategory(category: String, name: String)
}
