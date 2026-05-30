package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.categorization.ResolvedCategory
import com.fidriyanto.banktracker.data.model.ParsedTransaction

interface CategoryDataSource {
    suspend fun resolve(tx: ParsedTransaction): ResolvedCategory
}
