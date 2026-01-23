package com.tony.appbooster.wear.domain.model.wearable

/**
 * Represents the phone's status and readiness to handle watch optimization.
 *
 * Received from phone via Wearable Data Layer.
 *
 * @property isConnected Whether the phone is connected to the watch.
 * @property isAdbConnectedToWatch Whether phone has ADB connection to watch.
 * @property isShizukuAvailable Whether Shizuku is ready on phone.
 * @property watchIpAddress The watch's IP address known to phone.
 * @property lastSeen Timestamp when phone was last seen.
 */
data class PhoneStatus(
    val isConnected: Boolean = false,
    val isAdbConnectedToWatch: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val watchIpAddress: String? = null,
    val lastSeen: Long = 0L
)

/**
 * Represents the current optimization status received from the phone.
 *
 * @property isRunning Whether optimization is currently in progress.
 * @property isComplete Whether optimization has completed.
 * @property currentApp Name of the app currently being optimized.
 * @property progressCurrent Number of apps processed so far.
 * @property progressTotal Total number of apps to process.
 * @property optimizationType Type of optimization being performed.
 * @property errorMessage Error message if optimization failed.
 * @property timestamp Timestamp of the last update.
 */
data class OptimizationStatusFromPhone(
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val currentApp: String? = null,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val optimizationType: String = WearableConstants.OPTIMIZATION_TYPE_SPEED_PROFILE,
    val errorMessage: String? = null,
    val timestamp: Long = 0L
)

/**
 * Represents the connection mode for the watch.
 */
enum class ConnectionMode {
    /**
     * Watch connects to its own ADB daemon (requires self-pairing).
     * This mode has UX issues with pairing codes.
     */
    SELF_CONNECTION,

    /**
     * Phone acts as a bridge, connecting to watch's ADB.
     * Preferred mode with better UX.
     */
    PHONE_BRIDGE,

    /**
     * Not yet determined or detecting.
     */
    UNKNOWN
}
