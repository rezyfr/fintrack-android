package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.BudgetLineRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import com.fidriyanto.banktracker.data.db.LedgerBudgetLineEntity
import com.fidriyanto.banktracker.data.db.LedgerDao
import com.fidriyanto.banktracker.domain.model.BudgetLine
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - budget lines in the offline store. Create and edit store the same fields as the
// server bodies: an edit changes the category only when it names one, and a cleared due day or
// subcategory is cleared. A delete of a line that is gone succeeds.
@Singleton
class OfflineBudgetLineDataSource @Inject constructor(
    private val dao: LedgerDao,
) : BudgetLineRemoteDataSource {
    override suspend fun getBudgetLines(): List<BudgetLineDto> = dao.activeBudgetLines().map { it.toDto() }

    override suspend fun create(line: BudgetLine): Result<Unit> = runCatching {
        dao.insertBudgetLine(
            LedgerBudgetLineEntity(
                name = line.name, kind = line.kind, currency = line.currency, target = line.target,
                sortOrder = line.sortOrder, active = true, category = line.category, subcategory = line.subcategory,
                dueDay = line.dueDay,
            ),
        )
    }

    override suspend fun update(line: BudgetLine): Result<Unit> = runCatching {
        val row = requireNotNull(dao.budgetLine(line.id)) { "Budget line ${line.id} not found" }
        val edited = row.copy(name = line.name, kind = line.kind, currency = line.currency, target = line.target, dueDay = line.dueDay)
        dao.updateBudgetLine(if (line.category != null) edited.copy(category = line.category, subcategory = line.subcategory) else edited)
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching { dao.deleteBudgetLine(id) }
}
