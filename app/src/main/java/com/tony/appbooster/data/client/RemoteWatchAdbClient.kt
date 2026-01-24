package com.tony.appbooster.data.client

import android.content.Context
import android.util.Log
import com.tony.appbooster.data.client.pairing.AdbPairingClient
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
     * Checks if ADB keys exist.
     */
    fun hasAdbKeys(): Boolean = privateKeyFile.exists() && publicKeyFile.exists()

    /**
     * Imports ADB keys from external source (e.g., PC's ~/.android/adbkey files).
     *
     * This allows reusing a PC's trusted ADB keys so the phone can connect
     * to devices that already trust the PC.
     *
     * @param privateKeyPem The private key in PEM format (content of adbkey file).
     * @param publicKeyBase64 The public key in base64 format (content of adbkey.pub file).
     * @return Result indicating success or failure.
     */
    suspend fun importAdbKeys(privateKeyPem: String, publicKeyBase64: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Importing ADB keys...")

            // Validate the keys look reasonable
            if (!privateKeyPem.contains("PRIVATE KEY")) {
                return@withContext Result.failure(IllegalArgumentException(
                    "Invalid private key format. Expected PEM format with 'PRIVATE KEY' header."
                ))
            }

            if (publicKeyBase64.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException(
                    "Public key cannot be empty."
                ))
            }

            // Write the keys to files
            privateKeyFile.writeText(privateKeyPem.trim())
            publicKeyFile.writeText(publicKeyBase64.trim())

            Log.d(TAG, "ADB keys imported successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import ADB keys", e)
            Result.failure(e)
        }
    }

    /**
     * Clears stored ADB keys, forcing regeneration on next use.
     */
    suspend fun clearAdbKeys(): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (privateKeyFile.exists()) privateKeyFile.delete()
            if (publicKeyFile.exists()) publicKeyFile.delete()
            Log.d(TAG, "ADB keys cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear ADB keys", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the current public key for display/sharing.
     *
     * @return The public key content or null if not generated yet.
     */
    fun getPublicKey(): String? {
        return if (publicKeyFile.exists()) {
            publicKeyFile.readText()
        } else {
            null
        }
    }

    /**
     * Stores the watch's IP and port for reconnection.
     */
    private var lastWatchIp: String? = null
    private var lastWatchPort: Int? = null

    /**
     * Pairs with the watch using the pairing code.
     *
     * Uses SPAKE2+ protocol for Android 11+ wireless debugging pairing.
     *
     * @param watchIp The watch's IP address.
     * @param port The pairing port (from Wireless Debugging > Pair with code).
     * @param pairingCode The 6-digit pairing code.
     * @return Result indicating success or failure.
     */
    suspend fun pair(watchIp: String, port: Int, pairingCode: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Starting SPAKE2+ pairing with watch at $watchIp:$port")

            // Get or create the key pair
            val keyPair = getOrCreateKeyPair()

            // Perform real SPAKE2+ pairing
            val pairingClient = AdbPairingClient(ioDispatcher)
            val result = pairingClient.pair(
                host = watchIp,
                port = port,
                pairingCode = pairingCode,
                keyPair = keyPair
            )

            when (result) {
                is AdbPairingClient.PairingResult.Success -> {
                    Log.d(TAG, "Pairing successful with device: ${result.deviceName}")
                    Result.success(Unit)
                }
                is AdbPairingClient.PairingResult.Failure -> {
                    Log.e(TAG, "Pairing failed: ${result.error}", result.exception)
                    Result.failure(
                        result.exception ?: Exception(result.error)
                    )
                }
            }
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
                val errorMsg = e.message ?: "Connection failed"
                Log.e(TAG, "Failed to connect to watch ADB", e)

                // Check for authorization/key rejection errors
                val userMessage = when {
                    errorMsg.contains("AUTH", ignoreCase = true) ||
                    errorMsg.contains("unauthorized", ignoreCase = true) ||
                    errorMsg.contains("key", ignoreCase = true) ||
                    errorMsg.contains("[10000000", ignoreCase = true) ->
                        "Connection rejected: Watch doesn't trust this phone.\n\n" +
                        "You need to pair THIS PHONE with the watch:\n" +
                        "1. On watch: Wireless Debugging → Pair new device\n" +
                        "2. Enter the pairing info above and tap 'Pair Device'\n\n" +
                        "(Pairing from a PC only authorizes that PC, not this phone)"

                    errorMsg.contains("refused", ignoreCase = true) ->
                        "Connection refused.\n\n" +
                        "Make sure:\n" +
                        "• Wireless Debugging is ON on the watch\n" +
                        "• You're using the CONNECTION port (not pairing port)\n" +
                        "• Both devices are on the same network"

                    errorMsg.contains("timeout", ignoreCase = true) ->
                        "Connection timed out.\n\n" +
                        "Check the IP address and port, and ensure the watch is reachable."

                    else -> "Connection failed: $errorMsg"
                }

                _connectionState.value = ConnectionState.Error(userMessage)
                Result.failure(Exception(userMessage))
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
