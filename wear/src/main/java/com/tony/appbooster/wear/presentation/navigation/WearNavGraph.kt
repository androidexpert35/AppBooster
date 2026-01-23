package com.tony.appbooster.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import com.tony.appbooster.wear.presentation.screen.PairingScreen
import com.tony.appbooster.wear.presentation.screen.WearHomeScreen

/**
 * Navigation graph for the Wear OS app.
 *
 * @param repository The ADB repository for passing to screens.
 * @param startDestination The initial screen to display.
 */
@Composable
fun WearNavGraph(
    repository: WearAdbRepository,
    startDestination: String = WearScreen.Home.route
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(WearScreen.Home.route) {
            WearHomeScreen(
                onNavigateToPairing = {
                    navController.navigate(WearScreen.Pairing.route)
                }
            )
        }

        composable(WearScreen.Pairing.route) {
            PairingScreen(
                repository = repository,
                onPairingComplete = {
                    navController.popBackStack()
                }
            )
        }
    }
}
