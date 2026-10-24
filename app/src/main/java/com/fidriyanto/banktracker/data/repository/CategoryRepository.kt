package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun refresh(): Result<Unit>
    suspend fun saveCategory(name: String): Result<Unit>
    suspend fun deleteCategory(name: String): Result<Unit>
    suspend fun saveSubcategory(category: String, name: String): Result<Unit>
    suspend fun deleteSubcategory(category: String, name: String): Result<Unit>
}
