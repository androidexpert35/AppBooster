package com.tony.appbooster.data.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri
import com.tony.appbooster.BuildConfig
import com.tony.appbooster.IShellService
import com.tony.appbooster.domain.client.ShizukuShellClient
import com.tony.appbooster.domain.model.shizuku.ShellResult
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ShizukuShellClient] that uses Shizuku API for privileged shell access.
 *
 * Uses Shizuku's UserService mechanism to run a shell service with elevated privileges.
 * Commands executed through this service run with shell (ADB) UID, enabling system-level
 * operations like app optimization.
 */
@Singleton
class ShizukuShellClientImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : ShizukuShellClient {

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    override val state: StateFlow<ShizukuState> = _state.asStateFlow()

    @Volatile
    private var shellService: IShellService? = null
    private val serviceMutex = Mutex()

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "ShellService connected")
            shellService = IShellService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "ShellService disconnected")
            shellService = null
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        updateState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        shellService = null
        // Recompute state instead of forcing a value, so we don't mask "NotInstalled" etc.
        updateState()
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission result received: requestCode=$requestCode, granted=$granted")
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Recompute state using live checks (binder may have died while the dialog was open).
            updateState()

            // If permission was granted and we're truly ready, bind the user service.
            if (granted && _state.value == ShizukuState.Ready) {
                bindShellService()
            }
        }
    }

    init {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        updateState()
    }

    override suspend fun refreshState() {
        withContext(ioDispatcher) {
            updateState()
        }
    }

    private fun updateState() {
        val newState = computeStateSafely()
        if (_state.value != newState) {
            Log.d(TAG, "Shizuku state updated: ${_state.value} -> $newState")
        }
        _state.value = newState

        // Keep the user service binding consistent with state to avoid stale "Ready" behavior.
        if (newState == ShizukuState.Ready) {
            if (shellService == null) {
                bindShellService()
            }
        } else {
            shellService = null
            unbindShellService()
        }
    }

    /**
     * Computes the current Shizuku availability state.
     *
     * The ordering is important:
     * 1) Not installed
     * 2) Installed but server not running (binder not alive)
     * 3) Server running but permission missing
     * 4) Ready
     */
    private fun computeStateSafely(): ShizukuState {
        val installed = isShizukuInstalled()
        if (!installed) return ShizukuState.NotInstalled

        val binderAlive = isBinderAlive()
        if (!binderAlive) return ShizukuState.NotRunning

        // If the binder is alive, Shizuku is running. Now check permission.
        val granted = hasPermissionWhenBinderAlive()
        return if (granted) ShizukuState.Ready else ShizukuState.PermissionRequired
    }

    private fun isBinderAlive(): Boolean {
        return runCatching { Shizuku.pingBinder() }
            .onFailure { e -> Log.w(TAG, "Failed to ping Shizuku binder", e) }
            .getOrDefault(false)
    }

    /**
     * Checks whether this app currently has Shizuku permission.
     *
     * This method must only be called when the binder is alive; otherwise, Shizuku APIs can
     * throw or return misleading values.
     */
    private fun hasPermissionWhenBinderAlive(): Boolean {
        return runCatching {
            if (Shizuku.isPreV11()) {
                // AppBooster requires the v11+ runtime-permission workflow. For older Shizuku,
                // treat as permission missing so UI can guide the user to update.
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        }
            .onFailure { e -> Log.w(TAG, "Failed to check Shizuku permission", e) }
            .getOrDefault(false)
    }

    private fun bindShellService() {
        try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            Log.d(TAG, "Binding ShellService...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind ShellService", e)
        }
    }

    private fun unbindShellService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unbind ShellService", e)
        }
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override suspend fun requestPermission() {
        Log.d(TAG, "requestPermission() called")

        if (!isShizukuInstalled()) {
            _state.value = ShizukuState.NotInstalled
            return
        }

        // Check if Shizuku server is running (binder alive). If not, we can't request permission.
        val binderAlive = isBinderAlive()
        if (!binderAlive) {
            Log.w(TAG, "Shizuku binder not alive, cannot request permission")
            _state.value = ShizukuState.NotRunning
            return
        }

        // Check if already have permission.
        if (hasPermissionWhenBinderAlive()) {
            Log.d(TAG, "Already have Shizuku permission")
            _state.value = ShizukuState.Ready
            bindShellService()
            return
        }

        if (Shizuku.isPreV11()) {
            Log.w(TAG, "Shizuku version is pre-v11, not supported")
            _state.value = ShizukuState.Error("Shizuku version too old. Please update Shizuku.")
            return
        }

        Log.d(TAG, "Requesting Shizuku permission with request code: $PERMISSION_REQUEST_CODE")

        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            Log.d(TAG, "Permission request sent to Shizuku successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
            _state.value = ShizukuState.Error(e.message ?: "Failed to request permission")
        }
    }

    override fun isReady(): Boolean = _state.value == ShizukuState.Ready

    /**
     * Waits for the shell service to be bound and ready.
     *
     * @param timeoutMs Maximum time to wait in milliseconds.
     * @return true if service is connected, false on timeout.
     */
    private suspend fun waitForServiceConnection(timeoutMs: Long = 2000): Boolean {
        val startTime = System.currentTimeMillis()
        while (shellService == null && (System.currentTimeMillis() - startTime) < timeoutMs) {
            delay(50)
        }
        return shellService != null
    }

    override suspend fun execute(command: String): ShellResult = withContext(ioDispatcher) {
        if (!isReady()) {
            throw IllegalStateException("Shizuku is not ready. Current state: ${_state.value}")
        }

        // If service is null (e.g. fresh start or race condition), try to bind and wait.
        if (shellService == null) {
            bindShellService()
            val connected = waitForServiceConnection()
            if (!connected) {
                // Double-check permission status if binding fails
                updateState()
                if (!isReady()) {
                    throw IllegalStateException("Shizuku permission or state invalid during connection.")
                }
                return@withContext ShellResult(
                    exitCode = -1,
                    output = "",
                    error = "Shizuku service binding timed out. Please ensure Shizuku is running and permission is granted."
                )
            }
        }

        serviceMutex.withLock {
            // NOTE: Avoid duplicate logging. AdbShellClientImpl already logs the command.
            // Log.d(TAG, "Executing command: $command")

            val service = shellService
            if (service != null) {
                try {
                    val result = service.executeCommand(command)
                    ShellResult(
                        exitCode = result[0].toIntOrNull() ?: -1,
                        output = result[1],
                        error = result[2]
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Command execution via service failed", e)
                    // Fallback cleanup
                    shellService = null
                    bindShellService()
                    ShellResult(
                        exitCode = -1,
                        output = "",
                        error = "Service connection lost: ${e.message}"
                    )
                }
            } else {
                ShellResult(
                    exitCode = -1,
                    output = "",
                    error = "ShellService lost unexpectedly."
                )
            }
        }
    }

    override fun executeStreaming(command: String): Flow<Result<String>> = flow {
        if (!isReady()) {
            emit(Result.failure(IllegalStateException("Shizuku is not ready")))
            return@flow
        }

        // For streaming, execute once and emit lines.
        val result = runCatching { execute(command) }
            .getOrElse { throwable ->
                emit(Result.failure(throwable))
                return@flow
            }

        if (!result.isSuccess) {
            emit(Result.failure(IllegalStateException(result.error.ifBlank { "Command failed" })))
            return@flow
        }

        result.output
            .lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .forEach { line -> emit(Result.success(line)) }
    }

    override fun openShizukuApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            openShizukuInstallPage()
        }
    }

    override fun openShizukuInstallPage() {
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$SHIZUKU_PACKAGE".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            }
    }

    companion object {
        private const val TAG = "ShizukuShellClient"
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}

