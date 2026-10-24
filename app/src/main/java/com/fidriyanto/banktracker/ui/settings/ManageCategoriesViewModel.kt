package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.usecase.DeleteCategoryUseCase
import com.fidriyanto.banktracker.domain.usecase.DeleteSubcategoryUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.SaveCategoryUseCase
import com.fidriyanto.banktracker.domain.usecase.SaveSubcategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val saveSubcategory: SaveSubcategoryUseCase,
    private val deleteSubcategoryUseCase: DeleteSubcategoryUseCase,
) : ViewModel() {
    // ac: manage-categories — the Settings screen lists every category and its subcategories
    val categories: StateFlow<List<Category>> =
        getCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch { getCategories.refresh() }
    }

    // ac: manage-categories — the user can add a custom category
    fun addCategory(name: String) = viewModelScope.launch {
        saveCategory(name).onFailure { _error.value = it.message }
    }

    fun delete(category: Category) = viewModelScope.launch {
        deleteCategory(category).onFailure { _error.value = it.message }
    }

    // ac: manage-categories — add, rename, or delete subcategories under any category
    fun addSubcategory(category: String, name: String) = viewModelScope.launch {
        saveSubcategory(category, name).onFailure { _error.value = it.message }
    }

    fun deleteSubcategory(category: String, name: String) = viewModelScope.launch {
        deleteSubcategoryUseCase(category, name).onFailure { _error.value = it.message }
    }

    fun dismissError() { _error.value = null }
}
