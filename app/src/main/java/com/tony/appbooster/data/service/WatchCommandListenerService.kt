package com.tony.appbooster.data.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tony.appbooster.data.client.RemoteWatchAdbClient
import com.tony.appbooster.domain.client.WearableDataClient
import com.tony.appbooster.domain.model.wearable.WatchOptimizationStatus
import com.tony.appbooster.domain.model.wearable.WearableConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service that listens for messages from the watch app via Wearable Data Layer.
 *
 * Handles commands like:
 * - Start/cancel optimization on the watch
 * - Connect/disconnect from watch ADB
 * - Respond to ping requests
 *
 * This runs in the background and processes commands even when the phone app UI isn't visible.
 */
@AndroidEntryPoint
class WatchCommandListenerService : WearableListenerService() {

    @Inject
    lateinit var wearableDataClient: WearableDataClient

    @Inject
    lateinit var remoteWatchAdbClient: RemoteWatchAdbClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        val nodeId = messageEvent.sourceNodeId
        val path = messageEvent.path
        val data = messageEvent.data

        Log.d(TAG, "Message received from $nodeId: $path")

        when (path) {
            WearableConstants.PATH_PING -> handlePing(nodeId)
            WearableConstants.PATH_START_OPTIMIZATION -> handleStartOptimization(data)
            WearableConstants.PATH_CANCEL_OPTIMIZATION -> handleCancelOptimization()
            WearableConstants.PATH_CONNECT_REQUEST -> handleConnectRequest(data)
            WearableConstants.PATH_DISCONNECT_REQUEST -> handleDisconnectRequest()
            else -> Log.w(TAG, "Unknown message path: $path")
        }
    }

    /**
     * Responds to ping from watch with phone's readiness status.
     */
    private fun handlePing(nodeId: String) {
        serviceScope.launch {
            val isReady = remoteWatchAdbClient.isConnected()
            wearableDataClient.sendPong(nodeId, isReady)
        }
    }

    /**
     * Handles optimization start request from watch.
     */
    private fun handleStartOptimization(data: ByteArray) {
        serviceScope.launch {
            try {
                val optimizationType = String(data)
                Log.d(TAG, "Starting optimization on watch: $optimizationType")

                // Check if connected to watch ADB
                if (!remoteWatchAdbClient.isConnected()) {
                    sendError("Not connected to watch. Enable Wireless Debugging on watch.")
                    return@launch
                }

                // Get packages to optimize
                val packagesResult = remoteWatchAdbClient.getInstalledPackages()
                val packages = packagesResult.getOrElse {
                    sendError("Failed to get package list: ${it.message}")
                    return@launch
                }

                // Filter to user apps (exclude system apps that can't be compiled)
                val appsToOptimize = packages.filter { pkg ->
                    !pkg.startsWith("com.google.android.") &&
                    !pkg.startsWith("com.android.") &&
                    !pkg.startsWith("android")
                }

                val total = appsToOptimize.size
                var current = 0

                // Send initial status
                wearableDataClient.sendOptimizationStatus(
                    WatchOptimizationStatus(
                        isRunning = true,
                        progressCurrent = 0,
                        progressTotal = total,
                        optimizationType = optimizationType
                    )
                )

                // Compile each app
                val compileMode = when (optimizationType) {
                    WearableConstants.OPTIMIZATION_TYPE_FULL -> "speed"
                    else -> "speed-profile"
                }

                for (pkg in appsToOptimize) {
                    current++

                    // Send progress update
                    wearableDataClient.sendOptimizationStatus(
                        WatchOptimizationStatus(
                            isRunning = true,
                            currentApp = pkg,
                            progressCurrent = current,
                            progressTotal = total,
                            optimizationType = optimizationType
                        )
                    )

                    // Compile the package
                    remoteWatchAdbClient.compilePackage(pkg, compileMode)

                    // Small delay to prevent overwhelming the watch
                    kotlinx.coroutines.delay(100)
                }

                // Send completion status
                wearableDataClient.sendOptimizationStatus(
                    WatchOptimizationStatus(
                        isRunning = false,
                        isComplete = true,
                        progressCurrent = total,
                        progressTotal = total,
                        optimizationType = optimizationType
                    )
                )

                Log.d(TAG, "Optimization complete: $total apps processed")

            } catch (e: Exception) {
                Log.e(TAG, "Optimization failed", e)
                sendError("Optimization failed: ${e.message}")
            }
        }
    }

    /**
     * Handles optimization cancel request from watch.
     */
    private fun handleCancelOptimization() {
        serviceScope.launch {
            // Note: In a real implementation, we'd track the running job and cancel it.
            // For now, we just send a status update.
            wearableDataClient.sendOptimizationStatus(
                WatchOptimizationStatus(
                    isRunning = false,
                    isComplete = false,
                    errorMessage = "Cancelled by user"
                )
            )
        }
    }

    /**
     * Handles ADB connect request from watch.
     */
    private fun handleConnectRequest(data: ByteArray) {
        serviceScope.launch {
            try {
                // Parse connection request: "ip:port"
                val requestStr = String(data)
                val parts = requestStr.split(":")
                if (parts.size != 2) {
                    sendError("Invalid connection request format")
                    return@launch
                }

                val ip = parts[0]
                val port = parts[1].toIntOrNull() ?: run {
                    sendError("Invalid port number")
                    return@launch
                }

                Log.d(TAG, "Connecting to watch ADB at $ip:$port")

                val result = remoteWatchAdbClient.connect(ip, port)
                result.onSuccess {
                    Log.d(TAG, "Connected to watch ADB")
                }.onFailure { e ->
                    sendError("Connection failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connect request handling failed", e)
                sendError("Connection error: ${e.message}")
            }
        }
    }

    /**
     * Handles ADB disconnect request from watch.
     */
    private fun handleDisconnectRequest() {
        serviceScope.launch {
            remoteWatchAdbClient.disconnect()
        }
    }

    /**
     * Sends an error status to the watch.
     */
    private suspend fun sendError(message: String) {
        wearableDataClient.sendOptimizationStatus(
            WatchOptimizationStatus(
                isRunning = false,
                isComplete = false,
                errorMessage = message
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "WatchCommandListener"
    }
}
