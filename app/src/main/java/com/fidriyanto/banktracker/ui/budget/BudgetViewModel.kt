package com.fidriyanto.banktracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.repository.BudgetRepository
import com.fidriyanto.banktracker.data.repository.DashboardRepository
import com.fidriyanto.banktracker.domain.model.MonthlyOverviewSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetCategoryRow(
    val category: String,
    val budgetLimit: Double,
    val amountSpent: Double,
) {
    val progress: Float = if (budgetLimit > 0) (amountSpent / budgetLimit).toFloat() else 0f
    // ac: set-category-budget — rows where spending exceeds the budget are highlighted in the error colour
    val isOverBudget: Boolean = budgetLimit > 0 && amountSpent > budgetLimit
}

data class BudgetUiState(
    val rows: List<BudgetCategoryRow> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    // ac: set-category-budget — currency selector lets the user switch between THB and IDR budgets
    val currency: String = "THB",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    // ac: set-category-budget — tapping a row opens an inline edit field to update the budget limit
    val editingCategory: String? = null,
    val editValue: String = "",
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState(isLoading = true))
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    private var thbEntity: MonthlyBudgetEntity? = null
    private var idrEntity: MonthlyBudgetEntity? = null
    private var thbSpending: MonthlyOverviewSummary? = null
    private var idrSpending: MonthlyOverviewSummary? = null

    val currentMonth: String = run {
        val d = LocalDate.now()
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    init {
        loadBudgets()
        observeSpending()
        refresh()
    }

    private fun loadBudgets() = viewModelScope.launch {
        budgetRepository.get()
            .onSuccess { entities ->
                thbEntity = entities.firstOrNull { it.currency == "THB" }
                idrEntity = entities.firstOrNull { it.currency == "IDR" }
                rebuildRows()
                _state.update { it.copy(isLoading = false) }
            }
            .onFailure { e ->
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
    }

    private fun observeSpending() = viewModelScope.launch {
        dashboardRepository.observeForMonths(listOf(currentMonth)).collect { (thbRows, idrRows) ->
            thbSpending = thbRows.firstOrNull()
            idrSpending = idrRows.firstOrNull()
            rebuildRows()
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isRefreshing = true) }
        dashboardRepository.refresh(listOf(currentMonth))
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
        _state.update { it.copy(isRefreshing = false) }
    }

    fun selectCurrency(currency: String) {
        _state.update { it.copy(currency = currency, editingCategory = null, editValue = "") }
        rebuildRows()
    }

    fun startEditing(category: String) {
        val current = _state.value.rows.firstOrNull { it.category == category }
        val prefill = if (current != null && current.budgetLimit > 0)
            current.budgetLimit.toLong().toString()
        else ""
        _state.update { it.copy(editingCategory = category, editValue = prefill) }
    }

    fun updateEditValue(v: String) = _state.update { it.copy(editValue = v) }

    fun cancelEditing() = _state.update { it.copy(editingCategory = null, editValue = "") }

    // ac: set-category-budget — saving a budget limit persists it to the budgets table in Supabase
    fun saveEdit() = viewModelScope.launch {
        val s = _state.value
        val cat = s.editingCategory ?: return@launch
        val newVal = s.editValue.toDoubleOrNull() ?: return@launch
        _state.update { it.copy(isSaving = true) }

        val existing = if (s.currency == "THB") {
            thbEntity ?: MonthlyBudgetEntity("THB", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        } else {
            idrEntity ?: MonthlyBudgetEntity("IDR", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val updated = existing.copyWith(cat, newVal)

        budgetRepository.upsert(updated)
            .onSuccess {
                if (s.currency == "THB") thbEntity = updated else idrEntity = updated
                rebuildRows()
                _state.update { it.copy(isSaving = false, editingCategory = null, editValue = "") }
            }
            .onFailure { e ->
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
    }

    private fun rebuildRows() {
        val currency = _state.value.currency
        val entity = if (currency == "THB") thbEntity else idrEntity
        val spending = if (currency == "THB") thbSpending else idrSpending
        // ac: set-category-budget — each category row shows the category name, budget limit, and current month spending
        val rows = buildCategoryRows(entity, spending)
        val totalBudget = rows.sumOf { it.budgetLimit }
        val totalSpent = rows.sumOf { it.amountSpent }
        _state.update { it.copy(rows = rows, totalBudget = totalBudget, totalSpent = totalSpent) }
    }

    private fun buildCategoryRows(
        entity: MonthlyBudgetEntity?,
        summary: MonthlyOverviewSummary?,
    ): List<BudgetCategoryRow> = listOf(
        BudgetCategoryRow("Bills",              entity?.bills             ?: 0.0, summary?.bills             ?: 0.0),
        BudgetCategoryRow("Subscriptions",      entity?.subscriptions     ?: 0.0, summary?.subscriptions     ?: 0.0),
        BudgetCategoryRow("Entertainment",      entity?.entertainment     ?: 0.0, summary?.entertainment     ?: 0.0),
        BudgetCategoryRow("Food & Drink",       entity?.foodDrink         ?: 0.0, summary?.foodDrink         ?: 0.0),
        BudgetCategoryRow("Groceries",          entity?.groceries         ?: 0.0, summary?.groceries         ?: 0.0),
        BudgetCategoryRow("Health & Wellbeing", entity?.healthWellbeing   ?: 0.0, summary?.healthWellbeing   ?: 0.0),
        BudgetCategoryRow("Other",              entity?.other             ?: 0.0, summary?.other             ?: 0.0),
        BudgetCategoryRow("Shopping",           entity?.shopping          ?: 0.0, summary?.shopping          ?: 0.0),
        BudgetCategoryRow("Transport",          entity?.transport         ?: 0.0, summary?.transport         ?: 0.0),
        BudgetCategoryRow("Travel",             entity?.travel            ?: 0.0, summary?.travel            ?: 0.0),
        BudgetCategoryRow("Business",           entity?.business          ?: 0.0, summary?.business          ?: 0.0),
        BudgetCategoryRow("Gifts",              entity?.gifts             ?: 0.0, summary?.gifts             ?: 0.0),
    )

    private fun MonthlyBudgetEntity.copyWith(category: String, value: Double) = when (category) {
        "Bills"              -> copy(bills = value)
        "Subscriptions"      -> copy(subscriptions = value)
        "Entertainment"      -> copy(entertainment = value)
        "Food & Drink"       -> copy(foodDrink = value)
        "Groceries"          -> copy(groceries = value)
        "Health & Wellbeing" -> copy(healthWellbeing = value)
        "Other"              -> copy(other = value)
        "Shopping"           -> copy(shopping = value)
        "Transport"          -> copy(transport = value)
        "Travel"             -> copy(travel = value)
        "Business"           -> copy(business = value)
        "Gifts"              -> copy(gifts = value)
        else                 -> this
    }
}
