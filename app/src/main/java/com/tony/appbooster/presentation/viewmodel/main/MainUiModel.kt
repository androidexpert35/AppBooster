package com.tony.appbooster.presentation.viewmodel.main

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbConnectionState

/**
 * UI-level model aggregating ADB connection and optimization data
 * required by the main setup screen.
 *
 * @param connectionState Current ADB connection status.
 * @param logs Shell output lines visible in the console area.
 * @param optimizationProgress Progress of the active optimization job.
 * @param optimizationAnalysis Pre-scan analysis showing how many apps need optimization.
 * @param dismissedResultRunIds Set of optimization run ids for which the user dismissed the
 * result card (completed/canceled). Used to prevent the card from reappearing when returning
 * to the Dashboard for the same result.
 */
data class MainUiModel(
    val connectionState: AdbConnectionState = AdbConnectionState.Disconnected,
    val logs: List<String> = emptyList(),
    val optimizationProgress: OptimizationProgress = OptimizationProgress(),
    val optimizationAnalysis: OptimizationAnalysis = OptimizationAnalysis(),
    val optimizationMode: AppOptimizationType = AppOptimizationType.SPEED_PROFILE,
    val dismissedResultRunIds: Set<Long> = emptySet()
)
