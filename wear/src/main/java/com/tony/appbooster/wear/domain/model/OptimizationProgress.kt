package com.tony.appbooster.wear.domain.model

/**
 * Represents the current state of app optimization progress on Wear OS.
 *
 * @property runId Identifier of the current/most recent optimization run.
 * @property isRunning Whether optimization is currently in progress.
 * @property result Final result of the most recent run.
 * @property currentAppPackage The package name of the app currently being optimized.
 * @property progress Progress value from 0.0 to 1.0.
 * @property processedCount Number of apps already optimized.
 * @property totalCount Total number of apps to optimize.
 */
data class OptimizationProgress(
    val runId: Long = 0L,
    val isRunning: Boolean = false,
    val result: OptimizationResult = OptimizationResult.None,
    val currentAppPackage: String = "",
    val progress: Float = 0f,
    val processedCount: Int = 0,
    val totalCount: Int = 0
)

/**
 * Describes the final outcome of an optimization run.
 */
sealed interface OptimizationResult {
    /**
     * No run has finished yet, or state was reset for a new run.
     */
    data object None : OptimizationResult

    /**
     * The optimization flow finished normally.
     */
    data object Completed : OptimizationResult

    /**
     * The optimization flow was stopped before completion.
     */
    data object Canceled : OptimizationResult
}
