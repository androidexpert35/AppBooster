package com.tony.appbooster.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    data object ShizukuSetup : Screen("shizuku_setup", Icons.Rounded.Security)
    data object Dashboard : Screen("dashboard", Icons.Rounded.Build)
    data object Settings : Screen("settings", Icons.Rounded.Settings)
    data object Watch : Screen("watch", Icons.Rounded.Watch)
}