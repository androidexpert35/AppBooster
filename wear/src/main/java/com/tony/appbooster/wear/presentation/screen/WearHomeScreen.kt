package com.tony.appbooster.wear.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.tony.appbooster.wear.domain.model.AdbConnectionState
import com.tony.appbooster.wear.domain.model.OptimizationResult
import com.tony.appbooster.wear.presentation.model.WearHomeUiState
import com.tony.appbooster.wear.presentation.viewmodel.WearHomeViewModel

/**
 * Main home screen for the Wear OS app.
 *
 * Displays connection status, optimization controls, and progress.
 *
 * @param viewModel The ViewModel for this screen.
 * @param onNavigateToPairing Callback to navigate to pairing screen.
 */
@Composable
fun WearHomeScreen(
    viewModel: WearHomeViewModel = hiltViewModel(),
    onNavigateToPairing: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WearHomeContent(
        uiState = uiState,
        onStartOptimization = viewModel::startOptimization,
        onCancelOptimization = viewModel::cancelOptimization,
        onToggleMode = viewModel::toggleMode,
        onNavigateToPairing = onNavigateToPairing,
        onRetryConnection = viewModel::retryConnection
    )
}

@Composable
private fun WearHomeContent(
    uiState: WearHomeUiState,
    onStartOptimization: () -> Unit,
    onCancelOptimization: () -> Unit,
    onToggleMode: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onRetryConnection: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(200))
            },
            contentKey = { state ->
                // Use a key that represents the main display state
                when {
                    state.optimizationProgress.isRunning -> "running"
                    state.optimizationProgress.result == OptimizationResult.Completed -> "complete"
                    state.isLoading -> "loading"
                    state.isUsingPhoneBridge && state.isPhoneAdbReady -> "phone_ready"
                    state.isUsingPhoneBridge && state.isPhoneConnected -> "phone_connected"
                    state.isUsingPhoneBridge -> "waiting_phone"
                    state.connectionState == AdbConnectionState.Connected -> "local_ready"
                    state.needsSetup -> "setup"
                    state.errorMessage != null -> "error"
                    else -> "disconnected"
                }
            },
            label = "home_state"
        ) { state ->
            when {
                state.optimizationProgress.isRunning -> {
                    OptimizingContent(
                        progress = state.optimizationProgress.progress,
                        currentPackage = state.optimizationProgress.currentAppPackage,
                        processedCount = state.optimizationProgress.processedCount,
                        totalCount = state.optimizationProgress.totalCount,
                        onCancel = onCancelOptimization
                    )
                }
                state.optimizationProgress.result == OptimizationResult.Completed -> {
                    OptimizationCompleteContent(
                        appsOptimized = state.optimizationProgress.totalCount,
                        onStartAgain = onStartOptimization
                    )
                }
                state.isLoading -> {
                    ConnectingContent()
                }
                // Phone Bridge Mode - ADB Ready (can start optimization)
                state.isUsingPhoneBridge && state.isPhoneAdbReady -> {
                    PhoneReadyContent(
                        selectedMode = state.selectedMode.name,
                        onStart = onStartOptimization,
                        onToggleMode = onToggleMode
                    )
                }
                // Phone Bridge Mode - Phone connected but ADB not ready
                state.isUsingPhoneBridge && state.isPhoneConnected -> {
                    WaitingForPhoneSetupContent()
                }
                // Phone Bridge Mode - Waiting for phone
                state.isUsingPhoneBridge -> {
                    WaitingForPhoneContent()
                }
                // Local/Self-Connection Mode - Connected
                state.connectionState == AdbConnectionState.Connected -> {
                    ReadyToOptimizeContent(
                        selectedMode = state.selectedMode.name,
                        onStart = onStartOptimization,
                        onToggleMode = onToggleMode
                    )
                }
                state.needsSetup -> {
                    SetupRequiredContent(onNavigateToPairing = onNavigateToPairing)
                }
                state.connectionState is AdbConnectionState.Error -> {
                    ErrorContent(
                        message = state.connectionState.message,
                        onRetry = onRetryConnection,
                        onSetup = onNavigateToPairing
                    )
                }
                else -> {
                    DisconnectedContent(
                        onConnect = onRetryConnection,
                        onSetup = onNavigateToPairing
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupRequiredContent(onNavigateToPairing: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Setup Required",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use the phone app to connect, or tap below for manual setup",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onNavigateToPairing) {
            Text("Manual Setup")
        }
    }
}

@Composable
private fun ReadyToOptimizeContent(
    selectedMode: String,
    onStart: () -> Unit,
    onToggleMode: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ready",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Large play button
        Button(
            onClick = onStart,
            modifier = Modifier.size(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Start Optimization",
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode toggle
        Text(
            text = selectedMode.replace("_", " "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Content shown when phone is connected and ADB is ready.
 * This is the preferred phone bridge mode.
 */
@Composable
private fun PhoneReadyContent(
    selectedMode: String,
    onStart: () -> Unit,
    onToggleMode: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Phone Ready",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "via ADB",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Large play button
        Button(
            onClick = onStart,
            modifier = Modifier.size(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Start Optimization",
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = selectedMode.replace("_", " "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Content shown when waiting for phone to complete ADB setup.
 */
@Composable
private fun WaitingForPhoneSetupContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Setup on Phone",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Open the Watch tab in the phone app and connect to this watch",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Content shown when waiting for phone app to connect.
 */
@Composable
private fun WaitingForPhoneContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Waiting for Phone",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Open AppBooster on your phone",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OptimizingContent(
    progress: Float,
    currentPackage: String,
    processedCount: Int,
    totalCount: Int,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$processedCount / $totalCount",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = currentPackage.substringAfterLast('.'),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = "Stop",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun OptimizationCompleteContent(
    appsOptimized: Int,
    onStartAgain: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete!",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4CAF50)
        )
        Text(
            text = "$appsOptimized apps optimized",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connecting...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DisconnectedContent(
    onConnect: () -> Unit,
    onSetup: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Disconnected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enable Wireless Debugging",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onConnect) {
            Text("Retry")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onSetup: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
