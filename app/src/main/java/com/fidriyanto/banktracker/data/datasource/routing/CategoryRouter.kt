package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineCategoryDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CategoryRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CategoryRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to CategoryRemoteDataSourceImpl or OfflineCategoryDataSource, by the storage the user chose.
@Singleton
class CategoryRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: CategoryRemoteDataSourceImpl,
    offline: OfflineCategoryDataSource,
) : CategoryRemoteDataSource {
    private val pick = StorageSwitch<CategoryRemoteDataSource>(storage, server, offline)

    override suspend fun getCategories(): List<CategoryDto> = pick().getCategories()

    override suspend fun upsertCategory(category: CategoryDto) = pick().upsertCategory(category)

    override suspend fun deleteCategory(name: String) = pick().deleteCategory(name)

    override suspend fun getSubcategories(): List<SubcategoryDto> = pick().getSubcategories()

    override suspend fun insertSubcategory(subcategory: SubcategoryDto) = pick().insertSubcategory(subcategory)

    override suspend fun deleteSubcategory(category: String, name: String) = pick().deleteSubcategory(category, name)
}
