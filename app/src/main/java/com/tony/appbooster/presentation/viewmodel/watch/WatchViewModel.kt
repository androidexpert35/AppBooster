package com.tony.appbooster.presentation.viewmodel.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tony.appbooster.data.client.RemoteWatchAdbClient
import com.tony.appbooster.domain.client.WearableDataClient
import com.tony.appbooster.domain.model.wearable.PhoneReadinessStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchViewModel @Inject constructor(
    private val remoteWatchAdbClient: RemoteWatchAdbClient,
    private val wearableDataClient: WearableDataClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchUiState())

    val uiState: StateFlow<WatchUiState> = combine(
        _uiState,
        remoteWatchAdbClient.connectionState,
        wearableDataClient.isWatchConnected
    ) { state, adbState, isWatchConnected ->
        state.copy(
            adbConnectionState = adbState,
            isWatchConnected = isWatchConnected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WatchUiState()
    )

    init {
        // Broadcast readiness initially
        viewModelScope.launch {
            broadcastReadiness()
        }
    }

    fun updateInputs(
        ip: String? = null,
        pairPort: String? = null,
        pairCode: String? = null,
        connectPort: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                pairingIp = ip ?: current.pairingIp,
                pairingPort = pairPort ?: current.pairingPort,
                pairingCode = pairCode ?: current.pairingCode,
                connectionPort = connectPort ?: current.connectionPort
            )
        }
    }

    fun pair() {
        val state = _uiState.value
        val ip = state.pairingIp
        val port = state.pairingPort.toIntOrNull()
        val code = state.pairingCode

        if (ip.isBlank() || port == null || code.isBlank()) {
            _uiState.update { it.copy(lastError = "Invalid pairing input") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastError = null) }
            // Note: This relies on pair() being available in RemoteWatchAdbClient
            val result = remoteWatchAdbClient.pair(ip, port, code)
            result.onSuccess {
                _uiState.update { it.copy(isBusy = false, lastMessage = "Pairing successful! Now connect.") }
            }.onFailure { e ->
                _uiState.update { it.copy(isBusy = false, lastError = "Pairing failed: ${e.message}") }
            }
        }
    }

    fun connect() {
        val state = _uiState.value
        val ip = state.pairingIp // Use same IP
        val port = state.connectionPort.toIntOrNull()

        if (ip.isBlank() || port == null) {
            _uiState.update { it.copy(lastError = "Invalid connection input") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastError = null) }
            val result = remoteWatchAdbClient.connect(ip, port)
            result.onSuccess {
                _uiState.update { it.copy(isBusy = false, lastMessage = "Connected to watch!") }
                broadcastReadiness()
            }.onFailure { e ->
                _uiState.update { it.copy(isBusy = false, lastError = "Connection failed: ${e.message}") }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            remoteWatchAdbClient.disconnect()
            broadcastReadiness()
        }
    }

    private suspend fun broadcastReadiness() {
        val adbConnected = remoteWatchAdbClient.isConnected()
        wearableDataClient.sendPhoneReadiness(
            PhoneReadinessStatus(
                isPhoneConnected = true,
                isAdbConnectedToWatch = adbConnected,
                isShizukuAvailable = false // Not checking Shizuku for this context yet
            )
        )
    }
}

data class WatchUiState(
    val pairingIp: String = "192.168.",
    val pairingPort: String = "",
    val pairingCode: String = "",
    val connectionPort: String = "",
    val isBusy: Boolean = false,
    val lastError: String? = null,
    val lastMessage: String? = null,
    val adbConnectionState: RemoteWatchAdbClient.ConnectionState = RemoteWatchAdbClient.ConnectionState.Disconnected,
    val isWatchConnected: Boolean = false
)
