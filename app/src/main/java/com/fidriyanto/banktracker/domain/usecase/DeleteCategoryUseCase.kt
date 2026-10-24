package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.domain.model.Category
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    // ac: manage-categories — a default category cannot be deleted, since the Dashboard and
    // Budget tab category breakdowns are keyed to the original 13 categories
    suspend operator fun invoke(category: Category): Result<Unit> {
        if (category.isDefault) {
            return Result.failure(IllegalStateException("${category.name} is a default category and cannot be deleted"))
        }
        return categoryRepository.deleteCategory(category.name)
    }
}
