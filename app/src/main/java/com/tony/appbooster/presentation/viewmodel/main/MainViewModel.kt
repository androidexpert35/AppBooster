package com.tony.appbooster.presentation.viewmodel.main

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.alkemy.boxapp.presentation.navigation.interfaces.NavigationManager
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository
import com.tony.appbooster.domain.usecase.ConnectAdbUseCase
import com.tony.appbooster.domain.usecase.ObserveAppOptimizationTypeUseCase
import com.tony.appbooster.domain.usecase.OptimizeAppUseCase
import com.tony.appbooster.presentation.viewmodel.base.BaseViewModel
import com.tony.appbooster.presentation.worker.OptimizationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for orchestrating the ADB connection and optimization
 * flows and exposing them as a unified [MainUiModel] state for the setup screen.
 *
 * The business purpose is to coordinate wireless ADB discovery, connection,
 * and ART optimization while surfacing progress, logs, and errors to the UI.
 *
 * @param connectAdbUseCase Use case that discovers the wireless debug port and connects to ADB.
 * @param optimizeAppUseCase Use case that triggers ART optimization on the connected device.
 * @param repository Repository exposing ADB connection, logs, and optimization progress.
 * @param navigationManager Manager used to dispatch navigation commands from the ViewModel.
 * @return A ViewModel instance exposing a single `StateFlow<UIState<MainUiModel>>`.
 * @throws IllegalStateException If required dependencies are not provided by DI.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val connectAdbUseCase: ConnectAdbUseCase,
    private val optimizeAppUseCase: OptimizeAppUseCase,
    private val getOptimizeAppUseCase: ObserveAppOptimizationTypeUseCase,
    private val repository: AdbRepository,
    @param:ApplicationContext private val appContext: Context,
    navigationManager: NavigationManager
) : BaseViewModel<MainUiModel, MainUiEvent, MainUiEffect>(navigationManager) {

    override val LOG_TAG: String = "MainViewModel"

    private val dismissedResultRunIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        observeRepository()
        observeOptimizationMode()
    }

    /**
     * Observes the persisted optimization mode setting so the Dashboard UI
     * can reflect changes immediately after the user updates Settings.
     */
    private fun observeOptimizationMode() {
        viewModelScope.launch(exceptionHandler) {
            getOptimizeAppUseCase()
                .collect { resource ->
                    val current = uiState.value.data ?: MainUiModel()
                    val newMode = when (resource) {
                        is Resource.Success -> resource.data
                        is Resource.Error -> current.optimizationMode
                    }
                    updateUiData(current.copy(optimizationMode = newMode))
                }
        }
    }

    /**
     * Starts the wireless ADB setup flow by discovering the port and connecting
     * to the local ADB instance. The result is pushed through [launchUiStateUpdate],
     * updating the UI status and surfacing any domain errors.
     *
     * @return Unit when the asynchronous operation has been launched.
     * @throws IllegalStateException If the use case throws unexpectedly.
     */
    fun startConnectionSequence() {
        launchUiStateUpdate(
            dataFetchBlock = { connectAdbUseCase() },
            processSuccess = {
                uiState.value.data ?: MainUiModel()
            }
        )
    }

    /**
     * Requests cancellation of an active optimization run.
     *
     * The business purpose is to allow the user to stop long-running work
     * without leaving the UI in a loading state.
     */
    fun stopOptimization() {
        launchUiStateUpdate(
            dataFetchBlock = { repository.cancelOptimization() },
            skipLoading = true,
            processSuccess = {
                // Keep current UI data; progress/logs are emitted from repository flows.
                uiState.value.data ?: MainUiModel()
            }
        )
    }


    /**
     * Observes ADB connection, command output and optimization progress from the
     * repository and maps them into a single [MainUiModel] pushed to the UI.
     *
     * This keeps the UI reactive to underlying ADB state without duplicating
     * logic in the composables.
     *
     * @return Unit when the observation coroutine has been launched.
     * @throws IllegalStateException If the coroutine scope is not available.
     */
    private fun observeRepository() {
        viewModelScope.launch(exceptionHandler) {
            combine(
                repository.connectionState,
                repository.commandOutput,
                repository.optimizationProgress,
                repository.optimizationAnalysis,
                dismissedResultRunIds
            ) { connectionState, logs, progress, analysis, dismissedRunIds ->
                MainUiModel(
                    connectionState = connectionState,
                    logs = logs,
                    optimizationProgress = progress,
                    optimizationAnalysis = analysis,
                    dismissedResultRunIds = dismissedRunIds
                )
            }.collect { model ->
                // Reuse BaseViewModel helper to update only data while preserving status.
                updateUiData(model)
            }
        }
    }

    /**
     * Triggers a pre-optimization analysis scan to determine which apps need optimization.
     * This is called automatically when the dashboard loads.
     */
    fun triggerAnalysis() {
        val mode = uiState.value.data?.optimizationMode ?: return
        viewModelScope.launch(exceptionHandler) {
            // First ensure Shizuku is connected
            val connectionResult = connectAdbUseCase()
            if (connectionResult is Resource.Success) {
                repository.analyzeOptimizationStatus(mode)
            }
        }
    }

    override fun handleEvent(event: MainUiEvent) {
        when (event) {
            MainUiEvent.OnConnectClicked -> startConnectionSequence()
            MainUiEvent.OnStartOptimizationClicked -> onStartOptimizationRequested()
            MainUiEvent.OnStopOptimizationClicked -> onStopOptimizationRequested()
            MainUiEvent.OnDismissOptimizationResultClicked -> onDismissOptimizationResultRequested()
            MainUiEvent.OnAnalyzeAppsClicked -> triggerAnalysis()
        }
    }

    private fun onDismissOptimizationResultRequested() {
        val runId = uiState.value.data?.optimizationProgress?.runId ?: 0L
        if (runId == 0L) return

        dismissedResultRunIds.update { current ->
            if (current.contains(runId)) current else current + runId
        }
    }

    private fun onStartOptimizationRequested() {
        val optimizationMode = uiState.value.data?.optimizationMode
        if (optimizationMode == null) {
            emitEffect(MainUiEffect.ShowSnackbar(appContext.getString(R.string.error_select_optimization_mode_first)))
            return
        }

        launchUiStateUpdate(
            dataFetchBlock = { connectAdbUseCase() },
            skipLoading = true,
            processSuccess = {
                // Keep current UI data; repository flows will push connection/progress/log updates.
                uiState.value.data ?: MainUiModel()
            },
            invokeOnCompletion = { success ->
                if (success) {
                    // New run will produce a new progress.runId from the repository; no explicit reset needed.
                    OptimizationWorker.enqueue(appContext, optimizationMode)
                }
            }
        )
    }

    private fun onStopOptimizationRequested() {
        // Cancel WorkManager so background execution stops and notification goes away.
        OptimizationWorker.cancel(appContext)

        // Also request repository-side cancellation immediately to update UI/progress flow.
        stopOptimization()
    }
}
