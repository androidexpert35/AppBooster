package com.tony.appbooster.wear.presentation.navigation

/**
 * Navigation routes for the Wear OS app.
 */
sealed class WearScreen(val route: String) {
    /**
     * Home screen with optimization controls.
     */
    data object Home : WearScreen("home")

    /**
     * ADB pairing setup screen.
     */
    data object Pairing : WearScreen("pairing")
}
