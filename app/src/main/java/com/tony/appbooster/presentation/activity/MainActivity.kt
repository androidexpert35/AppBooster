package com.tony.appbooster.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import com.tony.appbooster.presentation.navigation.AppNavigator
import com.tony.appbooster.presentation.navigation.interfaces.NavigationManager
import com.tony.appbooster.presentation.tools.LocalWindowSizeClass
import com.tony.appbooster.presentation.ui.theme.AppBoosterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppBoosterTheme {
                CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                    AppNavigator(navigationManager = navigationManager)
                }
            }
        }
    }
}
