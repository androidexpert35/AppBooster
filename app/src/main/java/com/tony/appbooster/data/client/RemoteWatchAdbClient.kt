package com.tony.appbooster.data.client

import android.content.Context
import android.util.Log
import com.tony.appbooster.di.IoDispatcher
import dadb.AdbKeyPair
import dadb.Dadb
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADB client for connecting to a remote Wear OS device over wireless debugging.
 *
 * Uses the dadb library to establish a connection to the watch's ADB daemon
 * and execute shell commands for app optimization.
 *
 * Connection flow:
 * 1. Watch enables Wireless Debugging and shares IP/port with phone via DataLayer
 * 2. User initiates pairing (one-time) - enters pairing code from watch
 * 3. Phone stores RSA keys for persistent authorization
 * 4. Phone connects to watch's ADB port and can execute commands
 *
 * Note: The dadb library does not support Android 11+ PAKE pairing protocol.
 * Users must pair using `adb pair` command first, or the watch must already
 * trust the phone's ADB key.
 *
 * @property context Application context for file storage.
 * @property ioDispatcher Dispatcher for IO operations.
 */
@Singleton
class RemoteWatchAdbClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Represents the connection state to the watch's ADB.
     */
    sealed interface ConnectionState {
        data object Disconnected : ConnectionState
        data object Connecting : ConnectionState
        data object Connected : ConnectionState
        data class Error(val message: String) : ConnectionState
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /**
     * Current connection state to the watch's ADB daemon.
     */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var adbSession: Dadb? = null
    private val connectionMutex = Mutex()

    private val privateKeyFile: File
        get() = File(context.filesDir, "watch_adb_key")

    private val publicKeyFile: File
        get() = File(context.filesDir, "watch_adb_key.pub")

    /**
     * Stores the watch's IP and port for reconnection.
     */
    private var lastWatchIp: String? = null
    private var lastWatchPort: Int? = null

    /**
     * Pairs with the watch using the pairing code.
     *
     * @param watchIp The watch's IP address.
     * @param port The pairing port (from Wireless Debugging > Pair with code).
     * @param pairingCode The 6-digit pairing code.
     * @return Result indicating success or failure.
     */
    suspend fun pair(watchIp: String, port: Int, pairingCode: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Pairing with watch at $watchIp:$port")

            // Get or create the key pair
            val keyPair = getOrCreateKeyPair()

            // Attempt pairing
            // Note: dadb library pairing API seems to be unresolved in this context.
            // Temporarily disabling automatic pairing.
            // Dadb.pair(watchIp, port, pairingCode, keyPair)
            throw UnsupportedOperationException("Automatic pairing not supported yet. Please pair via PC.")

            Log.d(TAG, "Pairing successful")
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = "Pairing failed: ${e.message}"
            Log.e(TAG, msg, e)
            Result.failure(e)
        }
    }

    /**
     * Connects to the watch's ADB daemon.
     *
     * @param watchIp The watch's IP address (from watch's wireless debugging settings).
     * @param port The connection port (NOT the pairing port).
     * @return Result indicating success or failure.
     */
    suspend fun connect(watchIp: String, port: Int): Result<Unit> = withContext(ioDispatcher) {
        connectionMutex.withLock {
            try {
                _connectionState.value = ConnectionState.Connecting
                Log.d(TAG, "Connecting to watch ADB at $watchIp:$port")

                // Close existing connection if any
                adbSession?.close()

                // Get or create the key pair for authentication
                val keyPair = getOrCreateKeyPair()

                // Create connection to watch
                val connection = Dadb.create(
                    host = watchIp,
                    port = port,
                    keyPair = keyPair
                )

                // Verify connection with a simple command
                val result = connection.shell("echo connected")
                val output = result.allOutput.trim()

                if (output == "connected") {
                    adbSession = connection
                    lastWatchIp = watchIp
                    lastWatchPort = port
                    _connectionState.value = ConnectionState.Connected
                    Log.d(TAG, "Connected to watch ADB successfully")
                    Result.success(Unit)
                } else {
                    connection.close()
                    val errorMsg = "Connection verification failed: $output"
                    _connectionState.value = ConnectionState.Error(errorMsg)
                    Result.failure(IllegalStateException(errorMsg))
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                Log.e(TAG, "Failed to connect to watch ADB", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Disconnects from the watch's ADB daemon.
     */
    suspend fun disconnect() = withContext(ioDispatcher) {
        connectionMutex.withLock {
            adbSession?.close()
            adbSession = null
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Disconnected from watch ADB")
        }
    }

    /**
     * Executes a shell command on the watch.
     *
     * @param command The shell command to execute.
     * @return Result containing command output or failure.
     */
    suspend fun executeCommand(command: String): Result<String> = withContext(ioDispatcher) {
        connectionMutex.withLock {
            val connection = adbSession
            if (connection == null) {
                return@withContext Result.failure(IllegalStateException("Not connected to watch ADB"))
            }

            try {
                val result = connection.shell(command)
                val output = result.allOutput
                Log.d(TAG, "Command executed: $command -> ${output.take(100)}")
                Result.success(output)
            } catch (e: Exception) {
                Log.e(TAG, "Command failed: $command", e)
                // Connection might be broken - update state
                _connectionState.value = ConnectionState.Error("Command failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Checks if currently connected to the watch.
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.Connected

    /**
     * Attempts to reconnect using last known IP and port.
     *
     * @return Result indicating success or failure.
     */
    suspend fun reconnect(): Result<Unit> {
        val ip = lastWatchIp
        val port = lastWatchPort
        if (ip != null && port != null) {
            return connect(ip, port)
        }
        return Result.failure(IllegalStateException("No previous connection info available"))
    }

    /**
     * Gets the list of installed packages on the watch.
     *
     * @return Result containing list of package names.
     */
    suspend fun getInstalledPackages(): Result<List<String>> = withContext(ioDispatcher) {
        executeCommand("pm list packages").map { output ->
            output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotBlank() }
        }
    }

    /**
     * Compiles a package on the watch using speed-profile mode.
     *
     * @param packageName The package to compile.
     * @return Result indicating success or failure.
     */
    suspend fun compilePackage(packageName: String, mode: String = "speed-profile"): Result<String> {
        val command = "cmd package compile -m $mode -f $packageName"
        return executeCommand(command)
    }

    /**
     * Gets or creates the RSA key pair for ADB authentication.
     */
    private fun getOrCreateKeyPair(): AdbKeyPair {
        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            Log.d(TAG, "Generating new ADB key pair for watch connection")
            AdbKeyPair.generate(privateKeyFile, publicKeyFile)
        }
        return AdbKeyPair.read(privateKeyFile, publicKeyFile)
    }

    companion object {
        private const val TAG = "RemoteWatchAdbClient"
    }
}
