package com.tony.appbooster.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.usecase.ConnectAdbUseCase
import com.tony.appbooster.wear.domain.usecase.PairAdbUseCase
import com.tony.appbooster.wear.presentation.model.PairingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the ADB pairing screen.
 *
 * Handles the one-time pairing flow with the local ADB daemon
 * and subsequent connection.
 *
 * @property pairAdbUseCase Use case for pairing with ADB.
 * @property connectAdbUseCase Use case for connecting to ADB.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairAdbUseCase: PairAdbUseCase,
    private val connectAdbUseCase: ConnectAdbUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    /**
     * Updates the pairing port input.
     */
    fun onPairingPortChanged(port: String) {
        _uiState.update { it.copy(pairingPort = port.filter { c -> c.isDigit() }) }
    }

    /**
     * Updates the pairing code input.
     */
    fun onPairingCodeChanged(code: String) {
        _uiState.update { it.copy(pairingCode = code.filter { c -> c.isDigit() }.take(6)) }
    }

    /**
     * Updates the connection port input.
     */
    fun onConnectionPortChanged(port: String) {
        _uiState.update { it.copy(connectionPort = port.filter { c -> c.isDigit() }) }
    }

    /**
     * Initiates the pairing process.
     */
    fun pair() {
        val state = _uiState.value
        if (!state.canPair) return

        val port = state.pairingPort.toIntOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, errorMessage = null) }

            when (val result = pairAdbUseCase(port, state.pairingCode)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isPairing = false, pairingSuccess = true) }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isPairing = false,
                            errorMessage = "Pairing failed. Check the code and try again."
                        )
                    }
                }
            }
        }
    }

    /**
     * Initiates the connection after successful pairing.
     */
    fun connect() {
        val state = _uiState.value
        if (!state.canConnect) return

        val port = state.connectionPort.toIntOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }

            when (val result = connectAdbUseCase(port)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isConnecting = false) }
                    // Connection successful - navigation will be handled by the screen
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = "Connection failed. Check the port and try again."
                        )
                    }
                }
            }
        }
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Marks pairing as complete without actually pairing.
     * Used when the device was already paired via PC/ADB.
     */
    fun markAsPaired() {
        _uiState.update { it.copy(pairingSuccess = true) }
    }
}
