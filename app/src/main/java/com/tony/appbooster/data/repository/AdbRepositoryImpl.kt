package com.tony.appbooster.data.repository

import android.content.Context
import com.tony.appbooster.domain.client.AdbShellDataSource
import com.tony.appbooster.domain.model.common.AppCompilationInfo
import com.tony.appbooster.domain.model.common.LogEntryType
import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationLogEntry
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

    private val _logEntries = MutableStateFlow<List<OptimizationLogEntry>>(emptyList())
    override val logEntries = _logEntries.asStateFlow()

    private val _optimizationProgress = MutableStateFlow(OptimizationProgress())
    override val optimizationProgress = _optimizationProgress.asStateFlow()

    private val _optimizationAnalysis = MutableStateFlow(OptimizationAnalysis())
    override val optimizationAnalysis = _optimizationAnalysis.asStateFlow()

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
            // Reset cancellation for a new run and clear previous logs
            optimizationCancelRequested.set(false)
            clearLogEntries()

            val allPackages = queryInstalledPackages()

            if (allPackages.isEmpty()) {
                addLog("No packages found for optimization.")
                addLogEntry(LogEntryType.INFO, "No packages found")
                return@runCatching
            }

            addLog("Found ${allPackages.size} installed packages.")
            addLog("Analyzing optimization status...")
            addLogEntry(LogEntryType.START, "Starting optimization", detail = "${allPackages.size} apps found")

            // Query compilation status and filter apps that need optimization
            val packagesToOptimize = filterPackagesForOptimization(allPackages, compileMode)
            val skippedCount = allPackages.size - packagesToOptimize.size
            val total = packagesToOptimize.size

            if (total == 0) {
                addLog("✓ All apps are already optimized (${skippedCount} apps skipped).")
                addLog("No optimization needed at this time.")
                addLogEntry(LogEntryType.COMPLETE, "All apps optimized!", detail = "$skippedCount apps already optimal")

                val runId = System.currentTimeMillis()
                _optimizationProgress.value = OptimizationProgress(
                    runId = runId,
                    isRunning = false,
                    result = OptimizationResult.Completed,
                    totalCount = 0,
                    processedCount = 0,
                    skippedCount = skippedCount,
                    progress = 1f
                )
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
                skippedCount = skippedCount,
                progress = 0f
            )

            addLog("Optimizing $total apps ($skippedCount already optimized, skipped).")
            addLog("(Excluding ${SELF_PACKAGE_NAME} to prevent self-crash)")
            addLog("Starting compilation (Mode: $compileMode)...")
            addLogEntry(LogEntryType.INFO, "Mode: $compileMode", detail = "$total to optimize, $skippedCount skipped")

            packagesToOptimize.forEachIndexed { index, packageName ->
                if (optimizationCancelRequested.get()) {
                    addLog("⏹ Optimization cancelled.")
                    addLogEntry(LogEntryType.CANCELLED, "Optimization cancelled", detail = "${index} apps completed")
                    _optimizationProgress.value = _optimizationProgress.value.copy(
                        isRunning = false,
                        result = OptimizationResult.Canceled,
                        currentAppPackage = ""
                    )
                    return@runCatching
                }

                _optimizationProgress.value = _optimizationProgress.value.copy(
                    currentAppPackage = packageName
                )

                addLogEntry(LogEntryType.OPTIMIZING, "Optimizing...", packageName = packageName)

                val command = "cmd package compile -m $compileMode -f $packageName"
                addLog("> $command")

                val result = shellDataSource.executeCommand(command)
                result.fold(
                    onSuccess = { output ->
                        addLog("Success: optimized $packageName")
                        addLogEntry(LogEntryType.SUCCESS, "Optimized", packageName = packageName)
                        // Only log output if it contains more info than just "Success"
                        val trimmed = output.trim()
                        if (trimmed.isNotBlank() && !trimmed.equals("Success", ignoreCase = true)) {
                            addLog(trimmed)
                        }
                    },
                    onFailure = { throwable ->
                        addLog("Failure: $packageName - ${throwable.message}")
                        addLogEntry(LogEntryType.ERROR, "Failed", packageName = packageName, detail = throwable.message)
                    }
                )

                val newCount = index + 1
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    processedCount = newCount,
                    progress = newCount.toFloat() / total.toFloat()
                )
            }

            addLog("✓ Optimization complete! $total apps optimized, $skippedCount skipped.")
            addLogEntry(LogEntryType.COMPLETE, "Optimization complete!", detail = "$total apps optimized")

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
                addLogEntry(LogEntryType.ERROR, "Optimization failed", detail = throwable.message)
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
     * Analyzes all installed apps to determine which need optimization.
     *
     * This is a lightweight scan that checks compilation status without
     * actually performing optimization.
     *
     * @param mode The optimization mode to analyze against.
     * @return [Resource] with [OptimizationAnalysis] results.
     */
    override suspend fun analyzeOptimizationStatus(
        mode: AppOptimizationType
    ): Resource<OptimizationAnalysis> {
        return runCatching {
            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = true)

            val allPackages = queryInstalledPackages()

            if (allPackages.isEmpty()) {
                val result = OptimizationAnalysis(
                    totalAppsScanned = 0,
                    appsNeedingOptimization = 0,
                    appsAlreadyOptimized = 0,
                    isScanning = false,
                    lastScanTimeMs = System.currentTimeMillis()
                )
                _optimizationAnalysis.value = result
                return@runCatching result
            }

            val compileMode = mode.value
            var needsOptimization = 0
            var alreadyOptimized = 0

            for (packageName in allPackages) {
                val compilationInfo = queryPackageCompilationInfo(packageName, compileMode)
                if (compilationInfo.needsOptimization) {
                    needsOptimization++
                } else {
                    alreadyOptimized++
                }
            }

            val result = OptimizationAnalysis(
                totalAppsScanned = allPackages.size,
                appsNeedingOptimization = needsOptimization,
                appsAlreadyOptimized = alreadyOptimized,
                isScanning = false,
                lastScanTimeMs = System.currentTimeMillis()
            )
            _optimizationAnalysis.value = result
            result
        }.fold(
            onSuccess = { analysis ->
                Resource.Success(analysis)
            },
            onFailure = { throwable ->
                _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = false)
                Resource.Error(
                    ResourceError.LogicError(
                        errorMessage = "Analysis failed: ${throwable.message}",
                        errorCode = "ADB_ANALYSIS_FAILED"
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
                // Handle different line ending formats (Windows CRLF, Unix LF, old Mac CR)
                val normalizedOutput = output
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")

                val lines = normalizedOutput.split("\n")
                addLog("Raw lines count: ${lines.size}")

                val packages = mutableListOf<String>()

                for (rawLine in lines) {
                    val trimmed = rawLine.trim()
                    if (trimmed.isEmpty()) continue

                    // Extract package name from various formats
                    val packageName = when {
                        // Standard format: "package:com.example.app"
                        trimmed.startsWith("package:") -> {
                            trimmed.removePrefix("package:").trim()
                        }
                        // Some Android versions: just the package name with a dot
                        trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.contains("=") -> {
                            trimmed
                        }
                        else -> {
                            // Skip non-package lines (errors, warnings, status messages)
                            null
                        }
                    }

                    if (packageName != null && packageName.isNotEmpty()) {
                        // Validate it looks like a package name (contains at least one dot)
                        if (packageName.contains(".")) {
                            // Exclude self to prevent crash during optimization
                            if (packageName != SELF_PACKAGE_NAME && !packageName.contains(SELF_PACKAGE_NAME)) {
                                packages.add(packageName)
                            }
                        }
                    }
                }

                if (packages.isEmpty()) {
                    // Helps diagnose scenarios where the shell command succeeded but returned unexpected output.
                    addLog("pm list packages returned no parsable packages.")
                    addLog("Raw output length: ${output.length} chars")
                    // Show first part of raw output for debugging
                    val preview = output.take(500).replace("\n", "\\n")
                    addLog("Preview: $preview")

                    // Try alternative command
                    addLog("Trying alternative: pm list packages -3")
                    return@fold tryAlternativePackageList()
                }

                addLog("Found ${packages.size} packages")
                packages
            },
            onFailure = { throwable ->
                addLog("Failed to query installed packages: ${throwable.message}")
                emptyList()
            }
        )
    }

    /**
     * Fallback method to query packages using alternative commands.
     */
    private suspend fun tryAlternativePackageList(): List<String> {
        // Try with -3 flag for third-party apps only
        val command = "pm list packages -3"
        addLog("> $command")
        val result = shellDataSource.executeCommand(command)

        return result.fold(
            onSuccess = { output ->
                val normalizedOutput = output
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")

                val packages = normalizedOutput.split("\n")
                    .mapNotNull { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("package:") -> {
                                val pkg = trimmed.removePrefix("package:").trim()
                                if (pkg.contains(".") && pkg != SELF_PACKAGE_NAME && !pkg.contains(SELF_PACKAGE_NAME)) pkg else null
                            }
                            trimmed.contains(".") && !trimmed.contains(" ") -> {
                                val pkg = trimmed
                                if (pkg != SELF_PACKAGE_NAME && !pkg.contains(SELF_PACKAGE_NAME)) pkg else null
                            }
                            else -> null
                        }
                    }

                if (packages.isNotEmpty()) {
                    addLog("Alternative command found ${packages.size} packages")
                } else {
                    addLog("Alternative command also returned no packages")
                    addLog("Output preview: ${output.take(300).replace("\n", "\\n")}")
                }
                packages
            },
            onFailure = { throwable ->
                addLog("Alternative command failed: ${throwable.message}")
                emptyList()
            }
        )
    }

    /**
     * Filters packages that actually need optimization based on their current
     * compilation status and the time since last optimization.
     *
     * This implements smart optimization logic:
     * - Apps updated after their last compilation are always included
     * - Apps optimized within the last 7 days are skipped (unless updated)
     * - Apps with lower-quality compiler filters are included
     * - Apps with no compilation info are included
     *
     * @param packages List of all installed package names to evaluate.
     * @param targetFilter The target compiler filter (e.g., "speed", "speed-profile").
     * @return Filtered list of packages that should be optimized.
     */
    private suspend fun filterPackagesForOptimization(
        packages: List<String>,
        targetFilter: String
    ): List<String> {
        val packagesToOptimize = mutableListOf<String>()

        for (packageName in packages) {
            val compilationInfo = queryPackageCompilationInfo(packageName, targetFilter)

            if (compilationInfo.needsOptimization) {
                packagesToOptimize.add(packageName)
            } else {
                // Generate detailed skip reason for logging
                val skipMessage = when (val reason = compilationInfo.skipReason) {
                    is AppCompilationInfo.SkipReason.RecentlyOptimized -> {
                        "optimized ${reason.daysAgo} days ago with ${reason.filter}"
                    }
                    is AppCompilationInfo.SkipReason.AlreadyOptimal -> {
                        "already optimal (${reason.filter})"
                    }
                    AppCompilationInfo.SkipReason.SystemApp -> "system app"
                    null -> "already optimized"
                }
                addLog("⏭ Skip: $packageName ($skipMessage)")
            }
        }

        return packagesToOptimize
    }

    /**
     * Queries the compilation status for a single package using a simple approach:
     * 1. Use `pm compile --check` if available (Android 10+)
     * 2. Fall back to dumpsys package for compiler filter info
     *
     * This simplified approach is more reliable than checking oat file timestamps.
     *
     * @param packageName Package to query.
     * @param targetFilter The optimization filter we intend to apply.
     * @return [AppCompilationInfo] with parsed compilation details.
     */
    private suspend fun queryPackageCompilationInfo(
        packageName: String,
        targetFilter: String
    ): AppCompilationInfo {
        // Try the simple compile --check approach first (Android 10+)
        // This returns the current compilation status directly
        val checkCommand = "cmd package compile --check $packageName 2>/dev/null"
        val checkResult = shellDataSource.executeCommand(checkCommand)

        var compilerFilter: String? = null
        var lastUpdateTimeMs: Long? = null

        checkResult.getOrNull()?.let { output ->
            // Output format varies but often includes the filter like "speed-profile" or "speed"
            val trimmed = output.trim().lowercase()
            when {
                trimmed.contains("speed-profile") -> compilerFilter = "speed-profile"
                trimmed.contains("everything") -> compilerFilter = "everything"
                trimmed.contains("speed") && !trimmed.contains("profile") -> compilerFilter = "speed"
                trimmed.contains("quicken") -> compilerFilter = "quicken"
                trimmed.contains("verify") -> compilerFilter = "verify"
                trimmed.contains("extract") -> compilerFilter = "extract"
            }
        }

        // If --check didn't work, try dumpsys package for just the essential info
        if (compilerFilter == null) {
            // Use grep to get only the lines we need - much faster than full dumpsys
            val grepCommand = "dumpsys package $packageName 2>/dev/null | grep -E '(status=|lastUpdateTime=|firstInstallTime=)' | head -5"
            val grepResult = shellDataSource.executeCommand(grepCommand)

            grepResult.getOrNull()?.lines()?.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.contains("status=") && compilerFilter == null -> {
                        val match = Regex("""status=(\w+[-\w]*)""").find(trimmed)
                        compilerFilter = match?.groupValues?.getOrNull(1)
                    }
                    trimmed.startsWith("lastUpdateTime=") && lastUpdateTimeMs == null -> {
                        val timeStr = trimmed.substringAfter("lastUpdateTime=").trim()
                        lastUpdateTimeMs = parseTimestamp(timeStr)
                    }
                    trimmed.startsWith("firstInstallTime=") && lastUpdateTimeMs == null -> {
                        val timeStr = trimmed.substringAfter("firstInstallTime=").trim()
                        lastUpdateTimeMs = parseTimestamp(timeStr)
                    }
                }
            }
        }

        // Simplified optimization decision:
        // - If we couldn't determine the filter, assume it needs optimization
        // - If the filter matches target (speed/speed-profile), it's already optimized
        // - Otherwise, it needs optimization
        val needsOptimization: Boolean
        val skipReason: AppCompilationInfo.SkipReason?

        val filter = compilerFilter // Smart cast helper
        when {
            filter == null -> {
                // Unknown state - optimize it
                needsOptimization = true
                skipReason = null
            }
            isFilterOptimalForTarget(filter, targetFilter) -> {
                // Already has the target optimization
                needsOptimization = false
                skipReason = AppCompilationInfo.SkipReason.RecentlyOptimized(0, filter)
            }
            else -> {
                // Has a lower quality filter - optimize it
                needsOptimization = true
                skipReason = null
            }
        }

        return AppCompilationInfo(
            packageName = packageName,
            compilerFilter = compilerFilter,
            lastCompilationTimeMs = null, // We don't need this for the decision
            lastUpdateTimeMs = lastUpdateTimeMs,
            oatFileExists = compilerFilter != null,
            skipReason = skipReason,
            needsOptimization = needsOptimization
        )
    }

    /**
     * Determines if the current compiler filter is optimal for the target filter.
     *
     * @param currentFilter The current compiler filter applied to the app.
     * @param targetFilter The target filter the user wants to apply.
     * @return True if the app doesn't need re-optimization.
     */
    private fun isFilterOptimalForTarget(currentFilter: String, targetFilter: String): Boolean {
        val current = currentFilter.lowercase()
        val target = targetFilter.lowercase()

        // "everything" is always optimal
        if (current == "everything") return true

        // "speed" is optimal for both speed and speed-profile targets
        if (current == "speed" && (target == "speed" || target == "speed-profile")) return true

        // "speed-profile" is optimal for speed-profile target
        if (current == "speed-profile" && target == "speed-profile") return true

        return false
    }

    /**
     * Attempts to parse a timestamp string from dumpsys output.
     *
     * Handles various formats Android may output:
     * - Epoch milliseconds as a number
     * - Human-readable date strings
     *
     * @param timeStr Raw timestamp string from dumpsys.
     * @return Epoch milliseconds, or null if parsing fails.
     */
    private fun parseTimestamp(timeStr: String): Long? {
        // Try parsing as epoch millis directly
        timeStr.toLongOrNull()?.let { return it }

        // Try parsing common Android date format: "2024-01-15 10:30:45"
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            formatter.parse(timeStr)?.time
        } catch (_: Exception) {
            null
        }
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

    /**
     * Adds a structured log entry for beautiful UI rendering.
     *
     * @param type The type of log entry.
     * @param message Human-readable message.
     * @param packageName Optional package name this entry relates to.
     * @param detail Optional additional detail.
     */
    private fun addLogEntry(
        type: LogEntryType,
        message: String,
        packageName: String? = null,
        detail: String? = null
    ) {
        val entry = OptimizationLogEntry(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            type = type,
            packageName = packageName,
            message = message,
            detail = detail
        )
        val currentList = _logEntries.value.toMutableList()
        currentList.add(entry)
        // Keep only last 100 entries to prevent memory bloat
        if (currentList.size > 100) {
            _logEntries.value = currentList.takeLast(100)
        } else {
            _logEntries.value = currentList
        }
    }

    /**
     * Clears all log entries. Called when starting a new optimization run.
     */
    private fun clearLogEntries() {
        _logEntries.value = emptyList()
    }
}
