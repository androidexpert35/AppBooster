package com.tony.appbooster.domain.model.wearable

/**
 * Represents the current status of the watch optimization process.
 *
 * Synced from phone to watch via Wearable Data Layer.
 *
 * @property isRunning Whether optimization is currently in progress.
 * @property isComplete Whether optimization has completed.
 * @property currentApp Name of the app currently being optimized.
 * @property progressCurrent Number of apps processed so far.
 * @property progressTotal Total number of apps to process.
 * @property optimizationType Type of optimization being performed.
 * @property errorMessage Error message if optimization failed, null otherwise.
 * @property timestamp Timestamp of the last update.
 */
data class WatchOptimizationStatus(
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val currentApp: String? = null,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val optimizationType: String = WearableConstants.OPTIMIZATION_TYPE_SPEED_PROFILE,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the phone's readiness to handle watch optimization.
 *
 * @property isPhoneConnected Whether the phone app is connected to the watch.
 * @property isAdbConnectedToWatch Whether phone has ADB connection to watch.
 * @property isShizukuAvailable Whether Shizuku is ready on phone (for local ops).
 * @property watchIpAddress The watch's IP address if known.
 * @property lastSeen Timestamp when phone was last seen.
 */
data class PhoneReadinessStatus(
    val isPhoneConnected: Boolean = false,
    val isAdbConnectedToWatch: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val watchIpAddress: String? = null,
    val lastSeen: Long = 0L
)

/**
 * Request from watch to start optimization on the watch.
 *
 * @property optimizationType Type of optimization to perform.
 */
data class OptimizationRequest(
    val optimizationType: String
)

/**
 * Request from watch to pair phone with watch's ADB.
 *
 * @property watchIp The watch's IP address.
 * @property pairingPort The pairing port from wireless debugging settings.
 * @property pairingCode The 6-digit pairing code.
 */
data class WatchPairingRequest(
    val watchIp: String,
    val pairingPort: Int,
    val pairingCode: String
)

/**
 * Request from watch to connect phone to watch's ADB.
 *
 * @property watchIp The watch's IP address.
 * @property connectionPort The connection port from wireless debugging settings.
 */
data class WatchConnectionRequest(
    val watchIp: String,
    val connectionPort: Int
)
