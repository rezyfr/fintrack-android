package com.fidriyanto.banktracker.ui.navigation

import com.fidriyanto.banktracker.ui.common.BookSwitch
import com.fidriyanto.banktracker.ui.common.DataChanges
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.ui.add.AddScreen
import com.fidriyanto.banktracker.ui.balances.BalancesScreen
import com.fidriyanto.banktracker.ui.budgetglance.BudgetGlanceScreen
import com.fidriyanto.banktracker.ui.ask.AskScreen
import com.fidriyanto.banktracker.ui.cards.CardsScreen
import com.fidriyanto.banktracker.ui.common.LocalFloatingToolbarInset
import com.fidriyanto.banktracker.ui.common.SegmentedToggle
import com.fidriyanto.banktracker.ui.dashboard.DashboardScreen
import com.fidriyanto.banktracker.ui.feed.FeedScreen
import com.fidriyanto.banktracker.ui.home.HomeScreen
import com.fidriyanto.banktracker.ui.installments.InstallmentsScreen
import com.fidriyanto.banktracker.ui.settings.SettingsScreen
import com.fidriyanto.banktracker.ui.theme.AppShape
import com.fidriyanto.banktracker.ui.theme.Space
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.fidriyanto.banktracker.ui.common.BookSwitchSheet
import com.fidriyanto.banktracker.ui.common.rememberTick

// ac: four-tab-nav-with-add-fab — the four bottom tabs. Add is deliberately NOT here; it is a
// central floating button. Home is first, so it is the launch destination.
sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Home : Tab("home", "Home", Icons.Outlined.Home)
    object Transactions : Tab("transactions", "Transactions", Icons.Outlined.SwapVert)
    object Budget : Tab("budget", "Budget", Icons.Outlined.Savings)
    object Accounts : Tab("accounts", "Accounts", Icons.Outlined.AccountBalanceWallet)
}

// Settings is a page of its own, not a tab. The menu button of the Accounts tab opens it.
private const val SETTINGS_ROUTE = "settings"

