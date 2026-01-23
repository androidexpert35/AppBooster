package com.tony.appbooster.data.client

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.tony.appbooster.di.IoDispatcher
import com.tony.appbooster.domain.client.WearableDataClient
import com.tony.appbooster.domain.model.wearable.PhoneReadinessStatus
import com.tony.appbooster.domain.model.wearable.WatchOptimizationStatus
import com.tony.appbooster.domain.model.wearable.WearableConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [WearableDataClient] using Google Play Services Wearable API.
 *
 * Handles communication between the phone app and watch app via:
 * - [DataClient] for synced state (optimization status, phone readiness)
 * - [MessageClient] for commands and responses
 * - [NodeClient] for discovering connected watches
 *
 * @property context Application context for Wearable API access.
 * @property ioDispatcher Dispatcher for IO operations.
 */
@Singleton
class WearableDataClientImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WearableDataClient {

    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }

    override val isWatchConnected: Flow<Boolean> = callbackFlow {
        // Initial check
        try {
            val nodes = nodeClient.connectedNodes.await()
            send(nodes.any { node -> node.isNearby })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connected nodes", e)
            send(false)
        }

        // Use CapabilityClient to listen for capability changes
        val capabilityListener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            trySend(capabilityInfo.nodes.any { node -> node.isNearby })
        }

        try {
            capabilityClient.addListener(capabilityListener, WATCH_CAPABILITY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add capability listener", e)
        }

        awaitClose {
            try {
                capabilityClient.removeListener(capabilityListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove capability listener", e)
            }
        }
    }

    override suspend fun sendOptimizationStatus(status: WatchOptimizationStatus) {
        withContext(ioDispatcher) {
            try {
                val request = PutDataMapRequest.create(WearableConstants.DATA_OPTIMIZATION_STATUS).apply {
                    dataMap.putBoolean(WearableConstants.KEY_IS_RUNNING, status.isRunning)
                    dataMap.putBoolean(WearableConstants.KEY_IS_COMPLETE, status.isComplete)
                    dataMap.putString(WearableConstants.KEY_CURRENT_APP, status.currentApp ?: "")
                    dataMap.putInt(WearableConstants.KEY_PROGRESS_CURRENT, status.progressCurrent)
                    dataMap.putInt(WearableConstants.KEY_PROGRESS_TOTAL, status.progressTotal)
                    dataMap.putString(WearableConstants.KEY_OPTIMIZATION_TYPE, status.optimizationType)
                    dataMap.putString(WearableConstants.KEY_ERROR_MESSAGE, status.errorMessage ?: "")
                    dataMap.putLong(WearableConstants.KEY_TIMESTAMP, status.timestamp)
                }
                    .asPutDataRequest()
                    .setUrgent() // Immediate sync for real-time progress updates

                dataClient.putDataItem(request).await()
                Log.d(TAG, "Sent optimization status: ${status.progressCurrent}/${status.progressTotal}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send optimization status", e)
            }
        }
    }

    override suspend fun sendPhoneReadiness(status: PhoneReadinessStatus) {
        withContext(ioDispatcher) {
            try {
                val request = PutDataMapRequest.create(WearableConstants.DATA_PHONE_STATUS).apply {
                    dataMap.putBoolean(WearableConstants.KEY_PHONE_READY, status.isPhoneConnected)
                    dataMap.putBoolean(WearableConstants.KEY_ADB_CONNECTED, status.isAdbConnectedToWatch)
                    dataMap.putBoolean(WearableConstants.KEY_SHIZUKU_AVAILABLE, status.isShizukuAvailable)
                    dataMap.putString(WearableConstants.KEY_WATCH_IP, status.watchIpAddress ?: "")
                    dataMap.putLong(WearableConstants.KEY_TIMESTAMP, status.lastSeen)
                }
                    .asPutDataRequest()
                    .setUrgent()

                dataClient.putDataItem(request).await()
                Log.d(TAG, "Sent phone readiness: connected=${status.isPhoneConnected}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send phone readiness", e)
            }
        }
    }

    override suspend fun sendPong(nodeId: String, isReady: Boolean) {
        withContext(ioDispatcher) {
            try {
                val payload = if (isReady) "ready".toByteArray() else "not_ready".toByteArray()
                messageClient.sendMessage(nodeId, WearableConstants.PATH_PONG, payload).await()
                Log.d(TAG, "Sent pong to $nodeId: ready=$isReady")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send pong", e)
            }
        }
    }

    override suspend fun getConnectedWatchNodeId(): String? = withContext(ioDispatcher) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.firstOrNull { node -> node.isNearby }?.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get watch node ID", e)
            null
        }
    }

    override suspend fun clearSyncedData() {
        withContext(ioDispatcher) {
            try {
                // Delete all data items we've created
                val dataItems = dataClient.dataItems.await()
                dataItems.forEach { dataItem ->
                    if (dataItem.uri.path?.startsWith("/status") == true) {
                        dataClient.deleteDataItems(dataItem.uri).await()
                    }
                }
                dataItems.release()
                Log.d(TAG, "Cleared synced data")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear synced data", e)
            }
        }
    }

    companion object {
        private const val TAG = "WearableDataClient"

        /**
         * Capability name for discovering watch nodes.
         * Must match the capability declared in wear app's wear.xml.
         */
        private const val WATCH_CAPABILITY = "appbooster_watch"
    }
}
