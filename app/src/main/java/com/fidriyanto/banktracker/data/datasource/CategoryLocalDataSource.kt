package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.CategoryEntity
import com.fidriyanto.banktracker.data.db.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryLocalDataSource {
    fun observeCategories(): Flow<List<CategoryEntity>>
    fun observeSubcategories(): Flow<List<SubcategoryEntity>>
    suspend fun replaceAll(categories: List<CategoryEntity>, subcategories: List<SubcategoryEntity>)
    suspend fun upsertCategory(row: CategoryEntity)
    suspend fun upsertSubcategory(row: SubcategoryEntity)
    suspend fun deleteCategory(name: String)
    suspend fun deleteSubcategory(category: String, name: String)
}
