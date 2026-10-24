package com.fidriyanto.banktracker.ui.settings.rules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fidriyanto.banktracker.domain.model.Rule

// Where the user is inside Rules. A suggestion can open the editor with its rule filled in, or the saved transactions page.
private sealed interface RulesPage {
    data object List : RulesPage
    data object Suggest : RulesPage
    data class Editor(val ruleId: String?, val seed: Rule? = null) : RulesPage
    data class Past(val rule: Rule) : RulesPage
}

@Composable
fun RulesFlow(onBack: () -> Unit) {
    var page by remember { mutableStateOf<RulesPage>(RulesPage.List) }
    // Each page that opens gets its own view model. A view model that is reused keeps the state of the last visit,
    // such as an editor that is already closed, and the page would close at once.
    var opens by remember { mutableIntStateOf(0) }
    val go = { next: RulesPage -> opens++; page = next }
    when (val p = page) {
        RulesPage.List -> RulesScreen(onBack = onBack, onEdit = { go(RulesPage.Editor(it)) }, onSuggest = { go(RulesPage.Suggest) })
        RulesPage.Suggest -> SuggestRulesScreen(
            onBack = { go(RulesPage.List) },
            onEdit = { go(RulesPage.Editor(null, it)) },
            onApply = { go(RulesPage.Past(it)) },
            vmKey = "suggest-$opens",
        )
        is RulesPage.Editor -> RuleEditorScreen(ruleId = p.ruleId, seed = p.seed, onClose = { go(RulesPage.List) }, vmKey = "editor-$opens")
        is RulesPage.Past -> PastRulesScreen(rule = p.rule, onClose = { go(RulesPage.Suggest) }, vmKey = "past-$opens")
    }
}
