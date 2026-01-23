package com.tony.appbooster.wear.presentation.model

import com.tony.appbooster.wear.domain.model.AdbConnectionState
import com.tony.appbooster.wear.domain.model.OptimizationProgress
import com.tony.appbooster.wear.domain.model.OptimizationType

/**
 * UI state for the main Wear OS screen.
 *
 * @property connectionState Current ADB connection state.
 * @property optimizationProgress Current optimization progress.
 * @property selectedMode The selected optimization mode.
 * @property hasPaired Whether the device has been paired with ADB before.
 * @property isLoading Whether a connection or pairing operation is in progress.
 * @property errorMessage Error message to display, if any.
 */
data class WearHomeUiState(
    val connectionState: AdbConnectionState = AdbConnectionState.Disconnected,
    val optimizationProgress: OptimizationProgress = OptimizationProgress(),
    val selectedMode: OptimizationType = OptimizationType.SPEED_PROFILE,
    val hasPaired: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Whether optimization can be started (connected and not already running).
     */
    val canStartOptimization: Boolean
        get() = connectionState == AdbConnectionState.Connected &&
                !optimizationProgress.isRunning

    /**
     * Whether the setup/pairing flow should be shown.
     */
    val needsSetup: Boolean
        get() = !hasPaired || connectionState == AdbConnectionState.PairingRequired
}
