package com.fidriyanto.banktracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fidriyanto.banktracker.ui.add.AddScreen
import com.fidriyanto.banktracker.ui.feed.FeedScreen
import com.fidriyanto.banktracker.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Feed : Screen("feed", "Feed", Icons.Outlined.List)
    object Add : Screen("add", "Add", Icons.Outlined.Add)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

private val screens = listOf(Screen.Feed, Screen.Add, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = current?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Feed.route) {
            composable(Screen.Feed.route) { FeedScreen() }
            composable(Screen.Add.route) { AddScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
