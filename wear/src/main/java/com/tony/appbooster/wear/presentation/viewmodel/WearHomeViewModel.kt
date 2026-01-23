package com.tony.appbooster.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.Resource
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
 * Manages ADB connection state, optimization progress, and user actions.
 *
 * @property repository Repository for ADB operations.
 * @property connectAdbUseCase Use case for connecting to ADB.
 * @property optimizeAppsUseCase Use case for running optimization.
 * @property cancelOptimizationUseCase Use case for cancelling optimization.
 */
@HiltViewModel
class WearHomeViewModel @Inject constructor(
    private val repository: WearAdbRepository,
    private val connectAdbUseCase: ConnectAdbUseCase,
    private val optimizeAppsUseCase: OptimizeAppsUseCase,
    private val cancelOptimizationUseCase: CancelOptimizationUseCase
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalState())

    /**
     * Combined UI state from repository flows and local state.
     */
    val uiState: StateFlow<WearHomeUiState> = combine(
        repository.connectionState,
        repository.optimizationProgress,
        _localState
    ) { connectionState, progress, local ->
        WearHomeUiState(
            connectionState = connectionState,
            optimizationProgress = progress,
            selectedMode = local.selectedMode,
            hasPaired = local.hasPaired,
            isLoading = local.isLoading,
            errorMessage = local.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WearHomeUiState()
    )

    init {
        checkPairingStatus()
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
     */
    fun startOptimization() {
        if (!uiState.value.canStartOptimization) return

        viewModelScope.launch {
            _localState.update { it.copy(errorMessage = null) }
            when (val result = optimizeAppsUseCase(uiState.value.selectedMode)) {
                is Resource.Success -> {
                    // Success handled by progress flow
                }
                is Resource.Error -> {
                    _localState.update { it.copy(errorMessage = result.error.toString()) }
                }
            }
        }
    }

    /**
     * Cancels the currently running optimization.
     */
    fun cancelOptimization() {
        viewModelScope.launch {
            cancelOptimizationUseCase()
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
     * Local state that doesn't come from repository flows.
     */
    private data class LocalState(
        val selectedMode: OptimizationType = OptimizationType.SPEED_PROFILE,
        val hasPaired: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )
}
