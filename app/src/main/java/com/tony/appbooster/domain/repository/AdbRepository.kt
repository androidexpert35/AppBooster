package com.tony.appbooster.domain.repository

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationLogEntry
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository that coordinates all ADB-related operations such as establishing
 * connections, pairing devices, and executing optimization commands while
 * exposing observable connection state and progress for the UI layer.
 *
 * The business purpose is to provide a single orchestration point for all
 * ADB-based workflows so that presentation and domain logic remain decoupled
 * from the underlying transport and protocol details.
 */
interface AdbRepository {

    /**
     * Exposes the current high-level ADB connection state so that the UI can
     * react to transitions such as connecting, connected, and errors.
     *
     * @return [StateFlow] emitting the latest [AdbConnectionState] updates.
     */
    val connectionState: StateFlow<AdbConnectionState>

    /**
     * Exposes a chronological list of shell command output lines, allowing
     * the UI to render a live terminal-like log for the user.
     *
     * @return [StateFlow] emitting immutable snapshots of shell output lines.
     */
    val commandOutput: StateFlow<List<String>>

    /**
     * Exposes structured log entries for beautiful UI rendering.
     *
     * @return [StateFlow] emitting list of [OptimizationLogEntry] items.
     */
    val logEntries: StateFlow<List<OptimizationLogEntry>>

    /**
     * Exposes progress for long-running optimization flows including the
     * currently optimized app and overall completion percentage.
     *
     * @return [StateFlow] emitting [OptimizationProgress] snapshots.
     */
    val optimizationProgress: StateFlow<OptimizationProgress>


    /**
     * Ensures the shell client is ready for command execution.
     *
     * For Shizuku-based implementations, this verifies Shizuku is running
     * and permission is granted.
     *
     * @return [Resource] indicating success or failure with details.
     */
    suspend fun ensureConnected(): Resource<Unit>

    /**
     * Requests cancellation of the currently running optimization flow, if any.
     *
     * The business purpose is to allow the user to safely stop long-running
     * operations without leaving the UI in a stuck loading state.
     *
     * Implementations should:
     * - stop executing further shell commands as soon as possible
     * - update [optimizationProgress] to `isRunning = false`
     * - append a user-visible log line explaining the cancellation
     *
     * @return [Resource.Success] when cancellation was requested successfully,
     *         or [Resource.Error] when cancellation could not be performed.
     */
    suspend fun cancelOptimization(): Resource<Unit>

    /**
     * Executes the full optimization pipeline for all installed packages
     * using the specified optimization strategy.
     *
     * The business purpose is to improve app performance by driving the
     * platform dexopt tooling over an established ADB connection.
     *
     * @param mode Optimization strategy that maps to the compile mode.
     * @return [Resource] indicating whether the optimization run succeeded.
     */
    suspend fun executeOptimizationCommand(
        mode: AppOptimizationType
    ): Resource<Unit>

    /**
     * Exposes the result of pre-optimization analysis, showing how many
     * apps need optimization vs are already optimized.
     *
     * @return [StateFlow] emitting [OptimizationAnalysis] snapshots.
     */
    val optimizationAnalysis: StateFlow<OptimizationAnalysis>

    /**
     * Attempts to cancel the currently running analysis scan.
     */
    suspend fun cancelAnalysis(): Resource<Unit>

    /**
     * Analyzes all installed apps to determine which need optimization.
     *
     * This is a lightweight scan that checks compilation status without
     * actually performing optimization. Results are exposed via [optimizationAnalysis].
     *
     * @param mode The optimization mode to analyze against.
     * @return [Resource] indicating success or failure.
     */
    suspend fun analyzeOptimizationStatus(mode: AppOptimizationType): Resource<OptimizationAnalysis>

    /**
     * Clears the latest optimization result and its associated counters.
     *
     * Business purpose:
     * - Keeps the dashboard coherent when the user dismisses a completed/canceled result card.
     * - Prevents stale run counters (e.g., "20 apps need optimization") from lingering after dismissal.
     * - Provides a single source of truth reset point for presentation.
     *
     * Notes:
     * - This does not re-run analysis.
     * - This does not affect the current run if [optimizationProgress] is still running.
     *
     * @return [Resource.Success] when the snapshot was cleared, or [Resource.Error] on failure.
     */
    suspend fun clearOptimizationResult(): Resource<Unit>
}
