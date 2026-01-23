package com.tony.appbooster.wear.presentation.model

import com.tony.appbooster.wear.domain.model.AdbConnectionState
import com.tony.appbooster.wear.domain.model.OptimizationProgress
import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.wearable.ConnectionMode

/**
 * UI state for the main Wear OS screen.
 *
 * @property connectionState Current ADB connection state (for self-connection mode).
 * @property optimizationProgress Current optimization progress.
 * @property selectedMode The selected optimization mode.
 * @property hasPaired Whether the device has been paired with ADB before.
 * @property isLoading Whether a connection or pairing operation is in progress.
 * @property errorMessage Error message to display, if any.
 * @property connectionMode Current connection mode (phone bridge vs self).
 * @property isPhoneConnected Whether the phone app is connected via Wearable API.
 * @property isPhoneAdbReady Whether the phone has an active ADB connection to this watch.
 */
data class WearHomeUiState(
    val connectionState: AdbConnectionState = AdbConnectionState.Disconnected,
    val optimizationProgress: OptimizationProgress = OptimizationProgress(),
    val selectedMode: OptimizationType = OptimizationType.SPEED_PROFILE,
    val hasPaired: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val connectionMode: ConnectionMode = ConnectionMode.UNKNOWN,
    val isPhoneConnected: Boolean = false,
    val isPhoneAdbReady: Boolean = false
) {
    /**
     * Whether optimization can be started.
     * In phone bridge mode: requires phone to be connected and ADB ready.
     * In self-connection mode: requires local ADB connection.
     */
    val canStartOptimization: Boolean
        get() = when (connectionMode) {
            ConnectionMode.PHONE_BRIDGE -> isPhoneConnected && isPhoneAdbReady && !optimizationProgress.isRunning
            ConnectionMode.SELF_CONNECTION -> connectionState == AdbConnectionState.Connected && !optimizationProgress.isRunning
            ConnectionMode.UNKNOWN -> false
        }

    /**
     * Whether the setup/pairing flow should be shown.
     * In phone bridge mode: setup is done on phone, not here.
     */
    val needsSetup: Boolean
        get() = when (connectionMode) {
            ConnectionMode.PHONE_BRIDGE -> !isPhoneAdbReady
            ConnectionMode.SELF_CONNECTION -> !hasPaired || connectionState == AdbConnectionState.PairingRequired
            ConnectionMode.UNKNOWN -> true
        }

    /**
     * Whether using the preferred phone bridge mode.
     */
    val isUsingPhoneBridge: Boolean
        get() = connectionMode == ConnectionMode.PHONE_BRIDGE
}
