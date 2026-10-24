package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.CategoryRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import com.fidriyanto.banktracker.data.db.CategoryDao
import com.fidriyanto.banktracker.data.db.CategoryEntity
import com.fidriyanto.banktracker.data.db.SubcategoryEntity
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - in offline mode the category tables on the phone are the only copy. The first read
// fills them with the default categories, as the server's migration does for a new database. A write does
// nothing here, because CategoryRepository writes the same tables right after.
@Singleton
class OfflineCategoryDataSource @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRemoteDataSource {
    override suspend fun getCategories(): List<CategoryDto> {
        if (dao.observeCategories().first().isEmpty()) seedDefaults()
        return dao.observeCategories().first().map { CategoryDto(it.name, it.sortOrder, it.isDefault) }
    }

    override suspend fun getSubcategories(): List<SubcategoryDto> {
        if (dao.observeCategories().first().isEmpty()) seedDefaults()
        return dao.observeSubcategories().first().map { SubcategoryDto(it.id, it.category, it.name, it.sortOrder) }
    }

    override suspend fun upsertCategory(category: CategoryDto) = Unit

    override suspend fun deleteCategory(name: String) = Unit

    override suspend fun insertSubcategory(subcategory: SubcategoryDto) = Unit

    override suspend fun deleteSubcategory(category: String, name: String) = Unit

    private suspend fun seedDefaults() {
        dao.upsertCategories(DEFAULT_CATEGORIES.mapIndexed { i, c -> CategoryEntity(c.name, i, isDefault = true) })
        dao.upsertSubcategories(
            DEFAULT_CATEGORIES.flatMap { c -> c.subcategories.mapIndexed { i, s -> SubcategoryEntity(category = c.name, name = s, sortOrder = i) } },
        )
    }
}
