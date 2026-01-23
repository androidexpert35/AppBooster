package com.tony.appbooster.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tony.appbooster.wear.data.client.PhoneBridgeClient
import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.model.wearable.ConnectionMode
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import com.tony.appbooster.wear.domain.usecase.CancelOptimizationUseCase
import com.tony.appbooster.wear.domain.usecase.ConnectAdbUseCase
import com.tony.appbooster.wear.domain.usecase.OptimizeAppsUseCase
import com.tony.appbooster.wear.presentation.model.WearHomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main Wear OS home screen.
 *
 * Supports two connection modes:
 * 1. **Phone Bridge (Preferred)**: Phone connects to watch's ADB and executes commands.
 *    Better UX as user enters pairing info on phone, not the small watch screen.
 * 2. **Self-Connection (Fallback)**: Watch connects to its own ADB daemon.
 *    Has UX issues with pairing codes disappearing when switching apps.
 *
 * @property repository Repository for local ADB operations.
 * @property phoneBridgeClient Client for phone communication via Wearable Data Layer.
 * @property connectAdbUseCase Use case for connecting to ADB.
 * @property optimizeAppsUseCase Use case for running optimization.
 * @property cancelOptimizationUseCase Use case for cancelling optimization.
 */
@HiltViewModel
class WearHomeViewModel @Inject constructor(
    private val repository: WearAdbRepository,
    private val phoneBridgeClient: PhoneBridgeClient,
    private val connectAdbUseCase: ConnectAdbUseCase,
    private val optimizeAppsUseCase: OptimizeAppsUseCase,
    private val cancelOptimizationUseCase: CancelOptimizationUseCase
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalState())

    /**
     * Combined UI state from repository flows, phone bridge, and local state.
     */
    val uiState: StateFlow<WearHomeUiState> = combine(
        repository.connectionState,
        repository.optimizationProgress,
        phoneBridgeClient.connectionMode,
        phoneBridgeClient.phoneStatus,
        phoneBridgeClient.optimizationStatus,
        _localState
    ) { values ->
        val connectionState = values[0] as com.tony.appbooster.wear.domain.model.AdbConnectionState
        val localProgress = values[1] as com.tony.appbooster.wear.domain.model.OptimizationProgress
        val connectionMode = values[2] as ConnectionMode
        val phoneStatus = values[3] as com.tony.appbooster.wear.domain.model.wearable.PhoneStatus
        val phoneOptimization = values[4] as com.tony.appbooster.wear.domain.model.wearable.OptimizationStatusFromPhone
        val local = values[5] as LocalState

        // Use phone bridge progress when in bridge mode and phone is updating
        val effectiveProgress = if (connectionMode == ConnectionMode.PHONE_BRIDGE &&
            (phoneOptimization.isRunning || phoneOptimization.isComplete || phoneOptimization.errorMessage != null)) {
            phoneBridgeClient.toOptimizationProgress(phoneOptimization)
        } else {
            localProgress
        }

        // Determine error message - prioritize phone errors in bridge mode
        val errorMessage = when {
            connectionMode == ConnectionMode.PHONE_BRIDGE && phoneOptimization.errorMessage != null ->
                phoneOptimization.errorMessage
            else -> local.errorMessage
        }

        WearHomeUiState(
            connectionState = connectionState,
            optimizationProgress = effectiveProgress,
            selectedMode = local.selectedMode,
            hasPaired = local.hasPaired,
            isLoading = local.isLoading,
            errorMessage = errorMessage,
            connectionMode = connectionMode,
            isPhoneConnected = phoneStatus.isConnected,
            isPhoneAdbReady = phoneStatus.isAdbConnectedToWatch
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WearHomeUiState()
    )

    init {
        checkPairingStatus()
        observePhoneConnection()
        startObservingPhoneData()
    }

    /**
     * Observes phone connection status via Wearable Data Layer.
     */
    private fun observePhoneConnection() {
        viewModelScope.launch {
            phoneBridgeClient.isPhoneConnected.collect { isConnected ->
                if (isConnected) {
                    // Phone is connected - this is the preferred mode
                    // Ping phone to check if it's ready
                    phoneBridgeClient.pingPhone()
                }
            }
        }
    }

    /**
     * Starts observing data changes from the phone.
     */
    private fun startObservingPhoneData() {
        viewModelScope.launch {
            phoneBridgeClient.startObservingPhoneData().collect {
                // Data updates are automatically handled by the phoneBridgeClient flows
            }
        }
    }

    /**
     * Checks if the device has been paired before and attempts auto-connect.
     */
    private fun checkPairingStatus() {
        viewModelScope.launch {
            val hasPaired = repository.hasPaired()
            _localState.update { it.copy(hasPaired = hasPaired) }

            // If previously paired, attempt auto-connect
            if (hasPaired) {
                attemptAutoConnect()
            }
        }
    }

    /**
     * Attempts to auto-connect to ADB.
     */
    private fun attemptAutoConnect() {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }
            when (val result = connectAdbUseCase.autoConnect()) {
                is Resource.Success -> {
                    _localState.update { it.copy(isLoading = false, errorMessage = null) }
                }
                is Resource.Error -> {
                    _localState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Auto-connect failed. Enable Wireless Debugging."
                        )
                    }
                }
            }
        }
    }

    /**
     * Starts the optimization process with the selected mode.
     *
     * Uses phone bridge mode if available, otherwise falls back to self-connection.
     */
    fun startOptimization() {
        if (!uiState.value.canStartOptimization) return

        viewModelScope.launch {
            _localState.update { it.copy(errorMessage = null) }

            val mode = uiState.value.selectedMode

            // Prefer phone bridge mode
            if (uiState.value.isUsingPhoneBridge) {
                val result = phoneBridgeClient.requestStartOptimization(mode)
                result.onFailure { e ->
                    _localState.update { it.copy(errorMessage = "Failed to start: ${e.message}") }
                }
                // Success is handled by the phone status updates via Data Layer
            } else {
                // Fallback to self-connection mode
                when (val result = optimizeAppsUseCase(mode)) {
                    is Resource.Success -> {
                        // Success handled by progress flow
                    }
                    is Resource.Error -> {
                        _localState.update { it.copy(errorMessage = result.error.toString()) }
                    }
                }
            }
        }
    }

    /**
     * Cancels the currently running optimization.
     */
    fun cancelOptimization() {
        viewModelScope.launch {
            if (uiState.value.isUsingPhoneBridge) {
                phoneBridgeClient.requestCancelOptimization()
            } else {
                cancelOptimizationUseCase()
            }
        }
    }

    /**
     * Toggles the optimization mode.
     */
    fun toggleMode() {
        _localState.update { state ->
            val newMode = when (state.selectedMode) {
                OptimizationType.SPEED_PROFILE -> OptimizationType.FULL_OPTIMIZATION
                OptimizationType.FULL_OPTIMIZATION -> OptimizationType.SPEED_PROFILE
            }
            state.copy(selectedMode = newMode)
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _localState.update { it.copy(errorMessage = null) }
    }

    /**
     * Retries connection after an error.
     */
    fun retryConnection() {
        attemptAutoConnect()
    }

    /**
     * Requests the phone to connect to this watch's ADB.
     * User must have Wireless Debugging enabled and provide the connection port.
     *
     * @param connectionPort The port shown in Wireless Debugging settings (NOT pairing port).
     */
    fun requestPhoneConnection(connectionPort: Int) {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = phoneBridgeClient.requestPhoneConnect(connectionPort)

            result.onSuccess {
                _localState.update { it.copy(isLoading = false) }
                // Connection status will be updated via phone status updates
            }.onFailure { e ->
                _localState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Connection request failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Gets the watch's current IP address for display to user.
     */
    fun getWatchIpAddress(): String? {
        return phoneBridgeClient.getWatchIpAddress()
    }

    /**
     * Local state that doesn't come from repository flows.
     */
    private data class LocalState(
        val selectedMode: OptimizationType = OptimizationType.SPEED_PROFILE,
        val hasPaired: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )
}
