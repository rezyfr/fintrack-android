package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import javax.inject.Inject

class CategoryRemoteDataSourceImpl @Inject constructor(
    private val service: CategoryApiService,
) : CategoryRemoteDataSource {
    override suspend fun getCategories(): List<CategoryDto> = service.getCategories()

    override suspend fun upsertCategory(category: CategoryDto) {
        val response = service.putCategory(category)
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }

    override suspend fun deleteCategory(name: String) {
        val response = service.deleteCategory(name)
        if (!response.isSuccessful && response.code() != 404) error("API error: HTTP ${response.code()}")
    }

    override suspend fun getSubcategories(): List<SubcategoryDto> = service.getSubcategories()

    override suspend fun insertSubcategory(subcategory: SubcategoryDto) {
        val response = service.insertSubcategory(subcategory)
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }

    override suspend fun deleteSubcategory(category: String, name: String) {
        val response = service.deleteSubcategory(category, name)
        if (!response.isSuccessful && response.code() != 404) error("API error: HTTP ${response.code()}")
    }
}
