package com.tony.appbooster.domain.model.wearable

/**
 * Constants for Wearable Data Layer communication between phone and watch.
 *
 * Defines message paths for commands and data paths for status sync.
 */
object WearableConstants {

    // ============== Message Paths (Commands from Watch to Phone) ==============

    /**
     * Request to start optimization on the watch.
     * Payload: [OptimizationRequest] serialized as JSON.
     */
    const val PATH_START_OPTIMIZATION = "/optimization/start"

    /**
     * Request to cancel the current optimization.
     * Payload: None.
     */
    const val PATH_CANCEL_OPTIMIZATION = "/optimization/cancel"

    /**
     * Request to pair with the watch's ADB daemon.
     * Payload: [WatchPairingRequest] serialized as JSON.
     */
    const val PATH_PAIR_REQUEST = "/adb/pair"

    /**
     * Request to connect to the watch's ADB.
     * Payload: [WatchConnectionRequest] serialized as JSON.
     */
    const val PATH_CONNECT_REQUEST = "/adb/connect"

    /**
     * Request to disconnect from the watch's ADB.
     * Payload: None.
     */
    const val PATH_DISCONNECT_REQUEST = "/adb/disconnect"

    /**
     * Ping to check if phone app is ready.
     * Payload: None.
     */
    const val PATH_PING = "/ping"

    /**
     * Response to ping.
     * Payload: [PhoneStatus] serialized as JSON.
     */
    const val PATH_PONG = "/pong"

    // ============== Data Paths (Status sync from Phone to Watch) ==============

    /**
     * Data item path for optimization status updates.
     * Contains: [OptimizationStatusData] serialized.
     */
    const val DATA_OPTIMIZATION_STATUS = "/status/optimization"

    /**
     * Data item path for ADB connection status.
     * Contains: [AdbConnectionStatusData] serialized.
     */
    const val DATA_ADB_STATUS = "/status/adb"

    /**
     * Data item path for phone readiness status.
     * Contains: [PhoneReadinessData] serialized.
     */
    const val DATA_PHONE_STATUS = "/status/phone"

    // ============== Data Keys ==============

    const val KEY_OPTIMIZATION_TYPE = "optimization_type"
    const val KEY_PROGRESS_CURRENT = "progress_current"
    const val KEY_PROGRESS_TOTAL = "progress_total"
    const val KEY_CURRENT_APP = "current_app"
    const val KEY_IS_RUNNING = "is_running"
    const val KEY_IS_COMPLETE = "is_complete"
    const val KEY_ERROR_MESSAGE = "error_message"
    const val KEY_TIMESTAMP = "timestamp"

    const val KEY_ADB_CONNECTED = "adb_connected"
    const val KEY_ADB_STATE = "adb_state"

    const val KEY_PHONE_READY = "phone_ready"
    const val KEY_SHIZUKU_AVAILABLE = "shizuku_available"

    const val KEY_PAIRING_PORT = "pairing_port"
    const val KEY_PAIRING_CODE = "pairing_code"
    const val KEY_CONNECTION_PORT = "connection_port"
    const val KEY_WATCH_IP = "watch_ip"

    // ============== Optimization Types (matching watch domain) ==============

    const val OPTIMIZATION_TYPE_SPEED_PROFILE = "speed_profile"
    const val OPTIMIZATION_TYPE_FULL = "full"
}
