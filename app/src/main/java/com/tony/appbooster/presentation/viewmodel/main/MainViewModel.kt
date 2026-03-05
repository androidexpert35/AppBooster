package com.tony.appbooster.presentation.viewmodel.main

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.alkemy.boxapp.presentation.navigation.interfaces.NavigationManager
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.usecase.adb.ConnectAdbUseCase
import com.tony.appbooster.domain.usecase.adb.ObserveAdbConnectionStateUseCase
import com.tony.appbooster.domain.usecase.analysis.ObserveOptimizationAnalysisUseCase
import com.tony.appbooster.domain.usecase.analysis.StartAnalysisUseCase
import com.tony.appbooster.domain.usecase.analysis.StopAnalysisUseCase
import com.tony.appbooster.domain.usecase.optimization.DismissOptimizationResultUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveCommandOutputUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationLogEntriesUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationProgressUseCase
import com.tony.appbooster.domain.usecase.optimization.StartOptimizationUseCase
import com.tony.appbooster.domain.usecase.optimization.StopOptimizationUseCase
import com.tony.appbooster.domain.usecase.settings.ObserveAppOptimizationTypeUseCase
import com.tony.appbooster.presentation.error.ResourceErrorUiMapper
import com.tony.appbooster.presentation.viewmodel.base.BaseViewModel
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
 * @param navigationManager Manager used to dispatch navigation commands from the ViewModel.
 * @return A ViewModel instance exposing a single `StateFlow<UIState<MainUiModel>>`.
 * @throws IllegalStateException If required dependencies are not provided by DI.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val connectAdbUseCase: ConnectAdbUseCase,
    private val stopOptimizationUseCase: StopOptimizationUseCase,
    private val stopAnalysisUseCase: StopAnalysisUseCase,
    private val getOptimizeAppUseCase: ObserveAppOptimizationTypeUseCase,
    private val observeAdbConnectionStateUseCase: ObserveAdbConnectionStateUseCase,
    private val observeCommandOutputUseCase: ObserveCommandOutputUseCase,
    private val observeOptimizationLogEntriesUseCase: ObserveOptimizationLogEntriesUseCase,
    private val observeOptimizationProgressUseCase: ObserveOptimizationProgressUseCase,
    private val observeOptimizationAnalysisUseCase: ObserveOptimizationAnalysisUseCase,
    private val startAnalysisUseCase: StartAnalysisUseCase,
    private val startOptimizationUseCase: StartOptimizationUseCase,
    private val dismissOptimizationResultUseCase: DismissOptimizationResultUseCase,
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
            val connectionStateFlow = observeAdbConnectionStateUseCase()
            val commandOutputFlow = observeCommandOutputUseCase()
            val logEntriesFlow = observeOptimizationLogEntriesUseCase()
            val progressFlow = observeOptimizationProgressUseCase()
            val analysisFlow = observeOptimizationAnalysisUseCase()

            // Kotlin's typed combine overloads support up to 5 flows; use nested combine to keep strong types.
            combine(
                combine(
                    connectionStateFlow,
                    commandOutputFlow,
                    logEntriesFlow
                ) { connectionState, logs, logEntries ->
                    Triple(connectionState, logs, logEntries)
                },
                combine(
                    progressFlow,
                    analysisFlow,
                    dismissedResultRunIds
                ) { progress, analysis, dismissedRunIds ->
                    Triple(progress, analysis, dismissedRunIds)
                }
            ) { first, second ->
                val (connectionState, logs, logEntries) = first
                val (progress, analysis, dismissedRunIds) = second

                // Preserve the current optimizationMode so it isn't reset to the default
                // whenever any of the repository flows emit a new value.
                val currentMode = uiState.value.data?.optimizationMode
                    ?: AppOptimizationType.SPEED_PROFILE

                MainUiModel(
                    connectionState = connectionState,
                    logs = logs,
                    logEntries = logEntries,
                    optimizationProgress = progress,
                    optimizationAnalysis = analysis,
                    dismissedResultRunIds = dismissedRunIds,
                    optimizationMode = currentMode
                )
            }.collect { model ->
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
            when (val result = startAnalysisUseCase(mode)) {
                is Resource.Success -> Unit
                is Resource.Error -> showErrorSnackbar(result.data)
            }
        }
    }

    private fun showErrorSnackbar(error: ResourceError) {
        emitEffect(MainUiEffect.ShowSnackbar(ResourceErrorUiMapper.toUserMessage(appContext, error)))
    }

    override fun handleEvent(event: MainUiEvent) {
        when (event) {
            MainUiEvent.OnConnectClicked -> startConnectionSequence()
            MainUiEvent.OnStartOptimizationClicked -> onStartOptimizationRequested()
            MainUiEvent.OnStopOptimizationClicked -> onStopOptimizationRequested()
            MainUiEvent.OnDismissOptimizationResultClicked -> onDismissOptimizationResultRequested()
            MainUiEvent.OnAnalyzeAppsClicked -> triggerAnalysis()
            MainUiEvent.OnStopAnalysisClicked -> onStopAnalysisRequested()
        }
    }

    private fun onStopAnalysisRequested() {
        launchUiStateUpdate(
            dataFetchBlock = { stopAnalysisUseCase() },
            skipLoading = true,
            processSuccess = { uiState.value.data ?: MainUiModel() }
        )
    }

    private fun onDismissOptimizationResultRequested() {
        val runId = uiState.value.data?.optimizationProgress?.runId ?: 0L
        if (runId == 0L) return

        // 1) Clear the underlying domain snapshot so no other UI surfaces show stale counts.
        viewModelScope.launch(exceptionHandler) {
            when (val result = dismissOptimizationResultUseCase()) {
                is Resource.Success -> Unit
                is Resource.Error -> showErrorSnackbar(result.data)
            }
        }

        // 2) Keep in-memory dismissal for this runId as an extra guard (e.g., if a worker re-emits).
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

        // Set loading state for the start button
        uiState.value.data?.let { currentData ->
            updateUiData(currentData.copy(isStartingOptimization = true))
        }

        launchUiStateUpdate(
            dataFetchBlock = { startOptimizationUseCase(optimizationMode) },
            skipLoading = true,
            processSuccess = {
                uiState.value.data ?: MainUiModel()
            },
            updateUiAfterError = { uiError ->
                val type = uiError.type
                val domainError = type as? ResourceError
                if (domainError != null) {
                    showErrorSnackbar(domainError)
                } else {
                    // Fallback to BaseViewModel-generated message when we don't have domain error.
                    emitEffect(MainUiEffect.ShowSnackbar(uiError.message))
                }

                uiState.value.data
            },
            invokeOnCompletion = {
                uiState.value.data?.let { currentData ->
                    updateUiData(currentData.copy(isStartingOptimization = false))
                }
            }
        )
    }

    private fun onStopOptimizationRequested() {
        launchUiStateUpdate(
            dataFetchBlock = { stopOptimizationUseCase() },
            skipLoading = true,
            processSuccess = { uiState.value.data ?: MainUiModel() }
        )
    }
}
