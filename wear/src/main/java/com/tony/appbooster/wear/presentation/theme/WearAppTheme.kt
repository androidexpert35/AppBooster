package com.tony.appbooster.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * Theme for the Wear OS app.
 *
 * Uses Material 3 for Wear OS with default color scheme.
 */
@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}
