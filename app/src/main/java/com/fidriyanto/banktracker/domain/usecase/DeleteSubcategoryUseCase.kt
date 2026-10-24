package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CategoryRepository
import javax.inject.Inject

class DeleteSubcategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    // ac: manage-categories — add, rename, or delete subcategories under any category
    suspend operator fun invoke(category: String, name: String): Result<Unit> =
        categoryRepository.deleteSubcategory(category, name)
}
