package com.fidriyanto.banktracker.fake

import com.fidriyanto.banktracker.data.model.SheetsRow
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSheetsSyncer @Inject constructor() : SheetsSyncer {
    var shouldSucceed = true
    var syncCalled = false
    var lastRow: SheetsRow? = null

    override suspend fun sync(row: SheetsRow): Result<Unit> {
        syncCalled = true
        lastRow = row
        return if (shouldSucceed) Result.success(Unit)
        else Result.failure(Exception("Fake sync failure"))
    }

    fun reset() {
        shouldSucceed = true
        syncCalled = false
        lastRow = null
    }
}
