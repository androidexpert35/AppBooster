package com.tony.appbooster.presentation.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.tony.appbooster.presentation.tools.LocalWindowSizeClass
import com.tony.appbooster.presentation.tools.NavigationLayoutType
import com.tony.appbooster.presentation.tools.navigationLayoutType
import com.tony.appbooster.presentation.viewmodel.main.MainViewModel

/**
 * Root shell composable for the main section of the app.
 *
 * Reads the current [WindowSizeClass] from [LocalWindowSizeClass] and
 * delegates to the matching navigation layout:
 * - **Compact** – [NavigationBar] at the bottom (phone portrait).
 * - **Medium** – [NavigationRail] on the leading edge (small tablet / phone landscape).
 * - **Expanded** – Permanent [NavigationDrawer] (large tablet / desktop).
 *
 * @param viewModel Shared [MainViewModel] driving the dashboard and settings states.
 */
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val windowSizeClass = LocalWindowSizeClass.current
    val navLayout = windowSizeClass.navigationLayoutType()

    when (navLayout) {
        NavigationLayoutType.BottomBar -> MainAppScreenBottomBar(viewModel)
        NavigationLayoutType.NavigationRail -> MainAppScreenRail(viewModel)
        NavigationLayoutType.NavigationDrawer -> MainAppScreenDrawer(viewModel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact – Bottom Navigation Bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Phone-portrait layout with a [NavigationBar] at the bottom.
 */
@Composable
private fun MainAppScreenBottomBar(viewModel: MainViewModel) {
    val bottomNavController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Settings)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
            ) {
                NavigationBar {
                    items.forEach { screen ->
                        val label = screen.navLabel()
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = { bottomNavController.navigateSingleTop(screen) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        MainNavHost(
            viewModel = viewModel,
            bottomNavController = bottomNavController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Medium – Navigation Rail
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Medium-width layout (small tablet / phone landscape) with a [NavigationRail]
 * on the leading edge and the content filling the remaining horizontal space.
 */
@Composable
private fun MainAppScreenRail(viewModel: MainViewModel) {
    val railNavController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Settings)

    Row(modifier = Modifier.fillMaxSize()) {
        val navBackStackEntry by railNavController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Spacer(Modifier.height(16.dp))
                items.forEach { screen ->
                    val label = screen.navLabel()
                    NavigationRailItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = { railNavController.navigateSingleTop(screen) }
                    )
                }
            }
        }

        VerticalDivider(thickness = 1.dp)

        MainNavHost(
            viewModel = viewModel,
            bottomNavController = railNavController,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expanded – Permanent Navigation Drawer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Large-tablet / desktop layout with a permanent navigation drawer showing
 * full-width labels and the app name as a header.
 */
@Composable
private fun MainAppScreenDrawer(viewModel: MainViewModel) {
    val drawerNavController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Settings)

    val navBackStackEntry by drawerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    PermanentNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // App name header
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    items.forEach { screen ->
                        val label = screen.navLabel()
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(label, fontWeight = FontWeight.Medium) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = { drawerNavController.navigateSingleTop(screen) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        MainNavHost(
            viewModel = viewModel,
            bottomNavController = drawerNavController,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared NavHost
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Inner [NavHost] shared across all three navigation shell variants.
 *
 * @param viewModel Shared [MainViewModel] passed down to screen composables.
 * @param bottomNavController The [NavController] used to navigate between top-level screens.
 * @param modifier Optional layout modifier (used to apply padding or weight by the caller).
 */
@Composable
private fun MainNavHost(
    viewModel: MainViewModel,
    bottomNavController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = bottomNavController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Returns the localised navigation label for this [Screen]. */
@Composable
private fun Screen.navLabel(): String = when (this) {
    Screen.Dashboard -> stringResource(R.string.nav_dashboard)
    Screen.Settings -> stringResource(R.string.nav_settings)
    else -> route
}

/** Navigate to [screen] with `launchSingleTop` and state-save/restore. */
private fun androidx.navigation.NavHostController.navigateSingleTop(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

