package com.tony.appbooster.wear.data.client

import com.tony.appbooster.wear.domain.client.WearShellClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the best available shell access method for Wear OS.
 *
 * Automatically detects and prioritizes shell access methods:
 * 1. Root (su) - Best option, no setup required
 * 2. Self-ADB - Requires one-time PC pairing
 *
 * Usage:
 * ```kotlin
 * val provider = shellProvider.getActiveClient()
 * provider?.execute("pm list packages")
 * ```
 *
 * @property rootShellClient Client for root shell access.
 * @property wearAdbClient Client for ADB-based shell access.
 */
@Singleton
class WearShellProvider @Inject constructor(
    private val rootShellClient: RootShellClient,
    private val wearAdbClient: WearAdbClient
) {
    /**
     * Represents the current shell access status.
     */
    sealed interface ShellStatus {
        /** No shell access available - setup required. */
        data object Unavailable : ShellStatus

        /** Checking for available shell methods. */
        data object Checking : ShellStatus

        /** Shell access available via the specified method. */
        data class Available(
            val method: ShellMethod,
            val client: WearShellClient
        ) : ShellStatus
    }

    /**
     * Available shell access methods.
     */
    enum class ShellMethod {
        /** Root access via su binary. */
        ROOT,

        /** Self-ADB connection to localhost. */
        ADB
    }

    private val _status = MutableStateFlow<ShellStatus>(ShellStatus.Unavailable)

    /**
     * Current shell access status.
     */
    val status: StateFlow<ShellStatus> = _status.asStateFlow()

    /**
     * Detects and returns the best available shell access method.
     *
     * Priority:
     * 1. Root - Instant, no setup
     * 2. ADB - Requires prior pairing
     *
     * @return The active shell client, or null if none available.
     */
    suspend fun detectAvailableMethod(): ShellStatus {
        _status.value = ShellStatus.Checking

        // Priority 1: Check for root
        if (rootShellClient.isRootAvailable()) {
            val status = ShellStatus.Available(
                method = ShellMethod.ROOT,
                client = RootShellClientAdapter(rootShellClient)
            )
            _status.value = status
            return status
        }

        // Priority 2: Check for ADB connection
        if (wearAdbClient.isConnected()) {
            val status = ShellStatus.Available(
                method = ShellMethod.ADB,
                client = AdbShellClientAdapter(wearAdbClient)
            )
            _status.value = status
            return status
        }

        _status.value = ShellStatus.Unavailable
        return ShellStatus.Unavailable
    }

    /**
     * Gets the currently active shell client, if any.
     */
    fun getActiveClient(): WearShellClient? {
        return when (val current = _status.value) {
            is ShellStatus.Available -> current.client
            else -> null
        }
    }

    /**
     * Checks if any shell method is currently available.
     */
    fun isShellAvailable(): Boolean = _status.value is ShellStatus.Available

    /**
     * Adapter to make RootShellClient conform to WearShellClient interface.
     */
    private class RootShellClientAdapter(
        private val rootClient: RootShellClient
    ) : WearShellClient {
        override val methodName: String = "Root (su)"

        override suspend fun isAvailable(): Boolean = rootClient.isRootAvailable()

        override suspend fun execute(command: String): Result<String> =
            rootClient.execute(command)
    }

    /**
     * Adapter to make WearAdbClient conform to WearShellClient interface.
     */
    private class AdbShellClientAdapter(
        private val adbClient: WearAdbClient
    ) : WearShellClient {
        override val methodName: String = "Wireless ADB"

        override suspend fun isAvailable(): Boolean = adbClient.isConnected()

        override suspend fun execute(command: String): Result<String> =
            adbClient.execute(command)
    }

    companion object {
        private const val TAG = "WearShellProvider"
    }
}
