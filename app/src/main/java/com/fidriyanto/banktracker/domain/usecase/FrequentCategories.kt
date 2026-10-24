package com.fidriyanto.banktracker.domain.usecase

private const val DATE_LENGTH = 10

// ac: category-picker - ranks categories by how often a list of uses picked them.
// A use is a pair of category and ISO date. The most used category comes first.
// A tie goes to the category with the most recent use, then to the order of names.
// A category that is not in names is ignored. The result holds at most limit names.
fun rankCategories(uses: List<Pair<String, String>>, names: List<String>, limit: Int): List<String> {
    val known = names.toSet()
    val counts = HashMap<String, Int>()
    val latest = HashMap<String, String>()
    for ((category, date) in uses) {
        if (category !in known) continue
        counts[category] = (counts[category] ?: 0) + 1
        val day = date.take(DATE_LENGTH)
        if (day > (latest[category] ?: "")) latest[category] = day
    }
    val order = names.withIndex().associate { it.value to it.index }
    return counts.keys
        .sortedWith(
            compareByDescending<String> { counts[it] }
                .thenByDescending { latest[it] }
                .thenBy { order[it] },
        )
        .take(limit.coerceAtLeast(0))
}
