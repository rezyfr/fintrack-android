package com.fidriyanto.banktracker.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDaoTest {
    private val db = inMemoryDatabase()
    private val dao = db.categoryDao()

    @After fun close() = db.close()

    @Test fun deleteMissingCategoriesKeepsOnlyTheListedNames() = runBlocking {
        dao.upsertCategories(listOf(CategoryEntity("Food", 0, true), CategoryEntity("Bills", 1, true), CategoryEntity("Pets", 2, false)))
        dao.deleteMissingCategories(listOf("Food", "Pets"))
        assertEquals(listOf("Food", "Pets"), dao.observeCategories().first().map { it.name })
    }

    @Test fun subcategoriesComeInSortOrderThenByName() = runBlocking {
        dao.upsertSubcategories(
            listOf(SubcategoryEntity(category = "Food", name = "Snacks", sortOrder = 1), SubcategoryEntity(category = "Food", name = "Cafe", sortOrder = 1), SubcategoryEntity(category = "Food", name = "Restaurant", sortOrder = 0)),
        )
        assertEquals(listOf("Restaurant", "Cafe", "Snacks"), dao.observeSubcategories().first().map { it.name })
    }

    @Test fun deleteSubcategoryRemovesOnlyThatPair() = runBlocking {
        dao.upsertSubcategories(listOf(SubcategoryEntity(category = "Food", name = "Cafe", sortOrder = 0), SubcategoryEntity(category = "Drinks", name = "Cafe", sortOrder = 0)))
        dao.deleteSubcategory("Food", "Cafe")
        assertEquals(listOf("Drinks"), dao.observeSubcategories().first().map { it.category })
    }

    @Test fun clearSubcategoriesKeepsTheCategories() = runBlocking {
        dao.upsertCategories(listOf(CategoryEntity("Food", 0, true)))
        dao.upsertSubcategories(listOf(SubcategoryEntity(category = "Food", name = "Cafe", sortOrder = 0)))
        dao.clearSubcategories()
        assertEquals(0, dao.observeSubcategories().first().size)
        assertEquals(1, dao.observeCategories().first().size)
    }
}
