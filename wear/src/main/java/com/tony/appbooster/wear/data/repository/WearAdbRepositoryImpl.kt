package com.tony.appbooster.wear.data.repository

import android.util.Log
import com.tony.appbooster.wear.data.client.WearAdbClient
import com.tony.appbooster.wear.domain.model.AdbConnectionState
import com.tony.appbooster.wear.domain.model.OptimizationProgress
import com.tony.appbooster.wear.domain.model.OptimizationResult
import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.model.ResourceError
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [WearAdbRepository] that uses [WearAdbClient] to connect
 * to the local ADB daemon and execute optimization commands.
 *
 * This enables Wear OS to optimize its own apps by connecting to itself
 * via wireless debugging, similar to how the phone app uses Shizuku.
 *
 * @property adbClient The ADB client for localhost connections.
 */
@Singleton
class WearAdbRepositoryImpl @Inject constructor(
    private val adbClient: WearAdbClient
) : WearAdbRepository {

    override val connectionState: StateFlow<AdbConnectionState> = adbClient.connectionState

    private val _optimizationProgress = MutableStateFlow(OptimizationProgress())
    override val optimizationProgress: StateFlow<OptimizationProgress> = _optimizationProgress.asStateFlow()

    private val _commandOutput = MutableStateFlow<List<String>>(emptyList())
    override val commandOutput: StateFlow<List<String>> = _commandOutput.asStateFlow()

    private val cancelRequested = AtomicBoolean(false)

    override suspend fun pair(port: Int, pairingCode: String): Resource<Unit> {
        return adbClient.pair(port, pairingCode).fold(
            onSuccess = {
                addLog("Pairing successful!")
                Resource.Success(Unit)
            },
            onFailure = { e ->
                addLog("Pairing failed: ${e.message}")
                Resource.Error(ResourceError.AdbError(e.message, e))
            }
        )
    }

    override suspend fun connect(port: Int): Resource<Unit> {
        addLog("Connecting to localhost:$port...")
        return adbClient.connect(port).fold(
            onSuccess = {
                addLog("Connected to ADB daemon")
                Resource.Success(Unit)
            },
            onFailure = { e ->
                addLog("Connection failed: ${e.message}")
                Resource.Error(ResourceError.AdbError(e.message, e))
            }
        )
    }

    override suspend fun autoConnect(): Resource<Unit> {
        // For now, auto-connect tries the default port
        // TODO: Implement mDNS discovery for the actual port
        addLog("Attempting auto-connect...")
        return connect(DEFAULT_ADB_PORT)
    }

    override suspend fun disconnect() {
        adbClient.disconnect()
        addLog("Disconnected from ADB")
    }

    override suspend fun hasPaired(): Boolean = adbClient.hasPaired()

    override suspend fun executeOptimization(mode: OptimizationType): Resource<Unit> {
        cancelRequested.set(false)

        return runCatching {
            // Get list of installed packages
            addLog("Querying installed packages...")
            val packagesResult = adbClient.execute("pm list packages")
            val packagesOutput = packagesResult.getOrThrow()

            val packages = packagesOutput.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotBlank() && it != SELF_PACKAGE }

            val total = packages.size
            if (total == 0) {
                addLog("No packages found to optimize")
                return Resource.Success(Unit)
            }

            val runId = System.currentTimeMillis()
            _optimizationProgress.value = OptimizationProgress(
                runId = runId,
                isRunning = true,
                result = OptimizationResult.None,
                totalCount = total,
                processedCount = 0,
                progress = 0f
            )

            addLog("Found $total packages to optimize (Mode: ${mode.value})")

            packages.forEachIndexed { index, packageName ->
                if (cancelRequested.get()) {
                    addLog("⏹ Optimization cancelled")
                    _optimizationProgress.value = _optimizationProgress.value.copy(
                        isRunning = false,
                        result = OptimizationResult.Canceled,
                        currentAppPackage = ""
                    )
                    return Resource.Success(Unit)
                }

                _optimizationProgress.value = _optimizationProgress.value.copy(
                    currentAppPackage = packageName
                )

                val command = "cmd package compile -m ${mode.value} -f $packageName"
                Log.d(TAG, "> $command")

                val result = adbClient.execute(command)
                result.fold(
                    onSuccess = { output ->
                        val trimmed = output.trim()
                        if (trimmed.isNotBlank() && !trimmed.equals("Success", ignoreCase = true)) {
                            Log.d(TAG, "Output: $trimmed")
                        }
                    },
                    onFailure = { e ->
                        addLog("Failed: $packageName - ${e.message}")
                    }
                )

                val newCount = index + 1
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    processedCount = newCount,
                    progress = newCount.toFloat() / total.toFloat()
                )
            }

            addLog("✓ Optimization complete! $total apps optimized")
            _optimizationProgress.value = _optimizationProgress.value.copy(
                isRunning = false,
                result = OptimizationResult.Completed,
                currentAppPackage = "",
                progress = 1f
            )
        }.fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { e ->
                addLog("Optimization failed: ${e.message}")
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    isRunning = false,
                    result = OptimizationResult.None
                )
                Resource.Error(ResourceError.AdbError("Optimization failed: ${e.message}", e))
            }
        )
    }

    override suspend fun cancelOptimization(): Resource<Unit> {
        cancelRequested.set(true)
        addLog("Cancellation requested...")
        return Resource.Success(Unit)
    }

    private fun addLog(message: String) {
        Log.d(TAG, message)
        _commandOutput.value = _commandOutput.value + message
    }

    companion object {
        private const val TAG = "WearAdbRepository"
        private const val DEFAULT_ADB_PORT = 5555
        private const val SELF_PACKAGE = "com.tony.appbooster.wear"
    }
}
