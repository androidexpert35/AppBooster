package com.tony.appbooster.wear.data.client

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.tony.appbooster.wear.domain.model.OptimizationProgress
import com.tony.appbooster.wear.domain.model.OptimizationResult
import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.wearable.ConnectionMode
import com.tony.appbooster.wear.domain.model.wearable.OptimizationStatusFromPhone
import com.tony.appbooster.wear.domain.model.wearable.PhoneStatus
import com.tony.appbooster.wear.domain.model.wearable.WearableConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for communicating with the phone app via Wearable Data Layer.
 *
 * This is the preferred mode for watch optimization - the phone connects to
 * the watch's wireless ADB and executes commands on behalf of the watch.
 *
 * Benefits over self-connection:
 * - Better UX for pairing (user enters code on phone, not watch)
 * - Phone can maintain persistent ADB connection
 * - Watch just displays status and triggers actions
 *
 * @property context Application context for Wearable API access.
 * @property ioDispatcher Dispatcher for IO operations.
 */
@Singleton
class PhoneBridgeClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private val _phoneStatus = MutableStateFlow(PhoneStatus())

    /**
     * Current phone status and readiness.
     */
    val phoneStatus: StateFlow<PhoneStatus> = _phoneStatus.asStateFlow()

    private val _optimizationStatus = MutableStateFlow(OptimizationStatusFromPhone())

    /**
     * Current optimization status from phone.
     */
    val optimizationStatus: StateFlow<OptimizationStatusFromPhone> = _optimizationStatus.asStateFlow()

    private val _connectionMode = MutableStateFlow(ConnectionMode.UNKNOWN)

    /**
     * Current connection mode (phone bridge vs self-connection).
     */
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private var phoneNodeId: String? = null

    /**
     * Flow indicating whether the phone is connected and reachable.
     */
    val isPhoneConnected: Flow<Boolean> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            val hasPhone = capabilityInfo.nodes.any { node -> node.isNearby }
            phoneNodeId = capabilityInfo.nodes.firstOrNull { node -> node.isNearby }?.id
            trySend(hasPhone)

            if (hasPhone) {
                _connectionMode.value = ConnectionMode.PHONE_BRIDGE
            }
        }

        // Initial check
        try {
            val capabilityInfo = capabilityClient.getCapability(
                WearableConstants.PHONE_CAPABILITY,
                CapabilityClient.FILTER_REACHABLE
            ).await()

            val hasPhone = capabilityInfo.nodes.any { node -> node.isNearby }
            phoneNodeId = capabilityInfo.nodes.firstOrNull { node -> node.isNearby }?.id
            send(hasPhone)

            if (hasPhone) {
                _connectionMode.value = ConnectionMode.PHONE_BRIDGE
            } else {
                _connectionMode.value = ConnectionMode.SELF_CONNECTION
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check phone capability", e)
            send(false)
            _connectionMode.value = ConnectionMode.SELF_CONNECTION
        }

        capabilityClient.addListener(listener, WearableConstants.PHONE_CAPABILITY)

        awaitClose {
            capabilityClient.removeListener(listener)
        }
    }

    /**
     * Starts observing data changes from the phone.
     *
     * Call this when the watch UI is active to receive real-time updates.
     */
    fun startObservingPhoneData(): Flow<Unit> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            processDataEvents(dataEvents)
            trySend(Unit)
        }

        dataClient.addListener(listener)

        // Load initial data
        loadCurrentData()
        send(Unit)

        awaitClose {
            dataClient.removeListener(listener)
        }
    }

    /**
     * Processes incoming data events from the phone.
     */
    private fun processDataEvents(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path ?: continue
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                when (path) {
                    WearableConstants.DATA_OPTIMIZATION_STATUS -> {
                        _optimizationStatus.value = OptimizationStatusFromPhone(
                            isRunning = dataMap.getBoolean(WearableConstants.KEY_IS_RUNNING),
                            isComplete = dataMap.getBoolean(WearableConstants.KEY_IS_COMPLETE),
                            currentApp = dataMap.getString(WearableConstants.KEY_CURRENT_APP)?.takeIf { str -> str.isNotBlank() },
                            progressCurrent = dataMap.getInt(WearableConstants.KEY_PROGRESS_CURRENT),
                            progressTotal = dataMap.getInt(WearableConstants.KEY_PROGRESS_TOTAL),
                            optimizationType = dataMap.getString(WearableConstants.KEY_OPTIMIZATION_TYPE)
                                ?: WearableConstants.OPTIMIZATION_TYPE_SPEED_PROFILE,
                            errorMessage = dataMap.getString(WearableConstants.KEY_ERROR_MESSAGE)?.takeIf { str -> str.isNotBlank() },
                            timestamp = dataMap.getLong(WearableConstants.KEY_TIMESTAMP)
                        )
                        Log.d(TAG, "Received optimization status: ${_optimizationStatus.value}")
                    }

                    WearableConstants.DATA_PHONE_STATUS -> {
                        _phoneStatus.value = PhoneStatus(
                            isConnected = dataMap.getBoolean(WearableConstants.KEY_PHONE_READY),
                            isAdbConnectedToWatch = dataMap.getBoolean(WearableConstants.KEY_ADB_CONNECTED),
                            isShizukuAvailable = dataMap.getBoolean(WearableConstants.KEY_SHIZUKU_AVAILABLE),
                            watchIpAddress = dataMap.getString(WearableConstants.KEY_WATCH_IP)?.takeIf { str -> str.isNotBlank() },
                            lastSeen = dataMap.getLong(WearableConstants.KEY_TIMESTAMP)
                        )
                        Log.d(TAG, "Received phone status: ${_phoneStatus.value}")
                    }
                }
            }
        }
    }

    /**
     * Loads current data items from the DataClient.
     */
    private suspend fun loadCurrentData() {
        try {
            val dataItems = dataClient.dataItems.await()
            for (dataItem in dataItems) {
                val path = dataItem.uri.path ?: continue
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

                when (path) {
                    WearableConstants.DATA_OPTIMIZATION_STATUS -> {
                        _optimizationStatus.value = OptimizationStatusFromPhone(
                            isRunning = dataMap.getBoolean(WearableConstants.KEY_IS_RUNNING),
                            isComplete = dataMap.getBoolean(WearableConstants.KEY_IS_COMPLETE),
                            currentApp = dataMap.getString(WearableConstants.KEY_CURRENT_APP)?.takeIf { str -> str.isNotBlank() },
                            progressCurrent = dataMap.getInt(WearableConstants.KEY_PROGRESS_CURRENT),
                            progressTotal = dataMap.getInt(WearableConstants.KEY_PROGRESS_TOTAL),
                            optimizationType = dataMap.getString(WearableConstants.KEY_OPTIMIZATION_TYPE)
                                ?: WearableConstants.OPTIMIZATION_TYPE_SPEED_PROFILE,
                            errorMessage = dataMap.getString(WearableConstants.KEY_ERROR_MESSAGE)?.takeIf { str -> str.isNotBlank() },
                            timestamp = dataMap.getLong(WearableConstants.KEY_TIMESTAMP)
                        )
                    }
                    WearableConstants.DATA_PHONE_STATUS -> {
                        _phoneStatus.value = PhoneStatus(
                            isConnected = dataMap.getBoolean(WearableConstants.KEY_PHONE_READY),
                            isAdbConnectedToWatch = dataMap.getBoolean(WearableConstants.KEY_ADB_CONNECTED),
                            isShizukuAvailable = dataMap.getBoolean(WearableConstants.KEY_SHIZUKU_AVAILABLE),
                            watchIpAddress = dataMap.getString(WearableConstants.KEY_WATCH_IP)?.takeIf { str -> str.isNotBlank() },
                            lastSeen = dataMap.getLong(WearableConstants.KEY_TIMESTAMP)
                        )
                    }
                }
            }
            dataItems.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load current data", e)
        }
    }

    /**
     * Sends a request to the phone to start optimization.
     *
     * @param type The optimization type to perform.
     * @return Result indicating success or failure.
     */
    suspend fun requestStartOptimization(type: OptimizationType): Result<Unit> = withContext(ioDispatcher) {
        val nodeId = phoneNodeId ?: run {
            return@withContext Result.failure(IllegalStateException("Phone not connected"))
        }

        try {
            val optimizationType = when (type) {
                OptimizationType.SPEED_PROFILE -> WearableConstants.OPTIMIZATION_TYPE_SPEED_PROFILE
                OptimizationType.FULL_OPTIMIZATION -> WearableConstants.OPTIMIZATION_TYPE_FULL
            }

            messageClient.sendMessage(
                nodeId,
                WearableConstants.PATH_START_OPTIMIZATION,
                optimizationType.toByteArray()
            ).await()

            Log.d(TAG, "Sent start optimization request: $optimizationType")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send start optimization request", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a request to the phone to cancel the current optimization.
     *
     * @return Result indicating success or failure.
     */
    suspend fun requestCancelOptimization(): Result<Unit> = withContext(ioDispatcher) {
        val nodeId = phoneNodeId ?: run {
            return@withContext Result.failure(IllegalStateException("Phone not connected"))
        }

        try {
            messageClient.sendMessage(
                nodeId,
                WearableConstants.PATH_CANCEL_OPTIMIZATION,
                byteArrayOf()
            ).await()

            Log.d(TAG, "Sent cancel optimization request")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send cancel optimization request", e)
            Result.failure(e)
        }
    }

    /**
     * Sends the watch's IP and ADB connection port to the phone for connection.
     *
     * @param port The ADB connection port (from Wireless Debugging settings).
     * @return Result indicating success or failure.
     */
    suspend fun requestPhoneConnect(port: Int): Result<Unit> = withContext(ioDispatcher) {
        val nodeId = phoneNodeId ?: run {
            return@withContext Result.failure(IllegalStateException("Phone not connected"))
        }

        try {
            val watchIp = getWatchIpAddress() ?: run {
                return@withContext Result.failure(IllegalStateException("Could not determine watch IP"))
            }

            val payload = "$watchIp:$port"
            messageClient.sendMessage(
                nodeId,
                WearableConstants.PATH_CONNECT_REQUEST,
                payload.toByteArray()
            ).await()

            Log.d(TAG, "Sent connect request: $payload")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send connect request", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a ping to the phone to check if it's ready.
     *
     * @return Result with readiness status or failure.
     */
    suspend fun pingPhone(): Result<Boolean> = withContext(ioDispatcher) {
        val nodeId = phoneNodeId ?: run {
            return@withContext Result.failure(IllegalStateException("Phone not connected"))
        }

        try {
            messageClient.sendMessage(
                nodeId,
                WearableConstants.PATH_PING,
                byteArrayOf()
            ).await()

            Log.d(TAG, "Sent ping to phone")
            // The response will come via the pong message handler
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ping phone", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the watch's IP address on the current Wi-Fi network.
     *
     * @return The IP address as a string, or null if not available.
     */
    fun getWatchIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses ?: continue
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get watch IP address", e)
        }
        return null
    }

    /**
     * Converts the optimization status from phone to the local progress model.
     */
    fun toOptimizationProgress(status: OptimizationStatusFromPhone): OptimizationProgress {
        val result = when {
            status.isComplete -> OptimizationResult.Completed
            status.errorMessage != null -> OptimizationResult.Canceled
            else -> OptimizationResult.None
        }

        return OptimizationProgress(
            runId = status.timestamp,
            isRunning = status.isRunning,
            result = result,
            currentAppPackage = status.currentApp ?: "",
            progress = if (status.progressTotal > 0) {
                status.progressCurrent.toFloat() / status.progressTotal
            } else 0f,
            processedCount = status.progressCurrent,
            totalCount = status.progressTotal
        )
    }

    companion object {
        private const val TAG = "PhoneBridgeClient"
    }
}
