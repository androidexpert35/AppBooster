package com.tony.appbooster.wear.data.client

import android.content.Context
import android.util.Log
import com.tony.appbooster.wear.data.client.pairing.AdbPairingClient
import com.tony.appbooster.wear.domain.model.AdbConnectionState
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
 * ADB client that connects to the local device's wireless debugging daemon.
 *
 * This enables self-optimization on Wear OS by connecting to localhost
 * where the ADB daemon (adbd) is listening when wireless debugging is enabled.
 *
 * The connection flow:
 * 1. User enables Wireless Debugging in Developer Options
 * 2. User pairs the app with the daemon (one-time)
 * 3. App connects to localhost:port
 * 4. App can now execute shell commands with ADB privileges
 *
 * @property context Application context for accessing private storage.
 * @property ioDispatcher Dispatcher for IO operations.
 */
@Singleton
class WearAdbClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val _connectionState = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val connectionState: StateFlow<AdbConnectionState> = _connectionState.asStateFlow()

    private var dadb: Dadb? = null
    private val connectionMutex = Mutex()

    private val privateKeyFile: File
        get() = File(context.filesDir, "adb_key")

    private val publicKeyFile: File
        get() = File(context.filesDir, "adb_key.pub")

    /**
     * Pairs with the local ADB daemon using the provided pairing code.
     *
     * This uses Android 11+ wireless debugging pairing protocol (SPAKE2+).
     * The pairing is a one-time operation; after successful pairing,
     * the RSA key is trusted and future connections don't need pairing.
     *
     * @param port The pairing port shown in Wireless Debugging settings.
     * @param pairingCode The 6-digit pairing code.
     * @return Result indicating success or failure with details.
     */
    suspend fun pair(port: Int, pairingCode: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Starting SPAKE2+ pairing on port $port")

            // Generate and store the key pair if not exists
            val keyPair = getOrCreateKeyPair()

            // Perform real SPAKE2+ pairing
            val pairingClient = AdbPairingClient(ioDispatcher)
            val result = pairingClient.pair(
                host = LOCALHOST,
                port = port,
                pairingCode = pairingCode,
                keyPair = keyPair
            )

            when (result) {
                is AdbPairingClient.PairingResult.Success -> {
                    Log.d(TAG, "Pairing successful with device: ${result.deviceName}")
                    storePairingSuccess()
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
            Log.e(TAG, "Pairing failed", e)
            Result.failure(e)
        }
    }

    /**
     * Connects to the local ADB daemon on the specified port.
     *
     * @param port The connection port (shown in Wireless Debugging, different from pairing port).
     * @return Result indicating success or failure.
     */
    suspend fun connect(port: Int): Result<Unit> = withContext(ioDispatcher) {
        connectionMutex.withLock {
            try {
                _connectionState.value = AdbConnectionState.Connecting
                Log.d(TAG, "Connecting to ADB on localhost:$port")

                // Close existing connection if any
                dadb?.close()

                // Get the stored key pair
                val keyPair = getOrCreateKeyPair()

                // Create connection to localhost
                val connection = Dadb.create(
                    host = LOCALHOST,
                    port = port,
                    keyPair = keyPair
                )

                // Verify connection with a simple command
                val result = connection.shell("echo connected")
                val output = result.allOutput.trim()

                if (output == "connected") {
                    dadb = connection
                    _connectionState.value = AdbConnectionState.Connected
                    Log.d(TAG, "ADB connection established")
                    Result.success(Unit)
                } else {
                    connection.close()
                    throw IllegalStateException("Connection verification failed: $output")
                }
            } catch (e: Exception) {
                _connectionState.value = AdbConnectionState.Error(e.message ?: "Connection failed")
                Log.e(TAG, "ADB connection failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Disconnects from the ADB daemon.
     */
    suspend fun disconnect() = withContext(ioDispatcher) {
        connectionMutex.withLock {
            dadb?.close()
            dadb = null
            _connectionState.value = AdbConnectionState.Disconnected
            Log.d(TAG, "ADB disconnected")
        }
    }

    /**
     * Checks if currently connected to ADB.
     */
    fun isConnected(): Boolean = dadb != null && _connectionState.value == AdbConnectionState.Connected

    /**
     * Executes a shell command and returns the output.
     *
     * @param command The shell command to execute.
     * @return Result containing the command output or an error.
     */
    suspend fun execute(command: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val connection = dadb ?: throw IllegalStateException("Not connected to ADB")
            val result = connection.shell(command)
            result.allOutput
        }
    }

    /**
     * Checks if we have previously completed pairing.
     */
    fun hasPaired(): Boolean {
        val prefsFile = File(context.filesDir, PREFS_FILE)
        return prefsFile.exists() && privateKeyFile.exists()
    }

    /**
     * Gets or creates the RSA key pair used for ADB authentication.
     */
    private fun getOrCreateKeyPair(): AdbKeyPair {
        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            privateKeyFile.parentFile?.mkdirs()
            // generate() writes to files and returns Unit
            AdbKeyPair.generate(privateKeyFile, publicKeyFile)
        }
        // Read the key pair from files
        return AdbKeyPair.read(privateKeyFile, publicKeyFile)
    }

    /**
     * Stores a flag indicating successful pairing.
     */
    private fun storePairingSuccess() {
        val prefsFile = File(context.filesDir, PREFS_FILE)
        prefsFile.writeText("paired=true")
    }

    companion object {
        private const val TAG = "WearAdbClient"
        private const val LOCALHOST = "localhost"
        private const val PREFS_FILE = "adb_prefs"
    }
}
