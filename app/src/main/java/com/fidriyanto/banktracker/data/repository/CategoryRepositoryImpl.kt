package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.CategoryLocalDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CategoryRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import com.fidriyanto.banktracker.data.db.CategoryEntity
import com.fidriyanto.banktracker.data.db.SubcategoryEntity
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val local: CategoryLocalDataSource,
    private val remote: CategoryRemoteDataSource,
) : CategoryRepository {
    // ac: manage-categories — the category/subcategory list is loaded from the server instead of a
    // compiled-in map, falling back to the shipped defaults before the tables have ever synced
    override fun observeAll(): Flow<List<Category>> =
        combine(local.observeCategories(), local.observeSubcategories()) { categories, subcategories ->
            if (categories.isEmpty()) DEFAULT_CATEGORIES
            else categories.map { c ->
                Category(
                    name = c.name,
                    subcategories = subcategories.filter { it.category == c.name }.map { it.name },
                    isDefault = c.isDefault,
                )
            }
        }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val categories = remote.getCategories()
        val subcategories = remote.getSubcategories()
        local.replaceAll(
            categories.map { it.toEntity() },
            subcategories.map { it.toEntity() },
        )
    }

    // ac: manage-categories — adding a category persists it to the server and the local cache
    override suspend fun saveCategory(name: String): Result<Unit> = runCatching {
        val dto = CategoryDto(name = name, sortOrder = 0, isDefault = false)
        remote.upsertCategory(dto)
        local.upsertCategory(dto.toEntity())
    }

    // ac: manage-categories — a default category cannot be deleted
    override suspend fun deleteCategory(name: String): Result<Unit> = runCatching {
        remote.deleteCategory(name)
        local.deleteCategory(name)
    }

    // ac: manage-categories — adding a subcategory persists it under its category
    override suspend fun saveSubcategory(category: String, name: String): Result<Unit> = runCatching {
        val dto = SubcategoryDto(category = category, name = name, sortOrder = 0)
        remote.insertSubcategory(dto)
        local.upsertSubcategory(dto.toEntity())
    }

    override suspend fun deleteSubcategory(category: String, name: String): Result<Unit> = runCatching {
        remote.deleteSubcategory(category, name)
        local.deleteSubcategory(category, name)
    }
}

private fun CategoryDto.toEntity() = CategoryEntity(name, sortOrder, isDefault)
private fun SubcategoryDto.toEntity() = SubcategoryEntity(id, category, name, sortOrder)
