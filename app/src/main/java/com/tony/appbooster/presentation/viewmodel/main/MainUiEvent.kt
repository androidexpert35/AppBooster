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
     * Requests stopping an active optimization workflow.
     */
    data object OnStopOptimizationClicked : MainUiEvent

    /**
     * Requests dismissing the optimization result card (completed/canceled) for the latest run.
     */
    data object OnDismissOptimizationResultClicked : MainUiEvent

    /**
     * Requests a pre-optimization analysis scan to determine which apps need optimization.
     */
    data object OnAnalyzeAppsClicked : MainUiEvent
}
