package com.tony.appbooster.presentation.viewmodel.main

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationLogEntry
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbConnectionState

/**
 * UI-level model aggregating ADB connection and optimization data
 * required by the main setup screen.
 *
 * @param connectionState Current ADB connection status.
 * @param logEntries Structured log entries for rich activity-feed rendering.
 * @param optimizationProgress Progress of the active optimization job.
 * @param optimizationAnalysis Pre-scan analysis showing how many apps need optimization.
 * @param optimizationMode Currently selected optimization mode from Settings.
 * @param isStartingOptimization Whether optimization start is in progress (for button loading).
 * @param isCurrentResultDismissed Whether the result card for the current run was dismissed.
 */
data class MainUiModel(
    val connectionState: AdbConnectionState = AdbConnectionState.Disconnected,
    val logEntries: List<OptimizationLogEntry> = emptyList(),
    val optimizationProgress: OptimizationProgress = OptimizationProgress(),
    val optimizationAnalysis: OptimizationAnalysis = OptimizationAnalysis(),
    val optimizationMode: AppOptimizationType = AppOptimizationType.SPEED_PROFILE,
    val isStartingOptimization: Boolean = false,
    val isCurrentResultDismissed: Boolean = false
)
