package com.fidriyanto.banktracker.ui.settings.custombanks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fidriyanto.banktracker.ui.settings.BankNotificationsScreen

// Where the user is inside Bank notifications: the list, or the editor of one own bank.
private sealed interface CustomBanksPage {
    data object List : CustomBanksPage
    data class Editor(val patternId: String?) : CustomBanksPage
}

@Composable
fun CustomBanksFlow(onBack: () -> Unit) {
    var page by remember { mutableStateOf<CustomBanksPage>(CustomBanksPage.List) }
    // Each editor that opens gets its own view model, so it never starts with the state of the last visit.
    var opens by remember { mutableIntStateOf(0) }
    val go = { next: CustomBanksPage -> opens++; page = next }
    when (val p = page) {
        CustomBanksPage.List -> BankNotificationsScreen(onBack = onBack, onAddBank = { go(CustomBanksPage.Editor(null)) }, onEditBank = { go(CustomBanksPage.Editor(it)) })
        is CustomBanksPage.Editor -> CustomBankEditorScreen(p.patternId, onClose = { go(CustomBanksPage.List) }, vmKey = "custom-bank-$opens")
    }
}
