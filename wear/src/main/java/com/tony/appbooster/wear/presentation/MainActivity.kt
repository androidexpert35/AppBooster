package com.tony.appbooster.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import com.tony.appbooster.wear.presentation.navigation.WearNavGraph
import com.tony.appbooster.wear.presentation.theme.WearAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for the Wear OS app.
 *
 * Entry point that sets up the Compose UI with navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: WearAdbRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    WearNavGraph(repository = repository)
                }
            }
        }
    }
}