private val tabs = listOf(Tab.Home, Tab.Transactions, Tab.Budget, Tab.Accounts)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // ac: four-tab-nav-with-add-fab — the Add form opens in a bottom sheet, so the tabs stay live
    var showAdd by remember { mutableStateOf(false) }
    // ac: on-device-ai — the Ask chat opens in a bottom sheet from the button beside the book switch
    var showAsk by remember { mutableStateOf(false) }
    // Switches tabs the same way for a tab tap and for a call to action on another page.
    val openTab: (Tab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    // ac: haptic-feedback — a light tick on every tab change and on Add
    val tick = rememberTick()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    // Settings and the pages inside it are full pages. They have their own back button, so the book switch and the toolbar hide.
    val inSettings = current?.route == SETTINGS_ROUTE
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val askSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Scaffold { padding ->
        // Test tags show as resource ids, so the end-to-end flows can tell apart labels that repeat.
        Box(Modifier.padding(padding).fillMaxSize().semantics { testTagsAsResourceId = true }) {
            // ac: floating-bottom-toolbar — the pages fill the whole screen and the toolbar floats over
            // them. Each page adds the toolbar inset as end padding to its scrolling content.
            CompositionLocalProvider(LocalFloatingToolbarInset provides 84.dp) {
                Column(Modifier.fillMaxSize()) {
                    // ac: currency-books — one book switch above every tab, so a single choice changes them all
                    if (!inSettings) {
                        Row(Modifier.padding(start = Space.lg, end = Space.sm, top = Space.sm, bottom = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                            BookSwitch(Modifier.weight(1f))
                            IconButton(onClick = { showAsk = true }) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = stringResource(R.string.ask_open), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    // ac: four-tab-nav-with-add-fab — Home is the start destination shown on launch
                    NavHost(navController, startDestination = Tab.Home.route, modifier = Modifier.weight(1f)) {
                        // ac: home-cycle-overview — the Home tab renders the overview as the launch screen
                        composable(Tab.Home.route) { HomeScreen(onOpenBudget = { openTab(Tab.Budget) }, onOpenTransactions = { openTab(Tab.Transactions) }) }
                        composable(Tab.Transactions.route) { FeedScreen() }
                        composable(Tab.Budget.route) { BudgetTab(onOpenTransactions = { openTab(Tab.Transactions) }) }
                        composable(Tab.Accounts.route) { BalancesScreen(onOpenMenu = { navController.navigate(SETTINGS_ROUTE) }) }
                        composable(SETTINGS_ROUTE) { SettingsScreen(onBack = { navController.popBackStack() }) }
                    }
                }
            }

            // ac: floating-bottom-toolbar — the tabs and the Add button sit on one floating row
            if (!inSettings) Row(
                Modifier.align(Alignment.BottomCenter).padding(start = Space.lg, end = Space.lg, bottom = Space.md),
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ac: floating-bottom-toolbar — a rounded toolbar above the content, not a full-width bar
                Surface(
                    shape = AppShape.xxl,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                ) {
                    Row(Modifier.padding(Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        tabs.forEach { tab ->
                            ToolbarItem(
                                tab = tab,
                                selected = current?.hierarchy?.any { it.route == tab.route } == true,
                                onClick = { tick(); openTab(tab) },
                            )
                        }
                    }
                }
                // ac: four-tab-nav-with-add-fab — Add is a floating button beside the toolbar, not a tab
                // ac: floating-bottom-toolbar — Add is its own rounded button to the right of the toolbar
                FloatingActionButton(
                    onClick = { tick(); showAdd = true },
                    shape = AppShape.xl,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(56.dp).testTag("add_fab"),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add transaction", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // ac: book-switch-sheet — a sheet that rises when the book changes, above the pages and the toolbar
        BookSwitchSheet(routeKey = current?.route)

        if (showAsk) {
            ModalBottomSheet(onDismissRequest = { showAsk = false }, sheetState = askSheetState) {
                AskScreen(onClose = { showAsk = false })
            }
        }

        // ac: four-tab-nav-with-add-fab — the Add button opens the form in a modal bottom sheet
        if (showAdd) {
            // ac: transaction-form-redesign — the form has its own close button, so the sheet has no drag handle
            ModalBottomSheet(
                onDismissRequest = { showAdd = false },
                sheetState = addSheetState,
                dragHandle = null,
            ) {
                // A bottom sheet is its own window, so it needs its own switch for test tags.
                Box(Modifier.semantics { testTagsAsResourceId = true }) {
                    AddScreen(onClose = { showAdd = false }, onSaved = { showAdd = false; DataChanges.notifyChanged() })
                }
            }
        }
    }
}

// ac: four-tab-nav-with-add-fab — the Budget tab reaches both the budget and installments views
@Composable
private fun BudgetTab(onOpenTransactions: () -> Unit) {
    var sub by rememberSaveable { mutableStateOf(0) }
    val labels = listOf("Budget", "Cards", "Installments", "Insights")
    Column(Modifier.fillMaxSize()) {
        SegmentedToggle(
            options = labels,
            selectedIndex = sub,
            onSelect = { sub = it },
            tags = listOf("sub_budget", "sub_cards", "sub_installments", "sub_insights"),
            modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm),
        )
        // ac: budget-subtab-transition — the four views slide along one axis, so moving between
        // them reads as a move and not a redraw
        AnimatedContent(
            targetState = sub,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(260)) { if (forward) it / 6 else -it / 6 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(260)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(160)))
            },
            label = "budget_sub",
        ) { index ->
            when (index) {
                0 -> BudgetGlanceScreen()
                1 -> CardsScreen()
                2 -> InstallmentsScreen()
                // ac: four-tab-nav-with-add-fab — the Insights view surfaces the spending dashboard
                else -> DashboardScreen(onOpenTransactions = onOpenTransactions)
            }
        }
    }
}

// ac: floating-bottom-toolbar — only the selected tab shows its name, to the right of its icon
@Composable
private fun ToolbarItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val shape = AppShape.xl
    Row(
        Modifier
            .testTag("tab_${tab.route}")
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
        AnimatedVisibility(visible = selected) {
            Text(
                tab.label,
                color = tint,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.padding(start = Space.sm),
            )
        }
    }
}
