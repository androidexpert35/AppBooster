package com.tony.appbooster.wear.domain.repository

import com.tony.appbooster.wear.domain.model.AdbConnectionState
import com.tony.appbooster.wear.domain.model.OptimizationProgress
import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.Resource
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for ADB operations on Wear OS.
 *
 * This repository manages the self-connection to the local ADB daemon
 * via wireless debugging, enabling privileged shell command execution
 * for app optimization on the watch itself.
 */
interface WearAdbRepository {

    /**
     * Current state of the ADB connection.
     */
    val connectionState: StateFlow<AdbConnectionState>

    /**
     * Live progress of the current optimization run.
     */
    val optimizationProgress: StateFlow<OptimizationProgress>

    /**
     * Log output from shell commands for debugging/display.
     */
    val commandOutput: StateFlow<List<String>>

    /**
     * Pairs with the local ADB daemon using the wireless debugging pairing code.
     * This is typically a one-time operation; credentials are stored for future use.
     *
     * @param port The pairing port shown in Wireless Debugging settings.
     * @param pairingCode The 6-digit pairing code shown in Wireless Debugging settings.
     * @return [Resource.Success] if pairing succeeded, [Resource.Error] otherwise.
     */
    suspend fun pair(port: Int, pairingCode: String): Resource<Unit>

    /**
     * Connects to the local ADB daemon.
     * Requires wireless debugging to be enabled and prior successful pairing.
     *
     * @param port The connection port (different from pairing port).
     * @return [Resource.Success] if connection succeeded, [Resource.Error] otherwise.
     */
    suspend fun connect(port: Int): Resource<Unit>

    /**
     * Attempts to auto-discover the ADB daemon port and connect.
     * Uses mDNS service discovery to find the wireless debugging service.
     *
     * @return [Resource.Success] if connection succeeded, [Resource.Error] otherwise.
     */
    suspend fun autoConnect(): Resource<Unit>

    /**
     * Disconnects from the ADB daemon.
     */
    suspend fun disconnect()

    /**
     * Checks if we have previously paired successfully.
     *
     * @return True if ADB keys are stored and pairing was completed before.
     */
    suspend fun hasPaired(): Boolean

    /**
     * Executes the optimization workflow for all installed packages.
     *
     * @param mode The optimization mode to use (speed-profile or speed).
     * @return [Resource.Success] when complete, [Resource.Error] on failure.
     */
    suspend fun executeOptimization(mode: OptimizationType): Resource<Unit>

    /**
     * Cancels the currently running optimization, if any.
     *
     * @return [Resource.Success] if cancellation was requested successfully.
     */
    suspend fun cancelOptimization(): Resource<Unit>
}
