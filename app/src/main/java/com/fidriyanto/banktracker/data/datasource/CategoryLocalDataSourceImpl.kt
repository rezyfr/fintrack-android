package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.CategoryDao
import com.fidriyanto.banktracker.data.db.CategoryEntity
import com.fidriyanto.banktracker.data.db.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryLocalDataSourceImpl @Inject constructor(
    private val dao: CategoryDao,
) : CategoryLocalDataSource {
    override fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeCategories()
    override fun observeSubcategories(): Flow<List<SubcategoryEntity>> = dao.observeSubcategories()

    override suspend fun replaceAll(categories: List<CategoryEntity>, subcategories: List<SubcategoryEntity>) {
        dao.upsertCategories(categories)
        dao.deleteMissingCategories(categories.map { it.name })
        dao.clearSubcategories()
        dao.upsertSubcategories(subcategories)
    }

    override suspend fun upsertCategory(row: CategoryEntity) = dao.upsertCategories(listOf(row))
    override suspend fun upsertSubcategory(row: SubcategoryEntity) = dao.upsertSubcategories(listOf(row))
    override suspend fun deleteCategory(name: String) = dao.deleteCategory(name)
    override suspend fun deleteSubcategory(category: String, name: String) = dao.deleteSubcategory(category, name)
}
