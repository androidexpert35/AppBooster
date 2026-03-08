package com.tony.appbooster.domain.repository

/**
 * Describes the current high-level state of the ADB / Shizuku shell connection.
 *
 * Presentation layers observe this sealed hierarchy to drive UI affordances
 * such as connection indicators, error banners, and action button enablement.
 */
sealed interface AdbConnectionState {

    /** No shell session is active and no connection attempt is in progress. */
    data object Disconnected : AdbConnectionState

    /** Actively scanning for an available ADB port on the target device. */
    data object SearchingPort : AdbConnectionState

    /** A connection attempt is underway but not yet confirmed. */
    data object Connecting : AdbConnectionState

    /** Shell session is established and ready to execute commands. */
    data object Connected : AdbConnectionState

    /**
     * The connection attempt failed or an existing session was lost.
     *
     * @property message Human-readable description of the failure cause.
     */
    data class Error(val message: String) : AdbConnectionState
}