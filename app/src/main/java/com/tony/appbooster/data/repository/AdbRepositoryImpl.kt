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
    @param:ApplicationContext private val context: Context,
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
    private val analysisCancelRequested = AtomicBoolean(false)

    /**
     * In-memory cache of packages we've successfully optimized in this session.
     * Maps package name to the timestamp when it was optimized.
     * This avoids re-checking packages we just optimized.
     */
    private val recentlyOptimizedPackages = mutableMapOf<String, Long>()

    /**
     * Cached output of `dumpsys package dexopt` for the current analysis run.
     *
     * Business purpose:
     * - Avoids running an expensive global dump for every package.
     * - Significantly improves emulator performance and reduces timeouts.
     */
    private var cachedDexoptDump: String? = null

    /**
     * Cached `dumpsys package <pkg>` outputs for the current run.
     *
     * Business purpose:
     * - Enables strong overlay detection without re-running dumpsys multiple times.
     */
    private val cachedPackageDumps = mutableMapOf<String, String>()

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

            // Mark canceled immediately so UI can render the canceled HeroCard even if WorkManager
            // cancels the worker before the optimization loop checks the flag.
            val current = _optimizationProgress.value
            _optimizationProgress.value = current.copy(
                isRunning = false,
                result = OptimizationResult.Canceled,
                currentAppPackage = "",
                progress = current.progress.coerceIn(0f, 1f)
            )

            optimizationCancelRequested.set(true)
            addLog("⏹ Cancelling optimization...")
            addLogEntry(LogEntryType.CANCELLED, "Optimization cancelled")
        }.fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { Resource.Error(ResourceError.LogicError(it.message)) }
        )
    }

    override suspend fun cancelAnalysis(): Resource<Unit> {
        return runCatching {
            if (!_optimizationAnalysis.value.isScanning) {
                addLog("No analysis is currently running.")
                return@runCatching
            }

            // Mark canceled immediately so UI stops showing scanning state right away.
            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
                isScanning = false,
                currentPackage = ""
            )

            analysisCancelRequested.set(true)
            addLog("⏹ Cancelling analysis...")
            addLogEntry(LogEntryType.CANCELLED, "Analysis cancelled")
        }.fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { Resource.Error(ResourceError.LogicError(it.message)) }
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

            // Reset per-run caches
            cachedDexoptDump = null
            cachedPackageDumps.clear()

            addLogEntry(LogEntryType.START, "Starting optimization", detail = "Mode: $compileMode")

            val allPackages = queryInstalledPackages()

            if (allPackages.isEmpty()) {
                addLog("No packages found for optimization.")
                addLogEntry(LogEntryType.INFO, "No packages found")
                return@runCatching
            }

            addLog("Found ${allPackages.size} installed packages.")

            // A cached analysis is valid only if it was computed for the same mode that is
            // being requested now. Switching from speed-profile to speed (or vice-versa)
            // changes which apps are eligible, so the cached package list would be wrong.
            val existingAnalysis = _optimizationAnalysis.value
            val analysisIsValid = existingAnalysis.lastScanTimeMs != null
                && existingAnalysis.totalAppsScanned > 0
                && existingAnalysis.mode == mode

            val packagesToOptimize: List<String>
            val skippedCount: Int

            if (analysisIsValid) {
                // Reuse existing analysis - use cached package list directly (no re-query!)
                addLog("Using existing analysis from this session")
                addLogEntry(
                    LogEntryType.INFO,
                    "Using cached analysis",
                    detail = "${existingAnalysis.appsNeedingOptimization} apps need optimization"
                )

                packagesToOptimize = existingAnalysis.packagesNeedingOptimization
                // No-profile apps are also skipped this run — include them in the skipped count
                // so result panels show the correct number of apps not being compiled.
                skippedCount = existingAnalysis.appsAlreadyOptimized + existingAnalysis.appsWithNoProfile
            } else {
                // Perform fresh analysis using the single source of truth.
                addLog("Analyzing optimization status...")
                addLogEntry(
                    LogEntryType.ANALYZING,
                    "Analyzing apps...",
                    detail = "Checking ${allPackages.size} apps"
                )

                when (val analysisResource = analyzeOptimizationStatus(mode)) {
                    is Resource.Success -> {
                        packagesToOptimize = analysisResource.data.packagesNeedingOptimization
                        skippedCount = analysisResource.data.appsAlreadyOptimized + analysisResource.data.appsWithNoProfile
                    }

                    is Resource.Error -> {
                        val message = when (val err = analysisResource.data) {
                            is ResourceError.LogicError -> err.errorMessage
                            is ResourceError.NetworkError -> err.errorMessage
                            is ResourceError.DatabaseError -> err.message
                            is ResourceError.SSLError -> "SSL error"
                            is ResourceError.UnknownError -> null
                        }
                        // Surface failure consistently; optimization can't proceed without package list.
                        throw IllegalStateException(message ?: "Analysis failed")
                    }
                }
            }

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
                        // Cache this package as successfully optimized
                        recentlyOptimizedPackages[packageName] = System.currentTimeMillis()
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

                // Cancellation can be requested while executing the compile command; check again before updating progress.
                if (optimizationCancelRequested.get()) {
                    addLog("⏹ Optimization cancelled.")
                    addLogEntry(LogEntryType.CANCELLED, "Optimization cancelled", detail = "${index + 1} apps completed")
                    _optimizationProgress.value = _optimizationProgress.value.copy(
                        isRunning = false,
                        result = OptimizationResult.Canceled,
                        currentAppPackage = ""
                    )
                    return@runCatching
                }

                val newCount = index + 1
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    processedCount = newCount,
                    progress = newCount.toFloat() / total.toFloat()
                )
            }

            // Final guard: never overwrite a cancellation state with "Completed".
            if (optimizationCancelRequested.get() || _optimizationProgress.value.result is OptimizationResult.Canceled) {
                addLog("⏹ Optimization cancelled.")
                addLogEntry(LogEntryType.CANCELLED, "Optimization cancelled")
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    isRunning = false,
                    result = OptimizationResult.Canceled,
                    currentAppPackage = ""
                )
                return@runCatching
            }

            addLog("✓ Optimization complete! $total apps optimized, $skippedCount skipped.")
            addLogEntry(LogEntryType.COMPLETE, "Optimization complete!", detail = "$total apps optimized")

            // Update analysis to reflect that all targeted apps are now optimized.
            // Preserve the real appsAlreadyOptimized and appsWithNoProfile counts from the
            // analysis — previously optimized + newly compiled = total optimized now.
            val prevAnalysis = _optimizationAnalysis.value
            _optimizationAnalysis.value = OptimizationAnalysis(
                totalAppsScanned = allPackages.size,
                appsNeedingOptimization = 0,
                appsAlreadyOptimized = prevAnalysis.appsAlreadyOptimized + total,
                appsWithNoProfile = prevAnalysis.appsWithNoProfile,
                isScanning = false,
                lastScanTimeMs = System.currentTimeMillis(),
                mode = mode
            )

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
     * actually performing optimization. Shows per-app progress in the activity feed.
     *
     * @param mode The optimization mode to analyze against.
     * @return [Resource] with [OptimizationAnalysis] results.
     */
    override suspend fun analyzeOptimizationStatus(
        mode: AppOptimizationType
    ): Resource<OptimizationAnalysis> {
        return runCatching {
            // Reset cancellation
            analysisCancelRequested.set(false)

            // Clear previous log entries for a fresh analysis view
            clearLogEntries()

            // Reset per-run caches
            cachedDexoptDump = null
            cachedPackageDumps.clear()

            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = true)

            // Add log entry for analysis start
            addLogEntry(LogEntryType.START, "Starting analysis", detail = "Checking optimization status")

            val allPackages = queryInstalledPackages()

            if (allPackages.isEmpty()) {
                addLogEntry(LogEntryType.INFO, "No packages found")
                val result = OptimizationAnalysis(
                    totalAppsScanned = 0,
                    appsNeedingOptimization = 0,
                    appsAlreadyOptimized = 0,
                    isScanning = false,
                    lastScanTimeMs = System.currentTimeMillis(),
                    mode = mode
                )
                _optimizationAnalysis.value = result
                return@runCatching result
            }

            addLogEntry(LogEntryType.ANALYZING, "Found ${allPackages.size} apps", detail = "Checking each app...")

            val compileMode = mode.value
            var needsOptimization = 0
            var alreadyOptimized = 0
            var noProfile = 0
            val totalApps = allPackages.size
            val packagesNeedingOptimizationList = mutableListOf<String>()

            // Initialize progress tracking
            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
                totalAppsToScan = totalApps,
                totalAppsScanned = 0,
                currentPackage = ""
            )

            for ((index, packageName) in allPackages.withIndex()) {
                // Check for cancellation
                if (analysisCancelRequested.get()) {
                    addLog("Analysis cancelled.")
                    _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = false)
                    // Return partial result or throw? Throwing to be caught by runCatching
                    throw java.util.concurrent.CancellationException("Analysis cancelled by user")
                }

                // Update current package being analyzed
                _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
                    currentPackage = packageName,
                    totalAppsScanned = index
                )

                val compilationInfo = queryPackageCompilationInfo(packageName, compileMode)

                if (compilationInfo.needsOptimization) {
                    needsOptimization++
                    packagesNeedingOptimizationList.add(packageName)
                    // Show apps that need optimization
                    addLogEntry(
                        LogEntryType.INFO,
                        "Needs optimization",
                        packageName = packageName,
                        detail = compilationInfo.compilerFilter?.let { "Current: $it" } ?: "Not compiled"
                    )
                } else {
                    val reason = when (val skip = compilationInfo.skipReason) {
                        is AppCompilationInfo.SkipReason.RecentlyOptimized -> {
                            alreadyOptimized++
                            "Optimized (${skip.filter})"
                        }
                        is AppCompilationInfo.SkipReason.AlreadyOptimal -> {
                            alreadyOptimized++
                            "Optimal (${skip.filter})"
                        }
                        is AppCompilationInfo.SkipReason.NoProfile -> {
                            noProfile++
                            "No profile (never used)"
                        }
                        else -> {
                            alreadyOptimized++
                            "Already optimized"
                        }
                    }

                    val logType = if (compilationInfo.skipReason is AppCompilationInfo.SkipReason.NoProfile) {
                        LogEntryType.NO_PROFILE
                    } else {
                        LogEntryType.SUCCESS
                    }

                    addLogEntry(
                        logType,
                        reason,
                        packageName = packageName
                    )
                }

                // Update progress state
                _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
                    totalAppsScanned = index + 1,
                    appsNeedingOptimization = needsOptimization,
                    appsAlreadyOptimized = alreadyOptimized,
                    appsWithNoProfile = noProfile
                )
            }

            val result = OptimizationAnalysis(
                totalAppsScanned = allPackages.size,
                totalAppsToScan = allPackages.size,
                appsNeedingOptimization = needsOptimization,
                appsAlreadyOptimized = alreadyOptimized,
                appsWithNoProfile = noProfile,
                packagesNeedingOptimization = packagesNeedingOptimizationList,
                isScanning = false,
                currentPackage = "",
                lastScanTimeMs = System.currentTimeMillis(),
                mode = mode
            )
            _optimizationAnalysis.value = result

            // Add completion log entry
            val noProfileSuffix = if (noProfile > 0) ", $noProfile no profile" else ""
            addLogEntry(
                LogEntryType.COMPLETE,
                "Analysis complete",
                detail = "$needsOptimization need optimization, $alreadyOptimized already optimized$noProfileSuffix"
            )

            result
        }.fold(
            onSuccess = { analysis ->
                Resource.Success(analysis)
            },
            onFailure = { throwable ->
                _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = false)
                addLogEntry(LogEntryType.ERROR, "Analysis failed", detail = throwable.message)
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
     * Queries the compilation status for a single package using multiple approaches
     * in order of reliability and performance cost:
     *
     * 1. **Session cache** – zero shell calls; reused for packages optimized this session.
     * 2. **`dumpsys package dexopt`** – single call cached for the entire run; returns the
     *    exact ART compiler filter for every package → early-return as soon as a match is found.
     * 3. **`dumpsys package <pkg>`** – per-package fallback, only when the global dexopt dump
     *    has no entry for this package.
     * 4. **`cmd package compile --check`** – per-package subprocess, last resort when neither
     *    dumpsys approach yields a usable filter; returns only a binary yes/no.
     * 5. **OAT file scan (`ls`)** – absolute last resort to detect presence of compiled
     *    artifacts when all other approaches fail.
     *
     * @param packageName Package to query.
     * @param targetFilter The optimization filter we intend to apply.
     * @return [AppCompilationInfo] with parsed compilation details.
     */
    private suspend fun queryPackageCompilationInfo(
        packageName: String,
        targetFilter: String
    ): AppCompilationInfo {

        // ── Step 1: session cache ─────────────────────────────────────────────────
        // Skip all shell work for packages already optimized this session.
        val cachedOptimizationTime = recentlyOptimizedPackages[packageName]
        if (cachedOptimizationTime != null) {
            val hoursSinceOptimization = (System.currentTimeMillis() - cachedOptimizationTime) / (1000 * 60 * 60)
            if (hoursSinceOptimization < 24) {
                return AppCompilationInfo(
                    packageName = packageName,
                    compilerFilter = targetFilter,
                    lastCompilationTimeMs = cachedOptimizationTime,
                    lastUpdateTimeMs = null,
                    oatFileExists = true,
                    skipReason = AppCompilationInfo.SkipReason.RecentlyOptimized(0, targetFilter),
                    needsOptimization = false
                )
            }
        }

        var lastUpdateTimeMs: Long? = null

        // ── Step 2: dumpsys package dexopt (single call, cached for the whole run) ─
        // Cheapest and most reliable: one shell call amortised across every package,
        // returning the exact ART compiler filter. Early-return immediately when matched.
        if (cachedDexoptDump == null) {
            addLogEntry(LogEntryType.ANALYZING, "Dexopt dump", detail = "dumpsys package dexopt (once per run)")
        } else {
            addLogEntry(LogEntryType.INFO, "Dexopt dump", packageName = packageName, detail = "cache hit")
        }

        val dexoptDump = cachedDexoptDump ?: run {
            val dexoptResult = shellDataSource.executeCommandDetailed("dumpsys package dexopt")
            val dexopt = dexoptResult.getOrNull()
            if (dexopt == null) {
                addLogEntry(LogEntryType.ERROR, "Dexopt dump failed", detail = dexoptResult.exceptionOrNull()?.message)
            } else if (!dexopt.isSuccess) {
                addLogEntry(LogEntryType.ERROR, "Dexopt dump failed",
                    detail = dexopt.stderr.trim().ifEmpty { "exitCode=${dexopt.exitCode}" })
            }
            if (dexopt?.isSuccess == true) dexopt.stdout else null
        }.also { cachedDexoptDump = it }

        if (dexoptDump != null) {
            val filter = DexoptStatusParser.parseCompilerFilterFromDexoptDump(packageName = packageName, dump = dexoptDump)
            if (filter != null) {
                addLogEntry(LogEntryType.INFO, "Dexopt status", packageName = packageName, detail = "filter=$filter")
                // Early-return: definitive filter found — no further shell calls needed.
                return resolveCompilationInfo(packageName, filter, lastUpdateTimeMs, targetFilter)
            }
        }

        // ── Step 3: dumpsys package <pkg> (per-package fallback) ─────────────────
        // Reached only when the global dexopt dump has no entry for this package.
        addLogEntry(LogEntryType.ANALYZING, "Fallback: package dump", packageName = packageName, detail = "dumpsys package")

        val packageResult = shellDataSource.executeCommand("dumpsys package $packageName")
        packageResult.getOrNull()?.let { output ->
            var filter: String? = null
            output.lineSequence().forEach { line ->
                val lower = line.trim().lowercase()
                when {
                    filter == null && (lower.contains("status=") || lower.contains("compiler") ||
                            lower.contains("compilerfilter") || lower.contains("compiler-filter")) ->
                        filter = DexoptStatusParser.parseCompilerFilterFromLine(lower)

                    lower.startsWith("lastupdatetime=") && lastUpdateTimeMs == null -> {
                        lastUpdateTimeMs = parseTimestamp(line.substringAfter("=").trim())
                    }
                }
            }
            if (filter != null) {
                addLogEntry(LogEntryType.INFO, "Dexopt status", packageName = packageName, detail = "filter=$filter")
                // Early-return: definitive filter found — skip steps 4 and 5.
                return resolveCompilationInfo(packageName, filter, lastUpdateTimeMs, targetFilter)
            } else {
                addLogEntry(LogEntryType.INFO, "Package dump", packageName = packageName, detail = "no compiler filter reported")
            }
        } ?: addLogEntry(LogEntryType.ERROR, "Package dump failed", packageName = packageName,
            detail = packageResult.exceptionOrNull()?.message)

        // ── Step 4: cmd package compile --check (per-package, binary yes/no) ──────
        // More expensive than dumpsys (spawns a new process per package) and only returns
        // a boolean — reached only when both dumpsys approaches yielded nothing.
        addLogEntry(LogEntryType.ANALYZING, "Fallback: compile check", packageName = packageName,
            detail = "cmd package compile --check")

        val checkResult = shellDataSource.executeCommandDetailed("cmd package compile --check $packageName")
        val check = checkResult.getOrNull()
        when {
            check == null ->
                addLogEntry(LogEntryType.ERROR, "Compile check failed", packageName = packageName,
                    detail = checkResult.exceptionOrNull()?.message)
            !check.isSuccess ->
                addLogEntry(LogEntryType.INFO, "Compile check unsupported", packageName = packageName,
                    detail = check.stderr.trim().ifEmpty { "exitCode=${check.exitCode}" })
            else -> {
                val checkOutput = check.stdout.trim()
                DexoptStatusParser.parseCompileCheckNeedsOptimization(checkOutput)?.let { needsOpt ->
                    addLogEntry(LogEntryType.INFO, "Compile check result", packageName = packageName,
                        detail = if (needsOpt) "needs optimization" else "already optimal")
                    return AppCompilationInfo(
                        packageName = packageName,
                        compilerFilter = null,
                        lastCompilationTimeMs = null,
                        lastUpdateTimeMs = lastUpdateTimeMs,
                        oatFileExists = false,
                        skipReason = if (needsOpt) null else AppCompilationInfo.SkipReason.AlreadyOptimal("system-check"),
                        needsOptimization = needsOpt
                    )
                }
                addLogEntry(LogEntryType.INFO, "Compile check unparseable", packageName = packageName,
                    detail = checkOutput.take(120))
            }
        }

        // ── Step 5: OAT file scan (absolute last resort) ──────────────────────────
        addLogEntry(LogEntryType.ANALYZING, "Fallback: oat scan", packageName = packageName,
            detail = "ls /data/app/.../oat")
        val oatResult = shellDataSource.executeCommand(
            "ls /data/app/*$packageName*/oat/arm64/*.odex || ls /data/app/*$packageName*/oat/arm/*.odex"
        )
        var oatFilter: String? = null
        oatResult.getOrNull()?.let { output ->
            if (output.trim().isNotEmpty() &&
                !output.contains("No such file", ignoreCase = true) &&
                !output.contains("Permission denied", ignoreCase = true)
            ) {
                oatFilter = "unknown-optimized"
                addLogEntry(LogEntryType.INFO, "OAT files found", packageName = packageName)
            } else {
                addLogEntry(LogEntryType.INFO, "OAT files not accessible", packageName = packageName)
            }
        } ?: addLogEntry(LogEntryType.ERROR, "OAT scan failed", packageName = packageName,
            detail = oatResult.exceptionOrNull()?.message)

        return resolveCompilationInfo(packageName, oatFilter, lastUpdateTimeMs, targetFilter)
    }

    /**
     * Applies the decision logic that maps a resolved [compilerFilter] (or null) to
     * [AppCompilationInfo.needsOptimization] and [AppCompilationInfo.skipReason].
     *
     * Extracted so that each fallback step can early-return as soon as a compiler filter
     * is known, without duplicating the branching logic.
     *
     * @param packageName Package being evaluated.
     * @param compilerFilter The compiler filter resolved by any fallback step, or null.
     * @param lastUpdateTimeMs App's last-update timestamp, if available.
     * @param targetFilter The optimization filter we intend to apply.
     * @return Fully resolved [AppCompilationInfo].
     */
    private suspend fun resolveCompilationInfo(
        packageName: String,
        compilerFilter: String?,
        lastUpdateTimeMs: Long?,
        targetFilter: String
    ): AppCompilationInfo {
        val needsOptimization: Boolean
        val skipReason: AppCompilationInfo.SkipReason?

        when {
            compilerFilter == null -> {
                needsOptimization = true
                skipReason = null
            }

            // "verify" means no runtime profile exists — the user has never opened the app.
            // Profile-guided compilation (speed-profile) is pointless without a profile.
            // For full (speed) mode, we still compile since it doesn't rely on profiles.
            compilerFilter == "verify" && targetFilter.lowercase() == "speed-profile" -> {
                needsOptimization = false
                skipReason = AppCompilationInfo.SkipReason.NoProfile(compilerFilter)
            }

            // Some Android builds list overlay/RRO packages in the Dexopt state section
            // without providing any compiler filter details. Only skip when we can confirm
            // it is an overlay — otherwise keep it eligible to avoid hiding real system apps.
            compilerFilter == "unknown-present" -> {
                val dump = cachedPackageDumps[packageName] ?: run {
                    shellDataSource.executeCommand("dumpsys package $packageName")
                        .getOrNull()?.also { cachedPackageDumps[packageName] = it }
                }
                val isOverlayLike = PackageClassifier.isOverlayLike(
                    packageName = packageName, dumpsysPackageOutput = dump
                )
                if (isOverlayLike) {
                    needsOptimization = false
                    skipReason = AppCompilationInfo.SkipReason.AlreadyOptimal("overlay/rro")
                    addLogEntry(LogEntryType.INFO, "Overlay classification",
                        packageName = packageName, detail = "Confirmed as overlay/RRO")
                } else {
                    needsOptimization = true
                    skipReason = null
                    addLogEntry(LogEntryType.INFO, "Overlay classification",
                        packageName = packageName, detail = "Not an overlay → keep eligible")
                }
            }

            // OAT files exist but exact filter is unreadable.
            // Assume optimized for speed; for speed-profile re-compile conservatively since
            // we can't confirm the profile was actually used.
            compilerFilter == "unknown-optimized" -> {
                needsOptimization = targetFilter.lowercase() == "speed-profile"
                skipReason = if (needsOptimization) null
                             else AppCompilationInfo.SkipReason.RecentlyOptimized(0, "compiled")
            }

            isFilterOptimalForTarget(compilerFilter, targetFilter) -> {
                needsOptimization = false
                skipReason = AppCompilationInfo.SkipReason.RecentlyOptimized(0, compilerFilter)
            }

            else -> {
                needsOptimization = true
                skipReason = null
            }
        }

        return AppCompilationInfo(
            packageName = packageName,
            compilerFilter = compilerFilter,
            lastCompilationTimeMs = null,
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

    /**
     * Clears the last optimization result, resetting progress and statistics.
     *
     * This is useful to dismiss the result card and ensure no stale data
     * is shown in the UI after an optimization run is completed.
     *
     * @return Always returns [Resource.Success] since this operation cannot fail.
     */
    override suspend fun clearOptimizationResult(): Resource<Unit> {
        return runCatching {
            val current = _optimizationProgress.value
            if (current.isRunning) {
                // Dismiss should never disrupt an active run; keep state unchanged.
                return@runCatching
            }

            // Reset the snapshot so every UI surface stops rendering stale counts.
            _optimizationProgress.value = OptimizationProgress(
                runId = 0L,
                isRunning = false,
                result = OptimizationResult.None,
                currentAppPackage = "",
                progress = 0f,
                processedCount = 0,
                skippedCount = 0,
                totalCount = 0
            )
        }.fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { Resource.Error(ResourceError.LogicError(it.message)) }
        )
    }
}
