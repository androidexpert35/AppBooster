package com.tony.appbooster.wear.presentation.model

/**
 * UI state for the ADB pairing screen.
 *
 * @property pairingPort The port number for pairing.
 * @property pairingCode The 6-digit pairing code.
 * @property connectionPort The port for ADB connection (after pairing).
 * @property isPairing Whether pairing is in progress.
 * @property isConnecting Whether connection is in progress.
 * @property pairingSuccess Whether pairing completed successfully.
 * @property errorMessage Error message to display, if any.
 */
data class PairingUiState(
    val pairingPort: String = "",
    val pairingCode: String = "",
    val connectionPort: String = "",
    val isPairing: Boolean = false,
    val isConnecting: Boolean = false,
    val pairingSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Whether the pair button should be enabled.
     */
    val canPair: Boolean
        get() = pairingPort.isNotBlank() &&
                pairingCode.length == 6 &&
                !isPairing &&
                !pairingSuccess

    /**
     * Whether the connect button should be enabled.
     */
    val canConnect: Boolean
        get() = connectionPort.isNotBlank() &&
                pairingSuccess &&
                !isConnecting
}
