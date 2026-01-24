package com.tony.appbooster.presentation.viewmodel.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tony.appbooster.data.client.RemoteWatchAdbClient
import com.tony.appbooster.domain.client.WearableDataClient
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.model.wearable.PhoneReadinessStatus
import com.tony.appbooster.domain.model.wearable.WatchOptimizationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * ViewModel for the Watch screen in the phone app.
 *
 * Manages the connection to the watch's ADB daemon and orchestrates
 * remote optimization execution. The phone app connects to the watch
 * via wireless debugging and executes optimization commands remotely.
 *
 * @property remoteWatchAdbClient Client for ADB connection to watch.
 * @property wearableDataClient Client for Data Layer communication.
 */
@HiltViewModel
class WatchViewModel @Inject constructor(
    private val remoteWatchAdbClient: RemoteWatchAdbClient,
    private val wearableDataClient: WearableDataClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchUiState())
    private val _optimizationProgress = MutableStateFlow(OptimizationProgress())
    private val _commandOutput = MutableStateFlow<List<String>>(emptyList())
    private val cancelRequested = AtomicBoolean(false)

    /**
     * Current UI state combining local state with connection states.
     */
    val uiState: StateFlow<WatchUiState> = combine(
        _uiState,
        remoteWatchAdbClient.connectionState,
        wearableDataClient.isWatchConnected,
        _optimizationProgress,
        _commandOutput
    ) { state, adbState, isWatchConnected, progress, logs ->
        state.copy(
            adbConnectionState = adbState,
            isWatchConnected = isWatchConnected,
            optimizationProgress = progress,
            commandLogs = logs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WatchUiState()
    )

    init {
        // Broadcast readiness initially
        viewModelScope.launch {
            broadcastReadiness()
        }
    }

    /**
     * Updates connection input fields.
     *
     * @param ip Watch IP address.
     * @param pairPort Pairing port from wireless debugging.
     * @param pairCode 6-digit pairing code.
     * @param connectPort Connection port for ADB.
     */
    fun updateInputs(
        ip: String? = null,
        pairPort: String? = null,
        pairCode: String? = null,
        connectPort: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                pairingIp = ip ?: current.pairingIp,
                pairingPort = pairPort ?: current.pairingPort,
                pairingCode = pairCode ?: current.pairingCode,
                connectionPort = connectPort ?: current.connectionPort
            )
        }
    }

    /**
     * Initiates pairing with the watch's ADB daemon.
     */
    fun pair() {
        val state = _uiState.value
        val ip = state.pairingIp
        val port = state.pairingPort.toIntOrNull()
        val code = state.pairingCode

        if (ip.isBlank() || port == null || code.isBlank()) {
            _uiState.update { it.copy(lastError = "Invalid pairing input") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastError = null) }
            val result = remoteWatchAdbClient.pair(ip, port, code)
            result.onSuccess {
                _uiState.update { it.copy(isBusy = false, lastMessage = "Pairing successful! Now connect.") }
            }.onFailure { e ->
                _uiState.update { it.copy(isBusy = false, lastError = "Pairing failed: ${e.message}") }
            }
        }
    }

    /**
     * Connects to the watch's ADB daemon.
     */
    fun connect() {
        val state = _uiState.value
        val ip = state.pairingIp
        val port = state.connectionPort.toIntOrNull()

        if (ip.isBlank() || port == null) {
            _uiState.update { it.copy(lastError = "Invalid connection input") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastError = null) }
            val result = remoteWatchAdbClient.connect(ip, port)
            result.onSuccess {
                _uiState.update { it.copy(isBusy = false, lastMessage = "Connected to watch!") }
                broadcastReadiness()
            }.onFailure { e ->
                _uiState.update { it.copy(isBusy = false, lastError = "Connection failed: ${e.message}") }
            }
        }
    }

    /**
     * Disconnects from the watch's ADB daemon.
     */
    fun disconnect() {
        viewModelScope.launch {
            remoteWatchAdbClient.disconnect()
            _optimizationProgress.value = OptimizationProgress()
            broadcastReadiness()
        }
    }

    /**
     * Shows the key import dialog.
     */
    fun showKeyImportDialog() {
        _uiState.update { it.copy(showKeyImportDialog = true) }
    }

    /**
     * Hides the key import dialog.
     */
    fun hideKeyImportDialog() {
        _uiState.update { it.copy(showKeyImportDialog = false, privateKeyInput = "", publicKeyInput = "") }
    }

    /**
     * Updates key import input fields.
     */
    fun updateKeyInputs(privateKey: String? = null, publicKey: String? = null) {
        _uiState.update { current ->
            current.copy(
                privateKeyInput = privateKey ?: current.privateKeyInput,
                publicKeyInput = publicKey ?: current.publicKeyInput
            )
        }
    }

    /**
     * Imports ADB keys from user input.
     *
     * Users can copy their PC's adbkey and adbkey.pub file contents here.
     */
    fun importKeys() {
        val state = _uiState.value
        if (state.privateKeyInput.isBlank() || state.publicKeyInput.isBlank()) {
            _uiState.update { it.copy(lastError = "Both private and public keys are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lastError = null) }
            val result = remoteWatchAdbClient.importAdbKeys(state.privateKeyInput, state.publicKeyInput)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        lastMessage = "Keys imported! Now tap Connect.",
                        showKeyImportDialog = false,
                        privateKeyInput = "",
                        publicKeyInput = ""
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isBusy = false, lastError = "Import failed: ${e.message}") }
            }
        }
    }

    /**
     * Cycles through available optimization modes.
     */
    fun toggleOptimizationMode() {
        _uiState.update { current ->
            val modes = AppOptimizationType.entries
            val currentIndex = modes.indexOf(current.selectedOptimizationType)
            val nextIndex = (currentIndex + 1) % modes.size
            current.copy(selectedOptimizationType = modes[nextIndex])
        }
    }

    /**
     * Starts optimization on the connected watch.
     *
     * Executes package compilation commands remotely via ADB connection.
     */
    fun startWatchOptimization() {
        if (!remoteWatchAdbClient.isConnected()) {
            _uiState.update { it.copy(lastError = "Not connected to watch") }
            return
        }

        val mode = _uiState.value.selectedOptimizationType

        viewModelScope.launch {
            cancelRequested.set(false)
            _commandOutput.value = emptyList()

            addLog("Starting watch optimization (Mode: ${mode.value})...")
            broadcastOptimizationStatus(isRunning = true)

            runCatching {
                // Get list of installed packages on watch
                addLog("Querying installed packages on watch...")
                val packagesResult = remoteWatchAdbClient.getInstalledPackages()
                val packages = packagesResult.getOrThrow()
                    .filter { !it.contains("com.tony.appbooster") }

                val total = packages.size
                if (total == 0) {
                    addLog("No packages found on watch")
                    return@runCatching
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

                addLog("Found $total packages to optimize")

                packages.forEachIndexed { index, packageName ->
                    if (cancelRequested.get()) {
                        addLog("⏹ Optimization cancelled")
                        _optimizationProgress.value = _optimizationProgress.value.copy(
                            isRunning = false,
                            result = OptimizationResult.Canceled,
                            currentAppPackage = ""
                        )
                        broadcastOptimizationStatus(isRunning = false, isCancelled = true)
                        return@runCatching
                    }

                    _optimizationProgress.value = _optimizationProgress.value.copy(
                        currentAppPackage = packageName
                    )

                    // Sync status to watch for display
                    broadcastOptimizationStatus(
                        isRunning = true,
                        currentApp = packageName,
                        current = index,
                        total = total
                    )

                    val result = remoteWatchAdbClient.compilePackage(packageName, mode.value)
                    result.fold(
                        onSuccess = { output ->
                            val trimmed = output.trim()
                            if (trimmed.isNotBlank() && !trimmed.equals("Success", ignoreCase = true)) {
                                addLog("$packageName: $trimmed")
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
                broadcastOptimizationStatus(isRunning = false, isComplete = true)

            }.onFailure { e ->
                addLog("Optimization failed: ${e.message}")
                _optimizationProgress.value = _optimizationProgress.value.copy(
                    isRunning = false,
                    result = OptimizationResult.None
                )
                _uiState.update { it.copy(lastError = "Optimization failed: ${e.message}") }
                broadcastOptimizationStatus(isRunning = false, error = e.message)
            }
        }
    }

    /**
     * Cancels the currently running optimization.
     */
    fun cancelOptimization() {
        cancelRequested.set(true)
        addLog("Cancellation requested...")
    }

    /**
     * Clears the current message/error state.
     */
    fun clearMessages() {
        _uiState.update { it.copy(lastError = null, lastMessage = null) }
    }

    private suspend fun broadcastReadiness() {
        val adbConnected = remoteWatchAdbClient.isConnected()
        wearableDataClient.sendPhoneReadiness(
            PhoneReadinessStatus(
                isPhoneConnected = true,
                isAdbConnectedToWatch = adbConnected,
                isShizukuAvailable = false
            )
        )
    }

    private suspend fun broadcastOptimizationStatus(
        isRunning: Boolean,
        isComplete: Boolean = false,
        isCancelled: Boolean = false,
        currentApp: String? = null,
        current: Int = 0,
        total: Int = 0,
        error: String? = null
    ) {
        wearableDataClient.sendOptimizationStatus(
            WatchOptimizationStatus(
                isRunning = isRunning,
                isComplete = isComplete,
                currentApp = currentApp,
                progressCurrent = current,
                progressTotal = total,
                optimizationType = _uiState.value.selectedOptimizationType.value,
                errorMessage = error
            )
        )
    }

    private fun addLog(message: String) {
        _commandOutput.value = _commandOutput.value + message
    }
}

/**
 * UI state for the Watch screen.
 *
 * @property pairingIp Watch IP address for connection.
 * @property pairingPort Pairing port from wireless debugging.
 * @property pairingCode 6-digit pairing code.
 * @property connectionPort Connection port for ADB.
 * @property isBusy Whether a connection operation is in progress.
 * @property lastError Last error message, if any.
 * @property lastMessage Last success message, if any.
 * @property adbConnectionState Current ADB connection state.
 * @property isWatchConnected Whether watch is connected via Wear OS.
 * @property selectedOptimizationType Selected optimization mode.
 * @property optimizationProgress Current optimization progress.
 * @property commandLogs Log output from optimization commands.
 * @property showKeyImportDialog Whether to show the key import dialog.
 * @property privateKeyInput Private key input for import.
 * @property publicKeyInput Public key input for import.
 */
data class WatchUiState(
    val pairingIp: String = "192.168.",
    val pairingPort: String = "",
    val pairingCode: String = "",
    val connectionPort: String = "",
    val isBusy: Boolean = false,
    val lastError: String? = null,
    val lastMessage: String? = null,
    val adbConnectionState: RemoteWatchAdbClient.ConnectionState = RemoteWatchAdbClient.ConnectionState.Disconnected,
    val isWatchConnected: Boolean = false,
    val selectedOptimizationType: AppOptimizationType = AppOptimizationType.SPEED_PROFILE,
    val optimizationProgress: OptimizationProgress = OptimizationProgress(),
    val commandLogs: List<String> = emptyList(),
    val showKeyImportDialog: Boolean = false,
    val privateKeyInput: String = "",
    val publicKeyInput: String = ""
)
