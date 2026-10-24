package com.fidriyanto.banktracker.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import com.fidriyanto.banktracker.domain.model.INCOME_CATEGORIES
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetFrequentCategoriesUseCase
import com.fidriyanto.banktracker.ui.add.INVESTMENT_CATEGORIES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ac: category-picker - the most used category names for each type id, for the Edit sheet.
// Loaded once when the screen opens. FeedViewModel is at its size limit, so this lives apart.
@HiltViewModel
class EditFrequentCategoriesViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val getFrequent: GetFrequentCategoriesUseCase,
) : ViewModel() {
    private val _frequent = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val frequentByType: StateFlow<Map<String, List<String>>> = _frequent.asStateFlow()

    init {
        viewModelScope.launch {
            val expense = getCategories().first().ifEmpty { DEFAULT_CATEGORIES }.map { it.name }
            val names = mapOf("expense" to expense, "income" to INCOME_CATEGORIES, "investment" to INVESTMENT_CATEGORIES)
            _frequent.value = names.mapValues { (type, list) -> getFrequent(type, list) }
        }
    }
}
