package com.tony.appbooster.wear.domain.client

/**
 * Abstraction for shell command execution on Wear OS.
 *
 * Provides a unified interface for executing privileged shell commands
 * regardless of the underlying mechanism (Root, ADB, etc.).
 *
 * Implementations:
 * - [com.tony.appbooster.wear.data.client.RootShellClient] - Uses su for rooted devices
 * - [com.tony.appbooster.wear.data.client.WearAdbClient] - Uses self-ADB connection
 */
interface WearShellClient {

    /**
     * Checks if this shell client is available and ready to execute commands.
     *
     * @return True if the client can execute commands.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Executes a shell command and returns the output.
     *
     * @param command The shell command to execute.
     * @return Result containing command output on success, or exception on failure.
     */
    suspend fun execute(command: String): Result<String>

    /**
     * Returns a human-readable name for this shell method.
     *
     * Used for displaying connection status to the user.
     */
    val methodName: String
}
