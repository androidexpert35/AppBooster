package com.tony.appbooster.presentation.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tony.appbooster.R
import com.tony.appbooster.presentation.navigation.Screen
import com.tony.appbooster.presentation.screen.dashboard.DashboardScreen
import com.tony.appbooster.presentation.screen.settings.SettingsScreen
import com.tony.appbooster.presentation.screen.watch.WatchScreen
import com.tony.appbooster.presentation.viewmodel.main.MainViewModel

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val bottomNavController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Watch, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val label = when (screen) {
                        Screen.Dashboard -> stringResource(R.string.nav_dashboard)
                        Screen.Settings -> stringResource(R.string.nav_settings)
                        Screen.Watch -> "Watch"
                        else -> ""
                    }

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel)
            }
            composable(Screen.Watch.route) {
                WatchScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}