package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto

interface CategoryRemoteDataSource {
    suspend fun getCategories(): List<CategoryDto>
    suspend fun upsertCategory(category: CategoryDto)
    suspend fun deleteCategory(name: String)
    suspend fun getSubcategories(): List<SubcategoryDto>
    suspend fun insertSubcategory(subcategory: SubcategoryDto)
    suspend fun deleteSubcategory(category: String, name: String)
}
