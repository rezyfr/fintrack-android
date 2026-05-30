package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.categorization.ResolvedCategory
import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryDataSourceImpl @Inject constructor(
    private val categoryResolver: CategoryResolver,
    private val prefs: SecurePrefs
) : CategoryDataSource {
    override suspend fun resolve(tx: ParsedTransaction): ResolvedCategory =
        categoryResolver.resolve(tx, prefs.promptPayThreshold, prefs.claudeApiKey)
}
