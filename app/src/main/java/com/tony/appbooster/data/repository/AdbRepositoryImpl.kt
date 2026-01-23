package com.tony.appbooster.data.repository

import android.content.Context
import com.tony.appbooster.domain.client.AdbShellDataSource
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbConnectionState
import com.tony.appbooster.domain.repository.AdbRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Shizuku-based [AdbRepository] implementation that orchestrates
 * privileged shell operations for app optimization.
 *
 * Uses Shizuku to execute shell commands with ADB-level privileges,
 * enabling system optimizations without requiring a PC connection.
 *
 * @param context Application context for system-level integration.
 * @param shellDataSource Data source that executes shell commands via Shizuku.
 */
class AdbRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellDataSource: AdbShellDataSource
) : AdbRepository {

    companion object {
        /**
         * Package name of this app, excluded from optimization to prevent self-crash.
         */
        private const val SELF_PACKAGE_NAME = "com.tony.appbooster"
    }

    private val _connectionState =
        MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    override val connectionState = _connectionState.asStateFlow()

    private val _commandOutput = MutableStateFlow<List<String>>(emptyList())
    override val commandOutput = _commandOutput.asStateFlow()

    private val _optimizationProgress = MutableStateFlow(OptimizationProgress())
    override val optimizationProgress = _optimizationProgress.asStateFlow()

    private val optimizationCancelRequested = AtomicBoolean(false)

    /**
     * Ensures Shizuku is ready and validates shell access with a health check.
     *
     * @return [Resource.Success] when ready, or [Resource.Error] with details.
     */
    override suspend fun ensureConnected(): Resource<Unit> {
        return runCatching {
            _connectionState.value = AdbConnectionState.Connecting
            addLog("Validating Shizuku shell access...")

            // Simple shell health check
            val command = "echo connected"
            addLog("> $command")

            val healthResult = shellDataSource.executeCommand(command)

            val healthOutput = healthResult.getOrElse { throwable ->
                throw throwable
            }.trim()

            addLog("Shell response: $healthOutput")

            _connectionState.value = AdbConnectionState.Connected
            addLog("Shizuku shell session ready.")
        }.fold(
            onSuccess = {
                Resource.Success(Unit)
            },
            onFailure = { throwable ->
                _connectionState.value = AdbConnectionState.Error(
                    message = throwable.message ?: "Failed to validate Shizuku connection."
                )
                addLog("Error: ${throwable.message}")
                Resource.Error(
                    ResourceError.LogicError(
                        errorMessage = throwable.message ?: ("Shizuku is not ready. " +
                                "Please ensure Shizuku is installed, running, and permission is granted.")
                    )
                )
            }
        )
    }

    /**
     * Requests cancellation of the ongoing optimization process, if any.
     *
     * This function sets a cancellation flag that is checked between
     * package optimization commands. The current optimization step will
     * complete before the process is marked as cancelled.
     *
     * @return [Resource.Success] if cancellation is successfully requested,
     * or [Resource.Error] with details if the request fails.
     */
    override suspend fun cancelOptimization(): Resource<Unit> {
        return runCatching {
            if (!_optimizationProgress.value.isRunning) {
                addLog("No optimization is currently running.")
                return@runCatching
            }

            // Flag is checked between package commands so we can stop quickly and safely.
            optimizationCancelRequested.set(true)
            addLog("Cancellation requested. Finishing current step...")

            _optimizationProgress.value = _optimizationProgress.value.copy(
                isRunning = false,
                result = OptimizationResult.Canceled,
                currentAppPackage = ""
            )
        }.fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { throwable ->
                addLog("Failed to cancel optimization: ${throwable.message}")
                Resource.Error(
                    ResourceError.LogicError(
                        errorMessage = throwable.message ?: "Failed to cancel optimization."
                    )
                )
            }
        )
    }

    /**
     * Runs a full ADB-based optimization routine using the Dadb-backed
     * shell data source, compiling all installed packages with the
     * configured compilation mode and scheduling a background dexopt
     * job when complete.
     *
     * The business purpose is to encapsulate the entire optimization
     * workflow behind a single domain-facing call, updating progress
     * state so the UI can surface live feedback to the user.
     *
     * @param mode Application optimization mode that determines the
     * compilation strategy passed into the package manager command.
     * @return \[Resource.Success\] when the optimization flow completes,
     * or \[Resource.Error\] describing a logical failure when compilation
     * or background job scheduling is interrupted.
     */
    override suspend fun executeOptimizationCommand(
        mode: AppOptimizationType
    ): Resource<Unit> {
        val compileMode = mode.value
        return runCatching {
            // Reset cancellation for a new run
            optimizationCancelRequested.set(false)

            val packages = queryInstalledPackages()
            val total = packages.size

            if (total == 0) {
                addLog("No packages found for optimization.")
                return@runCatching
            }

            // Use a monotonic-ish id (timestamp is enough here) so UI can key one-time affordances per run.
            val runId = System.currentTimeMillis()

            _optimizationProgress.value = OptimizationProgress(
                runId = runId,
                isRunning = true,
                result = OptimizationResult.None,
                totalCount = total,
                processedCount = 0,
                progress = 0f
            )

            addLog("Found $total packages to optimize.")
            addLog("(Excluding ${SELF_PACKAGE_NAME} to prevent self-crash)")
            addLog("Starting real compilation (Mode: $compileMode)...")

            packages.forEachIndexed { index, packageName ->
                if (optimizationCancelRequested.get()) {
                    addLog("\u007f Optimization cancelled.")
                    _optimizationProgress.value = _optimizationProgress.value.copy(
                        isRunning = false,
                        result = OptimizationResult.Canceled,
                        currentAppPackage = ""
                    )
                    return@runCatching
                }

                // Double-check: skip self package to prevent crash
                if (packageName == SELF_PACKAGE_NAME || packageName.contains(SELF_PACKAGE_NAME)) {
                    addLog("Skipping self package: $packageName")
                    val newCount = index + 1
                    _optimizationProgress.value = _optimizationProgress.value.copy(
                        processedCount = newCount,
                        progress = newCount.toFloat() / total.toFloat()
                    )
                    return@forEachIndexed
                }

                _optimizationProgress.value = _optimizationProgress.value.copy(
                    currentAppPackage = packageName
                )

                val command = "cmd package compile -m $compileMode -f $packageName"
                addLog("> $command")

                val result = shellDataSource.executeCommand(command)
                result.fold(
                    onSuccess = { output ->
                        addLog("Success: optimized $packageName")
                        // Only log output if it contains more info than just "Success"
                        val trimmed = output.trim()
                        if (trimmed.isNotBlank() && !trimmed.equals("Success", ignoreCase = true)) {
                            addLog(trimmed)
                        }
                    },
                    onFailure = { throwable ->
                        addLog("Failure: $packageName - ${throwable.message}")
                    }
                )

                val newCount = index + 1
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    processedCount = newCount,
                    progress = newCount.toFloat() / total.toFloat()
                )
            }

            addLog("\u007f Optimization complete! $total apps optimized.")

            _optimizationProgress.value = _optimizationProgress.value.copy(
                isRunning = false,
                result = OptimizationResult.Completed,
                currentAppPackage = "",
                progress = 1f
            )
        }.fold(
            onSuccess = {
                Resource.Success(Unit)
            },
            onFailure = { throwable ->
                addLog("Optimization failed: ${throwable.message}")
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    isRunning = false
                )
                Resource.Error(
                    ResourceError.LogicError(
                        errorMessage = "Optimization failed: ${throwable.message}",
                        errorCode = "ADB_OPTIMIZATION_FAILED"
                    )
                )
            }
        )
    }

    /**
     * Queries all installed package names from the connected device using
     * the Dadb-backed shell data source and normalizes the raw output into
     * a clean list of package identifiers.
     *
     * The business purpose is to provide a reusable, device-agnostic way
     * to discover all packages subject to optimization, hiding the raw
     * shell command output format from callers.
     *
     * @return List of normalized application package names, or an empty
     * list when the shell command fails or returns no data.
     */
    private suspend fun queryInstalledPackages(): List<String> {
        val command = "pm list packages"
        addLog("> $command")
        val result = shellDataSource.executeCommand(command)

        return result.fold(
            onSuccess = { output ->
                val lines = output.lines()
                val packages = lines.mapNotNull { rawLine ->
                    val trimmed = rawLine.trim()
                    if (trimmed.isEmpty()) {
                        null
                    } else {
                        val prefix = "package:"
                        val packageName = if (trimmed.startsWith(prefix)) {
                            trimmed.removePrefix(prefix).trim()
                        } else {
                            trimmed
                        }
                        // Exclude self to prevent crash during optimization
                        if (packageName == SELF_PACKAGE_NAME || packageName.contains(SELF_PACKAGE_NAME)) {
                            addLog("Skipping self: $packageName")
                            null
                        } else {
                            packageName
                        }
                    }
                }

                if (packages.isEmpty()) {
                    // Helps diagnose scenarios where the shell command succeeded but returned unexpected output.
                    addLog("pm list packages returned no parsable packages. Raw output (first 500 chars):")
                    addLog(output.take(500))
                }

                packages
            },
            onFailure = { throwable ->
                addLog("Failed to query installed packages: ${throwable.message}")
                emptyList()
            }
        )
    }

    /**
     * Appends a new log entry to the internal command output history so
     * that the UI can render a complete textual trace of all ADB-related
     * operations coordinated by this repository.
     *
     * The business purpose is to provide user-facing transparency and
     * troubleshooting context for all executed commands and state changes.
     *
     * @param line Single textual log entry to append in execution order.
     * @return Unit once the internal state flow has been updated.
     */
    private fun addLog(line: String) {
        val currentList = _commandOutput.value.toMutableList()
        currentList.add(line)
        _commandOutput.value = currentList
    }
}
