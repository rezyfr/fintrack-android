package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CategoryRepository
import javax.inject.Inject

class SaveSubcategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    // ac: manage-categories — add, rename, or delete subcategories under any category
    suspend operator fun invoke(category: String, name: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name is required"))
        return categoryRepository.saveSubcategory(category, name.trim())
    }
}
