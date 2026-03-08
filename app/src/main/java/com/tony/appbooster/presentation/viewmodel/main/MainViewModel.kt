package com.tony.appbooster.presentation.viewmodel.main

import android.content.Context
import androidx.lifecycle.viewModelScope
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
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationLogEntriesUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationProgressUseCase
import com.tony.appbooster.domain.usecase.optimization.StartOptimizationUseCase
import com.tony.appbooster.domain.usecase.optimization.StopOptimizationUseCase
import com.tony.appbooster.domain.usecase.settings.ObserveAppOptimizationTypeUseCase
import com.tony.appbooster.presentation.error.ResourceErrorUiMapper
import com.tony.appbooster.presentation.navigation.interfaces.NavigationManager
import com.tony.appbooster.presentation.viewmodel.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for orchestrating the ADB connection and optimization
 * flows and exposing them as a unified [MainUiModel] state for the setup screen.
 *
 * The business purpose is to coordinate wireless ADB discovery, connection,
 * analysis, and ART optimization while surfacing progress and errors to the UI.
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
    private val observeAppOptimizationTypeUseCase: ObserveAppOptimizationTypeUseCase,
    private val observeAdbConnectionStateUseCase: ObserveAdbConnectionStateUseCase,
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
    private val isStartingOptimization = MutableStateFlow(false)

    init {
        observeRepository()
    }

    /**
     * Starts the wireless ADB setup flow by discovering the port and connecting
     * to the local ADB instance. The result is pushed through [launchUiStateUpdate],
     * updating the UI status and surfacing any domain errors.
     */
    fun startConnectionSequence() {
        launchUiStateUpdate(
            dataFetchBlock = { connectAdbUseCase() },
            processSuccess = { uiState.value.data ?: MainUiModel() }
        )
    }

    /**
     * Observes repository-backed dashboard state and maps it into one immutable
     * [MainUiModel] source of truth for the screen.
     *
     * This avoids split state where some fields are updated through ad-hoc `copy()` calls
     * while others come from flows, which previously caused mode/loading data to be
     * overwritten by unrelated emissions.
     */
    private fun observeRepository() {
        viewModelScope.launch(exceptionHandler) {
            val connectionStateFlow = observeAdbConnectionStateUseCase()
            val logEntriesFlow = observeOptimizationLogEntriesUseCase()
            val progressFlow = observeOptimizationProgressUseCase()
            val analysisFlow = observeOptimizationAnalysisUseCase()
            val optimizationModeFlow = observeAppOptimizationTypeUseCase()
                .runningFold(AppOptimizationType.SPEED_PROFILE) { currentMode, resource ->
                    when (resource) {
                        is Resource.Success -> resource.data
                        is Resource.Error -> currentMode
                    }
                }

            combine(
                combine(
                    connectionStateFlow,
                    logEntriesFlow,
                    progressFlow
                ) { connectionState, logEntries, progress ->
                    Triple(connectionState, logEntries, progress)
                },
                combine(
                    analysisFlow,
                    dismissedResultRunIds,
                    optimizationModeFlow,
                    isStartingOptimization
                ) { analysis, dismissedRunIds, optimizationMode, starting ->
                    DashboardMetaState(
                        analysis = analysis,
                        dismissedRunIds = dismissedRunIds,
                        optimizationMode = optimizationMode,
                        isStartingOptimization = starting
                    )
                }
            ) { first, second ->
                val (connectionState, logEntries, progress) = first
                MainUiModel(
                    connectionState = connectionState,
                    logEntries = logEntries,
                    optimizationProgress = progress,
                    optimizationAnalysis = second.analysis,
                    optimizationMode = second.optimizationMode,
                    isStartingOptimization = second.isStartingOptimization,
                    isCurrentResultDismissed = second.dismissedRunIds.contains(progress.runId)
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
            MainUiEvent.OnStartOptimizationClicked -> onStartOptimizationRequested(forceOptimize = false)
            MainUiEvent.OnForceOptimizationClicked -> onStartOptimizationRequested(forceOptimize = true)
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

        viewModelScope.launch(exceptionHandler) {
            when (val result = dismissOptimizationResultUseCase()) {
                is Resource.Success -> Unit
                is Resource.Error -> showErrorSnackbar(result.data)
            }
        }

        // Keep an in-memory guard for the active run in case background state re-emits briefly.
        dismissedResultRunIds.update { current ->
            if (current.contains(runId)) current else current + runId
        }
    }

    private fun onStartOptimizationRequested(forceOptimize: Boolean = false) {
        val optimizationMode = uiState.value.data?.optimizationMode
        if (optimizationMode == null) {
            emitEffect(
                MainUiEffect.ShowSnackbar(
                    appContext.getString(R.string.error_select_optimization_mode_first)
                )
            )
            return
        }

        isStartingOptimization.value = true

        launchUiStateUpdate(
            dataFetchBlock = { startOptimizationUseCase(optimizationMode, forceOptimize) },
            skipLoading = true,
            processSuccess = { uiState.value.data ?: MainUiModel() },
            updateUiAfterError = { uiError ->
                val domainError = uiError.type as? ResourceError
                if (domainError != null) {
                    showErrorSnackbar(domainError)
                } else {
                    emitEffect(MainUiEffect.ShowSnackbar(uiError.message))
                }
                uiState.value.data
            },
            invokeOnCompletion = {
                isStartingOptimization.value = false
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

    /**
     * Internal dashboard-only state that is combined into [MainUiModel].
     *
     * Keeping these fields grouped avoids threading multiple loosely-related primitives
     * through nested `combine` lambdas.
     */
    private data class DashboardMetaState(
        val analysis: com.tony.appbooster.domain.model.common.OptimizationAnalysis,
        val dismissedRunIds: Set<Long>,
        val optimizationMode: AppOptimizationType,
        val isStartingOptimization: Boolean
    )
}
