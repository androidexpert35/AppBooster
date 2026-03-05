
package com.tony.appbooster.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.tony.appbooster.presentation.screen.main.MainAppScreen
import com.tony.appbooster.presentation.screen.shizuku.ShizukuSetupScreen

@Composable
fun AppBoosterNavigationGraph(
    navController: NavHostController
) {

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = MainNavigationGraph.MainFlowNode.route,
            modifier = Modifier.weight(1f)
        ) {
            mainNavGraph(navController = navController)
        }
    }
}

/**
 * Defines the main navigation flow:
 * - Shizuku setup screen -> Home (MainAppScreen with Dashboard)
 * - Settings screen.
 *
 * @param navController controller used to navigate between setup and main flow.
 */
fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = MainNavigationGraph.ShizukuSetupScreen.route,
        route = MainNavigationGraph.MainFlowNode.route
    ) {
        composable(MainNavigationGraph.ShizukuSetupScreen.route) {
            ShizukuSetupScreen(
                viewModel = hiltViewModel(),
                onSetupComplete = {
                    navController.navigate(MainNavigationGraph.HomeScreen.route) {
                        popUpTo(MainNavigationGraph.ShizukuSetupScreen.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MainNavigationGraph.HomeScreen.route) {
            MainAppScreen(viewModel = hiltViewModel())
        }
    }
}
