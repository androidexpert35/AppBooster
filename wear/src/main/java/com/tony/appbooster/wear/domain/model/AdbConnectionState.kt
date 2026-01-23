package com.tony.appbooster.wear.domain.model

/**
 * Represents the current state of the ADB connection.
 */
sealed interface AdbConnectionState {
    /**
     * No connection established.
     */
    data object Disconnected : AdbConnectionState

    /**
     * Searching for the ADB daemon port.
     */
    data object SearchingPort : AdbConnectionState

    /**
     * Pairing with the ADB daemon is required.
     */
    data object PairingRequired : AdbConnectionState

    /**
     * Currently attempting to connect.
     */
    data object Connecting : AdbConnectionState

    /**
     * Successfully connected to ADB daemon.
     */
    data object Connected : AdbConnectionState

    /**
     * Connection failed with an error.
     *
     * @property message Description of what went wrong.
     */
    data class Error(val message: String) : AdbConnectionState
}
