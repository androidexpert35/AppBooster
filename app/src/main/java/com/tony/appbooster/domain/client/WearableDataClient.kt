package com.tony.appbooster.domain.client

import com.tony.appbooster.domain.model.wearable.PhoneReadinessStatus
import com.tony.appbooster.domain.model.wearable.WatchOptimizationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Client interface for Wearable Data Layer communication with the watch.
 *
 * Provides bidirectional communication between phone and watch apps:
 * - Phone sends status updates to watch via DataClient
 * - Phone receives commands from watch via MessageClient
 * - Watch observes status via DataClient changes
 */
interface WearableDataClient {

    /**
     * Flow indicating whether a paired watch is connected and reachable.
     */
    val isWatchConnected: Flow<Boolean>

    /**
     * Sends the current optimization status to the watch.
     *
     * This updates a DataItem that the watch observes for UI updates.
     *
     * @param status The current optimization status.
     */
    suspend fun sendOptimizationStatus(status: WatchOptimizationStatus)

    /**
     * Sends the phone's readiness status to the watch.
     *
     * @param status The phone's current readiness state.
     */
    suspend fun sendPhoneReadiness(status: PhoneReadinessStatus)

    /**
     * Sends a response to a ping from the watch.
     *
     * @param nodeId The watch's node ID to send the response to.
     * @param isReady Whether the phone is ready to handle optimization.
     */
    suspend fun sendPong(nodeId: String, isReady: Boolean)

    /**
     * Retrieves the connected watch's node ID, if available.
     *
     * @return The node ID of the connected watch, or null if not connected.
     */
    suspend fun getConnectedWatchNodeId(): String?

    /**
     * Clears all synced data (used when disconnecting).
     */
    suspend fun clearSyncedData()
}
