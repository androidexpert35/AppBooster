package com.tony.appbooster.presentation.viewmodel.main

/**
 * User intents originating from the Dashboard/Main screen.
 *
 * The business purpose of this contract is to enforce Unidirectional Data Flow:
 * UI emits events → [com.tony.appbooster.presentation.viewmodel.main.MainViewModel] handles them →
 * UI state/effects update.
 */
sealed interface MainUiEvent {

    /**
     * Requests establishing a wireless ADB connection.
     */
    data object OnConnectClicked : MainUiEvent

    /**
     * Requests starting the current optimization workflow.
     */
    data object OnStartOptimizationClicked : MainUiEvent

    /**
     * Requests starting a forced optimization that compiles every installed
     * package regardless of its current compilation status. Useful after
     * OTA updates or when the user wants to re-optimise all apps.
     */
    data object OnForceOptimizationClicked : MainUiEvent

    /**
     * Requests stopping the current optimization workflow.
     */
    data object OnStopOptimizationClicked : MainUiEvent

    /**
     * Requests stopping the current analysis workflow.
     */
    data object OnStopAnalysisClicked : MainUiEvent


    /**
     * Requests dismissing the optimization result card (completed/canceled) for the latest run.
     */
    data object OnDismissOptimizationResultClicked : MainUiEvent

    /**
     * Requests a pre-optimization analysis scan to determine which apps need optimization.
     */
    data object OnAnalyzeAppsClicked : MainUiEvent
}
