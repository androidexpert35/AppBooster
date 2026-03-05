package com.example.schedapp.presentation.navigation.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/**
 * Adds a composable destination to the navigation graph that provides a scoped ViewModel.
 *
 * This function is used to define a composable screen in a navigation graph where the ViewModel
 * is scoped to a parent route. The ViewModel is retrieved using Hilt's `hiltViewModel` function
 * and is scoped to the parent route's back stack entry.
 *
 * @param T The type of the ViewModel to be provided to the composable.
 * @param navController The NavHostController used to manage navigation.
 * @param parentRoute The route of the parent destination to which the ViewModel is scoped.
 * @param screenRoute The route of the current composable destination.
 * @param arguments A list of arguments for the composable destination. Defaults to an empty list.
 * @param content A composable lambda that defines the UI for the destination. It receives the
 * ViewModel and the NavBackStackEntry as parameters.
 */
@Suppress("unused")
inline fun <reified T : ViewModel> NavGraphBuilder.scopedViewModelComposable(
    navController: NavHostController?,
    parentRoute: String,
    screenRoute: String,
    arguments: List<NamedNavArgument> = emptyList(),
    crossinline content: @Composable (viewModel: T, backStackEntry: NavBackStackEntry) -> Unit
) {
    composable(
        route = screenRoute,
        arguments = arguments
    ) { backStackEntry ->
        // Retrieve the parent back stack entry to scope the ViewModel to the parent route.
        val parentEntry = if (navController != null) remember(backStackEntry) {
            navController.getBackStackEntry(parentRoute)
        } else backStackEntry
        // Retrieve the ViewModel scoped to the parent route.
        val viewModel = hiltViewModel<T>(parentEntry)
        // Call the composable content with the ViewModel and back stack entry.
        content(viewModel, backStackEntry)
    }
}
