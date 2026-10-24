package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CategoryRepository
import com.fidriyanto.banktracker.domain.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    // ac: manage-categories — the Add Transaction form's category and subcategory pickers
    // reflect the current list instead of a compiled-in map
    operator fun invoke(): Flow<List<Category>> = categoryRepository.observeAll()

    suspend fun refresh(): Result<Unit> = categoryRepository.refresh()
}
