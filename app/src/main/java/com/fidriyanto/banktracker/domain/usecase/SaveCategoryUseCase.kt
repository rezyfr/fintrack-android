package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    // ac: manage-categories — the user can add a custom category
    suspend operator fun invoke(name: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name is required"))
        return categoryRepository.saveCategory(name.trim())
    }
}
