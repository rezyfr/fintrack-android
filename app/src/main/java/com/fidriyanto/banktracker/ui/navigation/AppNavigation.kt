package com.fidriyanto.banktracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fidriyanto.banktracker.ui.add.AddScreen
import com.fidriyanto.banktracker.ui.balances.BalancesScreen
import com.fidriyanto.banktracker.ui.budget.BudgetScreen
import com.fidriyanto.banktracker.ui.cards.CardsScreen
import com.fidriyanto.banktracker.ui.dashboard.DashboardScreen
import com.fidriyanto.banktracker.ui.feed.FeedScreen
import com.fidriyanto.banktracker.ui.installments.InstallmentsScreen
import com.fidriyanto.banktracker.ui.settings.SettingsScreen

// ac: four-tab-nav-with-add-fab — the four bottom tabs. Add is deliberately NOT here; it is a
// central floating button. Home is first, so it is the launch destination.
sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Home : Tab("home", "Home", Icons.Outlined.Home)
    object Transactions : Tab("transactions", "Transactions", Icons.Outlined.SwapVert)
    object Budget : Tab("budget", "Budget", Icons.Outlined.Savings)
    object Accounts : Tab("accounts", "Accounts", Icons.Outlined.AccountBalanceWallet)
}

private val tabs = listOf(Tab.Home, Tab.Transactions, Tab.Budget, Tab.Accounts)

private const val ADD_ROUTE = "add"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = current?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        // ac: four-tab-nav-with-add-fab — Add is a central floating button, not a tab
        floatingActionButton = {
            // Solid pine primary with a light glyph, matching the approved direction.
            FloatingActionButton(
                onClick = { navController.navigate(ADD_ROUTE) { launchSingleTop = true } },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        // Consume the Scaffold insets, then add extra bottom room so scrolling content clears the
        // centre floating button, which overlaps the top edge of the navigation bar.
        Box(Modifier.padding(padding).padding(bottom = 48.dp)) {
            // ac: four-tab-nav-with-add-fab — Home is the start destination shown on launch
            NavHost(navController, startDestination = Tab.Home.route) {
                composable(Tab.Home.route) { DashboardScreen() }
                composable(Tab.Transactions.route) { FeedScreen() }
                composable(Tab.Budget.route) { BudgetTab() }
                composable(Tab.Accounts.route) { AccountsTab() }
                composable(ADD_ROUTE) { AddScreen() }
            }
        }
    }
}

// ac: four-tab-nav-with-add-fab — the Budget tab reaches both the budget and installments views
@Composable
private fun BudgetTab() {
    var sub by rememberSaveable { mutableStateOf(0) }
    val labels = listOf("Budget", "Cards", "Installments")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = sub) {
            labels.forEachIndexed { i, label ->
                Tab(selected = sub == i, onClick = { sub = i }, text = { Text(label) })
            }
        }
        when (sub) {
            0 -> BudgetScreen()
            1 -> CardsScreen()
            else -> InstallmentsScreen()
        }
    }
}

// ac: four-tab-nav-with-add-fab — the Accounts tab reaches both balances and settings
@Composable
private fun AccountsTab() {
    var sub by rememberSaveable { mutableStateOf(0) }
    val labels = listOf("Balances", "Settings")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = sub) {
            labels.forEachIndexed { i, label ->
                Tab(selected = sub == i, onClick = { sub = i }, text = { Text(label) })
            }
        }
        when (sub) {
            0 -> BalancesScreen()
            else -> SettingsScreen()
        }
    }
}
