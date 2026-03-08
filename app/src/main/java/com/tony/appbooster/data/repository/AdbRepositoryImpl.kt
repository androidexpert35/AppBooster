package com.tony.appbooster.data.repository

import com.tony.appbooster.data.util.CompilationInfoResolver
import com.tony.appbooster.data.util.OptimizationLogger
import com.tony.appbooster.data.util.PackageListQueryService
import com.tony.appbooster.domain.client.AdbShellDataSource
import com.tony.appbooster.domain.model.common.LogEntryType
import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbConnectionState
import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Shizuku-based [AdbRepository] implementation that orchestrates
 * privileged shell operations for app optimisation.
 *
 * This class is a **thin orchestrator**: heavy responsibilities are
 * delegated to single-purpose helpers:
 * - [OptimizationLogger] — log state management.
 * - [PackageListQueryService] — `pm list packages` queries and parsing.
 * - [CompilationInfoResolver] — per-package compilation status resolution.
 *
 * @property shellDataSource Data source that executes shell commands via Shizuku.
 * @property logger Shared structured logger for diagnostic output.
 * @property packageQuery Service for querying installed packages.
 * @property compilationResolver Service for resolving per-package compilation status.
 * @constructor Creates the repository with all required collaborators.
 */
class AdbRepositoryImpl @Inject constructor(
    private val shellDataSource: AdbShellDataSource,
    private val logger: OptimizationLogger,
    private val packageQuery: PackageListQueryService,
    private val compilationResolver: CompilationInfoResolver
) : AdbRepository {

    private val _connectionState =
        MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    override val connectionState = _connectionState.asStateFlow()

    override val commandOutput get() = logger.commandOutput
    override val logEntries get() = logger.logEntries

    private val _optimizationProgress = MutableStateFlow(OptimizationProgress())
    override val optimizationProgress = _optimizationProgress.asStateFlow()

    private val _optimizationAnalysis = MutableStateFlow(OptimizationAnalysis())
    override val optimizationAnalysis = _optimizationAnalysis.asStateFlow()

    private val optimizationCancelRequested = AtomicBoolean(false)
    private val analysisCancelRequested = AtomicBoolean(false)

    // ─────────────────────────────────────────────────────────────────────────────
    // Connection
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Ensures Shizuku is ready and validates shell access with a health check.
     *
     * @return [Resource.Success] when ready, or [Resource.Error] with details.
     */
    override suspend fun ensureConnected(): Resource<Unit> = runCatching {
        _connectionState.value = AdbConnectionState.Connecting
        logger.addLog("Validating Shizuku shell access...")

        val command = "echo connected"
        logger.addLog("> $command")

        val healthOutput = shellDataSource.executeCommand(command)
            .getOrThrow()
            .trim()

        logger.addLog("Shell response: $healthOutput")
        _connectionState.value = AdbConnectionState.Connected
        logger.addLog("Shizuku shell session ready.")
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { throwable ->
            _connectionState.value = AdbConnectionState.Error(
                message = throwable.message ?: "Failed to validate Shizuku connection."
            )
            logger.addLog("Error: ${throwable.message}")
            Resource.Error(
                ResourceError.LogicError(
                    errorMessage = throwable.message
                        ?: "Shizuku is not ready. Please ensure Shizuku is installed, running, and permission is granted."
                )
            )
        }
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // Cancellation
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Requests cancellation of the ongoing optimisation process, if any.
     *
     * Sets a flag that is checked between package compilation commands.
     * The current step will complete before the process is marked as cancelled.
     *
     * @return [Resource.Success] if cancellation is successfully requested,
     *         or [Resource.Error] with details if the request fails.
     */
    override suspend fun cancelOptimization(): Resource<Unit> = runCatching {
        if (!_optimizationProgress.value.isRunning) {
            logger.addLog("No optimization is currently running.")
            return@runCatching
        }

        val current = _optimizationProgress.value
        _optimizationProgress.value = current.copy(
            isRunning = false,
            result = OptimizationResult.Canceled,
            currentAppPackage = "",
            progress = current.progress.coerceIn(0f, 1f)
        )

        optimizationCancelRequested.set(true)
        logger.addLog("⏹ Cancelling optimization...")
        logger.addLogEntry(LogEntryType.CANCELLED, "Optimization cancelled")
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { Resource.Error(ResourceError.LogicError(it.message)) }
    )

    /**
     * Requests cancellation of the ongoing analysis scan, if any.
     *
     * @return [Resource.Success] if cancellation is successfully requested,
     *         or [Resource.Error] with details if the request fails.
     */
    override suspend fun cancelAnalysis(): Resource<Unit> = runCatching {
        if (!_optimizationAnalysis.value.isScanning) {
            logger.addLog("No analysis is currently running.")
            return@runCatching
        }

        _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
            isScanning = false,
            currentPackage = ""
        )

        analysisCancelRequested.set(true)
        logger.addLog("⏹ Cancelling analysis...")
        logger.addLogEntry(LogEntryType.CANCELLED, "Analysis cancelled")
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { Resource.Error(ResourceError.LogicError(it.message)) }
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // Optimisation
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Runs the full optimisation routine: analyses which packages need
     * compilation, then compiles them one-by-one using the requested mode.
     *
     * @param mode Optimisation strategy that maps to the compile mode.
     * @return [Resource.Success] when the flow completes,
     *         or [Resource.Error] describing the failure.
     */
    override suspend fun executeOptimizationCommand(
        mode: AppOptimizationType,
        forceOptimize: Boolean
    ): Resource<Unit> {
        val compileMode = mode.value
        return runCatching {
            resetForNewRun()
            val forceLabel = if (forceOptimize) " (Force)" else ""
            logger.addLogEntry(LogEntryType.START, "Starting optimization",
                detail = "Mode: $compileMode$forceLabel")

            val allPackages = packageQuery.queryInstalledPackages()
            if (allPackages.isEmpty()) {
                logger.addLog("No packages found for optimization.")
                logger.addLogEntry(LogEntryType.INFO, "No packages found")
                return@runCatching
            }
            logger.addLog("Found ${allPackages.size} installed packages.")

            val packagesToOptimize: List<String>
            val skippedCount: Int

            if (forceOptimize) {
                // Force mode: compile every package, skip nothing
                packagesToOptimize = allPackages
                skippedCount = 0
                logger.addLog("Force mode enabled — all ${allPackages.size} packages will be compiled.")
                logger.addLogEntry(LogEntryType.INFO, "Force mode",
                    detail = "Bypassing analysis — compiling all apps")
            } else {
                val (resolved, skipped) = resolvePackagesToOptimize(mode, allPackages)
                packagesToOptimize = resolved
                skippedCount = skipped
            }

            if (packagesToOptimize.isEmpty()) {
                handleAllAlreadyOptimized(skippedCount)
                return@runCatching
            }

            val total = packagesToOptimize.size
            val runId = System.currentTimeMillis()

            _optimizationProgress.value = OptimizationProgress(
                runId = runId,
                isRunning = true,
                result = OptimizationResult.None,
                totalCount = total,
                skippedCount = skippedCount,
                progress = 0f
            )

            logOptimizationStart(total, skippedCount, compileMode)

            compilePackages(packagesToOptimize, compileMode, total)

            // Guard: never overwrite a cancellation with "Completed"
            if (wasCancelled()) return@runCatching

            finaliseCompletion(allPackages.size, total, skippedCount, mode)
        }.fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { throwable ->
                logger.addLog("Optimization failed: ${throwable.message}")
                logger.addLogEntry(LogEntryType.ERROR, "Optimization failed", detail = throwable.message)
                _optimizationProgress.value = _optimizationProgress.value.copy(isRunning = false)
                Resource.Error(
                    ResourceError.LogicError(
                        errorMessage = "Optimization failed: ${throwable.message}",
                        errorCode = "ADB_OPTIMIZATION_FAILED"
                    )
                )
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Analysis
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Analyses all installed apps to determine which need optimisation.
     *
     * This is a lightweight scan that checks compilation status without
     * actually performing optimisation. Results are exposed via
     * [optimizationAnalysis].
     *
     * @param mode The optimisation mode to analyse against.
     * @return [Resource] with [OptimizationAnalysis] results.
     */
    override suspend fun analyzeOptimizationStatus(
        mode: AppOptimizationType
    ): Resource<OptimizationAnalysis> = runCatching {
        analysisCancelRequested.set(false)
        logger.clearLogEntries()
        compilationResolver.resetCaches()

        _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = true)
        logger.addLogEntry(LogEntryType.START, "Starting analysis", detail = "Checking optimization status")

        val allPackages = packageQuery.queryInstalledPackages()
        if (allPackages.isEmpty()) {
            logger.addLogEntry(LogEntryType.INFO, "No packages found")
            return@runCatching emptyAnalysisResult(mode)
        }

        logger.addLogEntry(LogEntryType.ANALYZING, "Found ${allPackages.size} apps",
            detail = "Checking each app...")

        performAnalysisScan(allPackages, mode)
    }.fold(
        onSuccess = { Resource.Success(it) },
        onFailure = { throwable ->
            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(isScanning = false)
            logger.addLogEntry(LogEntryType.ERROR, "Analysis failed", detail = throwable.message)
            Resource.Error(
                ResourceError.LogicError(
                    errorMessage = "Analysis failed: ${throwable.message}",
                    errorCode = "ADB_ANALYSIS_FAILED"
                )
            )
        }
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // Dismissal
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Clears the last optimisation result, resetting progress and statistics.
     *
     * @return Always returns [Resource.Success] since this operation cannot fail.
     */
    override suspend fun clearOptimizationResult(): Resource<Unit> = runCatching {
        if (_optimizationProgress.value.isRunning) return@runCatching

        _optimizationProgress.value = OptimizationProgress()
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { Resource.Error(ResourceError.LogicError(it.message)) }
    )

    // ═══════════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Resets cancellation flags, log entries, and per-run caches. */
    private fun resetForNewRun() {
        optimizationCancelRequested.set(false)
        logger.clearLogEntries()
        compilationResolver.resetCaches()
    }

    /**
     * Determines which packages need optimisation, reusing a cached analysis
     * when valid or performing a fresh one.
     *
     * @return Pair of (packages needing optimisation, count of skipped apps).
     */
    private suspend fun resolvePackagesToOptimize(
        mode: AppOptimizationType,
        allPackages: List<String>
    ): Pair<List<String>, Int> {
        val existing = _optimizationAnalysis.value
        val analysisIsValid = existing.lastScanTimeMs != null &&
            existing.totalAppsScanned > 0 &&
            existing.mode == mode

        if (analysisIsValid) {
            logger.addLog("Using existing analysis from this session")
            logger.addLogEntry(LogEntryType.INFO, "Using cached analysis",
                detail = "${existing.appsNeedingOptimization} apps need optimization")
            return existing.packagesNeedingOptimization to
                (existing.appsAlreadyOptimized + existing.appsWithNoProfile)
        }

        logger.addLog("Analyzing optimization status...")
        logger.addLogEntry(LogEntryType.ANALYZING, "Analyzing apps...",
            detail = "Checking ${allPackages.size} apps")

        return when (val result = analyzeOptimizationStatus(mode)) {
            is Resource.Success -> {
                val data = result.data
                data.packagesNeedingOptimization to
                    (data.appsAlreadyOptimized + data.appsWithNoProfile)
            }
            is Resource.Error -> {
                val message = when (val err = result.data) {
                    is ResourceError.LogicError -> err.errorMessage
                    is ResourceError.NetworkError -> err.errorMessage
                    is ResourceError.DatabaseError -> err.message
                    is ResourceError.SSLError -> "SSL error"
                    is ResourceError.UnknownError -> null
                }
                throw IllegalStateException(message ?: "Analysis failed")
            }
        }
    }

    /** Handles the case where every package is already optimised. */
    private fun handleAllAlreadyOptimized(skippedCount: Int) {
        logger.addLog("✓ All apps are already optimized ($skippedCount apps skipped).")
        logger.addLog("No optimization needed at this time.")
        logger.addLogEntry(LogEntryType.COMPLETE, "All apps optimized!",
            detail = "$skippedCount apps already optimal")

        _optimizationProgress.value = OptimizationProgress(
            runId = System.currentTimeMillis(),
            isRunning = false,
            result = OptimizationResult.Completed,
            skippedCount = skippedCount,
            progress = 1f
        )
    }

    /** Emits initial log messages for the compilation loop. */
    private fun logOptimizationStart(total: Int, skippedCount: Int, compileMode: String) {
        logger.addLog("Optimizing $total apps ($skippedCount already optimized, skipped).")
        logger.addLog("(Excluding ${PackageListQueryService.SELF_PACKAGE_NAME} to prevent self-crash)")
        logger.addLog("Starting compilation (Mode: $compileMode)...")
        logger.addLogEntry(LogEntryType.INFO, "Mode: $compileMode",
            detail = "$total to optimize, $skippedCount skipped")
    }

    /**
     * Iterates over [packages] and compiles each one, checking for
     * cancellation between iterations.
     */
    private suspend fun compilePackages(
        packages: List<String>,
        compileMode: String,
        total: Int
    ) {
        packages.forEachIndexed { index, packageName ->
            if (checkCancelled(index)) return

            _optimizationProgress.value = _optimizationProgress.value.copy(
                currentAppPackage = packageName
            )
            logger.addLogEntry(LogEntryType.OPTIMIZING, "Optimizing...", packageName = packageName)

            val command = "cmd package compile -m $compileMode -f $packageName"
            logger.addLog("> $command")

            shellDataSource.executeCommand(command).fold(
                onSuccess = { output ->
                    logger.addLog("Success: optimized $packageName")
                    logger.addLogEntry(LogEntryType.SUCCESS, "Optimized", packageName = packageName)
                    compilationResolver.markOptimized(packageName)
                    val trimmed = output.trim()
                    if (trimmed.isNotBlank() && !trimmed.equals("Success", ignoreCase = true)) {
                        logger.addLog(trimmed)
                    }
                },
                onFailure = { throwable ->
                    logger.addLog("Failure: $packageName - ${throwable.message}")
                    logger.addLogEntry(LogEntryType.ERROR, "Failed",
                        packageName = packageName, detail = throwable.message)
                }
            )

            if (checkCancelled(index + 1)) return

            val newCount = index + 1
            _optimizationProgress.value = _optimizationProgress.value.copy(
                processedCount = newCount,
                progress = newCount.toFloat() / total.toFloat()
            )
        }
    }

    /** Returns true (and updates state) if cancellation was requested. */
    private fun checkCancelled(completedCount: Int): Boolean {
        if (!optimizationCancelRequested.get() &&
            _optimizationProgress.value.result !is OptimizationResult.Canceled
        ) return false

        logger.addLog("⏹ Optimization cancelled.")
        logger.addLogEntry(LogEntryType.CANCELLED, "Optimization cancelled",
            detail = "$completedCount apps completed")
        _optimizationProgress.value = _optimizationProgress.value.copy(
            isRunning = false,
            result = OptimizationResult.Canceled,
            currentAppPackage = ""
        )
        return true
    }

    /** Quick check combining both cancellation signals. */
    private fun wasCancelled(): Boolean =
        optimizationCancelRequested.get() ||
            _optimizationProgress.value.result is OptimizationResult.Canceled

    /** Updates analysis and progress state after a successful run. */
    private fun finaliseCompletion(
        totalInstalled: Int,
        optimisedCount: Int,
        skippedCount: Int,
        mode: AppOptimizationType
    ) {
        logger.addLog("✓ Optimization complete! $optimisedCount apps optimized.")
        logger.addLogEntry(LogEntryType.COMPLETE, "Optimization complete!",
            detail = "$optimisedCount apps optimized")

        val prevAnalysis = _optimizationAnalysis.value
        _optimizationAnalysis.value = OptimizationAnalysis(
            totalAppsScanned = totalInstalled,
            // freshly compiled this run + apps that were already optimal (skipped)
            appsNeedingOptimization = 0,
            appsAlreadyOptimized = optimisedCount + skippedCount,
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
    }

    /** Builds an empty analysis result for when no packages are found. */
    private fun emptyAnalysisResult(mode: AppOptimizationType): OptimizationAnalysis {
        val result = OptimizationAnalysis(
            isScanning = false,
            lastScanTimeMs = System.currentTimeMillis(),
            mode = mode
        )
        _optimizationAnalysis.value = result
        return result
    }

    /**
     * Scans all [packages] to build a full [OptimizationAnalysis].
     *
     * @param packages All installed package names.
     * @param mode The optimisation mode to analyse against.
     * @return Completed [OptimizationAnalysis].
     */
    private suspend fun performAnalysisScan(
        packages: List<String>,
        mode: AppOptimizationType
    ): OptimizationAnalysis {
        val compileMode = mode.value
        var needsOptimization = 0
        var alreadyOptimized = 0
        var noProfile = 0
        val totalApps = packages.size
        val packagesNeedingList = mutableListOf<String>()

        _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
            totalAppsToScan = totalApps,
            totalAppsScanned = 0,
            currentPackage = ""
        )

        for ((index, packageName) in packages.withIndex()) {
            if (analysisCancelRequested.get()) {
                throw java.util.concurrent.CancellationException("Analysis cancelled by user")
            }

            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
                currentPackage = packageName,
                totalAppsScanned = index
            )

            val info = compilationResolver.queryPackageCompilationInfo(packageName, compileMode)

            if (info.needsOptimization) {
                needsOptimization++
                packagesNeedingList.add(packageName)
                logger.addLogEntry(LogEntryType.INFO, "Needs optimization",
                    packageName = packageName,
                    detail = info.compilerFilter?.let { "Current: $it" } ?: "Not compiled")
            } else {
                classifySkippedPackage(info).let { (logType, reason) ->
                    when (logType) {
                        LogEntryType.NO_PROFILE -> noProfile++
                        else -> alreadyOptimized++
                    }
                    logger.addLogEntry(logType, reason, packageName = packageName)
                }
            }

            _optimizationAnalysis.value = _optimizationAnalysis.value.copy(
                totalAppsScanned = index + 1,
                appsNeedingOptimization = needsOptimization,
                appsAlreadyOptimized = alreadyOptimized,
                appsWithNoProfile = noProfile
            )
        }

        val result = OptimizationAnalysis(
            totalAppsScanned = totalApps,
            totalAppsToScan = totalApps,
            appsNeedingOptimization = needsOptimization,
            appsAlreadyOptimized = alreadyOptimized,
            appsWithNoProfile = noProfile,
            packagesNeedingOptimization = packagesNeedingList,
            isScanning = false,
            currentPackage = "",
            lastScanTimeMs = System.currentTimeMillis(),
            mode = mode
        )
        _optimizationAnalysis.value = result

        val noProfileSuffix = if (noProfile > 0) ", $noProfile no profile" else ""
        logger.addLogEntry(LogEntryType.COMPLETE, "Analysis complete",
            detail = "$needsOptimization need optimization, $alreadyOptimized already optimized$noProfileSuffix")

        return result
    }

    /**
     * Resolves a user-friendly log type and reason string for a skipped package.
     *
     * @param info Compilation info for the skipped package.
     * @return Pair of (log entry type, human-readable reason).
     */
    private fun classifySkippedPackage(
        info: com.tony.appbooster.domain.model.common.AppCompilationInfo
    ): Pair<LogEntryType, String> = when (val skip = info.skipReason) {
        is com.tony.appbooster.domain.model.common.AppCompilationInfo.SkipReason.RecentlyOptimized ->
            LogEntryType.SUCCESS to "Optimized (${skip.filter})"
        is com.tony.appbooster.domain.model.common.AppCompilationInfo.SkipReason.AlreadyOptimal ->
            LogEntryType.SUCCESS to "Optimal (${skip.filter})"
        is com.tony.appbooster.domain.model.common.AppCompilationInfo.SkipReason.NoProfile ->
            LogEntryType.NO_PROFILE to "No profile (never used)"
        else ->
            LogEntryType.SUCCESS to "Already optimized"
    }
}
